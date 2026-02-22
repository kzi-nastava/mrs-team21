package com.drumigo.mobile.ui.support;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import com.drumigo.mobile.data.api.SupportApiService;
import com.drumigo.mobile.data.model.support.SupportMessageCreateRequest;
import com.drumigo.mobile.data.model.support.SupportMessageResponse;
import com.drumigo.mobile.databinding.FragmentSupportBinding;
import com.drumigo.mobile.session.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Support chat: user (passenger/driver) talks to admin support. Uses same API as frontend: my-chat, messages.
 */
public class SupportFragment extends Fragment {

    private static final long POLL_INTERVAL_MS = 2500L;

    private FragmentSupportBinding binding;
    private SupportApiService supportApiService;
    private SessionManager sessionManager;
    private SupportMessageAdapter adapter;
    private Handler pollHandler;
    private final Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            loadMessagesSilent();
            if (pollHandler != null) pollHandler.postDelayed(this, POLL_INTERVAL_MS);
        }
    };
    private boolean isSending;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSupportBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        supportApiService = ApiClient.getSupportApiService();
        sessionManager = SessionManager.getInstance(requireContext());
        long userId = sessionManager.getUserId();

        RecyclerView recyclerView = binding.supportRecyclerView;
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new SupportMessageAdapter(userId);
        recyclerView.setAdapter(adapter);

        MaterialButton sendButton = binding.supportSendButton;
        TextInputEditText input = binding.supportMessageInput;
        sendButton.setOnClickListener(v -> sendMessage(input.getText() != null ? input.getText().toString().trim() : ""));
        pollHandler = new Handler(Looper.getMainLooper());

        String role = sessionManager.getRole() != null ? sessionManager.getRole().toUpperCase(Locale.ENGLISH) : "";
        if ("ADMIN".equals(role)) {
            setLoading(false);
            binding.supportEmptyText.setText(getString(R.string.support_admin_use_web));
            binding.supportEmptyText.setVisibility(View.VISIBLE);
            binding.supportMessageInput.setVisibility(View.GONE);
            binding.supportSendButton.setVisibility(View.GONE);
            binding.supportRecyclerView.setVisibility(View.GONE);
        } else {
            loadMessages(true);
            startPolling();
        }
    }

    @Override
    public void onDestroyView() {
        stopPolling();
        binding = null;
        super.onDestroyView();
    }

    private void loadMessages(boolean showLoading) {
        if (showLoading) {
            setLoading(true);
            setError(null);
        }
        supportApiService.getMyChat().enqueue(new Callback<List<SupportMessageResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<SupportMessageResponse>> call,
                                  @NonNull Response<List<SupportMessageResponse>> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    adapter.setMessages(response.body());
                    updateEmptyVisibility(response.body().isEmpty());
                } else {
                    setError(getString(R.string.support_error));
                    updateEmptyVisibility(true);
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<SupportMessageResponse>> call, @NonNull Throwable t) {
                setLoading(false);
                setError(getString(R.string.support_error));
                updateEmptyVisibility(adapter.getItemCount() == 0);
            }
        });
    }

    private void loadMessagesSilent() {
        if (binding == null) return;
        supportApiService.getMyChat().enqueue(new Callback<List<SupportMessageResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<SupportMessageResponse>> call,
                                  @NonNull Response<List<SupportMessageResponse>> response) {
                if (binding == null) return;
                if (response.isSuccessful() && response.body() != null) {
                    adapter.setMessages(response.body());
                    updateEmptyVisibility(response.body().isEmpty());
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<SupportMessageResponse>> call, @NonNull Throwable t) {}
        });
    }

    private void sendMessage(String content) {
        if (content.isEmpty() || isSending || binding == null) return;
        isSending = true;
        binding.supportSendButton.setEnabled(false);
        SupportMessageCreateRequest request = new SupportMessageCreateRequest(null, content);
        supportApiService.sendMessage(request).enqueue(new Callback<SupportMessageResponse>() {
            @Override
            public void onResponse(@NonNull Call<SupportMessageResponse> call,
                                  @NonNull Response<SupportMessageResponse> response) {
                isSending = false;
                if (binding != null) {
                    binding.supportSendButton.setEnabled(true);
                    binding.supportMessageInput.setText("");
                }
                if (response.isSuccessful()) {
                    loadMessagesSilent();
                } else {
                    Toast.makeText(requireContext(), R.string.support_error, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<SupportMessageResponse> call, @NonNull Throwable t) {
                isSending = false;
                if (binding != null) binding.supportSendButton.setEnabled(true);
                Toast.makeText(requireContext(), R.string.support_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setLoading(boolean loading) {
        if (binding == null) return;
        binding.supportLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.supportRecyclerView.setVisibility(loading ? View.GONE : View.VISIBLE);
    }

    private void setError(String message) {
        if (binding == null) return;
        binding.supportErrorText.setText(message != null ? message : "");
        binding.supportErrorText.setVisibility(message != null && !message.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void updateEmptyVisibility(boolean empty) {
        if (binding == null) return;
        binding.supportEmptyText.setVisibility(empty && !binding.supportLoading.isShown() ? View.VISIBLE : View.GONE);
    }

    private void startPolling() {
        if (pollHandler != null) {
            pollHandler.postDelayed(pollRunnable, POLL_INTERVAL_MS);
        }
    }

    private void stopPolling() {
        if (pollHandler != null) {
            pollHandler.removeCallbacks(pollRunnable);
        }
    }
}
