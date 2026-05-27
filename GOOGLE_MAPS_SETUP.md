# 🗺️ Google Maps API Setup Guide

## ✅ What I've Created

I've implemented a complete Google Maps-based pricing system:

### 1. **Database Schema** (`schema_pricing.sql`)
- `pricing_rules` table - Base fares, per km rates, per minute rates
- `driver_earnings` table - Track driver payments
- `locations` table - Popular pickup/dropoff locations
- Default pricing for ECONOMY, PREMIUM, ELITE categories

### 2. **Google Maps Service** (`GoogleMapsService.java`)
- Calculates distance and duration between two locations
- Uses Google Maps Distance Matrix API
- Returns distance in km and duration in minutes

### 3. **Pricing Service** (`PricingService.java`)
- Calculates trip price based on:
  - Base fare
  - Distance (km × rate)
  - Time (minutes × rate)
  - Booking fee
- Enforces minimum fare
- Gets pricing rules from database

### 4. **DTOs and Enums**
- `TripCategory` enum (ECONOMY, PREMIUM, ELITE)
- `PriceEstimateDTO` - Price breakdown

---

## 🔑 Step 1: Get Google Maps API Key

### 1.1 Create Google Cloud Project

1. Go to: https://console.cloud.google.com/
2. Click "Select a project" → "New Project"
3. Name: "EthioRide"
4. Click "Create"

### 1.2 Enable Distance Matrix API

1. In Google Cloud Console, go to: **APIs & Services** → **Library**
2. Search for: "Distance Matrix API"
3. Click on it
4. Click "Enable"

### 1.3 Create API Key

1. Go to: **APIs & Services** → **Credentials**
2. Click "Create Credentials" → "API Key"
3. Copy the API key (looks like: `AIzaSyD...`)
4. Click "Restrict Key" (recommended)
5. Under "API restrictions":
   - Select "Restrict key"
   - Check "Distance Matrix API"
6. Click "Save"

### 1.4 Enable Billing

⚠️ **Important:** Google Maps API requires billing to be enabled

1. Go to: **Billing** in Google Cloud Console
2. Link a credit card
3. **Don't worry:** Google gives $200 free credit per month
4. Distance Matrix API costs: $0.005 per request
5. Example: 1000 requests = $5 (well within free tier)

---

## 🔧 Step 2: Configure the API Key

### 2.1 Update GoogleMapsService.java

Open: `server/src/main/java/com/ethioride/server/service/GoogleMapsService.java`

Find this line:
```java
private static final String API_KEY = "YOUR_GOOGLE_MAPS_API_KEY";
```

Replace with your actual API key:
```java
private static final String API_KEY = "AIzaSyD...your-actual-key...";
```

### 2.2 (Better) Use Configuration File

Create: `server/src/main/resources/maps.properties`

```properties
google.maps.api.key=AIzaSyD...your-actual-key...
google.maps.enabled=true
```

Then update `GoogleMapsService.java` to read from properties file.

---

## 📊 Step 3: Setup Database

### 3.1 Run the Pricing Schema

```bash
mysql -u root -p < server/src/main/resources/schema_pricing.sql
```

This creates:
- Pricing rules for ECONOMY, PREMIUM, ELITE
- Driver earnings table
- Popular locations in Addis Ababa

### 3.2 Verify Tables Created

```sql
mysql -u root -p
USE ethioride;

SHOW TABLES;
-- Should show: pricing_rules, driver_earnings, locations

SELECT * FROM pricing_rules;
-- Should show 3 rows (ECONOMY, PREMIUM, ELITE)

SELECT * FROM locations;
-- Should show 8 popular locations in Addis Ababa
```

---

## 🧪 Step 4: Test the System

### 4.1 Add JSON Library

The Google Maps service uses JSON parsing. Add to `lib/` folder:

Download: https://repo1.maven.org/maven2/org/json/json/20231013/json-20231013.jar

Or use this minimal JSON parser (I can create one if needed).

### 4.2 Test Price Calculation

Create a test file: `TestPricing.java`

