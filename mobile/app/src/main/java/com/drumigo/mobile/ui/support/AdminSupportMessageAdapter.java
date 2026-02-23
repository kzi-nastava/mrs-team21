package com.drumigo.mobile.ui.support;

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

public class AdminSupportMessageAdapter extends RecyclerView.Adapter<AdminSupportMessageAdapter.ViewHolder> {

    private final List<SupportMessageResponse> messages = new ArrayList<>();
    private final long currentUserId;

    public AdminSupportMessageAdapter(long currentUserId) {
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
        String fullName = (safe(msg.senderName) + " " + safe(msg.senderSurname)).trim();
        if (fullName.isEmpty()) {
            boolean mine = msg.senderId != null && msg.senderId == currentUserId;
            fullName = mine ? "Admin" : "User";
        }
        holder.author.setText(fullName);
        holder.time.setText(formatTime(msg.createdAt));
        holder.content.setText(safe(msg.content));
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    private static String safe(String s) {
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
        } catch (Exception ignored) {
        }
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
