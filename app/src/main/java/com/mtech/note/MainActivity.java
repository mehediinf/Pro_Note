package com.mtech.note;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.mtech.note.offline.AppDatabase;
import com.mtech.note.offline.LocalNoteEntity;
import com.mtech.note.sync.SyncScheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    FloatingActionButton addNoteBtn;
    RecyclerView recyclerView;
    MaterialToolbar toolbar;
    TextView emptyStateText;

    MixedNoteAdapter mixedAdapter;
    ActionMode actionMode;
    private boolean suppressSelectionCallback = false;

    private final List<NoteListItem> onlineNotes  = new ArrayList<>();
    private final List<NoteListItem> offlineNotes = new ArrayList<>();
    private ListenerRegistration firestoreListener;
    private final Executor bgExecutor = Executors.newSingleThreadExecutor();

    private static final int SORT_NEWEST   = 0;
    private static final int SORT_OLDEST   = 1;
    private static final int SORT_TITLE_AZ = 2;
    private int    sortMode          = SORT_NEWEST;
    private String activeSearchQuery = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        addNoteBtn     = findViewById(R.id.add_note_btn);
        recyclerView   = findViewById(R.id.recycler_view);
        toolbar        = findViewById(R.id.toolbar);
        emptyStateText = findViewById(R.id.empty_state_text);

        addNoteBtn.setOnClickListener(v ->
                startActivity(new Intent(this, NoteDetailsActivity.class)));

        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> toolbar.showOverflowMenu());

        setupAdapter();
    }

    // Adapter

    private void setupAdapter() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setItemAnimator(null);

        mixedAdapter = new MixedNoteAdapter(this, new MixedNoteAdapter.Listener() {

            @Override
            public void onOpenOnlineNote(Note note, String docId) {
                Intent i = new Intent(MainActivity.this, NoteDetailsActivity.class);
                i.putExtra("title",   note.title);
                i.putExtra("content", note.content);
                i.putExtra("docId",   docId);
                startActivity(i);
            }

            @Override
            public void onOpenOfflineNote(String localId, String title, String content) {
                // Open as editable note — no docId, pass localId so it can update the right row
                Intent i = new Intent(MainActivity.this, NoteDetailsActivity.class);
                i.putExtra("title",   title);
                i.putExtra("content", content);
                i.putExtra("localId", localId);
                startActivity(i);
            }

            @Override
            public void onSelectionStartRequested() {
                if (actionMode == null)
                    actionMode = startSupportActionMode(selectionCallback);
            }

            @Override
            public void onSelectionCountChanged(int count) {
                if (suppressSelectionCallback) return;
                if (count <= 0) { if (actionMode != null) actionMode.finish(); return; }
                if (actionMode == null) actionMode = startSupportActionMode(selectionCallback);
                if (actionMode != null) actionMode.setTitle(count + " selected");
            }
        });

        recyclerView.setAdapter(mixedAdapter);
    }

    // Data loading

    private void startFirestoreListener() {
        firestoreListener = buildQuery().addSnapshotListener((snapshots, error) -> {
            if (error != null || snapshots == null) return;
            onlineNotes.clear();
            for (QueryDocumentSnapshot doc : snapshots)
                onlineNotes.add(NoteListItem.online(doc.getId(), doc.toObject(Note.class)));
            mergeAndSubmit();
        });
    }

    private void loadOfflineNotes() {
        bgExecutor.execute(() -> {
            try {
                List<LocalNoteEntity> pending =
                        AppDatabase.getInstance(getApplicationContext())
                                .localNoteDao().getPendingSync();
                List<NoteListItem> items = new ArrayList<>();
                if (pending != null) {
                    for (LocalNoteEntity e : pending) {
                        items.add(NoteListItem.offline(
                                e.localId, e.title, e.content, e.timestampMs));
                    }
                }
                offlineNotes.clear();
                offlineNotes.addAll(items);
            } catch (Exception ignored) {}
            runOnUiThread(this::mergeAndSubmit);
        });
    }

    private void mergeAndSubmit() {
        List<NoteListItem> merged = new ArrayList<>();

        // Offline-only notes at the TOP with pending badge
        merged.addAll(offlineNotes);

        // Online notes after
        merged.addAll(onlineNotes);

        // Search filter
        if (activeSearchQuery != null && !activeSearchQuery.isEmpty()) {
            String q = activeSearchQuery.toLowerCase();
            List<NoteListItem> filtered = new ArrayList<>();
            for (NoteListItem it : merged)
                if (it.getDisplayTitle().toLowerCase().contains(q)) filtered.add(it);
            merged = filtered;
        }

        mixedAdapter.submitList(new ArrayList<>(merged));
        updateEmptyState(merged);

        // Badge
        int pendingCount = offlineNotes.size();
        View badge = findViewById(R.id.offline_badge);
        if (badge != null) {
            if (pendingCount > 0) {
                badge.setVisibility(View.VISIBLE);
                TextView t = badge.findViewById(R.id.offline_badge_text);
                if (t != null) t.setText(pendingCount + " note(s) not yet synced");
            } else {
                badge.setVisibility(View.GONE);
            }
        }
    }

    private void updateEmptyState(List<NoteListItem> merged) {
        boolean empty = merged == null || merged.isEmpty();
        emptyStateText.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    // Lifecycle
    @Override
    protected void onStart() {
        super.onStart();
        startFirestoreListener();
        loadOfflineNotes();
        SyncScheduler.enqueue(getApplicationContext());
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (firestoreListener != null) { firestoreListener.remove(); firestoreListener = null; }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadOfflineNotes();
    }

    // Options menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setQueryHint("Search title…");
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String q) {
                activeSearchQuery = q == null ? null : q.trim();
                mergeAndSubmit();
                searchView.clearFocus();
                return true;
            }
            @Override public boolean onQueryTextChange(String s) { return false; }
        });
        searchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override public boolean onMenuItemActionExpand(MenuItem item)   { return true; }
            @Override public boolean onMenuItemActionCollapse(MenuItem item) {
                activeSearchQuery = null; mergeAndSubmit(); return true;
            }
        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_logout) {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return true;
        } else if (id == R.id.action_sort_newest)  { sortMode = SORT_NEWEST;   restartListener(); return true; }
        else if  (id == R.id.action_sort_oldest)   { sortMode = SORT_OLDEST;   restartListener(); return true; }
        else if  (id == R.id.action_sort_title)    { sortMode = SORT_TITLE_AZ; restartListener(); return true; }
        return super.onOptionsItemSelected(item);
    }

    private void restartListener() {
        if (firestoreListener != null) { firestoreListener.remove(); firestoreListener = null; }
        onlineNotes.clear();
        startFirestoreListener();
    }

    private Query buildQuery() {
        if (sortMode == SORT_OLDEST)
            return Utility.getCollectionReferenceForNotes().orderBy("timestamp", Query.Direction.ASCENDING);
        if (sortMode == SORT_TITLE_AZ)
            return Utility.getCollectionReferenceForNotes().orderBy("title", Query.Direction.ASCENDING);
        return Utility.getCollectionReferenceForNotes().orderBy("timestamp", Query.Direction.DESCENDING);
    }

    // Selection / Delete
    private final ActionMode.Callback selectionCallback = new ActionMode.Callback() {
        @Override public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.menu_selection, menu); return true;
        }
        @Override public boolean onPrepareActionMode(ActionMode mode, Menu menu) { return false; }
        @Override public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            if (item.getItemId() == R.id.action_delete)     { deleteSelected(); return true; }
            if (item.getItemId() == R.id.action_select_all) { mixedAdapter.selectAll(); return true; }
            return false;
        }
        @Override public void onDestroyActionMode(ActionMode mode) {
            actionMode = null;
            suppressSelectionCallback = true;
            mixedAdapter.clearSelectionAndExit();
            suppressSelectionCallback = false;
        }
    };

    private void deleteSelected() {
        List<NoteListItem> onlineSel  = mixedAdapter.getSelectedOnlineItems();
        List<NoteListItem> offlineSel = mixedAdapter.getSelectedOfflineItems();

        // Delete offline notes from Room
        if (!offlineSel.isEmpty()) {
            bgExecutor.execute(() -> {
                for (NoteListItem it : offlineSel) {
                    try {
                        AppDatabase.getInstance(getApplicationContext())
                                .localNoteDao().deleteByLocalId(it.localId);
                    } catch (Exception ignored) {}
                }
                runOnUiThread(() -> loadOfflineNotes());
            });
        }

        // Delete online notes from Firestore
        if (!onlineSel.isEmpty()) {
            List<RestoreEntry> deleted = new ArrayList<>();
            WriteBatch batch = FirebaseFirestore.getInstance().batch();
            for (NoteListItem it : onlineSel) {
                DocumentReference ref = Utility.getCollectionReferenceForNotes().document(it.docId);
                deleted.add(new RestoreEntry(ref, it.note));
                batch.delete(ref);
            }
            int total = onlineSel.size() + offlineSel.size();
            batch.commit()
                    .addOnSuccessListener(u -> {
                        if (actionMode != null) actionMode.finish();
                        Snackbar.make(findViewById(R.id.main),
                                        total + " note(s) deleted", Snackbar.LENGTH_LONG)
                                .setAction("Undo", v -> restoreOnline(deleted))
                                .show();
                    })
                    .addOnFailureListener(e ->
                            Utility.showToast(this, "Delete failed: " + e.getMessage()));
        } else {
            if (actionMode != null) actionMode.finish();
        }
    }

    private void restoreOnline(List<RestoreEntry> deleted) {
        WriteBatch batch = FirebaseFirestore.getInstance().batch();
        for (RestoreEntry e : deleted) batch.set(e.ref, e.note);
        batch.commit().addOnFailureListener(e ->
                Utility.showToast(this, "Undo failed: " + e.getMessage()));
    }

    private record RestoreEntry(DocumentReference ref, Note note) {
    }
}