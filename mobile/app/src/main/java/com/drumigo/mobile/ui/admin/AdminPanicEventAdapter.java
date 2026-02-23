package com.drumigo.mobile.ui.admin;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.drumigo.mobile.R;
import com.drumigo.mobile.data.model.panic.PanicEventResponse;
import com.drumigo.mobile.ui.history.RideHistoryActivity;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AdminPanicEventAdapter extends RecyclerView.Adapter<AdminPanicEventAdapter.ViewHolder> {

    private final List<PanicEventResponse> items = new ArrayList<>();

    public void setItems(List<PanicEventResponse> list) {
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
            .inflate(R.layout.item_admin_panic_event, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (position < 0 || position >= items.size()) return;
        PanicEventResponse e = items.get(position);
        holder.time.setText(formatDate(e.createdAt));
        holder.rideId.setText("Ride #" + (e.rideId != null ? e.rideId : ""));
        holder.userEmail.setText(e.userEmail != null ? e.userEmail : "");
        if (e.reason != null && !e.reason.trim().isEmpty()) {
            holder.reason.setVisibility(View.VISIBLE);
            holder.reason.setText(e.reason);
        } else {
            holder.reason.setVisibility(View.GONE);
        }
        holder.viewRide.setOnClickListener(v -> {
            Context ctx = v.getContext();
            ctx.startActivity(new Intent(ctx, RideHistoryActivity.class));
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private static String formatDate(String iso) {
        if (iso == null || iso.isEmpty()) return "—";
        try {
            SimpleDateFormat in = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
            in.setLenient(true);
            Date date = in.parse(iso, new ParsePosition(0));
            if (date == null) {
                SimpleDateFormat alt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
                date = alt.parse(iso, new ParsePosition(0));
            }
            if (date == null) return iso;
            SimpleDateFormat out = new SimpleDateFormat("dd/MM/yyyy, HH:mm", Locale.getDefault());
            return out.format(date);
        } catch (Exception ex) {
            return iso;
        }
    }

    static final class ViewHolder extends RecyclerView.ViewHolder {
        final TextView time;
        final TextView rideId;
        final TextView userEmail;
        final TextView reason;
        final Button viewRide;

        ViewHolder(View itemView) {
            super(itemView);
            time = itemView.findViewById(R.id.panicItemTime);
            rideId = itemView.findViewById(R.id.panicItemRideId);
            userEmail = itemView.findViewById(R.id.panicItemUserEmail);
            reason = itemView.findViewById(R.id.panicItemReason);
            viewRide = itemView.findViewById(R.id.panicItemViewRide);
        }
    }
}
