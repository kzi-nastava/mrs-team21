package com.drumigo.mobile.ui.history;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.drumigo.mobile.R;
import com.drumigo.mobile.data.model.Ride;
import java.util.List;

public class RideHistoryAdapter extends RecyclerView.Adapter<RideHistoryAdapter.RideViewHolder> {

    private static final String TAG = "RideHistoryAdapter";

    private List<Ride> rides;
    private Context context;

    public RideHistoryAdapter(List<Ride> rides, Context context) {
        this.rides = rides;
        this.context = context;
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
        try {
            if (rides == null || position < 0 || position >= rides.size()) {
                Log.w(TAG, "Invalid position or empty rides list: " + position);
                return;
            }

            Ride ride = rides.get(position);
            if (ride == null) return;

            if (holder.dateText != null) holder.dateText.setText(nullToEmpty(ride.getDate()));
            if (holder.timeText != null) holder.timeText.setText(nullToEmpty(ride.getTime()));
            if (holder.fromText != null) holder.fromText.setText(nullToEmpty(ride.getOrigin()));
            if (holder.toText != null) holder.toText.setText(nullToEmpty(ride.getDestination()));

            if (holder.passengerAvatarsContainer != null) {
                holder.passengerAvatarsContainer.removeAllViews();
                String[] initials = ride.getPassengerInitials();
                if (initials != null) {
                    for (String passengerInitials : initials) {
                        // use the itemView context which is guaranteed non-null at runtime
                        TextView avatar = createPassengerAvatar(passengerInitials == null ? "" : passengerInitials, holder.itemView.getContext());
                        holder.passengerAvatarsContainer.addView(avatar);
                    }
                }
            }

            if (holder.passengerCountText != null) {
                int count = ride.getPassengerCount();
                holder.passengerCountText.setText(count + (count == 1 ? " passenger" : " passengers"));
            }

            String status = ride.getStatus();
            if (status != null && status.equals("Completed")) {
                if (holder.statusBadge != null) holder.statusBadge.setBackgroundResource(R.drawable.bg_status_completed);
                if (holder.statusText != null) holder.statusText.setText("Completed");
                if (holder.statusText != null) holder.statusText.setTextColor(ContextCompat.getColor(context, R.color.success));
                if (holder.statusIcon != null) {
                    holder.statusIcon.setImageResource(R.drawable.ic_check);
                    holder.statusIcon.setColorFilter(ContextCompat.getColor(context, R.color.success));
                }
            } else if (status != null && status.equals("Cancelled")) {
                if (holder.statusBadge != null) holder.statusBadge.setBackgroundResource(R.drawable.bg_status_cancelled);
                if (holder.statusText != null) holder.statusText.setText("Cancelled");
                if (holder.statusText != null) holder.statusText.setTextColor(ContextCompat.getColor(context, R.color.danger));
                if (holder.statusIcon != null) {
                    holder.statusIcon.setImageResource(R.drawable.ic_close_circle);
                    holder.statusIcon.setColorFilter(ContextCompat.getColor(context, R.color.danger));
                }
            }

            if (holder.cancelledByText != null) {
                String cancelledBy = ride.getCancelledBy();
                holder.cancelledByText.setText(cancelledBy == null ? "—" : cancelledBy);
                holder.cancelledByText.setVisibility(View.VISIBLE);
            }

            if (holder.priceText != null) {
                String price = ride.getPrice();
                holder.priceText.setText(price == null ? "—" : price);
                if (price == null || price.equals("—")) {
                    holder.priceText.setTextColor(ContextCompat.getColor(context, R.color.text_light));
                } else {
                    holder.priceText.setTextColor(ContextCompat.getColor(context, R.color.success));
                }
            }

            if (holder.panicIndicator != null) {
                if (ride.hasPanic()) {
                    holder.panicIndicator.setBackgroundResource(R.drawable.bg_panic_active);
                    if (holder.panicIcon != null) holder.panicIcon.setColorFilter(ContextCompat.getColor(context, R.color.danger));
                } else {
                    holder.panicIndicator.setBackgroundResource(R.drawable.bg_panic_inactive);
                    if (holder.panicIcon != null) holder.panicIcon.setColorFilter(ContextCompat.getColor(context, R.color.text_light));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error binding ride history item at position " + position, e);
        }
    }

    @Override
    public int getItemCount() {
        return rides == null ? 0 : rides.size();
    }

    public void updateRides(List<Ride> newRides) {
        this.rides = newRides == null ? java.util.Collections.emptyList() : newRides;
        notifyDataSetChanged();
    }

    private TextView createPassengerAvatar(String initials, Context ctx) {
        if (ctx == null) ctx = this.context; // fallback to adapter context
        if (ctx == null) {
            Log.w(TAG, "Context is unexpectedly null when creating avatar; this may cause a runtime exception");
        }

        TextView avatar = new TextView(ctx);
        float density = ctx.getResources().getDisplayMetrics().density;
        int sizePx = Math.round(32 * density);
        int marginPx = Math.round(-8 * density);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(sizePx, sizePx);
        params.setMarginStart(marginPx);
        avatar.setLayoutParams(params);

        avatar.setText(initials);
        avatar.setTextSize(11);
        try {
            avatar.setTextColor(ContextCompat.getColor(ctx, R.color.white));
        } catch (Exception e) {
            avatar.setTextColor(0xFFFFFFFF);
        }
        avatar.setGravity(android.view.Gravity.CENTER);
        avatar.setBackgroundResource(R.drawable.bg_passenger_avatar);

        return avatar;
    }

    private int dpToPx(int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    static class RideViewHolder extends RecyclerView.ViewHolder {
        TextView dateText, timeText, fromText, toText;
        LinearLayout passengerAvatarsContainer;
        TextView passengerCountText;
        View statusBadge;
        ImageView statusIcon;
        TextView statusText, cancelledByText, priceText;
        View panicIndicator;
        ImageView panicIcon;

        public RideViewHolder(@NonNull View itemView) {
            super(itemView);
            dateText = itemView.findViewById(R.id.dateText);
            timeText = itemView.findViewById(R.id.timeText);
            fromText = itemView.findViewById(R.id.fromText);
            toText = itemView.findViewById(R.id.toText);
            passengerAvatarsContainer = itemView.findViewById(R.id.passengerAvatarsContainer);
            passengerCountText = itemView.findViewById(R.id.passengerCountText);
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
