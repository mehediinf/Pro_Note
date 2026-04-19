package com.mtech.note.offline;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.mtech.note.NoteAttachment;

import java.util.List;

@Entity(tableName = "local_notes")
@TypeConverters(Converters.class)
public class LocalNoteEntity {
    @PrimaryKey
    @NonNull
    public String localId; // UUID

    @Nullable
    public String firestoreDocId;

    public String title;
    public String content;

    public long timestampMs;

    public List<NoteAttachment> attachments;

    public boolean pendingSync;

    public LocalNoteEntity(@NonNull String localId,
                           @Nullable String firestoreDocId,
                           String title,
                           String content,
                           long timestampMs,
                           List<NoteAttachment> attachments,
                           boolean pendingSync) {
        this.localId = localId;
        this.firestoreDocId = firestoreDocId;
        this.title = title;
        this.content = content;
        this.timestampMs = timestampMs;
        this.attachments = attachments;
        this.pendingSync = pendingSync;
    }
}

