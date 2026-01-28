package com.ftn.drumigo.dto.ride.request;

import com.ftn.drumigo.domain.enums.CancelReasonType;
import jakarta.validation.constraints.NotNull;

public record RideCancelByDriverRequest(
    @NotNull(message = "Cancel reason is required")
    CancelReasonType cancelReasonType,

    String reason
) {}
