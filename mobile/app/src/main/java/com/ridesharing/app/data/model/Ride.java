package com.ridesharing.app.data.model;

public class Ride {
    private Long id;
    private String date;
    private String time;
    private String origin;
    private String destination;
    private String[] passengerInitials;
    private int passengerCount;
    private String status;
    private String cancelledBy;
    private String price;
    private boolean hasPanic;

    public Ride(Long id, String date, String time, String origin, String destination,
                String[] passengerInitials, int passengerCount, String status,
                String cancelledBy, String price, boolean hasPanic) {
        this.id = id;
        this.date = date;
        this.time = time;
        this.origin = origin;
        this.destination = destination;
        this.passengerInitials = passengerInitials;
        this.passengerCount = passengerCount;
        this.status = status;
        this.cancelledBy = cancelledBy;
        this.price = price;
        this.hasPanic = hasPanic;
    }

    public Long getId() { return id; }
    public String getDate() { return date; }
    public String getTime() { return time; }
    public String getOrigin() { return origin; }
    public String getDestination() { return destination; }
    public String[] getPassengerInitials() { return passengerInitials; }
    public int getPassengerCount() { return passengerCount; }
    public String getStatus() { return status; }
    public String getCancelledBy() { return cancelledBy; }
    public String getPrice() { return price; }
    public boolean hasPanic() { return hasPanic; }

    public void setId(Long id) { this.id = id; }
    public void setDate(String date) { this.date = date; }
    public void setTime(String time) { this.time = time; }
    public void setOrigin(String origin) { this.origin = origin; }
    public void setDestination(String destination) { this.destination = destination; }
    public void setPassengerInitials(String[] passengerInitials) { this.passengerInitials = passengerInitials; }
    public void setPassengerCount(int passengerCount) { this.passengerCount = passengerCount; }
    public void setStatus(String status) { this.status = status; }
    public void setCancelledBy(String cancelledBy) { this.cancelledBy = cancelledBy; }
    public void setPrice(String price) { this.price = price; }
    public void setHasPanic(boolean hasPanic) { this.hasPanic = hasPanic; }
}
