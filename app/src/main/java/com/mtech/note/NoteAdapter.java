package com.mtech.note;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.mtech.note.offline.AppDatabase;
import com.mtech.note.offline.LocalNoteEntity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class NoteAdapter extends FirestoreRecyclerAdapter<Note, NoteAdapter.NoteViewHolder> {

    private final Context context;
    private final Listener listener;
    private final Set<String> selectedIds  = new HashSet<>();
    private final Set<String> pendingDocIds = new HashSet<>();
    private boolean selectionMode = false;
    private final Executor bgExecutor = Executors.newSingleThreadExecutor();

    public interface Listener {
        void onOpenNote(Note note, String docId);
        void onSelectionStartRequested();
        void onSelectionCountChanged(int count);
    }

    public NoteAdapter(@NonNull FirestoreRecyclerOptions<Note> options,
                       Context context, Listener listener) {
        super(options);
        this.context  = context;
        this.listener = listener;
        loadPendingDocIds();
    }

    private void loadPendingDocIds() {
        bgExecutor.execute(() -> {
            try {
                List<LocalNoteEntity> pending =
                        AppDatabase.getInstance(context).localNoteDao().getPendingSync();
                pendingDocIds.clear();
                if (pending != null) {
                    for (LocalNoteEntity e : pending) {
                        if (e.firestoreDocId != null) pendingDocIds.add(e.firestoreDocId);
                    }
                }
                notifyDataSetChanged();
            } catch (Exception ignored) {}
        });
    }

    public void refreshPendingState() {
        loadPendingDocIds();
    }

    @Override
    protected void onBindViewHolder(@NonNull NoteViewHolder h, int position, @NonNull Note note) {
        String docId = getSnapshots().getSnapshot(position).getId();

        h.titleTextView.setText(note.title == null ? "" : note.title);
        h.contentTextView.setText(note.content == null ? "" : note.content);
        if (note.timestamp != null) {
            h.timestampTextView.setText(Utility.timestampToString(note.timestamp));
            h.timestampTextView.setVisibility(View.VISIBLE);
        } else {
            h.timestampTextView.setVisibility(View.GONE);
        }

        // Pending sync indicator
        boolean isPending = pendingDocIds.contains(docId);
        h.pendingSyncRow.setVisibility(isPending ? View.VISIBLE : View.GONE);

        // Selection state
        h.cardView.setChecked(selectedIds.contains(docId));

        h.itemView.setOnClickListener(v -> {
            if (selectionMode) {
                toggleSelection(docId);
                int p = h.getBindingAdapterPosition();
                if (p != RecyclerView.NO_POSITION) notifyItemChanged(p);
                return;
            }
            if (listener != null) listener.onOpenNote(note, docId);
        });

        h.itemView.setOnLongClickListener(v -> {
            if (!selectionMode) {
                selectionMode = true;
                if (listener != null) listener.onSelectionStartRequested();
            }
            toggleSelection(docId);
            int p = h.getBindingAdapterPosition();
            if (p != RecyclerView.NO_POSITION) notifyItemChanged(p);
            return true;
        });
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recycler_note_item, parent, false);
        return new NoteViewHolder(view);
    }

    static class NoteViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView, contentTextView, timestampTextView;
        MaterialCardView cardView;
        View pendingSyncRow;

        NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView     = itemView.findViewById(R.id.note_title_text_view);
            contentTextView   = itemView.findViewById(R.id.note_content_text_view);
            timestampTextView = itemView.findViewById(R.id.note_timestamp_text_view);
            cardView          = itemView.findViewById(R.id.note_card);
            pendingSyncRow    = itemView.findViewById(R.id.pending_sync_row);
        }
    }

    public boolean isSelectionMode() { return selectionMode; }

    public void clearSelectionAndExit() {
        selectedIds.clear();
        selectionMode = false;
        notifyDataSetChanged();
        if (listener != null) listener.onSelectionCountChanged(0);
    }

    public int getSelectedCount() { return selectedIds.size(); }

    public void selectAll() {
        selectedIds.clear();
        for (int i = 0; i < getSnapshots().size(); i++)
            selectedIds.add(getSnapshots().getSnapshot(i).getId());
        selectionMode = true;
        notifyDataSetChanged();
        if (listener != null) listener.onSelectionCountChanged(selectedIds.size());
    }

    public List<DocumentSnapshot> getSelectedSnapshots() {
        List<DocumentSnapshot> out = new ArrayList<>();
        for (int i = 0; i < getSnapshots().size(); i++) {
            DocumentSnapshot snap = getSnapshots().getSnapshot(i);
            if (selectedIds.contains(snap.getId())) out.add(snap);
        }
        return out;
    }

    private void toggleSelection(String docId) {
        if (selectedIds.contains(docId)) selectedIds.remove(docId);
        else selectedIds.add(docId);
        if (listener != null) listener.onSelectionCountChanged(selectedIds.size());
        if (selectedIds.isEmpty()) selectionMode = false;
    }
}