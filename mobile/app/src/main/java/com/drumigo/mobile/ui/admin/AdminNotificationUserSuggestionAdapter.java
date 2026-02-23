package com.drumigo.mobile.ui.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.drumigo.mobile.R;
import com.drumigo.mobile.data.model.user.UserSearchItem;

import java.util.ArrayList;
import java.util.List;

public class AdminNotificationUserSuggestionAdapter extends RecyclerView.Adapter<AdminNotificationUserSuggestionAdapter.ViewHolder> {

    private final List<UserSearchItem> items = new ArrayList<>();
    private OnUserSelectedListener listener;

    public interface OnUserSelectedListener {
        void onUserSelected(UserSearchItem user);
    }

    public void setOnUserSelectedListener(OnUserSelectedListener listener) {
        this.listener = listener;
    }

    public void setItems(List<UserSearchItem> list) {
        items.clear();
        if (list != null) {
            items.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_admin_notification_user_suggestion, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (position < 0 || position >= items.size()) return;
        UserSearchItem u = items.get(position);
        String label = userLabel(u);
        holder.label.setText(label);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onUserSelected(u);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static String userLabel(UserSearchItem u) {
        if (u == null) return "";
        String name = (u.name != null ? u.name : "").trim();
        String surname = (u.surname != null ? u.surname : "").trim();
        String full = (name + " " + surname).trim();
        String email = u.email != null ? u.email : "";
        if (full.isEmpty()) return email;
        return full + " (" + email + ")";
    }

    static final class ViewHolder extends RecyclerView.ViewHolder {
        final TextView label;

        ViewHolder(View itemView) {
            super(itemView);
            label = itemView.findViewById(R.id.suggestionLabel);
        }
    }
}
