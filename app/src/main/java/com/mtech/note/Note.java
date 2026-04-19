package com.mtech.note;

import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.List;

public class Note {
    String title;
    String content;
    Timestamp timestamp;
    List<NoteAttachment> attachments;

    public Note() {
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public List<NoteAttachment> getAttachments() {
        if (attachments == null) attachments = new ArrayList<>();
        return attachments;
    }

    public void setAttachments(List<NoteAttachment> attachments) {
        this.attachments = attachments;
    }
}
