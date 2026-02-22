package com.drumigo.mobile.ui.admin;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.drumigo.mobile.R;
import com.drumigo.mobile.data.api.ApiClient;
import com.drumigo.mobile.data.api.PanicApiService;
import com.drumigo.mobile.data.model.history.PageResponse;
import com.drumigo.mobile.data.model.panic.PanicEventResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminPanicNotificationsActivity extends AppCompatActivity {

    private static final int PAGE_SIZE = 10;
    private static final String SORT = "createdAt,desc";

    private PanicApiService panicApiService;
    private RecyclerView recyclerView;
    private ProgressBar loadingView;
    private TextView emptyText;
    private TextView errorText;
    private View paginationRow;
    private Button prevButton;
    private TextView pageText;
    private Button nextButton;

    private AdminPanicEventAdapter adapter;
    private int currentPage = 0;
    private int totalPages = 0;
    private int totalElements = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_panic_notifications);

        panicApiService = ApiClient.getPanicApiService();

        setupToolbar();
        bindViews();
        setupList();
        setupPagination();
        loadPage(0);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.nav_panic_notifications);
        }
    }

    private void bindViews() {
        recyclerView = findViewById(R.id.adminPanicRecyclerView);
        loadingView = findViewById(R.id.adminPanicLoading);
        emptyText = findViewById(R.id.adminPanicEmptyText);
        errorText = findViewById(R.id.adminPanicErrorText);
        paginationRow = findViewById(R.id.adminPanicPagination);
        prevButton = findViewById(R.id.adminPanicPrevButton);
        pageText = findViewById(R.id.adminPanicPageText);
        nextButton = findViewById(R.id.adminPanicNextButton);
    }

    private void setupList() {
        adapter = new AdminPanicEventAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void setupPagination() {
        if (prevButton != null) {
            prevButton.setOnClickListener(v -> {
                if (currentPage > 0) {
                    loadPage(currentPage - 1);
                }
            });
        }
        if (nextButton != null) {
            nextButton.setOnClickListener(v -> {
                if (currentPage < totalPages - 1) {
                    loadPage(currentPage + 1);
                }
            });
        }
    }

    private void loadPage(int page) {
        currentPage = page;
        recyclerView.setVisibility(View.GONE);
        emptyText.setVisibility(View.GONE);
        errorText.setVisibility(View.GONE);
        loadingView.setVisibility(View.VISIBLE);
        paginationRow.setVisibility(View.GONE);

        panicApiService.getPanicEvents(currentPage, PAGE_SIZE, SORT)
            .enqueue(new Callback<PageResponse<PanicEventResponse>>() {
                @Override
                public void onResponse(
                    @NonNull Call<PageResponse<PanicEventResponse>> call,
                    @NonNull Response<PageResponse<PanicEventResponse>> response
                ) {
                    loadingView.setVisibility(View.GONE);
                    if (!response.isSuccessful() || response.body() == null) {
                        errorText.setVisibility(View.VISIBLE);
                        errorText.setText(R.string.admin_panic_error);
                        return;
                    }
                    PageResponse<PanicEventResponse> body = response.body();
                    List<PanicEventResponse> content = body.content;
                    totalPages = body.totalPages;
                    totalElements = body.totalElements;

                    if (content == null || content.isEmpty()) {
                        emptyText.setVisibility(View.VISIBLE);
                    } else {
                        adapter.setItems(content);
                        recyclerView.setVisibility(View.VISIBLE);
                    }
                    updatePaginationUi();
                }

                @Override
                public void onFailure(
                    @NonNull Call<PageResponse<PanicEventResponse>> call,
                    @NonNull Throwable t
                ) {
                    loadingView.setVisibility(View.GONE);
                    errorText.setVisibility(View.VISIBLE);
                    errorText.setText(R.string.admin_panic_error);
                    updatePaginationUi();
                }
            });
    }

    private void updatePaginationUi() {
        if (totalElements == 0 && (adapter == null || adapter.getItemCount() == 0)) {
            paginationRow.setVisibility(View.GONE);
            return;
        }
        paginationRow.setVisibility(View.VISIBLE);
        int shownPages = Math.max(totalPages, 1);
        if (pageText != null) {
            pageText.setText(getString(R.string.admin_notifications_page_info, currentPage + 1, shownPages, totalElements));
        }
        if (prevButton != null) prevButton.setEnabled(currentPage > 0);
        if (nextButton != null) nextButton.setEnabled(currentPage < totalPages - 1);
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
