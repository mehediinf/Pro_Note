package com.mtech.note;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.widget.Toast;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class Utility {

    public static Note buildNoteForFirestore(Note note) {
        Note out = new Note();
        out.setTitle(note.getTitle());
        out.setContent(note.getContent());
        out.setTimestamp(note.getTimestamp());

        List<NoteAttachment> list = new ArrayList<>();
        for (NoteAttachment a : note.getAttachments()) {
            if (a == null) continue;

            if (NoteAttachment.TYPE_IMAGE.equals(a.getType())) {
                NoteAttachment img = new NoteAttachment();
                img.type = NoteAttachment.TYPE_IMAGE;
                img.value = null;
                img.localUri = null;
                list.add(img);
            } else if (NoteAttachment.TYPE_URL.equals(a.getType())) {
                NoteAttachment url = new NoteAttachment();
                url.type = NoteAttachment.TYPE_URL;
                url.value = a.getValue();
                list.add(url);
            } else if (NoteAttachment.TYPE_PAGE.equals(a.getType())) {
                NoteAttachment page = new NoteAttachment();
                page.type = NoteAttachment.TYPE_PAGE;
                page.title = a.getTitle();
                page.body = a.getBody();
                list.add(page);
            } else if (NoteAttachment.TYPE_TEXT.equals(a.getType())) {
                NoteAttachment text = new NoteAttachment();
                text.type = NoteAttachment.TYPE_TEXT;
                text.value = a.getValue();
                list.add(text);
            }
        }
        out.setAttachments(list);
        return out;
    }

    public static void showToast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    public static CollectionReference getCollectionReferenceForNotes() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        return FirebaseFirestore.getInstance().collection("notes")
                .document(currentUser.getUid()).collection("my_notes");
    }

    public static String timestampToString(Timestamp timestamp) {
        return new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
                .format(timestamp.toDate());
    }

    public static boolean isOnline(Context context) {
        ConnectivityManager cm = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        Network network = cm.getActiveNetwork();
        if (network == null) return false;
        NetworkCapabilities caps = cm.getNetworkCapabilities(network);
        if (caps == null) return false;
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }
}