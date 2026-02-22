package com.drumigo.mobile.ui.history;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.graphics.PorterDuff;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.drumigo.mobile.R;
import com.drumigo.mobile.data.model.Ride;

import java.util.Collections;
import java.util.List;

public class RideHistoryAdapter extends RecyclerView.Adapter<RideHistoryAdapter.RideViewHolder> {

    private static final String TAG = "RideHistoryAdapter";

    public interface OnRideClickListener {
        void onRideSelected(Ride ride);
    }

    public interface OnFavoriteToggleListener {
        void onFavoriteToggled(Ride ride);
    }

    private List<Ride> rides;
    private final Context context;
    private final OnRideClickListener onRideClickListener;
    private final boolean showFavoriteButton;
    private final OnFavoriteToggleListener onFavoriteToggleListener;

    public RideHistoryAdapter(List<Ride> rides, Context context, OnRideClickListener onRideClickListener,
                              boolean showFavoriteButton, OnFavoriteToggleListener onFavoriteToggleListener) {
        this.rides = rides == null ? Collections.emptyList() : rides;
        this.context = context;
        this.onRideClickListener = onRideClickListener;
        this.showFavoriteButton = showFavoriteButton;
        this.onFavoriteToggleListener = onFavoriteToggleListener;
    }

    @NonNull
    @Override
    public RideViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_ride_history, parent, false);
        return new RideViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RideViewHolder holder, int position) {
        if (position < 0 || position >= rides.size()) {
            Log.w(TAG, "Ignoring invalid adapter position " + position);
            return;
        }

        Ride ride = rides.get(position);
        if (ride == null) {
            return;
        }

        bindRouteAndTime(holder, ride);
        bindPassengerSection(holder, ride);
        bindStatusSection(holder, ride);
        bindBottomSummary(holder, ride);
        bindFavoriteStar(holder, ride);

        holder.itemView.setOnClickListener(v -> {
            if (onRideClickListener != null) {
                onRideClickListener.onRideSelected(ride);
            }
        });
    }

    private void bindFavoriteStar(RideViewHolder holder, Ride ride) {
        holder.favoriteStar.setVisibility(showFavoriteButton ? View.VISIBLE : View.GONE);
        if (!showFavoriteButton) {
            return;
        }
        holder.favoriteStar.setImageResource(ride.isFavorite() ? R.drawable.ic_star : R.drawable.ic_star_outline);
        int color = ride.isFavorite()
            ? ContextCompat.getColor(context, R.color.star_filled)
            : ContextCompat.getColor(context, R.color.text_light);
        holder.favoriteStar.setColorFilter(color, PorterDuff.Mode.SRC_IN);
        holder.favoriteStar.setOnClickListener(v -> {
            v.setClickable(false);
            if (onFavoriteToggleListener != null) {
                onFavoriteToggleListener.onFavoriteToggled(ride);
            }
            v.setClickable(true);
        });
    }

    private void bindRouteAndTime(RideViewHolder holder, Ride ride) {
        holder.dateText.setText(emptyToFallback(ride.getDate(), ""));
        holder.timeText.setText(emptyToFallback(ride.getTime(), ""));
        holder.fromText.setText(emptyToFallback(ride.getOrigin(), context.getString(R.string.ride_history_none)));
        holder.toText.setText(emptyToFallback(ride.getDestination(), context.getString(R.string.ride_history_none)));
    }

    private void bindPassengerSection(RideViewHolder holder, Ride ride) {
        int count = Math.max(0, ride.getPassengerCount());
        holder.passengerCountText.setText(count + (count == 1 ? " passenger" : " passengers"));
    }

    private void bindStatusSection(RideViewHolder holder, Ride ride) {
        RideStatusPresentation status = RideStatusPresentation.from(ride.getStatus());
        holder.statusBadge.setBackgroundResource(status.badgeBackgroundRes);
        holder.statusText.setText(status.label);
        holder.statusText.setTextColor(ContextCompat.getColor(context, status.textColorRes));
        holder.statusIcon.setImageResource(status.iconRes);
        holder.statusIcon.setColorFilter(ContextCompat.getColor(context, status.textColorRes));
    }

    private void bindBottomSummary(RideViewHolder holder, Ride ride) {
        String cancellationText = emptyToFallback(ride.getCancelledBy(), context.getString(R.string.ride_history_not_cancelled));
        holder.cancelledByText.setText(cancellationText);

        String amountText = emptyToFallback(ride.getPrice(), context.getString(R.string.ride_history_none));
        holder.priceText.setText(amountText);
        int amountColor = resolveAmountColorRes(ride.getPrice());
        holder.priceText.setTextColor(ContextCompat.getColor(context, amountColor));

        if (ride.hasPanic()) {
            holder.panicIndicator.setBackgroundResource(R.drawable.bg_panic_active);
            holder.panicIcon.setColorFilter(ContextCompat.getColor(context, R.color.danger));
        } else {
            holder.panicIndicator.setBackgroundResource(R.drawable.bg_panic_inactive);
            holder.panicIcon.setColorFilter(ContextCompat.getColor(context, R.color.text_light));
        }
    }

    @Override
    public int getItemCount() {
        return rides.size();
    }

    public void updateRides(List<Ride> newRides) {
        this.rides = newRides == null ? Collections.emptyList() : newRides;
        notifyDataSetChanged();
    }

    private static String emptyToFallback(String value, String fallback) {
        return isBlank(value) ? fallback : value;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private int resolveAmountColorRes(String value) {
        if (isBlank(value)) {
            return R.color.text_light;
        }
        return value.trim().startsWith("+") ? R.color.success : R.color.primary_dark;
    }

    static class RideViewHolder extends RecyclerView.ViewHolder {
        TextView dateText;
        TextView timeText;
        TextView fromText;
        TextView toText;
        TextView passengerCountText;
        ImageButton favoriteStar;
        View statusBadge;
        ImageView statusIcon;
        TextView statusText;
        TextView cancelledByText;
        TextView priceText;
        View panicIndicator;
        ImageView panicIcon;

        RideViewHolder(@NonNull View itemView) {
            super(itemView);
            dateText = itemView.findViewById(R.id.dateText);
            timeText = itemView.findViewById(R.id.timeText);
            fromText = itemView.findViewById(R.id.fromText);
            toText = itemView.findViewById(R.id.toText);
            passengerCountText = itemView.findViewById(R.id.passengerCountText);
            favoriteStar = itemView.findViewById(R.id.favoriteStar);
            statusBadge = itemView.findViewById(R.id.statusBadge);
            statusIcon = itemView.findViewById(R.id.statusIcon);
            statusText = itemView.findViewById(R.id.statusText);
            cancelledByText = itemView.findViewById(R.id.cancelledByText);
            priceText = itemView.findViewById(R.id.priceText);
            panicIndicator = itemView.findViewById(R.id.panicIndicator);
            panicIcon = itemView.findViewById(R.id.panicIcon);
        }
    }
}
