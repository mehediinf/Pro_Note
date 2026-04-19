package com.mtech.note;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MixedNoteAdapter extends ListAdapter<NoteListItem, MixedNoteAdapter.VH> {

    public interface Listener {
        /** Tap on an online note */
        void onOpenOnlineNote(Note note, String docId);
        /** Tap on an offline pending note */
        void onOpenOfflineNote(String localId, String title, String content);
        void onSelectionStartRequested();
        void onSelectionCountChanged(int count);
    }

    private final Context context;
    private final Listener listener;
    private boolean selectionMode = false;
    private final java.util.Set<String> selectedIds = new java.util.HashSet<>();

    private static final SimpleDateFormat DATE_FMT =
            new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());

    public MixedNoteAdapter(Context context, Listener listener) {
        super(DIFF_CALLBACK);
        this.context  = context;
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<NoteListItem> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<NoteListItem>() {
                @Override
                public boolean areItemsTheSame(@NonNull NoteListItem oldItem, @NonNull NoteListItem newItem) {
                    return false;
                }

                @Override
                public boolean areContentsTheSame(@NonNull NoteListItem oldItem, @NonNull NoteListItem newItem) {
                    return false;
                }

            };

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recycler_note_item, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        NoteListItem item = getItem(position);
        String uid = item.getUniqueId();

        // Title & content
        h.titleTextView.setText(item.getDisplayTitle());
        h.contentTextView.setText(item.getDisplayContent());

        // Timestamp
        if (item.source == NoteListItem.Source.ONLINE && item.note != null
                && item.note.timestamp != null) {
            h.timestampTextView.setText(
                    DATE_FMT.format(item.note.timestamp.toDate()));
            h.timestampTextView.setVisibility(View.VISIBLE);
        } else if (item.source == NoteListItem.Source.OFFLINE && item.timestampMs > 0) {
            h.timestampTextView.setText(DATE_FMT.format(new Date(item.timestampMs)));
            h.timestampTextView.setVisibility(View.VISIBLE);
        } else {
            h.timestampTextView.setVisibility(View.GONE);
        }

        // Offline pending badge — always show for OFFLINE source
        boolean isPending = item.source == NoteListItem.Source.OFFLINE;
        h.pendingSyncRow.setVisibility(isPending ? View.VISIBLE : View.GONE);

        // Card selection state
        h.cardView.setChecked(selectedIds.contains(uid));

        // Click
        h.itemView.setOnClickListener(v -> {
            if (selectionMode) {
                toggleSelection(uid);
                int p = h.getBindingAdapterPosition();
                if (p != RecyclerView.NO_POSITION) notifyItemChanged(p);
                return;
            }
            if (listener == null) return;
            if (item.source == NoteListItem.Source.ONLINE) {
                listener.onOpenOnlineNote(item.note, item.docId);
            } else {
                listener.onOpenOfflineNote(item.localId, item.title, item.content);
            }
        });

        // Long press → selection
        h.itemView.setOnLongClickListener(v -> {
            if (!selectionMode) {
                selectionMode = true;
                if (listener != null) listener.onSelectionStartRequested();
            }
            toggleSelection(uid);
            int p = h.getBindingAdapterPosition();
            if (p != RecyclerView.NO_POSITION) notifyItemChanged(p);
            return true;
        });
    }

    // Selection helpers

    private void toggleSelection(String uid) {
        if (selectedIds.contains(uid)) selectedIds.remove(uid);
        else selectedIds.add(uid);
        if (listener != null) listener.onSelectionCountChanged(selectedIds.size());
        if (selectedIds.isEmpty()) selectionMode = false;
    }

    public void clearSelectionAndExit() {
        selectedIds.clear();
        selectionMode = false;
        notifyDataSetChanged();
        if (listener != null) listener.onSelectionCountChanged(0);
    }

    public boolean isSelectionMode() { return selectionMode; }
    public int getSelectedCount()    { return selectedIds.size(); }

    public void selectAll() {
        selectedIds.clear();
        for (int i = 0; i < getItemCount(); i++)
            selectedIds.add(getItem(i).getUniqueId());
        selectionMode = true;
        notifyDataSetChanged();
        if (listener != null) listener.onSelectionCountChanged(selectedIds.size());
    }

    public java.util.List<NoteListItem> getSelectedOnlineItems() {
        java.util.List<NoteListItem> out = new java.util.ArrayList<>();
        for (int i = 0; i < getItemCount(); i++) {
            NoteListItem it = getItem(i);
            if (selectedIds.contains(it.getUniqueId()) && it.source == NoteListItem.Source.ONLINE)
                out.add(it);
        }
        return out;
    }

    public java.util.List<NoteListItem> getSelectedOfflineItems() {
        java.util.List<NoteListItem> out = new java.util.ArrayList<>();
        for (int i = 0; i < getItemCount(); i++) {
            NoteListItem it = getItem(i);
            if (selectedIds.contains(it.getUniqueId()) && it.source == NoteListItem.Source.OFFLINE)
                out.add(it);
        }
        return out;
    }

    // ViewHolder

    static class VH extends RecyclerView.ViewHolder {
        TextView titleTextView, contentTextView, timestampTextView;
        MaterialCardView cardView;
        View pendingSyncRow;

        VH(@NonNull View itemView) {
            super(itemView);
            titleTextView     = itemView.findViewById(R.id.note_title_text_view);
            contentTextView   = itemView.findViewById(R.id.note_content_text_view);
            timestampTextView = itemView.findViewById(R.id.note_timestamp_text_view);
            cardView          = itemView.findViewById(R.id.note_card);
            pendingSyncRow    = itemView.findViewById(R.id.pending_sync_row);
        }
    }
}