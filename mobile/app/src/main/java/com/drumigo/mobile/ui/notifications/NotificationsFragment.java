package com.drumigo.mobile.ui.notifications;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.drumigo.mobile.R;
import com.drumigo.mobile.data.api.ApiClient;
import com.drumigo.mobile.data.api.NotificationApiService;
import com.drumigo.mobile.data.model.NotificationResponse;
import com.drumigo.mobile.data.model.history.PageResponse;
import com.drumigo.mobile.databinding.FragmentNotificationsBinding;
import com.drumigo.mobile.session.SessionManager;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Notifications list for the current user (passenger or driver).
 * Shows all notifications with optional mark-as-read on tap.
 */
public class NotificationsFragment extends Fragment {

    private static final int PAGE_SIZE = 50;
    private static final String SORT = "createdAt,desc";

    private FragmentNotificationsBinding binding;
    private NotificationApiService notificationApiService;
    private SessionManager sessionManager;
    private NotificationListAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentNotificationsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        notificationApiService = ApiClient.getNotificationApiService();
        sessionManager = SessionManager.getInstance(requireContext());

        RecyclerView recyclerView = binding.notificationsRecyclerView;
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new NotificationListAdapter(true);
        recyclerView.setAdapter(adapter);

        loadNotifications();
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }

    private void loadNotifications() {
        long userId = sessionManager.getUserId();
        if (userId <= 0) {
            showError(getString(R.string.profile_error_load));
            return;
        }

        setLoading(true);
        binding.notificationsErrorText.setVisibility(View.GONE);
        binding.notificationsEmptyText.setVisibility(View.GONE);
        binding.notificationsRecyclerView.setVisibility(View.GONE);

        notificationApiService.getUserNotifications(userId, 0, PAGE_SIZE, SORT)
            .enqueue(new Callback<PageResponse<NotificationResponse>>() {
                @Override
                public void onResponse(
                    @NonNull Call<PageResponse<NotificationResponse>> call,
                    @NonNull Response<PageResponse<NotificationResponse>> response
                ) {
                    setLoading(false);
                    if (!response.isSuccessful() || response.body() == null) {
                        showError(getString(R.string.profile_error_load));
                        return;
                    }
                    List<NotificationResponse> content = response.body().content;
                    if (content == null || content.isEmpty()) {
                        binding.notificationsEmptyText.setVisibility(View.VISIBLE);
                        binding.notificationsEmptyText.setText(R.string.notifications_empty);
                    } else {
                        adapter.setItems(content);
                        binding.notificationsRecyclerView.setVisibility(View.VISIBLE);
                    }
                }

                @Override
                public void onFailure(
                    @NonNull Call<PageResponse<NotificationResponse>> call,
                    @NonNull Throwable t
                ) {
                    setLoading(false);
                    showError(getString(R.string.profile_error_load));
                }
            });
    }

    private void setLoading(boolean loading) {
        if (binding == null) return;
        binding.notificationsLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private void showError(String message) {
        if (binding == null) return;
        binding.notificationsErrorText.setVisibility(View.VISIBLE);
        binding.notificationsErrorText.setText(message);
    }

    /**
     * Call from MainActivity when opening from a system notification to refresh the list.
     */
    public void refreshIfVisible() {
        if (binding != null && isAdded()) {
            loadNotifications();
        }
    }
}
