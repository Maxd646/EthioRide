package com.ethioride.shared.dto;

import com.ethioride.shared.enums.RideCategory;
import java.io.Serializable;

public class PriceEstimateDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private double distanceKm;
    private double durationMinutes;
    private double baseFare;
    private double distanceFare;
    private double timeFare;
    private double bookingFee;
    private double totalFare;
    private RideCategory category;
    // Geocoded coordinates — used by the client to set pickup/dropoff lat/lng on TripRequestDTO
    private double originLat;
    private double originLng;
    private double destLat;
    private double destLng;

    public PriceEstimateDTO() {}

    public PriceEstimateDTO(double distanceKm, double durationMinutes, double baseFare,
                            double distanceFare, double timeFare, double bookingFee,
                            double totalFare, RideCategory category) {
        this.distanceKm      = distanceKm;
        this.durationMinutes = durationMinutes;
        this.baseFare        = baseFare;
        this.distanceFare    = distanceFare;
        this.timeFare        = timeFare;
        this.bookingFee      = bookingFee;
        this.totalFare       = totalFare;
        this.category        = category;
    }

    public double      getDistanceKm()      { return distanceKm; }
    public void        setDistanceKm(double v)      { this.distanceKm = v; }
    public double      getDurationMinutes() { return durationMinutes; }
    public void        setDurationMinutes(double v) { this.durationMinutes = v; }
    public double      getBaseFare()        { return baseFare; }
    public void        setBaseFare(double v)        { this.baseFare = v; }
    public double      getDistanceFare()    { return distanceFare; }
    public void        setDistanceFare(double v)    { this.distanceFare = v; }
    public double      getTimeFare()        { return timeFare; }
    public void        setTimeFare(double v)        { this.timeFare = v; }
    public double      getBookingFee()      { return bookingFee; }
    public void        setBookingFee(double v)      { this.bookingFee = v; }
    public double      getTotalFare()       { return totalFare; }
    public void        setTotalFare(double v)       { this.totalFare = v; }
    public RideCategory getCategory()       { return category; }
    public void        setCategory(RideCategory v)  { this.category = v; }
    public double      getOriginLat()       { return originLat; }
    public void        setOriginLat(double v)       { this.originLat = v; }
    public double      getOriginLng()       { return originLng; }
    public void        setOriginLng(double v)       { this.originLng = v; }
    public double      getDestLat()         { return destLat; }
    public void        setDestLat(double v)         { this.destLat = v; }
    public double      getDestLng()         { return destLng; }
    public void        setDestLng(double v)         { this.destLng = v; }
}
