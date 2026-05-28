package com.ethioride.passenger.ui;

import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

import java.net.URL;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Reusable JavaFX WebView wrapper around the Leaflet map.
 *
 * Usage:
 *   MapView mv = new MapView();
 *   mv.setOnLocationSelected((name, latLng) -> { ... });
 *   mv.setOnMapClick((lat, lng) -> { ... });
 *   root.setCenter(mv);
 *
 * Then call:
 *   mv.addDriverMarker(id, lat, lng, name, "ONLINE");
 *   mv.setPickup(lat, lng, label);
 *   mv.setDropoff(lat, lng, label);
 *   mv.drawRoute(pLat, pLng, dLat, dLng);
 *   mv.moveTo(lat, lng, zoom);
 */
public class MapView extends StackPane {

    private final WebView  webView;
    private final WebEngine engine;
    private boolean        ready = false;

    // Callbacks from JS → Java
    private BiConsumer<String, double[]> onLocationSelected;
    private BiConsumer<Double, Double>   onMapClick;

    /** Java object exposed to JavaScript as window.javaMap */
    public class JavaBridge {
        public void onLocationSelected(String name, double lat, double lng) {
            if (onLocationSelected != null)
                Platform.runLater(() -> onLocationSelected.accept(name, new double[]{lat, lng}));
        }
        public void onMapClick(double lat, double lng) {
            if (onMapClick != null)
                Platform.runLater(() -> onMapClick.accept(lat, lng));
        }
    }

    public MapView() {
        webView = new WebView();
        engine  = webView.getEngine();
        webView.setMaxWidth(Double.MAX_VALUE);
        webView.setMaxHeight(Double.MAX_VALUE);
        getChildren().add(webView);
        setMaxWidth(Double.MAX_VALUE);
        setMaxHeight(Double.MAX_VALUE);

        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                // Inject Java bridge into JS window
                JSObject win = (JSObject) engine.executeScript("window");
                win.setMember("javaMap", new JavaBridge());
                ready = true;
            }
        });

        // Load the map HTML from classpath resources
        URL mapUrl = getClass().getResource("/ui/map.html");
        if (mapUrl != null) {
            engine.load(mapUrl.toExternalForm());
        } else {
            // Fallback: inline minimal map if resource not found
            engine.loadContent(fallbackHtml());
        }
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public void setOnLocationSelected(BiConsumer<String, double[]> cb) { this.onLocationSelected = cb; }
    public void setOnMapClick(BiConsumer<Double, Double> cb)           { this.onMapClick = cb; }

    public void addDriverMarker(String id, double lat, double lng, String name, String status) {
        js("addDriverMarker('" + esc(id) + "'," + lat + "," + lng + ",'" + esc(name) + "','" + status + "')");
    }

    public void removeDriverMarker(String id) {
        js("removeDriverMarker('" + esc(id) + "')");
    }

    public void clearDriverMarkers() { js("clearDriverMarkers()"); }

    public void setPickup(double lat, double lng, String label) {
        js("setPickup(" + lat + "," + lng + ",'" + esc(label) + "')");
    }

    public void setDropoff(double lat, double lng, String label) {
        js("setDropoff(" + lat + "," + lng + ",'" + esc(label) + "')");
    }

    public void moveTo(double lat, double lng, int zoom) {
        js("moveTo(" + lat + "," + lng + "," + zoom + ")");
    }

    public void drawRoute(double pickupLat, double pickupLng, double dropoffLat, double dropoffLng) {
        js("drawRoute(" + pickupLat + "," + pickupLng + "," + dropoffLat + "," + dropoffLng + ")");
    }

    public void clearRoute() { js("clearRoute()"); }

    public void setSearchText(String text) {
        js("document.getElementById('search-input').value='" + esc(text) + "'");
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private void js(String script) {
        if (!ready) return;
        Platform.runLater(() -> {
            try { engine.executeScript(script); }
            catch (Exception e) { /* ignore JS errors */ }
        });
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("'", "\\'").replace("\n", " ");
    }

    private String fallbackHtml() {
        return "<html><body style='background:#0a0e1a;color:#f1f5f9;display:flex;" +
               "align-items:center;justify-content:center;height:100vh;font-family:Arial'>" +
               "<div>Map requires internet connection</div></body></html>";
    }
}
