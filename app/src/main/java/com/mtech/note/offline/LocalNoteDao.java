package com.mtech.note.offline;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface LocalNoteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(LocalNoteEntity note);

    @Update
    void update(LocalNoteEntity note);

    @Query("SELECT * FROM local_notes WHERE localId = :localId LIMIT 1")
    LocalNoteEntity getByLocalId(String localId);

    @Query("SELECT * FROM local_notes WHERE firestoreDocId = :firestoreDocId LIMIT 1")
    LocalNoteEntity getByFirestoreDocId(String firestoreDocId);

    @Query("UPDATE local_notes SET firestoreDocId = :firestoreDocId WHERE localId = :localId")
    void setFirestoreDocId(String localId, String firestoreDocId);

    @Query("SELECT * FROM local_notes WHERE pendingSync = 1 ORDER BY timestampMs ASC")
    List<LocalNoteEntity> getPendingSync();

    @Query("UPDATE local_notes SET pendingSync = 0, firestoreDocId = :firestoreDocId WHERE localId = :localId")
    void markSynced(String localId, String firestoreDocId);

    @Query("DELETE FROM local_notes WHERE localId = :localId")
    void deleteByLocalId(String localId);
}