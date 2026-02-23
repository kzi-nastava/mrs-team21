package com.drumigo.mobile.data.model.ride;

public class RideCancelByDriverRequest {
    public String cancelReasonType;
    public String reason;

    public RideCancelByDriverRequest(String cancelReasonType, String reason) {
        this.cancelReasonType = cancelReasonType;
        this.reason = reason;
    }
}
