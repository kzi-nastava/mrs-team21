package com.drumigo.mobile.ui.admin;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.drumigo.mobile.R;
import com.drumigo.mobile.data.api.ApiClient;
import com.drumigo.mobile.data.api.VehicleTypeApiService;
import com.drumigo.mobile.data.model.vehicletype.VehicleTypeResponse;
import com.drumigo.mobile.data.model.vehicletype.VehicleTypeUpdateRequest;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminVehicleTypePricingActivity extends AppCompatActivity implements AdminVehicleTypePricingAdapter.SaveListener {

    private VehicleTypeApiService vehicleTypeApiService;
    private RecyclerView recyclerView;
    private TextView loadingText;
    private TextView emptyText;
    private AdminVehicleTypePricingAdapter adapter;
    private final List<VehicleTypeResponse> items = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_vehicle_type_pricing);

        vehicleTypeApiService = ApiClient.getVehicleTypeApiService();

        setupToolbar();
        bindViews();
        setupList();
        loadVehicleTypes();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.admin_pricing_title);
        }
    }

    private void bindViews() {
        recyclerView = findViewById(R.id.pricingRecyclerView);
        loadingText = findViewById(R.id.pricingLoadingText);
        emptyText = findViewById(R.id.pricingEmptyText);
    }

    private void setupList() {
        adapter = new AdminVehicleTypePricingAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void loadVehicleTypes() {
        setLoading(true);
        vehicleTypeApiService.getAllVehicleTypes().enqueue(new Callback<List<VehicleTypeResponse>>() {
            @Override
            public void onResponse(
                @NonNull Call<List<VehicleTypeResponse>> call,
                @NonNull Response<List<VehicleTypeResponse>> response
            ) {
                setLoading(false);
                if (!response.isSuccessful() || response.body() == null) {
                    showError(getString(R.string.admin_pricing_load_failed));
                    items.clear();
                    adapter.submitList(new ArrayList<>());
                    updateEmptyState();
                    return;
                }
                items.clear();
                items.addAll(response.body());
                adapter.submitList(new ArrayList<>(items));
                updateEmptyState();
            }

            @Override
            public void onFailure(@NonNull Call<List<VehicleTypeResponse>> call, @NonNull Throwable t) {
                setLoading(false);
                showError(getString(R.string.admin_pricing_load_failed));
                items.clear();
                adapter.submitList(new ArrayList<>());
                updateEmptyState();
            }
        });
    }

    private void setLoading(boolean loading) {
        if (loadingText != null) {
            loadingText.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
        if (recyclerView != null) {
            recyclerView.setVisibility(loading ? View.GONE : View.VISIBLE);
        }
    }

    private void updateEmptyState() {
        if (emptyText != null && adapter != null) {
            emptyText.setVisibility(adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void onSave(long id, double startPrice, double pricePerKm) {
        adapter.setSavingId(id);
        VehicleTypeUpdateRequest request = new VehicleTypeUpdateRequest(startPrice, pricePerKm);
        vehicleTypeApiService.updateVehicleType(id, request).enqueue(new Callback<VehicleTypeResponse>() {
            @Override
            public void onResponse(
                @NonNull Call<VehicleTypeResponse> call,
                @NonNull Response<VehicleTypeResponse> response
            ) {
                adapter.setSavingId(-1L);
                if (!response.isSuccessful() || response.body() == null) {
                    showError(getString(R.string.admin_pricing_save_failed));
                    return;
                }
                VehicleTypeResponse updated = response.body();
                String name = updated.name != null ? updated.name : "";
                showMessage(getString(R.string.admin_pricing_save_success, name));
                int index = indexOfId(id);
                if (index >= 0) {
                    items.set(index, updated);
                    adapter.submitList(new ArrayList<>(items));
                }
            }

            @Override
            public void onFailure(@NonNull Call<VehicleTypeResponse> call, @NonNull Throwable t) {
                adapter.setSavingId(-1L);
                showError(getString(R.string.admin_pricing_save_failed));
            }
        });
    }

    private int indexOfId(long id) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).id != null && items.get(i).id == id) {
                return i;
            }
        }
        return -1;
    }

    private void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
