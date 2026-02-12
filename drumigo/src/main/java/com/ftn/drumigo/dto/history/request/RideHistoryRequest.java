package com.ftn.drumigo.dto.history.request;

import com.ftn.drumigo.domain.enums.RideStatus;
import com.ftn.drumigo.exception.BadRequestException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class RideHistoryRequest {

    private Instant from;
    private Instant to;
    private String status;
    private Boolean hasPanic;
    private int page = 0;
    private int size = 10;
    private String sort = "requestedAt,desc";

    public RideHistoryRequest() {}

    public RideHistoryRequest(Instant from, Instant to, String status, Boolean hasPanic,
                             int page, int size, String sort) {
        this.from = from;
        this.to = to;
        this.status = status;
        this.hasPanic = hasPanic;
        this.page = page;
        this.size = size;
        this.sort = sort;
    }

    public Instant getFrom() {
        return from;
    }

    public void setFrom(Instant from) {
        this.from = from;
    }

    public Instant getTo() {
        return to;
    }

    public void setTo(Instant to) {
        this.to = to;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getHasPanic() {
        return hasPanic;
    }

    public void setHasPanic(Boolean hasPanic) {
        this.hasPanic = hasPanic;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public String getSort() {
        return sort;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }

    /**
     * Parse the comma-separated status string into a list of RideStatus enums.
     * Returns null if status is null or empty.
     *
     * @return List of RideStatus or null
     * @throws BadRequestException if any status value is invalid
     */
    public List<RideStatus> parseStatuses() {
        if (status == null || status.isEmpty()) {
            return null;
        }
        try {
            return Arrays.stream(status.split(","))
                    .map(String::trim)
                    .map(RideStatus::valueOf)
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid status value. Valid values: " +
                    Arrays.toString(RideStatus.values()));
        }
    }

    /**
     * Convert request parameters to a Spring Data Pageable.
     * Uses the sort parameter to determine sort direction and field.
     * Defaults to sorting by requestedAt in descending order.
     *
     * @return Pageable with pagination and sort configuration
     */
    public Pageable toPageable() {
        String[] sortParams = sort == null ? new String[]{"requestedAt", "desc"} : sort.split(",");
        Sort.Direction direction = sortParams.length > 1 && sortParams[1].equalsIgnoreCase("asc")
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sortObj = Sort.by(direction, sortParams[0]);
        return PageRequest.of(page, size, sortObj);
    }
}

