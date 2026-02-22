package com.drumigo.mobile.data.model.support;

/**
 * Request body for POST /api/support/messages. For user-to-support, receiverId is null.
 */
public class SupportMessageCreateRequest {
    public Long receiverId;
    public String content;

    public SupportMessageCreateRequest(Long receiverId, String content) {
        this.receiverId = receiverId;
        this.content = content;
    }
}
