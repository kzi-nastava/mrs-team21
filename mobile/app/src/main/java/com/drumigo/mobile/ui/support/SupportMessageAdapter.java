package com.drumigo.mobile.ui.support;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.drumigo.mobile.R;
import com.drumigo.mobile.data.model.support.SupportMessageResponse;

import java.util.ArrayList;
import java.util.List;

public class SupportMessageAdapter extends RecyclerView.Adapter<SupportMessageAdapter.ViewHolder> {

    private final List<SupportMessageResponse> messages = new ArrayList<>();
    private final long currentUserId;

    public SupportMessageAdapter(long currentUserId) {
        this.currentUserId = currentUserId;
    }

    public void setMessages(List<SupportMessageResponse> list) {
        messages.clear();
        if (list != null) {
            messages.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_support_message, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (position < 0 || position >= messages.size()) return;
        SupportMessageResponse msg = messages.get(position);
        Context c = holder.itemView.getContext();

        boolean isMine = msg.senderId != null && msg.senderId == currentUserId;
        String author = isMine
            ? (nullToEmpty(msg.senderName) + " " + nullToEmpty(msg.senderSurname)).trim()
            : c.getString(R.string.nav_support);
        if (author.isEmpty()) author = isMine ? c.getString(R.string.profile_title) : c.getString(R.string.nav_support);

        holder.author.setText(author);
        holder.time.setText(formatTime(msg.createdAt));
        holder.content.setText(nullToEmpty(msg.content));
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static String formatTime(String iso) {
        if (iso == null || iso.isEmpty()) return "";
        try {
            int tIndex = iso.indexOf('T');
            if (tIndex >= 0 && tIndex + 1 < iso.length()) {
                String timePart = iso.substring(tIndex + 1);
                if (timePart.length() >= 5) return timePart.substring(0, 5);
            }
        } catch (Exception ignored) {}
        return iso;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView author;
        TextView time;
        TextView content;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            author = itemView.findViewById(R.id.supportMessageAuthor);
            time = itemView.findViewById(R.id.supportMessageTime);
            content = itemView.findViewById(R.id.supportMessageContent);
        }
    }
}
