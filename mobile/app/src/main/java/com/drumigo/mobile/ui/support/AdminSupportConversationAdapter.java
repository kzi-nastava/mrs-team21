package com.drumigo.mobile.ui.support;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.drumigo.mobile.R;
import com.drumigo.mobile.data.model.support.SupportConversationSummaryResponse;

import java.util.ArrayList;
import java.util.List;

public class AdminSupportConversationAdapter extends RecyclerView.Adapter<AdminSupportConversationAdapter.ViewHolder> {

    public interface OnConversationClickListener {
        void onConversationClicked(SupportConversationSummaryResponse conversation);
    }

    private final List<SupportConversationSummaryResponse> conversations = new ArrayList<>();
    private final OnConversationClickListener clickListener;

    public AdminSupportConversationAdapter(OnConversationClickListener clickListener) {
        this.clickListener = clickListener;
    }

    public void setConversations(List<SupportConversationSummaryResponse> items) {
        conversations.clear();
        if (items != null) {
            conversations.addAll(items);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_admin_support_conversation, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (position < 0 || position >= conversations.size()) return;
        SupportConversationSummaryResponse item = conversations.get(position);
        holder.userName.setText((safe(item.userName) + " " + safe(item.userSurname)).trim());
        holder.lastMessage.setText(safe(item.lastMessage));
        holder.time.setText(formatTime(item.lastMessageAt));
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) clickListener.onConversationClicked(item);
        });
    }

    @Override
    public int getItemCount() {
        return conversations.size();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
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
        TextView userName;
        TextView time;
        TextView lastMessage;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            userName = itemView.findViewById(R.id.adminConversationUser);
            time = itemView.findViewById(R.id.adminConversationTime);
            lastMessage = itemView.findViewById(R.id.adminConversationLastMessage);
        }
    }
}
