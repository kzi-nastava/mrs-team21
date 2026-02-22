package com.drumigo.mobile.ui.support;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.drumigo.mobile.R;
import com.drumigo.mobile.data.api.ApiClient;
import com.drumigo.mobile.data.api.SupportApiService;
import com.drumigo.mobile.data.model.support.SupportConversationSummaryResponse;
import com.drumigo.mobile.databinding.FragmentAdminSupportConversationsBinding;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminSupportConversationsFragment extends Fragment {

    private static final long POLL_INTERVAL_MS = 2500L;

    private FragmentAdminSupportConversationsBinding binding;
    private SupportApiService supportApiService;
    private AdminSupportConversationAdapter adapter;
    private Handler pollHandler;
    private final Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            loadConversations(false);
            if (pollHandler != null) {
                pollHandler.postDelayed(this, POLL_INTERVAL_MS);
            }
        }
    };

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAdminSupportConversationsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        supportApiService = ApiClient.getSupportApiService();
        pollHandler = new Handler(Looper.getMainLooper());

        adapter = new AdminSupportConversationAdapter(this::openConversation);
        binding.adminSupportConversationRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.adminSupportConversationRecyclerView.setAdapter(adapter);

        loadConversations(true);
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

    private void loadConversations(boolean showLoading) {
        if (binding == null) return;
        if (showLoading) {
            binding.adminSupportConversationsLoading.setVisibility(View.VISIBLE);
            binding.adminSupportConversationRecyclerView.setVisibility(View.GONE);
            binding.adminSupportConversationsError.setVisibility(View.GONE);
            binding.adminSupportConversationsEmpty.setVisibility(View.GONE);
        }

        supportApiService.getConversations().enqueue(new Callback<List<SupportConversationSummaryResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<SupportConversationSummaryResponse>> call,
                                   @NonNull Response<List<SupportConversationSummaryResponse>> response) {
                if (binding == null) return;
                binding.adminSupportConversationsLoading.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    List<SupportConversationSummaryResponse> list = response.body();
                    adapter.setConversations(list);
                    boolean empty = list.isEmpty();
                    binding.adminSupportConversationRecyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
                    binding.adminSupportConversationsEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
                    binding.adminSupportConversationsError.setVisibility(View.GONE);
                } else {
                    binding.adminSupportConversationRecyclerView.setVisibility(View.GONE);
                    binding.adminSupportConversationsEmpty.setVisibility(View.GONE);
                    binding.adminSupportConversationsError.setVisibility(View.VISIBLE);
                    binding.adminSupportConversationsError.setText(R.string.admin_support_load_error);
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<SupportConversationSummaryResponse>> call, @NonNull Throwable t) {
                if (binding == null) return;
                binding.adminSupportConversationsLoading.setVisibility(View.GONE);
                binding.adminSupportConversationRecyclerView.setVisibility(View.GONE);
                binding.adminSupportConversationsEmpty.setVisibility(View.GONE);
                binding.adminSupportConversationsError.setVisibility(View.VISIBLE);
                binding.adminSupportConversationsError.setText(R.string.admin_support_load_error);
            }
        });
    }

    private void openConversation(SupportConversationSummaryResponse conversation) {
        if (conversation == null || conversation.userId == null) {
            return;
        }
        Bundle args = new Bundle();
        args.putLong("userId", conversation.userId);
        args.putString("userName", conversation.userName);
        args.putString("userSurname", conversation.userSurname);
        NavController navController = NavHostFragment.findNavController(this);
        navController.navigate(R.id.adminSupportChatFragment, args);
    }
}
