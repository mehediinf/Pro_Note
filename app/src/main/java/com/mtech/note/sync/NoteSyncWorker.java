package com.mtech.note.sync;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.mtech.note.Note;
import com.mtech.note.NoteAttachment;
import com.mtech.note.Utility;
import com.mtech.note.offline.AppDatabase;
import com.mtech.note.offline.LocalNoteDao;
import com.mtech.note.offline.LocalNoteEntity;

import java.util.ArrayList;
import java.util.List;

public class NoteSyncWorker extends Worker {

    public NoteSyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context ctx = getApplicationContext();
        if (!Utility.isOnline(ctx)) return Result.retry();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return Result.retry();

        LocalNoteDao dao = AppDatabase.getInstance(ctx).localNoteDao();
        List<LocalNoteEntity> pending = dao.getPendingSync();
        if (pending == null || pending.isEmpty()) return Result.success();

        for (LocalNoteEntity local : pending) {
            try {
                String docId = local.firestoreDocId;
                DocumentReference ref;
                if (docId == null || docId.trim().isEmpty()) {
                    ref = Utility.getCollectionReferenceForNotes().document();
                    docId = ref.getId();
                } else {
                    ref = Utility.getCollectionReferenceForNotes().document(docId);
                }

                Note note = new Note();
                note.setTitle(local.title);
                note.setContent(local.content);
                note.setTimestamp(new Timestamp(local.timestampMs / 1000, 0));
                note.setAttachments(local.attachments == null ? new ArrayList<>() : local.attachments);

                Note noteForFirestore = Utility.buildNoteForFirestore(note);
                Tasks.await(ref.set(noteForFirestore));
                dao.markSynced(local.localId, docId);
            } catch (Exception e) {
                return Result.retry();
            }
        }

        return Result.success();
    }
}

