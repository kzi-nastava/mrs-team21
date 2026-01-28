package com.ftn.drumigo.dto.ride.request;

import com.ftn.drumigo.domain.enums.CancelReasonType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RideCancelByDriverRequest(
    @NotNull(message = "Cancel reason is required")
    CancelReasonType cancelReasonType,

    @Size(max = 500, message = "Cancel reason must not exceed 500 characters")
    String reason
) {}
