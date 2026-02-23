package com.drumigo.mobile.ui.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.drumigo.mobile.R;
import com.drumigo.mobile.data.model.user.UserResponse;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class AdminUserManagementAdapter extends RecyclerView.Adapter<AdminUserManagementAdapter.UserViewHolder> {

    interface ActionListener {
        void onBlock(@NonNull UserResponse user);
        void onUnblock(@NonNull UserResponse user);
    }

    private final ActionListener actionListener;
    private final List<UserResponse> items = new ArrayList<>();
    private long actionInProgressUserId = -1L;

    AdminUserManagementAdapter(@NonNull ActionListener actionListener) {
        this.actionListener = actionListener;
    }

    void submitList(@NonNull List<UserResponse> users) {
        items.clear();
        items.addAll(users);
        notifyDataSetChanged();
    }

    void setActionInProgressUserId(long userId) {
        actionInProgressUserId = userId;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_admin_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        UserResponse user = items.get(position);
        if (user == null) {
            return;
        }
        String fullName = formatName(user.name, user.surname);
        holder.nameText.setText(fullName);
        holder.emailText.setText(safe(user.email));
        holder.roleText.setText(displayRole(user.role));

        boolean blocked = Boolean.TRUE.equals(user.blocked);
        holder.statusText.setText(blocked ? R.string.admin_users_status_blocked : R.string.admin_users_status_active);
        holder.statusText.setSelected(blocked);
        holder.statusText.setBackgroundResource(
            blocked ? R.drawable.bg_status_cancelled : R.drawable.bg_status_completed
        );

        boolean actionBusy = actionInProgressUserId != -1L;
        holder.actionButton.setEnabled(!actionBusy);
        holder.actionButton.setText(blocked ? R.string.admin_users_action_unblock : R.string.admin_users_action_block);
        holder.actionButton.setOnClickListener(v -> {
            if (user.id == null) {
                return;
            }
            if (blocked) {
                actionListener.onUnblock(user);
            } else {
                actionListener.onBlock(user);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private String formatName(String firstName, String lastName) {
        String first = safe(firstName);
        String last = safe(lastName);
        if (first.isEmpty() && last.isEmpty()) {
            return "";
        }
        if (first.isEmpty()) {
            return last;
        }
        if (last.isEmpty()) {
            return first;
        }
        return String.format(Locale.ENGLISH, "%s %s", first, last);
    }

    private String displayRole(String role) {
        if (role == null) {
            return "";
        }
        String normalized = role.trim().toUpperCase(Locale.ENGLISH);
        if ("DRIVER".equals(normalized)) {
            return "Driver";
        }
        if ("PASSENGER".equals(normalized)) {
            return "Passenger";
        }
        return role;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    static final class UserViewHolder extends RecyclerView.ViewHolder {
        final TextView nameText;
        final TextView emailText;
        final TextView roleText;
        final TextView statusText;
        final MaterialButton actionButton;

        UserViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.userNameText);
            emailText = itemView.findViewById(R.id.userEmailText);
            roleText = itemView.findViewById(R.id.userRoleText);
            statusText = itemView.findViewById(R.id.userStatusText);
            actionButton = itemView.findViewById(R.id.userActionButton);
        }
    }
}
