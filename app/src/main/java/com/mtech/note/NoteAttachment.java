package com.mtech.note;

import androidx.annotation.Nullable;
import com.google.firebase.firestore.PropertyName;

public class NoteAttachment {
    public static final String TYPE_TEXT = "text";
    public static final String TYPE_IMAGE = "image";
    public static final String TYPE_URL = "url";
    public static final String TYPE_PAGE = "page";

    @PropertyName("type")
    public String type;

    @PropertyName("value")
    @Nullable
    public String value;

    @PropertyName("localUri")
    @Nullable
    public String localUri;

    @PropertyName("title")
    @Nullable
    public String title;

    @PropertyName("body")
    @Nullable
    public String body;

    public NoteAttachment() {}

    public static NoteAttachment text(String text) {
        NoteAttachment a = new NoteAttachment();
        a.type = TYPE_TEXT;
        a.value = text;
        return a;
    }

    public static NoteAttachment url(String url) {
        NoteAttachment a = new NoteAttachment();
        a.type = TYPE_URL;
        a.value = url;
        return a;
    }

    public static NoteAttachment imageLocal(String uri) {
        NoteAttachment a = new NoteAttachment();
        a.type = TYPE_IMAGE;
        a.localUri = uri;
        return a;
    }

    public static NoteAttachment imageRemote(String remoteUrl) {
        NoteAttachment a = new NoteAttachment();
        a.type = TYPE_IMAGE;
        a.value = remoteUrl;
        return a;
    }

    public static NoteAttachment page(String title, String body) {
        NoteAttachment a = new NoteAttachment();
        a.type = TYPE_PAGE;
        a.title = title;
        a.body = body;
        return a;
    }

    @PropertyName("type")
    public String getType() { return type; }
    @PropertyName("type")
    public void setType(String type) { this.type = type; }

    @PropertyName("value")
    @Nullable
    public String getValue() { return value; }
    @PropertyName("value")
    public void setValue(@Nullable String value) { this.value = value; }

    @PropertyName("localUri")
    @Nullable
    public String getLocalUri() { return localUri; }
    @PropertyName("localUri")
    public void setLocalUri(@Nullable String localUri) { this.localUri = localUri; }

    @PropertyName("title")
    @Nullable
    public String getTitle() { return title; }
    @PropertyName("title")
    public void setTitle(@Nullable String title) { this.title = title; }

    @PropertyName("body")
    @Nullable
    public String getBody() { return body; }
    @PropertyName("body")
    public void setBody(@Nullable String body) { this.body = body; }
}