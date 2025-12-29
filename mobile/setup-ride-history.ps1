# ============================================
# Ride History Setup Script for Windows
# ============================================

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Ride History Setup Script" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Get the current directory (should be project root)
$projectRoot = Get-Location
Write-Host "Project root: $projectRoot" -ForegroundColor Yellow
Write-Host ""

# Ask user to confirm
$confirm = Read-Host "Is this your Android project root? (Y/N)"
if ($confirm -ne "Y" -and $confirm -ne "y") {
    Write-Host "Please navigate to your project root and run again." -ForegroundColor Red
    exit
}

# Define base paths
$javaBase = "app\src\main\java\com\ridesharing\app"
$resBase = "app\src\main\res"

Write-Host "Creating folders..." -ForegroundColor Green

# Create Java folders
$folders = @(
    "$javaBase\ui\history",
    "$javaBase\data\model",
    "$resBase\layout",
    "$resBase\values",
    "$resBase\drawable"
)

foreach ($folder in $folders) {
    if (!(Test-Path $folder)) {
        New-Item -ItemType Directory -Path $folder -Force | Out-Null
        Write-Host "  Created: $folder" -ForegroundColor Gray
    }
}

Write-Host ""
Write-Host "Creating Java files..." -ForegroundColor Green

# ============================================
# RideHistoryActivity.java
# ============================================
$rideHistoryActivity = @"
package com.ridesharing.app.ui.history;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.ridesharing.app.R;
import com.ridesharing.app.data.model.Ride;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class RideHistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private RideHistoryAdapter adapter;
    private TextView fromDateText, toDateText, resultsCountText;
    private Button applyFilterButton;
    private Calendar fromDate, toDate;
    private SimpleDateFormat dateFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Ride History");
        }
    }

    private void initializeViews() {
        recyclerView = findViewById(R.id.recyclerViewRides);
        fromDateText = findViewById(R.id.fromDateText);
        toDateText = findViewById(R.id.toDateText);
        resultsCountText = findViewById(R.id.resultsCountText);
        applyFilterButton = findViewById(R.id.applyFilterButton);

        fromDateText.setText(dateFormat.format(fromDate.getTime()));
        toDateText.setText(dateFormat.format(toDate.getTime()));

        applyFilterButton.setOnClickListener(v -> applyFilter());
    }

    private void setupRecyclerView() {
        adapter = new RideHistoryAdapter(new ArrayList<>(), this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void setupDatePickers() {
        fromDateText.setOnClickListener(v -> showDatePicker(true));
        toDateText.setOnClickListener(v -> showDatePicker(false));
    }

    private void showDatePicker(boolean isFromDate) {
        Calendar calendar = isFromDate ? fromDate : toDate;
        
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                R.style.DatePickerTheme,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(year, month, dayOfMonth);
                    if (isFromDate) {
                        fromDateText.setText(dateFormat.format(calendar.getTime()));
                    } else {
                        toDateText.setText(dateFormat.format(calendar.getTime()));
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
        adapter.updateRides(rides);
        resultsCountText.setText("Showing " + rides.size() + " rides");
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
"@

Set-Content -Path "$javaBase\ui\history\RideHistoryActivity.java" -Value $rideHistoryActivity
Write-Host "  Created: RideHistoryActivity.java" -ForegroundColor Gray

# ============================================
# RideHistoryAdapter.java
# ============================================
$rideHistoryAdapter = @"
package com.ridesharing.app.ui.history;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.ridesharing.app.R;
import com.ridesharing.app.data.model.Ride;
import java.util.List;

public class RideHistoryAdapter extends RecyclerView.Adapter<RideHistoryAdapter.RideViewHolder> {

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
        Ride ride = rides.get(position);
        
        holder.dateText.setText(ride.getDate());
        holder.timeText.setText(ride.getTime());
        holder.fromText.setText(ride.getOrigin());
        holder.toText.setText(ride.getDestination());
        
        holder.passengerAvatarsContainer.removeAllViews();
        for (String passengerInitials : ride.getPassengerInitials()) {
            TextView avatar = createPassengerAvatar(passengerInitials);
            holder.passengerAvatarsContainer.addView(avatar);
        }
        holder.passengerCountText.setText(ride.getPassengerCount() + 
                (ride.getPassengerCount() == 1 ? " passenger" : " passengers"));
        
        if (ride.getStatus().equals("Completed")) {
            holder.statusBadge.setBackgroundResource(R.drawable.bg_status_completed);
            holder.statusText.setText("Completed");
            holder.statusText.setTextColor(ContextCompat.getColor(context, R.color.success));
            holder.statusIcon.setImageResource(R.drawable.ic_check);
            holder.statusIcon.setColorFilter(ContextCompat.getColor(context, R.color.success));
        } else if (ride.getStatus().equals("Cancelled")) {
            holder.statusBadge.setBackgroundResource(R.drawable.bg_status_cancelled);
            holder.statusText.setText("Cancelled");
            holder.statusText.setTextColor(ContextCompat.getColor(context, R.color.danger));
            holder.statusIcon.setImageResource(R.drawable.ic_close_circle);
            holder.statusIcon.setColorFilter(ContextCompat.getColor(context, R.color.danger));
        }
        
        if (ride.getCancelledBy() != null) {
            holder.cancelledByText.setText(ride.getCancelledBy());
            holder.cancelledByText.setVisibility(View.VISIBLE);
        } else {
            holder.cancelledByText.setText("—");
            holder.cancelledByText.setVisibility(View.VISIBLE);
        }
        
        holder.priceText.setText(ride.getPrice());
        if (ride.getPrice().equals("—")) {
            holder.priceText.setTextColor(ContextCompat.getColor(context, R.color.text_light));
        } else {
            holder.priceText.setTextColor(ContextCompat.getColor(context, R.color.success));
        }
        
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
        this.rides = newRides;
        notifyDataSetChanged();
    }

    private TextView createPassengerAvatar(String initials) {
        TextView avatar = new TextView(context);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                dpToPx(32), dpToPx(32)
        );
        params.setMarginStart(dpToPx(-8));
        avatar.setLayoutParams(params);
        
        avatar.setText(initials);
        avatar.setTextSize(11);
        avatar.setTextColor(ContextCompat.getColor(context, R.color.white));
        avatar.setGravity(android.view.Gravity.CENTER);
        avatar.setBackgroundResource(R.drawable.bg_passenger_avatar);
        
        return avatar;
    }

    private int dpToPx(int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
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
"@

Set-Content -Path "$javaBase\ui\history\RideHistoryAdapter.java" -Value $rideHistoryAdapter
Write-Host "  Created: RideHistoryAdapter.java" -ForegroundColor Gray

# ============================================
# Ride.java
# ============================================
$rideModel = @"
package com.ridesharing.app.data.model;

public class Ride {
    private Long id;
    private String date;
    private String time;
    private String origin;
    private String destination;
    private String[] passengerInitials;
    private int passengerCount;
    private String status;
    private String cancelledBy;
    private String price;
    private boolean hasPanic;

    public Ride(Long id, String date, String time, String origin, String destination,
                String[] passengerInitials, int passengerCount, String status,
                String cancelledBy, String price, boolean hasPanic) {
        this.id = id;
        this.date = date;
        this.time = time;
        this.origin = origin;
        this.destination = destination;
        this.passengerInitials = passengerInitials;
        this.passengerCount = passengerCount;
        this.status = status;
        this.cancelledBy = cancelledBy;
        this.price = price;
        this.hasPanic = hasPanic;
    }

    public Long getId() { return id; }
    public String getDate() { return date; }
    public String getTime() { return time; }
    public String getOrigin() { return origin; }
    public String getDestination() { return destination; }
    public String[] getPassengerInitials() { return passengerInitials; }
    public int getPassengerCount() { return passengerCount; }
    public String getStatus() { return status; }
    public String getCancelledBy() { return cancelledBy; }
    public String getPrice() { return price; }
    public boolean hasPanic() { return hasPanic; }

    public void setId(Long id) { this.id = id; }
    public void setDate(String date) { this.date = date; }
    public void setTime(String time) { this.time = time; }
    public void setOrigin(String origin) { this.origin = origin; }
    public void setDestination(String destination) { this.destination = destination; }
    public void setPassengerInitials(String[] passengerInitials) { this.passengerInitials = passengerInitials; }
    public void setPassengerCount(int passengerCount) { this.passengerCount = passengerCount; }
    public void setStatus(String status) { this.status = status; }
    public void setCancelledBy(String cancelledBy) { this.cancelledBy = cancelledBy; }
    public void setPrice(String price) { this.price = price; }
    public void setHasPanic(boolean hasPanic) { this.hasPanic = hasPanic; }
}
"@

Set-Content -Path "$javaBase\data\model\Ride.java" -Value $rideModel
Write-Host "  Created: Ride.java" -ForegroundColor Gray

Write-Host ""
Write-Host "Creating XML files..." -ForegroundColor Green
Write-Host "  Note: XML files are too large for PowerShell script." -ForegroundColor Yellow
Write-Host "  Creating marker files - you'll need to copy content manually." -ForegroundColor Yellow

# Create marker files
$markerFiles = @(
    "$resBase\layout\activity_ride_history.xml",
    "$resBase\layout\item_ride_history.xml",
    "$resBase\values\colors_ride_history.xml",
    "$resBase\values\strings_ride_history.xml",
    "$resBase\drawable\bg_input.xml",
    "$resBase\drawable\bg_status_completed.xml",
    "$resBase\drawable\bg_status_cancelled.xml",
    "$resBase\drawable\bg_passenger_avatar.xml",
    "$resBase\drawable\bg_route_dot_start.xml",
    "$resBase\drawable\bg_route_dot_end.xml",
    "$resBase\drawable\bg_route_line.xml",
    "$resBase\drawable\bg_panic_inactive.xml",
    "$resBase\drawable\bg_panic_active.xml",
    "$resBase\drawable\ic_arrow_back.xml",
    "$resBase\drawable\ic_calendar.xml",
    "$resBase\drawable\ic_check.xml",
    "$resBase\drawable\ic_close_circle.xml",
    "$resBase\drawable\ic_warning.xml"
)

foreach ($file in $markerFiles) {
    "<!-- TODO: Copy content from artifacts -->" | Set-Content -Path $file
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "  Setup Complete!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "Created:" -ForegroundColor Cyan
Write-Host "  - 3 Java files" -ForegroundColor White
Write-Host "  - Folder structure" -ForegroundColor White
Write-Host ""
Write-Host "NEXT STEPS:" -ForegroundColor Yellow
Write-Host "1. Copy XML content from artifacts into marker files" -ForegroundColor White
Write-Host "2. Update AndroidManifest.xml with activity" -ForegroundColor White
Write-Host "3. Sync Gradle & Build project" -ForegroundColor White
Write-Host ""
Write-Host "Press any key to exit..." -ForegroundColor Gray
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
Write-Host "Press any key to exit..." -ForegroundColor Gray
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
