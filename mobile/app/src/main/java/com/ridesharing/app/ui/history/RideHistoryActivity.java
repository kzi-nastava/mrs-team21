package com.ridesharing.app.ui.history;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.drumigo.mobile.R;
import com.ridesharing.app.data.model.Ride;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class RideHistoryActivity extends AppCompatActivity {

    private static final String TAG = "RideHistoryActivity";

    private RecyclerView recyclerView;
    private RideHistoryAdapter adapter;
    private TextView fromDateText, toDateText, resultsCountText;
    private Button applyFilterButton;
    private Calendar fromDate, toDate;
    private SimpleDateFormat dateFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_ride_history);

            dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.ENGLISH);
            fromDate = Calendar.getInstance();
            fromDate.set(2024, 0, 1);
            toDate = Calendar.getInstance();

            setupToolbar();
            initializeViews();
            setupRecyclerView();
            setupDatePickers();
            loadRideHistory();
        } catch (Exception e) {
            // Log and show a toast instead of crashing so we can diagnose in logs
            Log.e(TAG, "Error in onCreate: ", e);
            Toast.makeText(this, "Unable to open Ride History (see logs)", Toast.LENGTH_LONG).show();
        }
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);

            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setDisplayShowHomeEnabled(true);
            }
        } else {
            Log.w(TAG, "Toolbar not found in layout");
        }
    }

    private void initializeViews() {
        recyclerView = findViewById(R.id.recyclerViewRides);
        fromDateText = findViewById(R.id.fromDateText);
        toDateText = findViewById(R.id.toDateText);
        resultsCountText = findViewById(R.id.resultsCountText);
        applyFilterButton = findViewById(R.id.applyFilterButton);

        if (fromDateText != null) fromDateText.setText(dateFormat.format(fromDate.getTime()));
        if (toDateText != null) toDateText.setText(dateFormat.format(toDate.getTime()));

        if (applyFilterButton != null) {
            applyFilterButton.setOnClickListener(v -> applyFilter());
        } else {
            Log.w(TAG, "applyFilterButton not found in layout");
        }
    }

    private void setupRecyclerView() {
        adapter = new RideHistoryAdapter(new ArrayList<>(), this);
        if (recyclerView != null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            recyclerView.setAdapter(adapter);
        } else {
            Log.w(TAG, "RecyclerView not found in layout");
        }
    }

    private void setupDatePickers() {
        if (fromDateText != null) fromDateText.setOnClickListener(v -> showDatePicker(true));
        if (toDateText != null) toDateText.setOnClickListener(v -> showDatePicker(false));
    }

    private void showDatePicker(boolean isFromDate) {
        Calendar calendar = isFromDate ? fromDate : toDate;
        
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                0,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(year, month, dayOfMonth);
                    if (isFromDate) {
                        if (fromDateText != null) fromDateText.setText(dateFormat.format(calendar.getTime()));
                    } else {
                        if (toDateText != null) toDateText.setText(dateFormat.format(calendar.getTime()));
                    }
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        
        datePickerDialog.show();
    }

    private void applyFilter() {
        loadRideHistory();
    }

    private void loadRideHistory() {
        List<Ride> rides = getDummyRides();
        if (adapter != null) adapter.updateRides(rides);
        if (resultsCountText != null) resultsCountText.setText("Showing " + rides.size() + " rides");
    }

    private List<Ride> getDummyRides() {
        List<Ride> rides = new ArrayList<>();
        
        rides.add(new Ride(1L, "Dec 18, 2024", "14:30 - 14:52",
                "Bulevar Oslobođenja 76", "Faculty of Technical Sciences",
                new String[]{"JD"}, 1, "Completed", null, "+450 RSD", false));

        rides.add(new Ride(2L, "Dec 18, 2024", "12:15 - 12:38",
                "City Center Mall", "Novi Sad Airport",
                new String[]{"AN", "MK"}, 2, "Completed", null, "+980 RSD", false));

        rides.add(new Ride(3L, "Dec 17, 2024", "18:45 - 19:02",
                "Liman Park", "Spens Sports Center",
                new String[]{"SV"}, 1, "Cancelled", "By Passenger", "—", false));

        rides.add(new Ride(4L, "Dec 16, 2024", "22:10 - 22:35",
                "Petrovaradin Fortress", "Grbavica District",
                new String[]{"NK"}, 1, "Completed", null, "+620 RSD", true));

        rides.add(new Ride(5L, "Dec 15, 2024", "08:30 - 08:45",
                "Bus Station", "University Campus",
                new String[]{"TM", "LP", "+1"}, 3, "Completed", null, "+380 RSD", false));

        return rides;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
