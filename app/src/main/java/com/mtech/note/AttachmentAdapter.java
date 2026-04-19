package com.mtech.note;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class AttachmentAdapter extends RecyclerView.Adapter<AttachmentAdapter.VH> {

    public interface Listener {
        void onAttachmentClick(NoteAttachment attachment, int position);
        void onAttachmentDelete(NoteAttachment attachment, int position);
    }

    private final Context context;
    private final Listener listener;
    private final List<NoteAttachment> items = new ArrayList<>();
    private boolean editMode = false;

    public AttachmentAdapter(Context context, Listener listener) {
        this.context = context;
        this.listener = listener;
        setHasStableIds(false);
    }

    public void setEditMode(boolean editMode) {
        this.editMode = editMode;
        notifyDataSetChanged();
    }

    public void submitList(List<NoteAttachment> list) {
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recycler_attachment_item, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        NoteAttachment a = items.get(position);
        if (a == null) return;

        String type = a.getType();
        String title;
        String subtitle;
        int iconRes;

        switch (type == null ? "" : type) {
            case NoteAttachment.TYPE_IMAGE:
                title    = "Image";
                subtitle = (a.getValue() != null && !a.getValue().isEmpty()) ? a.getValue() : a.getLocalUri();
                iconRes  = R.drawable.image;
                break;
            case NoteAttachment.TYPE_URL:
                title    = "URL";
                subtitle = a.getValue();
                iconRes  = R.drawable.link;
                break;
            case NoteAttachment.TYPE_PAGE:
                title    = (a.getTitle() == null || a.getTitle().isEmpty()) ? "Page" : a.getTitle();
                subtitle = a.getBody();
                iconRes  = R.drawable.news;
                break;
            case NoteAttachment.TYPE_TEXT:
                title    = "Text";
                subtitle = a.getValue();
                iconRes  = R.drawable.text_snippet;
                break;
            default:
                title    = "Attachment";
                subtitle = "";
                iconRes  = R.drawable.attach_file;
                break;
        }

        h.icon.setImageResource(iconRes);
        h.title.setText(title);
        h.subtitle.setText(subtitle == null ? "" : subtitle);

        // Edit mode: show delete, hide open-hint arrow
        h.deleteBtn.setVisibility(editMode ? View.VISIBLE : View.GONE);
        h.openHint.setVisibility(editMode ? View.GONE : View.VISIBLE);

        h.itemView.setOnClickListener(v -> {
            int p = h.getBindingAdapterPosition();
            if (p == RecyclerView.NO_POSITION) return;
            if (listener != null) listener.onAttachmentClick(items.get(p), p);
        });

        h.deleteBtn.setOnClickListener(v -> {
            int p = h.getBindingAdapterPosition();
            if (p == RecyclerView.NO_POSITION) return;
            if (listener != null) listener.onAttachmentDelete(items.get(p), p);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView  icon;
        TextView   title;
        TextView   subtitle;
        ImageButton deleteBtn;
        ImageView  openHint;

        VH(@NonNull View itemView) {
            super(itemView);
            icon      = itemView.findViewById(R.id.attachment_icon);
            title     = itemView.findViewById(R.id.attachment_title);
            subtitle  = itemView.findViewById(R.id.attachment_subtitle);
            deleteBtn = itemView.findViewById(R.id.attachment_delete);
            openHint  = itemView.findViewById(R.id.attachment_open_hint);
        }
    }
}