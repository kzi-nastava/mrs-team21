package com.drumigo.mobile.data.model.support;

/**
 * Matches backend SupportMessageResponse. Field names must match JSON.
 */
public class SupportMessageResponse {
    public Long id;
    public Long senderId;
    public String senderName;
    public String senderSurname;
    public Long receiverId;
    public String receiverName;
    public String receiverSurname;
    public String content;
    public String createdAt;
}
