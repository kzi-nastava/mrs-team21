package com.drumigo.mobile.ui.notifications;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.drumigo.mobile.R;
import com.drumigo.mobile.data.model.NotificationResponse;
import com.drumigo.mobile.util.NotificationTypeLabels;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationListAdapter extends RecyclerView.Adapter<NotificationListAdapter.ViewHolder> {

    private final List<NotificationResponse> items = new ArrayList<>();
    private final boolean showTypeLabel;
    private final boolean showReadStatus;

    public NotificationListAdapter(boolean showTypeLabel) {
        this(showTypeLabel, false);
    }

    public NotificationListAdapter(boolean showTypeLabel, boolean showReadStatus) {
        this.showTypeLabel = showTypeLabel;
        this.showReadStatus = showReadStatus;
    }

    public void setItems(List<NotificationResponse> list) {
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
            .inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (position < 0 || position >= items.size()) return;
        NotificationResponse n = items.get(position);
        holder.message.setText(n.message != null ? n.message : "");
        String typeLabel = n.type != null ? NotificationTypeLabels.getLabel(n.type) : "";
        String dateStr = formatDate(n.createdAt);
        StringBuilder metaSb = new StringBuilder();
        if (showTypeLabel && !typeLabel.isEmpty()) {
            metaSb.append(typeLabel).append(" · ");
        }
        metaSb.append(dateStr);
        if (showReadStatus) {
            metaSb.append(n.readAt != null && !n.readAt.isEmpty()
                ? " · Read " + formatDate(n.readAt)
                : " · Unread");
        }
        holder.meta.setText(metaSb.toString());
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static String formatDate(String iso) {
        if (iso == null || iso.isEmpty()) return "";
        try {
            SimpleDateFormat in = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
            in.setLenient(true);
            Date date = in.parse(iso, new ParsePosition(0));
            if (date == null) {
                SimpleDateFormat alt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
                date = alt.parse(iso, new ParsePosition(0));
            }
            if (date == null) return iso;
            SimpleDateFormat out = new SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault());
            return out.format(date);
        } catch (Exception e) {
            return iso;
        }
    }

    static final class ViewHolder extends RecyclerView.ViewHolder {
        final TextView message;
        final TextView meta;

        ViewHolder(View itemView) {
            super(itemView);
            message = itemView.findViewById(R.id.notification_message);
            meta = itemView.findViewById(R.id.notification_meta);
        }
    }
}
