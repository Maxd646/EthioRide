package com.ethioride.driver.ui;

import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

import java.net.URL;
import java.util.function.BiConsumer;

/**
 * Driver-side Leaflet map WebView wrapper.
 * Shows driver's own position + active trip route.
 */
public class MapView extends StackPane {

    private final WebView  webView;
    private final WebEngine engine;
    private boolean        ready = false;

    private BiConsumer<String, double[]> onLocationSelected;

    public class JavaBridge {
        public void onLocationSelected(String name, double lat, double lng) {
            if (onLocationSelected != null)
                Platform.runLater(() -> onLocationSelected.accept(name, new double[]{lat, lng}));
        }
        public void onMapClick(double lat, double lng) {}
    }

    public MapView() {
        webView = new WebView();
        engine  = webView.getEngine();
        webView.setMaxWidth(Double.MAX_VALUE);
        webView.setMaxHeight(Double.MAX_VALUE);
        getChildren().add(webView);
        setMaxWidth(Double.MAX_VALUE);
        setMaxHeight(Double.MAX_VALUE);

        engine.getLoadWorker().stateProperty().addListener((obs, old, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                JSObject win = (JSObject) engine.executeScript("window");
                win.setMember("javaMap", new JavaBridge());
                ready = true;
            }
        });

        URL mapUrl = getClass().getResource("/ui/map.html");
        if (mapUrl != null) engine.load(mapUrl.toExternalForm());
    }

    public void setOnLocationSelected(BiConsumer<String, double[]> cb) { this.onLocationSelected = cb; }

    public void setDriverPosition(double lat, double lng) {
        String name = "You";
        try {
            com.ethioride.driver.state.DriverSessionState state =
                com.ethioride.driver.state.DriverSessionState.getInstance();
            if (state.getCurrentDriver() != null && state.getCurrentDriver().getFullName() != null) {
                name = state.getCurrentDriver().getFullName();
            }
        } catch (Exception ignored) {}
        final String driverName = name;
        js("addDriverMarker('self'," + lat + "," + lng + ",'" + esc(driverName) + "','ONLINE')");
        js("moveTo(" + lat + "," + lng + ",15)");
    }

    public void showTripRoute(double driverLat, double driverLng,
                              double pickupLat, double pickupLng,
                              double dropoffLat, double dropoffLng,
                              String pickupLabel, String dropoffLabel) {
        js("setPickup(" + pickupLat + "," + pickupLng + ",'" + esc(pickupLabel) + "')");
        js("setDropoff(" + dropoffLat + "," + dropoffLng + ",'" + esc(dropoffLabel) + "')");
        js("drawRoute(" + driverLat + "," + driverLng + "," + pickupLat + "," + pickupLng + ")");
    }

    public void clearRoute() { js("clearRoute()"); }

    public void moveTo(double lat, double lng, int zoom) {
        js("moveTo(" + lat + "," + lng + "," + zoom + ")");
    }

    private void js(String script) {
        if (!ready) return;
        Platform.runLater(() -> {
            try { engine.executeScript(script); } catch (Exception ignored) {}
        });
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("'", "\\'").replace("\n", " ");
    }
}
