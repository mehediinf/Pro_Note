package com.mtech.note;

public class NoteListItem {

    public enum Source { ONLINE, OFFLINE }

    public final Source source;

    // Online note fields
    public final String docId;
    public final Note   note;

    // Offline note fields
    public final String localId;
    public final String title;
    public final String content;
    public final long   timestampMs;

    /** Firestore note */
    public static NoteListItem online(String docId, Note note) {
        return new NoteListItem(Source.ONLINE, docId, note, null, null, null, 0);
    }

    /** Local-only pending note */
    public static NoteListItem offline(String localId, String title, String content, long tsMs) {
        return new NoteListItem(Source.OFFLINE, null, null, localId, title, content, tsMs);
    }

    private NoteListItem(Source source,
                         String docId, Note note,
                         String localId, String title, String content, long timestampMs) {
        this.source      = source;
        this.docId       = docId;
        this.note        = note;
        this.localId     = localId;
        this.title       = title;
        this.content     = content;
        this.timestampMs = timestampMs;
    }

    /** Display title regardless of source */
    public String getDisplayTitle() {
        if (source == Source.ONLINE) return note != null && note.title != null ? note.title : "";
        return title != null ? title : "";
    }

    /** Display content regardless of source */
    public String getDisplayContent() {
        if (source == Source.ONLINE) return note != null && note.content != null ? note.content : "";
        return content != null ? content : "";
    }

    /** Unique stable ID for DiffUtil */
    public String getUniqueId() {
        if (source == Source.ONLINE) return "online_" + docId;
        return "offline_" + localId;
    }
}