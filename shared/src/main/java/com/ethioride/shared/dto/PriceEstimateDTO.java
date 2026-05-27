package com.ethioride.shared.dto;

import com.ethioride.shared.enums.TripCategory;
import java.io.Serializable;

public class PriceEstimateDTO implements Serializable {
    private double distanceKm;
    private double durationMinutes;
    private double baseFare;
    private double distanceFare;
    private double timeFare;
    private double bookingFee;
    private double totalFare;
    private TripCategory category;
    
    public PriceEstimateDTO() {}
    
    public PriceEstimateDTO(double distanceKm, double durationMinutes, double baseFare,
                           double distanceFare, double timeFare, double bookingFee,
                           double totalFare, TripCategory category) {
        this.distanceKm = distanceKm;
        this.durationMinutes = durationMinutes;
        this.baseFare = baseFare;
        this.distanceFare = distanceFare;
        this.timeFare = timeFare;
        this.bookingFee = bookingFee;
        this.totalFare = totalFare;
        this.category = category;
    }
    
    public double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(double distanceKm) { this.distanceKm = distanceKm; }
    
    public double getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(double durationMinutes) { this.durationMinutes = durationMinutes; }
    
    public double getBaseFare() { return baseFare; }
    public void setBaseFare(double baseFare) { this.baseFare = baseFare; }
    
    public double getDistanceFare() { return distanceFare; }
    public void setDistanceFare(double distanceFare) { this.distanceFare = distanceFare; }
    
    public double getTimeFare() { return timeFare; }
    public void setTimeFare(double timeFare) { this.timeFare = timeFare; }
    
    public double getBookingFee() { return bookingFee; }
    public void setBookingFee(double bookingFee) { this.bookingFee = bookingFee; }
    
    public double getTotalFare() { return totalFare; }
    public void setTotalFare(double totalFare) { this.totalFare = totalFare; }
    
    public TripCategory getCategory() { return category; }
    public void setCategory(TripCategory category) { this.category = category; }
}
