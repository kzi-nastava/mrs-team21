package com.drumigo.mobile.ui.admin;

import android.text.Editable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.drumigo.mobile.R;
import com.drumigo.mobile.data.model.vehicletype.VehicleTypeResponse;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

final class AdminVehicleTypePricingAdapter extends RecyclerView.Adapter<AdminVehicleTypePricingAdapter.ViewHolder> {

    interface SaveListener {
        void onSave(long id, double startPrice, double pricePerKm);
    }

    private final SaveListener saveListener;
    private final List<VehicleTypeResponse> items = new ArrayList<>();
    private long savingId = -1L;

    AdminVehicleTypePricingAdapter(@NonNull SaveListener saveListener) {
        this.saveListener = saveListener;
    }

    void submitList(@NonNull List<VehicleTypeResponse> list) {
        items.clear();
        items.addAll(list);
        notifyDataSetChanged();
    }

    void setSavingId(long id) {
        savingId = id;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_admin_vehicle_type_pricing, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        VehicleTypeResponse item = items.get(position);
        if (item == null || item.id == null) {
            return;
        }

        holder.nameText.setText(item.name != null ? item.name : "");
        setPriceText(holder.startPriceEdit, item.startPrice);
        setPriceText(holder.pricePerKmEdit, item.pricePerKm);

        boolean saving = savingId == item.id;
        holder.saveButton.setEnabled(!saving);
        holder.saveButton.setText(saving ? R.string.admin_pricing_saving : R.string.admin_pricing_save);

        holder.saveButton.setOnClickListener(v -> {
            double startPrice = parsePrice(holder.startPriceEdit.getText());
            double pricePerKm = parsePrice(holder.pricePerKmEdit.getText());
            if (startPrice < 0 || pricePerKm < 0) {
                return;
            }
            saveListener.onSave(item.id, startPrice, pricePerKm);
        });
    }

    private static void setPriceText(EditText edit, Double value) {
        if (value == null) {
            edit.setText("");
            return;
        }
        String s = value == (long) value.doubleValue()
            ? String.valueOf((long) value.doubleValue())
            : String.valueOf(value);
        edit.setText(s);
    }

    private static double parsePrice(Editable e) {
        if (e == null) {
            return 0;
        }
        String s = e.toString().trim();
        if (s.isEmpty()) {
            return 0;
        }
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView nameText;
        final EditText startPriceEdit;
        final EditText pricePerKmEdit;
        final MaterialButton saveButton;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.vehicleTypeNameText);
            startPriceEdit = itemView.findViewById(R.id.startPriceEdit);
            pricePerKmEdit = itemView.findViewById(R.id.pricePerKmEdit);
            saveButton = itemView.findViewById(R.id.saveButton);
        }
    }
}
