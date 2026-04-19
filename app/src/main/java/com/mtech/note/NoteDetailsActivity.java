package com.mtech.note;

import android.os.Bundle;
import android.net.Uri;
import android.content.Intent;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.mtech.note.offline.AppDatabase;
import com.mtech.note.offline.LocalNoteDao;
import com.mtech.note.offline.LocalNoteEntity;
import com.mtech.note.sync.SyncScheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class NoteDetailsActivity extends AppCompatActivity {
    private static final String TAG = "NoteDetails";

    EditText titleEditText, contentEditText;
    TextView contentReadOnlyText;
    ImageButton saveNoteBtn, editNoteBtn;

    LinearLayout ll_AddImage, ll_AddUrl, ll_AddPage;
    TextView pageTitleTextView;
    String title, content, docId;
    boolean isEditMode = false;
    TextView deleteNoteTextViewBtn;
    View attachmentActions;
    TextView attachmentsSummary;
    RecyclerView attachmentsRecycler;
    AttachmentAdapter attachmentAdapter;

    private String localId;
    private List<NoteAttachment> attachments = new ArrayList<>();

    private ActivityResultLauncher<String[]> openImageLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_details);

        titleEditText = findViewById(R.id.notes_title_text);
        contentEditText = findViewById(R.id.notes_content_text);
        contentReadOnlyText = findViewById(R.id.notes_content_readonly);
        saveNoteBtn = findViewById(R.id.save_note_btn);
        editNoteBtn = findViewById(R.id.edit_note_btn);
        pageTitleTextView = findViewById(R.id.page_title);
        deleteNoteTextViewBtn = findViewById(R.id.delete_note_text_view_btn);
        attachmentActions = findViewById(R.id.attachment_actions);
        attachmentsSummary = findViewById(R.id.attachments_summary);
        attachmentsRecycler = findViewById(R.id.attachments_recycler);
        ll_AddImage = findViewById(R.id.ll_AddImage);
        ll_AddUrl = findViewById(R.id.ll_AddUrl);
        ll_AddPage = findViewById(R.id.ll_AddPage);

        ImageButton backBtn = findViewById(R.id.back_btn);
        backBtn.setOnClickListener(v -> finish());

        attachmentAdapter = new AttachmentAdapter(this, new AttachmentAdapter.Listener() {
            @Override
            public void onAttachmentClick(NoteAttachment attachment, int position) {
                if (attachment == null) return;
                if (isEditMode) {
                    editAttachment(position);
                    return;
                }
                openAttachment(attachment);
            }

            @Override
            public void onAttachmentDelete(NoteAttachment attachment, int position) {
                if (!isEditMode) return;
                deleteAttachment(position);
            }
        });
        attachmentsRecycler.setLayoutManager(new LinearLayoutManager(this));
        attachmentsRecycler.setItemAnimator(null);
        attachmentsRecycler.setAdapter(attachmentAdapter);

        openImageLauncher = registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
            if (uri == null) return;
            Uri copied = copyImageToInternalStorage(uri);
            if (copied == null) {
                Utility.showToast(NoteDetailsActivity.this, "Failed to add image");
                return;
            }
            attachments.add(NoteAttachment.imageLocal(copied.toString()));
            updateAttachmentsUi();
        });

        // Receive data
        title = getIntent().getStringExtra("title");
        content = getIntent().getStringExtra("content");
        docId = getIntent().getStringExtra("docId");

        String intentLocalId = getIntent().getStringExtra("localId");
        if (intentLocalId != null && !intentLocalId.isEmpty()) {
            localId = intentLocalId;
        }

        if (docId != null && !docId.isEmpty()) {
            isEditMode = false;
        }

        titleEditText.setText(title);
        contentEditText.setText(content);
        contentReadOnlyText.setMovementMethod(LinkMovementMethod.getInstance());
        if (docId != null && !docId.isEmpty()) {
            pageTitleTextView.setText("Note");
            deleteNoteTextViewBtn.setVisibility(View.VISIBLE);
            fetchNoteFromFirebase();
            enterReadMode();
        } else {
            // New note: start in edit mode
            pageTitleTextView.setText("Add New Note");
            enterEditMode();
        }

        saveNoteBtn.setOnClickListener((v) -> saveNote());
        editNoteBtn.setOnClickListener(v -> enterEditMode());

        ll_AddImage.setOnClickListener(v -> openImageLauncher.launch(new String[]{"image/*"}));
        ll_AddUrl.setOnClickListener(v -> promptForUrl());
        ll_AddPage.setOnClickListener(v -> promptForPage());

        deleteNoteTextViewBtn.setOnClickListener((v) -> deleteNoteFromFirebase());
    }

    private void fetchNoteFromFirebase() {
        if (docId == null || docId.isEmpty()) return;
        new Thread(() -> {
            LocalNoteEntity local = AppDatabase.getInstance(getApplicationContext()).localNoteDao().getByFirestoreDocId(docId);
            Log.d(TAG, "fetchNote docId=" + docId + " localFound=" + (local != null));
            if (local != null) {
                int attCount = local.attachments != null ? local.attachments.size() : 0;
                Log.d(TAG, "fetchNote using LOCAL, attachments count=" + attCount);
                if (local.attachments != null) {
                    for (int i = 0; i < local.attachments.size(); i++) {
                        NoteAttachment att = local.attachments.get(i);
                        if (att != null && NoteAttachment.TYPE_IMAGE.equals(att.getType()))
                            Log.d(TAG, "fetchNote attachment[" + i + "] IMAGE localUri=" + att.getLocalUri() + " value=" + att.getValue());
                    }
                }
                runOnUiThread(() -> {
                    titleEditText.setText(local.title);
                    contentEditText.setText(local.content);
                    attachments = local.attachments != null ? local.attachments : new ArrayList<>();
                    localId = local.localId;
                    updateAttachmentsUi();
                });
                return;
            }
            Log.d(TAG, "fetchNote using FIRESTORE (no local)");
            // No local version: load from Firestore (image attachments will have no localUri)
            runOnUiThread(() -> Utility.getCollectionReferenceForNotes().document(docId).get()
                    .addOnSuccessListener(snapshot -> {
                        if (snapshot == null || !snapshot.exists()) return;
                        Note note = snapshot.toObject(Note.class);
                        if (note == null) return;
                        titleEditText.setText(note.getTitle());
                        contentEditText.setText(note.getContent());
                        attachments = note.getAttachments();
                        updateAttachmentsUi();
                    }));
        }).start();
    }

    private void enterReadMode() {
        isEditMode = false;
        titleEditText.setEnabled(false);
        titleEditText.setFocusable(false);
        titleEditText.setFocusableInTouchMode(false);
        contentEditText.setEnabled(false);
        contentEditText.setFocusable(false);
        contentEditText.setFocusableInTouchMode(false);
        contentEditText.setVisibility(View.GONE);
        contentReadOnlyText.setVisibility(View.VISIBLE);
        renderReadOnlyContent();

        saveNoteBtn.setVisibility(View.GONE);
        attachmentActions.setVisibility(View.GONE);
        editNoteBtn.setVisibility(View.VISIBLE);
        attachmentAdapter.setEditMode(false);
    }

    private void enterEditMode() {
        isEditMode = true;
        titleEditText.setEnabled(true);
        titleEditText.setFocusableInTouchMode(true);
        contentEditText.setEnabled(true);
        contentEditText.setFocusableInTouchMode(true);
        contentEditText.setVisibility(View.VISIBLE);
        contentReadOnlyText.setVisibility(View.GONE);

        saveNoteBtn.setVisibility(View.VISIBLE);
        attachmentActions.setVisibility(View.VISIBLE);
        editNoteBtn.setVisibility(View.GONE);
        attachmentAdapter.setEditMode(true);

        titleEditText.requestFocus();
    }

    private void updateAttachmentsUi() {
        int count = attachments == null ? 0 : attachments.size();
        attachmentsSummary.setText("Attachments: " + count);
        attachmentAdapter.submitList(attachments);
        if (!isEditMode) renderReadOnlyContent();
    }

    private void renderReadOnlyContent() {
        StringBuilder sb = new StringBuilder();
        String body = contentEditText.getText() == null ? "" : contentEditText.getText().toString();
        if (!body.trim().isEmpty()) sb.append(body.trim());

        contentReadOnlyText.setText(sb.toString());
        Linkify.addLinks(contentReadOnlyText, Linkify.WEB_URLS);
    }

    private void deleteAttachment(int position) {
        if (position < 0 || position >= attachments.size()) return;
        NoteAttachment a = attachments.get(position);
        if (a != null && NoteAttachment.TYPE_IMAGE.equals(a.getType()) && a.getLocalUri() != null && !a.getLocalUri().isEmpty()) {
            String path = a.getLocalUri();
            if (path.startsWith("file:")) {
                try {
                    File f = new File(Uri.parse(path).getPath());
                    if (f.exists()) f.delete();
                } catch (Exception ignored) {}
            }
        }
        attachments.remove(position);
        updateAttachmentsUi();
    }

    private void editAttachment(int position) {
        if (position < 0 || position >= attachments.size()) return;
        NoteAttachment a = attachments.get(position);
        if (a == null) return;

        String type = a.getType();
        if (NoteAttachment.TYPE_URL.equals(type)) {
            EditText input = new EditText(this);
            input.setHint("https://example.com");
            input.setText(a.getValue());
            new AlertDialog.Builder(this)
                    .setTitle("Edit URL")
                    .setView(input)
                    .setPositiveButton("Save", (d, w) -> {
                        String url = input.getText().toString().trim();
                        if (url.isEmpty()) return;
                        a.setValue(url);
                        updateAttachmentsUi();
                    })
                    .setNegativeButton("Cancel", null)
                    .setNeutralButton("Delete", (d, w) -> deleteAttachment(position))
                    .show();
        } else if (NoteAttachment.TYPE_PAGE.equals(type)) {
            EditText title = new EditText(this);
            title.setHint("Page title");
            title.setText(a.getTitle());
            EditText body = new EditText(this);
            body.setHint("Page text");
            body.setMinLines(3);
            body.setGravity(android.view.Gravity.TOP);
            body.setText(a.getBody());

            android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
            layout.setOrientation(android.widget.LinearLayout.VERTICAL);
            int pad = (int) (16 * getResources().getDisplayMetrics().density);
            layout.setPadding(pad, pad, pad, pad);
            layout.addView(title);
            layout.addView(body);

            new AlertDialog.Builder(this)
                    .setTitle("Edit Page")
                    .setView(layout)
                    .setPositiveButton("Save", (d, w) -> {
                        a.setTitle(title.getText().toString().trim());
                        a.setBody(body.getText().toString());
                        updateAttachmentsUi();
                    })
                    .setNegativeButton("Cancel", null)
                    .setNeutralButton("Delete", (d, w) -> deleteAttachment(position))
                    .show();
        } else if (NoteAttachment.TYPE_TEXT.equals(type)) {
            EditText input = new EditText(this);
            input.setHint("Text");
            input.setText(a.getValue());
            new AlertDialog.Builder(this)
                    .setTitle("Edit Text")
                    .setView(input)
                    .setPositiveButton("Save", (d, w) -> {
                        String t = input.getText().toString();
                        if (t.trim().isEmpty()) return;
                        a.setValue(t);
                        updateAttachmentsUi();
                    })
                    .setNegativeButton("Cancel", null)
                    .setNeutralButton("Delete", (d, w) -> deleteAttachment(position))
                    .show();
        } else if (NoteAttachment.TYPE_IMAGE.equals(type)) {
            new AlertDialog.Builder(this)
                    .setTitle("Image")
                    .setMessage("Replace or delete this image.")
                    .setPositiveButton("Replace", (d, w) -> openImageLauncher.launch(new String[]{"image/*"}))
                    .setNegativeButton("Cancel", null)
                    .setNeutralButton("Delete", (d, w) -> deleteAttachment(position))
                    .show();
        }
    }

    private void openAttachment(NoteAttachment a) {
        String type = a.getType();
        Log.d(TAG, "openAttachment type=" + type + " value=" + a.getValue() + " localUri=" + a.getLocalUri());
        Intent i = new Intent(NoteDetailsActivity.this, AttachmentViewerActivity.class);
        i.putExtra(AttachmentViewerActivity.EXTRA_TYPE, type);
        if (NoteAttachment.TYPE_IMAGE.equals(type)) {
            String url = (a.getValue() != null && !a.getValue().isEmpty()) ? a.getValue() : a.getLocalUri();
            Log.d(TAG, "openAttachment IMAGE url passed to viewer: " + (url != null ? url : "null"));
            i.putExtra(AttachmentViewerActivity.EXTRA_URL, url);
        } else if (NoteAttachment.TYPE_URL.equals(type)) {
            i.putExtra(AttachmentViewerActivity.EXTRA_URL, a.getValue());
        } else if (NoteAttachment.TYPE_PAGE.equals(type)) {
            i.putExtra(AttachmentViewerActivity.EXTRA_TITLE, a.getTitle());
            i.putExtra(AttachmentViewerActivity.EXTRA_BODY, a.getBody());
        } else if (NoteAttachment.TYPE_TEXT.equals(type)) {
            i.putExtra(AttachmentViewerActivity.EXTRA_TITLE, "Text");
            i.putExtra(AttachmentViewerActivity.EXTRA_BODY, a.getValue());
        }
        startActivity(i);
    }

    private void promptForUrl() {
        EditText input = new EditText(this);
        input.setHint("https://example.com");
        new AlertDialog.Builder(this)
                .setTitle("Add URL")
                .setView(input)
                .setPositiveButton("Add", (d, w) -> {
                    String url = input.getText().toString().trim();
                    if (url.isEmpty()) return;
                    attachments.add(NoteAttachment.url(url));
                    updateAttachmentsUi();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void promptForPage() {
        EditText title = new EditText(this);
        title.setHint("Page title");
        EditText body = new EditText(this);
        body.setHint("Page text");
        body.setMinLines(3);
        body.setGravity(android.view.Gravity.TOP);

        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad, pad, pad);
        layout.addView(title);
        layout.addView(body);

        new AlertDialog.Builder(this)
                .setTitle("Add Page")
                .setView(layout)
                .setPositiveButton("Add", (d, w) -> {
                    String t = title.getText().toString().trim();
                    String b = body.getText().toString();
                    if (t.isEmpty() && b.trim().isEmpty()) return;
                    attachments.add(NoteAttachment.page(t, b));
                    updateAttachmentsUi();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    void saveNote() {
        String noteTitle = titleEditText.getText().toString();
        String noteContent = contentEditText.getText().toString();
        if (noteTitle == null || noteTitle.isEmpty()) {
            titleEditText.setError("Title is required");
            return;
        }
        Note note = new Note();
        note.setTitle(noteTitle);
        note.setContent(noteContent);
        note.setTimestamp(Timestamp.now());
        note.setAttachments(attachments);

        saveNoteOfflineFirst(note);
    }

    private void saveNoteOfflineFirst(Note note) {
        if (localId == null || localId.isEmpty()) localId = UUID.randomUUID().toString();
        boolean online = Utility.isOnline(this);
        boolean pending = !online;

        LocalNoteEntity entity = new LocalNoteEntity(
                localId,
                (docId != null && !docId.isEmpty()) ? docId : null,
                note.getTitle(),
                note.getContent(),
                System.currentTimeMillis(),
                note.getAttachments(),
                pending
        );

        new Thread(() -> {
            LocalNoteDao dao = AppDatabase.getInstance(getApplicationContext()).localNoteDao();
            dao.upsert(entity);
            runOnUiThread(() -> {
                if (!online) {
                    Utility.showToast(NoteDetailsActivity.this, "Saved offline. Will sync when online.");
                    SyncScheduler.enqueue(getApplicationContext());
                    enterReadMode();
                    finish();
                } else {
                    saveNoteToFirebase(note);
                }
            });
        }).start();
    }

    void saveNoteToFirebase(Note note) {
        DocumentReference documentReference;
        if (docId != null && !docId.isEmpty()) {
            documentReference = Utility.getCollectionReferenceForNotes().document(docId);
        } else {
            documentReference = Utility.getCollectionReferenceForNotes().document();
        }

        String finalDocId = documentReference.getId();
        new Thread(() -> {
            try {
                // Text syncs to Firestore; images stay local only (no Firebase Storage upload)
                Note noteForFirestore = Utility.buildNoteForFirestore(note);
                Tasks.await(documentReference.set(noteForFirestore));
                // Update local DB so next time we open by docId we find the note (with image localUri)
                if (localId != null && !localId.isEmpty()) {
                    AppDatabase.getInstance(getApplicationContext()).localNoteDao().setFirestoreDocId(localId, finalDocId);
                }
                runOnUiThread(() -> {
                    Utility.showToast(NoteDetailsActivity.this, "Note saved");
                    docId = finalDocId;
                    SyncScheduler.enqueue(getApplicationContext());
                    finish();
                });
            } catch (Exception e) {
                runOnUiThread(() -> Utility.showToast(NoteDetailsActivity.this, "Failed while saving note: " + e.getMessage()));
            }
        }).start();
    }

    private Uri copyImageToInternalStorage(Uri src) {
        try {
            InputStream in = getContentResolver().openInputStream(src);
            if (in == null) return null;
            File dir = new File(getFilesDir(), "images");
            if (!dir.exists()) dir.mkdirs();
            File outFile = new File(dir, "img_" + System.currentTimeMillis() + ".jpg");
            FileOutputStream out = new FileOutputStream(outFile);
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            out.flush();
            out.close();
            in.close();
            return Uri.fromFile(outFile);
        } catch (Exception e) {
            return null;
        }
    }

    void deleteNoteFromFirebase() {
        if (docId == null || docId.isEmpty()) {
            Utility.showToast(NoteDetailsActivity.this, "Invalid document ID");
            return;
        }

        DocumentReference documentReference = Utility.getCollectionReferenceForNotes().document(docId);

        // Check if the document exists before deleting
        documentReference.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot documentSnapshot = task.getResult();
                    if (documentSnapshot.exists()) {
                        // Document exists, proceed with deletion
                        documentReference.delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    // Note is deleted successfully
                                    Utility.showToast(NoteDetailsActivity.this, "Note deleted successfully");
                                    finish(); // Close the activity
                                } else {
                                    Utility.showToast(NoteDetailsActivity.this, "Failed while deleting note");
                                }
                            }
                        });
                    } else {
                        // Document doesn't exist
                        Utility.showToast(NoteDetailsActivity.this, "Note not found");
                    }
                } else {
                    // Error checking the document
                    Utility.showToast(NoteDetailsActivity.this, "Error checking document existence");
                }
            }
        });
    }
}