```java
import com.ethioride.server.service.PricingService;
import com.ethioride.shared.enums.TripCategory;

public class TestPricing {
    public static void main(String[] args) {
        try {
            PricingService pricing = new PricingService();
            
            // Test: Bole to Piassa
            PricingService.PriceEstimate estimate = pricing.calculatePrice(
                "Bole, Addis Ababa",
                "Piassa, Addis Ababa",
                TripCategory.ECONOMY
            );
            
            System.out.println("Price Estimate:");
            System.out.println(estimate);
            System.out.println("Total: " + estimate.getTotalFare() + " ETB");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

Run it:
```bash
javac TestPricing.java
java TestPricing
```

Expected output:
```
[GoogleMaps] Calculating distance: Bole, Addis Ababa → Piassa, Addis Ababa
[GoogleMaps] Distance: 5.23 km
[GoogleMaps] Duration: 12 minutes
[Pricing] Base: 50.0 ETB
[Pricing] Distance: 78.45 ETB
[Pricing] Time: 24.0 ETB
[Pricing] Booking Fee: 10.0 ETB
[Pricing] Total: 162.45 ETB
```

---

## 🚀 Step 5: Integrate with Trip Flow

### 5.1 Add Price Estimate Message Types

In `MessageType.java`:
```java
PRICE_ESTIMATE_REQUEST,
PRICE_ESTIMATE_RESPONSE,
```

### 5.2 Add Server Handler

In `EthioRideServer.java`:
```java
case PRICE_ESTIMATE_REQUEST -> handlePriceEstimate(msg, out);
```

```java
private void handlePriceEstimate(Message msg, ObjectOutputStream out) throws IOException {
    try {
        // Payload format: "origin|destination|category"
        String[] parts = msg.getPayload().toString().split("\\|", 3);
        
        PricingService pricing = new PricingService();
        PricingService.PriceEstimate estimate = pricing.calculatePrice(
            parts[0], parts[1], TripCategory.valueOf(parts[2])
        );
        
        // Convert to DTO
        PriceEstimateDTO dto = new PriceEstimateDTO(
            estimate.getDistanceKm(),
            estimate.getDurationMinutes(),
            estimate.getBaseFare(),
            estimate.getDistanceFare(),
            estimate.getTimeFare(),
            estimate.getBookingFee(),
            estimate.getTotalFare(),
            estimate.getCategory()
        );
        
        out.writeObject(new Message(MessageType.PRICE_ESTIMATE_RESPONSE, dto, "server"));
        out.flush();
    } catch (Exception e) {
        System.err.println("[Server] Price estimate error: " + e.getMessage());
        out.writeObject(new Message(MessageType.ERROR, "Price calculation failed", "server"));
        out.flush();
    }
}
```

### 5.3 Client Side (Passenger App)

When user selects pickup and dropoff:
```java
// Send price estimate request
String payload = pickup + "|" + dropoff + "|" + category;
Message request = new Message(MessageType.PRICE_ESTIMATE_REQUEST, payload, userId);
client.send(request);

// Receive response
// Response will be PriceEstimateDTO with full breakdown
```

---

## 💰 Pricing Formula

```
Base Fare:      50 ETB (ECONOMY), 80 ETB (PREMIUM), 120 ETB (ELITE)
Distance Fare:  distance_km × per_km_rate
Time Fare:      duration_min × per_minute_rate
Booking Fee:    10 ETB (ECONOMY), 15 ETB (PREMIUM), 20 ETB (ELITE)
─────────────────────────────────────────────────────────────────
Subtotal:       Base + Distance + Time
Total:          MAX(Subtotal + Booking Fee, Minimum Fare)
```

### Example: 5km trip, 12 minutes, ECONOMY

```
Base Fare:      50.00 ETB
Distance:       5 km × 15 ETB/km = 75.00 ETB
Time:           12 min × 2 ETB/min = 24.00 ETB
Booking Fee:    10.00 ETB
─────────────────────────────────────────
Total:          159.00 ETB
```

---

## 🎯 Admin Features

### Admin Can:

1. **View/Edit Pricing Rules**
   - Change base fare
   - Change per km rate
   - Change per minute rate
   - Change minimum fare
   - Change booking fee

2. **Add Popular Locations**
   - Name, address, coordinates
   - For autocomplete in passenger app

3. **View Driver Earnings**
   - Total earnings per driver
   - Commission deductions
   - Payment status

---

## 📝 Next Steps

1. ✅ Get Google Maps API key
2. ✅ Enable Distance Matrix API
3. ✅ Add API key to GoogleMapsService.java
4. ✅ Run schema_pricing.sql
5. ✅ Add JSON library to lib/
6. ✅ Test price calculation
7. ✅ Add message types for price estimates
8. ✅ Integrate with passenger app
9. ✅ Create admin UI for pricing rules

---

## 🐛 Troubleshooting

### "API key not valid"
- Check API key is correct
- Check Distance Matrix API is enabled
- Check API restrictions allow Distance Matrix API

### "REQUEST_DENIED"
- Billing not enabled
- API not enabled
- API key restrictions too strict

### "ZERO_RESULTS"
- Location not found
- Try more specific address
- Use coordinates instead: "9.0333,38.7469"

### High API costs
- Cache common routes
- Use predefined routes for popular trips
- Only call API for custom locations

---

*Setup Guide Created: 2026-05-07*
