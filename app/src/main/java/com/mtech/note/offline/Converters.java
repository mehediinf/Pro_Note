package com.mtech.note.offline;

import androidx.room.TypeConverter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mtech.note.NoteAttachment;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class Converters {
    private static final Gson gson = new Gson();
    private static final Type ATTACHMENT_LIST = new TypeToken<List<NoteAttachment>>(){}.getType();

    @TypeConverter
    public static String attachmentsToJson(List<NoteAttachment> attachments) {
        if (attachments == null) return "[]";
        return gson.toJson(attachments, ATTACHMENT_LIST);
    }

    @TypeConverter
    public static List<NoteAttachment> jsonToAttachments(String json) {
        if (json == null || json.trim().isEmpty()) return new ArrayList<>();
        List<NoteAttachment> list = gson.fromJson(json, ATTACHMENT_LIST);
        return list == null ? new ArrayList<>() : list;
    }
}

