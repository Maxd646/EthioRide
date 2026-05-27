package com.ethioride.passenger.ui;

import com.ethioride.passenger.network.ServerConnection;
import com.ethioride.passenger.state.SessionState;
import com.ethioride.shared.dto.TripRequestDTO;
import com.ethioride.shared.enums.TripStatus;
import com.ethioride.shared.protocol.Message;
import com.ethioride.shared.protocol.MessageType;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.util.List;

public class RideHistoryScreen {
    private final Stage stage;
    private VBox tripList;
    private Label lblSummary;

    public RideHistoryScreen(Stage stage) { this.stage = stage; }

    public void show() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color:#0a0e1a;");
        root.setLeft(buildSidebar());
        root.setCenter(buildContent());
        stage.setScene(new Scene(root, 900, 640));
        stage.setResizable(true);
        stage.show();
        loadHistory();
    }

    private VBox buildSidebar() {
        VBox s = new VBox(0);
        s.setPrefWidth(200);
        s.setStyle("-fx-background-color:#0d1526;-fx-border-color:#1e3a5f;-fx-border-width:0 1 0 0;");
        Label logo = new Label("🚕 EthioRide");
        logo.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        logo.setTextFill(Color.web("#3b82f6"));
        logo.setPadding(new Insets(24, 20, 20, 20));
        Button btnMap   = navBtn("🗺  Map");
        Button btnHist  = navBtn("🕐  Ride History");
        Button btnPay   = navBtn("💳  Payments");
        Button btnPromo = navBtn("🏷  Promotions");
        Button btnSet   = navBtn("⚙  Settings");
        btnHist.setStyle(btnHist.getStyle() + "-fx-background-color:#1e3a5f;");
        btnMap.setOnAction(e   -> new MainScreen(stage).show());
        btnPay.setOnAction(e   -> new PaymentsScreen(stage).show());
        btnPromo.setOnAction(e -> new PromotionsScreen(stage).show());
        btnSet.setOnAction(e   -> new SettingsScreen(stage).show());
        Region sp = new Region(); VBox.setVgrow(sp, Priority.ALWAYS);
        Button btnOut = navBtn("↩  Sign Out");
        btnOut.setOnAction(e -> { SessionState.getInstance().clear(); new LoginScreen(stage).show(); });
        s.getChildren().addAll(logo, btnMap, btnHist, btnPay, btnPromo, btnSet, sp, btnOut);
        return s;
    }

    private ScrollPane buildContent() {
        VBox content = new VBox(20);
        content.setPadding(new Insets(30));
        content.setStyle("-fx-background-color:#0a0e1a;");

        Label title = new Label("Ride History");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        title.setTextFill(Color.web("#f1f5f9"));

        lblSummary = new Label("Loading...");
        lblSummary.setFont(Font.font("Arial", 13));
        lblSummary.setTextFill(Color.web("#94a3b8"));

        tripList = new VBox(8);

        content.getChildren().addAll(title, lblSummary, tripList);
        ScrollPane sp = new ScrollPane(content);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color:#0a0e1a;-fx-background:#0a0e1a;");
        return sp;
    }

    @SuppressWarnings("unchecked")
    private void loadHistory() {
        String passengerId = SessionState.getInstance().isLoggedIn()
            ? SessionState.getInstance().getCurrentUser().getId() : null;

        Thread t = new Thread(() -> {
            try {
                ServerConnection conn = new ServerConnection();
                conn.connect();
                Message resp = conn.sendAndReceive(
                    new Message(MessageType.TRIP_LIST_REQUEST, null, "passenger"));
                conn.close();

                Platform.runLater(() -> {
                    if (resp.getType() == MessageType.TRIP_LIST_RESPONSE) {
                        List<TripRequestDTO> all = (List<TripRequestDTO>) resp.getPayload();
                        List<TripRequestDTO> mine = all.stream()
                            .filter(tr -> passengerId != null && passengerId.equals(tr.getPassengerId()))
                            .toList();
                        renderTrips(mine);
                    } else {
                        lblSummary.setText("Could not load trip history.");
                    }
                });
            } catch (Exception ex) {
                Platform.runLater(() -> lblSummary.setText("Server offline — cannot load history."));
            }
        }, "ride-history-thread");
        t.setDaemon(true);
        t.start();
    }

    private void renderTrips(List<TripRequestDTO> trips) {
        tripList.getChildren().clear();
        if (trips.isEmpty()) {
            lblSummary.setText("No trips yet.");
            Label empty = new Label("You haven't taken any rides yet.");
            empty.setTextFill(Color.web("#475569"));
            empty.setFont(Font.font("Arial", 14));
            tripList.getChildren().add(empty);
            return;
        }

        long completed = trips.stream().filter(t -> t.getStatus() == TripStatus.COMPLETED).count();
        double total   = trips.stream().filter(t -> t.getStatus() == TripStatus.COMPLETED)
                              .mapToDouble(TripRequestDTO::getFare).sum();
        lblSummary.setText(String.format("%d trips  •  %d completed  •  ETB %.2f spent",
            trips.size(), completed, total));

        for (TripRequestDTO trip : trips) {
            tripList.getChildren().add(buildTripCard(trip));
        }
    }

    private HBox buildTripCard(TripRequestDTO trip) {
        HBox card = new HBox(16);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle("-fx-background-color:#0d1526;-fx-background-radius:10px;" +
                      "-fx-border-color:#1e3a5f;-fx-border-radius:10px;-fx-padding:14;");

        // Status icon
        String icon = switch (trip.getStatus() == null ? TripStatus.PENDING : trip.getStatus()) {
            case COMPLETED  -> "✅";
            case CANCELLED  -> "❌";
            case IN_PROGRESS -> "🚗";
            default          -> "🕐";
        };
        Label lblIcon = new Label(icon);
        lblIcon.setFont(Font.font("Arial", 22));

        // Route info
        VBox info = new VBox(4);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label lblRoute = new Label(trip.getPickupLocation() + "  →  " + trip.getDropoffLocation());
        lblRoute.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        lblRoute.setTextFill(Color.web("#f1f5f9"));

        String cat = trip.getCategory() != null ? trip.getCategory().name() : "ECONOMY";
        String status = trip.getStatus() != null ? trip.getStatus().name() : "PENDING";
        Label lblMeta = new Label(cat + "  •  " + status);
        lblMeta.setFont(Font.font("Arial", 11));
        lblMeta.setTextFill(Color.web("#475569"));
        info.getChildren().addAll(lblRoute, lblMeta);

        // Fare
        Label lblFare = new Label(String.format("ETB %.2f", trip.getFare()));
        lblFare.setFont(Font.font("Arial", FontWeight.BOLD, 15));
        lblFare.setTextFill(Color.web("#3b82f6"));

        card.getChildren().addAll(lblIcon, info, lblFare);
        return card;
    }

    private Button navBtn(String text) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE); btn.setAlignment(Pos.CENTER_LEFT);
        btn.setFont(Font.font("Arial", 13));
        btn.setStyle("-fx-background-color:transparent;-fx-text-fill:#94a3b8;-fx-padding:10px 20px;-fx-cursor:hand;-fx-background-radius:6px;");
        return btn;
    }
}
