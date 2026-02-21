package com.drumigo.mobile.ui.admin;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.drumigo.mobile.R;
import com.drumigo.mobile.data.api.AdminApiService;
import com.drumigo.mobile.data.api.ApiClient;
import com.drumigo.mobile.data.api.UserApiService;
import com.drumigo.mobile.data.model.history.PageResponse;
import com.drumigo.mobile.data.model.user.UserResponse;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminUserManagementActivity extends AppCompatActivity implements AdminUserManagementAdapter.ActionListener {

    private static final int PAGE_SIZE = 10;
    private static final String DEFAULT_SORT = "email,asc";
    private static final String ROLE_PASSENGER = "PASSENGER";

    private AdminApiService adminApiService;
    private UserApiService userApiService;

    private RecyclerView recyclerView;
    private TextView loadingText;
    private TextView emptyText;
    private TextView paginationText;
    private Button previousButton;
    private Button nextButton;

    private AdminUserManagementAdapter adapter;
    private int currentPage = 0;
    private int totalPages = 0;
    private int totalElements = 0;
    private String selectedRoleFilter = ROLE_PASSENGER;
    private long actionInProgressUserId = -1L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_user_management);

        adminApiService = ApiClient.getAdminApiService();
        userApiService = ApiClient.getUserApiService();

        setupToolbar();
        bindViews();
        setupList();
        setupPagination();

        loadUsers();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.admin_users_title);
        }
    }

    private void bindViews() {
        recyclerView = findViewById(R.id.usersRecyclerView);
        loadingText = findViewById(R.id.usersLoadingText);
        emptyText = findViewById(R.id.usersEmptyText);
        paginationText = findViewById(R.id.usersPaginationText);
        previousButton = findViewById(R.id.usersPreviousButton);
        nextButton = findViewById(R.id.usersNextButton);
    }

    private void setupList() {
        adapter = new AdminUserManagementAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void setupPagination() {
        previousButton.setOnClickListener(v -> {
            if (currentPage <= 0) {
                return;
            }
            currentPage--;
            loadUsers();
        });
        nextButton.setOnClickListener(v -> {
            if (currentPage >= totalPages - 1) {
                return;
            }
            currentPage++;
            loadUsers();
        });
        updatePaginationControls();
    }

    private void loadUsers() {
        setLoading(true);
        adminApiService.getUsers(
            currentPage,
            PAGE_SIZE,
            DEFAULT_SORT,
            selectedRoleFilter
        ).enqueue(new Callback<PageResponse<UserResponse>>() {
            @Override
            public void onResponse(
                @NonNull Call<PageResponse<UserResponse>> call,
                @NonNull Response<PageResponse<UserResponse>> response
            ) {
                setLoading(false);
                if (!response.isSuccessful() || response.body() == null || response.body().content == null) {
                    showError(getString(R.string.admin_users_load_failed));
                    adapter.submitList(new ArrayList<>());
                    updateEmptyState();
                    return;
                }

                PageResponse<UserResponse> body = response.body();
                totalPages = body.totalPages;
                totalElements = body.totalElements;
                List<UserResponse> users = body.content == null ? new ArrayList<>() : body.content;
                adapter.submitList(users);
                adapter.setActionInProgressUserId(actionInProgressUserId);
                updatePaginationControls();
                updateEmptyState();
            }

            @Override
            public void onFailure(@NonNull Call<PageResponse<UserResponse>> call, @NonNull Throwable t) {
                setLoading(false);
                showError(getString(R.string.admin_users_load_failed));
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
        if (emptyText == null || adapter == null) {
            return;
        }
        emptyText.setVisibility(adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
    }

    private void updatePaginationControls() {
        if (paginationText != null) {
            int shownTotalPages = Math.max(totalPages, 1);
            paginationText.setText(getString(
                R.string.admin_users_pagination,
                currentPage + 1,
                shownTotalPages,
                totalElements
            ));
        }
        if (previousButton != null) {
            previousButton.setEnabled(currentPage > 0 && actionInProgressUserId == -1L);
        }
        if (nextButton != null) {
            nextButton.setEnabled(currentPage < totalPages - 1 && actionInProgressUserId == -1L);
        }
    }

    @Override
    public void onBlock(@NonNull UserResponse user) {
        if (user.id == null || user.blocked == Boolean.TRUE) {
            return;
        }
        setActionInProgress(user.id);
        userApiService.blockUser(user.id).enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(@NonNull Call<UserResponse> call, @NonNull Response<UserResponse> response) {
                clearActionInProgress();
                if (!response.isSuccessful()) {
                    showError(getString(R.string.admin_users_block_failed));
                    return;
                }
                showMessage(getString(R.string.admin_users_block_success));
                loadUsers();
            }

            @Override
            public void onFailure(@NonNull Call<UserResponse> call, @NonNull Throwable t) {
                clearActionInProgress();
                showError(getString(R.string.admin_users_block_failed));
            }
        });
    }

    @Override
    public void onUnblock(@NonNull UserResponse user) {
        if (user.id == null || user.blocked == Boolean.FALSE) {
            return;
        }
        setActionInProgress(user.id);
        userApiService.unblockUser(user.id).enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(@NonNull Call<UserResponse> call, @NonNull Response<UserResponse> response) {
                clearActionInProgress();
                if (!response.isSuccessful()) {
                    showError(getString(R.string.admin_users_unblock_failed));
                    return;
                }
                showMessage(getString(R.string.admin_users_unblock_success));
                loadUsers();
            }

            @Override
            public void onFailure(@NonNull Call<UserResponse> call, @NonNull Throwable t) {
                clearActionInProgress();
                showError(getString(R.string.admin_users_unblock_failed));
            }
        });
    }

    private void setActionInProgress(long userId) {
        actionInProgressUserId = userId;
        adapter.setActionInProgressUserId(actionInProgressUserId);
        updatePaginationControls();
    }

    private void clearActionInProgress() {
        actionInProgressUserId = -1L;
        adapter.setActionInProgressUserId(actionInProgressUserId);
        updatePaginationControls();
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
