package com.drumigo.mobile.ui.support;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.drumigo.mobile.R;
import com.drumigo.mobile.data.api.ApiClient;
import com.drumigo.mobile.data.api.SupportApiService;
import com.drumigo.mobile.data.model.support.SupportMessageCreateRequest;
import com.drumigo.mobile.data.model.support.SupportMessageResponse;
import com.drumigo.mobile.databinding.FragmentAdminSupportChatBinding;
import com.drumigo.mobile.session.SessionManager;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminSupportChatFragment extends Fragment {

    private static final long POLL_INTERVAL_MS = 2500L;

    private FragmentAdminSupportChatBinding binding;
    private SupportApiService supportApiService;
    private AdminSupportMessageAdapter adapter;
    private Handler pollHandler;
    private long conversationUserId;
    private boolean isSending;

    private final Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            loadMessages(false);
            if (pollHandler != null) {
                pollHandler.postDelayed(this, POLL_INTERVAL_MS);
            }
        }
    };

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAdminSupportChatBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        supportApiService = ApiClient.getSupportApiService();
        pollHandler = new Handler(Looper.getMainLooper());

        SessionManager sessionManager = SessionManager.getInstance(requireContext());
        long currentUserId = sessionManager.getUserId();
        adapter = new AdminSupportMessageAdapter(currentUserId);
        binding.adminSupportChatRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.adminSupportChatRecyclerView.setAdapter(adapter);

        Bundle args = getArguments();
        conversationUserId = args != null ? args.getLong("userId", 0L) : 0L;
        String userName = args != null ? args.getString("userName", "") : "";
        String userSurname = args != null ? args.getString("userSurname", "") : "";
        String fullName = (safe(userName) + " " + safe(userSurname)).trim();
        binding.adminSupportChatTitle.setText(
            fullName.isEmpty()
                ? getString(R.string.admin_support_chat_title_fallback)
                : getString(R.string.admin_support_chat_title, fullName)
        );

        binding.adminSupportChatSendButton.setOnClickListener(v -> {
            String content = binding.adminSupportChatInput.getText() != null
                ? binding.adminSupportChatInput.getText().toString().trim()
                : "";
            sendMessage(content);
        });

        if (conversationUserId <= 0) {
            binding.adminSupportChatLoading.setVisibility(View.GONE);
            binding.adminSupportChatError.setVisibility(View.VISIBLE);
            binding.adminSupportChatError.setText(R.string.admin_support_load_chat_error);
            binding.adminSupportChatSendButton.setEnabled(false);
            return;
        }

        loadMessages(true);
        pollHandler.postDelayed(pollRunnable, POLL_INTERVAL_MS);
    }

    @Override
    public void onDestroyView() {
        if (pollHandler != null) {
            pollHandler.removeCallbacks(pollRunnable);
        }
        binding = null;
        super.onDestroyView();
    }

    private void loadMessages(boolean showLoading) {
        if (binding == null || conversationUserId <= 0) return;
        if (showLoading) {
            binding.adminSupportChatLoading.setVisibility(View.VISIBLE);
            binding.adminSupportChatRecyclerView.setVisibility(View.GONE);
            binding.adminSupportChatError.setVisibility(View.GONE);
        }

        supportApiService.getConversation(conversationUserId).enqueue(new Callback<List<SupportMessageResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<SupportMessageResponse>> call,
                                   @NonNull Response<List<SupportMessageResponse>> response) {
                if (binding == null) return;
                binding.adminSupportChatLoading.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    adapter.setMessages(response.body());
                    binding.adminSupportChatRecyclerView.setVisibility(View.VISIBLE);
                    binding.adminSupportChatError.setVisibility(View.GONE);
                    if (!response.body().isEmpty()) {
                        binding.adminSupportChatRecyclerView.scrollToPosition(response.body().size() - 1);
                    }
                } else {
                    binding.adminSupportChatRecyclerView.setVisibility(View.GONE);
                    binding.adminSupportChatError.setVisibility(View.VISIBLE);
                    binding.adminSupportChatError.setText(R.string.admin_support_load_chat_error);
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<SupportMessageResponse>> call, @NonNull Throwable t) {
                if (binding == null) return;
                binding.adminSupportChatLoading.setVisibility(View.GONE);
                binding.adminSupportChatRecyclerView.setVisibility(View.GONE);
                binding.adminSupportChatError.setVisibility(View.VISIBLE);
                binding.adminSupportChatError.setText(R.string.admin_support_load_chat_error);
            }
        });
    }

    private void sendMessage(String content) {
        if (binding == null || conversationUserId <= 0 || content.isEmpty() || isSending) {
            return;
        }
        isSending = true;
        binding.adminSupportChatSendButton.setEnabled(false);

        SupportMessageCreateRequest request = new SupportMessageCreateRequest(conversationUserId, content);
        supportApiService.sendMessage(request).enqueue(new Callback<SupportMessageResponse>() {
            @Override
            public void onResponse(@NonNull Call<SupportMessageResponse> call,
                                   @NonNull Response<SupportMessageResponse> response) {
                isSending = false;
                if (binding != null) {
                    binding.adminSupportChatSendButton.setEnabled(true);
                }
                if (response.isSuccessful()) {
                    if (binding != null) {
                        binding.adminSupportChatInput.setText("");
                    }
                    loadMessages(false);
                } else {
                    Toast.makeText(requireContext(), R.string.admin_support_send_error, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<SupportMessageResponse> call, @NonNull Throwable t) {
                isSending = false;
                if (binding != null) {
                    binding.adminSupportChatSendButton.setEnabled(true);
                }
                Toast.makeText(requireContext(), R.string.admin_support_send_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
