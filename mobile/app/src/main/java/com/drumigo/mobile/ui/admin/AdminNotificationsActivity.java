package com.drumigo.mobile.ui.admin;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.drumigo.mobile.R;
import com.drumigo.mobile.data.api.ApiClient;
import com.drumigo.mobile.data.api.AdminApiService;
import com.drumigo.mobile.data.api.NotificationApiService;
import com.drumigo.mobile.data.model.NotificationResponse;
import com.drumigo.mobile.data.model.history.PageResponse;
import com.drumigo.mobile.data.model.user.UserSearchItem;
import com.drumigo.mobile.ui.notifications.NotificationListAdapter;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminNotificationsActivity extends AppCompatActivity {

    private static final int SEARCH_DEBOUNCE_MS = 300;
    private static final int PAGE_SIZE = 10;
    private static final String SORT = "createdAt,desc";

    private AdminApiService adminApiService;
    private NotificationApiService notificationApiService;

    private TextInputEditText searchInput;
    private TextView searchingText;
    private RecyclerView suggestionsList;
    private View selectedChip;
    private TextView selectedUserText;
    private View clearUserButton;
    private RecyclerView notificationsRecyclerView;
    private TextView hintText;
    private ProgressBar loadingView;
    private TextView emptyText;
    private TextView errorText;
    private View paginationRow;
    private Button prevButton;
    private TextView pageText;
    private Button nextButton;

    private AdminNotificationUserSuggestionAdapter suggestionAdapter;
    private NotificationListAdapter notificationAdapter;

    private UserSearchItem selectedUser;
    private boolean isSelectingUser;
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;
    private int currentPage = 0;
    private int totalPages = 0;
    private int totalElements = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_notifications);

        adminApiService = ApiClient.getAdminApiService();
        notificationApiService = ApiClient.getNotificationApiService();

        setupToolbar();
        bindViews();
        setupSearch();
        setupSuggestions();
        setupNotificationsList();
        setupPagination();
        setupClearUser();

        showHintState();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.admin_notifications_title);
        }
    }

    private void bindViews() {
        searchInput = findViewById(R.id.adminNotificationsSearchInput);
        searchingText = findViewById(R.id.adminNotificationsSearchingText);
        suggestionsList = findViewById(R.id.adminNotificationsSuggestionsList);
        selectedChip = findViewById(R.id.adminNotificationsSelectedChip);
        selectedUserText = findViewById(R.id.adminNotificationsSelectedUserText);
        clearUserButton = findViewById(R.id.adminNotificationsClearUserButton);
        notificationsRecyclerView = findViewById(R.id.adminNotificationsRecyclerView);
        hintText = findViewById(R.id.adminNotificationsHintText);
        loadingView = findViewById(R.id.adminNotificationsLoading);
        emptyText = findViewById(R.id.adminNotificationsEmptyText);
        errorText = findViewById(R.id.adminNotificationsErrorText);
        paginationRow = findViewById(R.id.adminNotificationsPagination);
        prevButton = findViewById(R.id.adminNotificationsPrevButton);
        pageText = findViewById(R.id.adminNotificationsPageText);
        nextButton = findViewById(R.id.adminNotificationsNextButton);
    }

    private void setupSearch() {
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (isSelectingUser) {
                    return;
                }
                selectedUser = null;
                updateSelectedChipVisibility();
                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }
                String q = s != null ? s.toString().trim() : "";
                if (q.length() < 2) {
                    suggestionAdapter.setItems(null);
                    suggestionsList.setVisibility(View.GONE);
                    showHintState();
                    return;
                }
                searchRunnable = () -> performSearch(q);
                searchHandler.postDelayed(searchRunnable, SEARCH_DEBOUNCE_MS);
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });
    }

    private void performSearch(String q) {
        searchingText.setVisibility(View.VISIBLE);
        adminApiService.searchUsersByEmail(q, 15).enqueue(new Callback<List<UserSearchItem>>() {
            @Override
            public void onResponse(
                @NonNull Call<List<UserSearchItem>> call,
                @NonNull Response<List<UserSearchItem>> response
            ) {
                searchingText.setVisibility(View.GONE);
                if (!response.isSuccessful() || response.body() == null) {
                    suggestionAdapter.setItems(List.of());
                } else {
                    suggestionAdapter.setItems(response.body());
                }
                boolean hasSuggestions = suggestionAdapter.getItemCount() > 0;
                suggestionsList.setVisibility(hasSuggestions ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onFailure(@NonNull Call<List<UserSearchItem>> call, @NonNull Throwable t) {
                searchingText.setVisibility(View.GONE);
                suggestionAdapter.setItems(List.of());
                suggestionsList.setVisibility(View.GONE);
            }
        });
    }

    private void setupSuggestions() {
        suggestionAdapter = new AdminNotificationUserSuggestionAdapter();
        suggestionAdapter.setOnUserSelectedListener(this::onUserSelected);
        suggestionsList.setLayoutManager(new LinearLayoutManager(this));
        suggestionsList.setAdapter(suggestionAdapter);
    }

    private void onUserSelected(UserSearchItem user) {
        selectedUser = user;
        isSelectingUser = true;
        try {
            if (searchInput != null) {
                searchInput.setText(user.email != null ? user.email : "");
                searchInput.clearFocus();
            }
            suggestionAdapter.setItems(null);
            suggestionsList.setVisibility(View.GONE);
            updateSelectedChipVisibility();
            if (selectedUserText != null) {
                selectedUserText.setText(AdminNotificationUserSuggestionAdapter.userLabel(user));
            }
            currentPage = 0;
            loadNotifications();
        } finally {
            isSelectingUser = false;
        }
    }

    private void setupClearUser() {
        if (clearUserButton != null) {
            clearUserButton.setOnClickListener(v -> clearSelectedUser());
        }
    }

    private void clearSelectedUser() {
        selectedUser = null;
        if (searchInput != null) searchInput.setText("");
        updateSelectedChipVisibility();
        showHintState();
    }

    private void updateSelectedChipVisibility() {
        if (selectedChip != null) {
            selectedChip.setVisibility(selectedUser != null ? View.VISIBLE : View.GONE);
        }
    }

    private void setupNotificationsList() {
        notificationAdapter = new NotificationListAdapter(true, true);
        notificationsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        notificationsRecyclerView.setAdapter(notificationAdapter);
    }

    private void setupPagination() {
        if (prevButton != null) {
            prevButton.setOnClickListener(v -> {
                if (currentPage > 0) {
                    currentPage--;
                    loadNotifications();
                }
            });
        }
        if (nextButton != null) {
            nextButton.setOnClickListener(v -> {
                if (currentPage < totalPages - 1) {
                    currentPage++;
                    loadNotifications();
                }
            });
        }
    }

    private void loadNotifications() {
        if (selectedUser == null || selectedUser.id == null) {
            return;
        }
        hintText.setVisibility(View.GONE);
        notificationsRecyclerView.setVisibility(View.GONE);
        emptyText.setVisibility(View.GONE);
        errorText.setVisibility(View.GONE);
        loadingView.setVisibility(View.VISIBLE);
        paginationRow.setVisibility(View.GONE);

        notificationApiService.getUserNotifications(selectedUser.id, currentPage, PAGE_SIZE, SORT)
            .enqueue(new Callback<PageResponse<NotificationResponse>>() {
                @Override
                public void onResponse(
                    @NonNull Call<PageResponse<NotificationResponse>> call,
                    @NonNull Response<PageResponse<NotificationResponse>> response
                ) {
                    loadingView.setVisibility(View.GONE);
                    if (!response.isSuccessful() || response.body() == null) {
                        errorText.setVisibility(View.VISIBLE);
                        errorText.setText(R.string.admin_notifications_error);
                        return;
                    }
                    PageResponse<NotificationResponse> body = response.body();
                    List<NotificationResponse> content = body.content;
                    totalPages = body.totalPages;
                    totalElements = body.totalElements;

                    if (content == null || content.isEmpty()) {
                        emptyText.setVisibility(View.VISIBLE);
                        emptyText.setText(R.string.admin_notifications_empty);
                    } else {
                        notificationAdapter.setItems(content);
                        notificationsRecyclerView.setVisibility(View.VISIBLE);
                    }
                    updatePaginationUi();
                }

                @Override
                public void onFailure(
                    @NonNull Call<PageResponse<NotificationResponse>> call,
                    @NonNull Throwable t
                ) {
                    loadingView.setVisibility(View.GONE);
                    errorText.setVisibility(View.VISIBLE);
                    errorText.setText(R.string.admin_notifications_error);
                    updatePaginationUi();
                }
            });
    }

    private void updatePaginationUi() {
        if (selectedUser == null) {
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

    private void showHintState() {
        hintText.setVisibility(View.VISIBLE);
        notificationsRecyclerView.setVisibility(View.GONE);
        loadingView.setVisibility(View.GONE);
        emptyText.setVisibility(View.GONE);
        errorText.setVisibility(View.GONE);
        paginationRow.setVisibility(View.GONE);
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
