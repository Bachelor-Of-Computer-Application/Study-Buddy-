package com.studybuddy.admin.controllers;

import com.studybuddy.admin.dao.AdminDAO;
import com.studybuddy.utils.EventBus;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

public class AdminRewardHistoryController {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilter;
    @FXML private ComboBox<String> studentFilter;
    @FXML private TableView<Map<String, Object>> transactionsTable;
    @FXML private TableColumn<Map<String, Object>, String> colDateTime;
    @FXML private TableColumn<Map<String, Object>, String> colQuestion;
    @FXML private TableColumn<Map<String, Object>, Integer> colPoints;
    @FXML private TableColumn<Map<String, Object>, String> colFromStudent;
    @FXML private TableColumn<Map<String, Object>, String> colToStudent;
    @FXML private TableColumn<Map<String, Object>, String> colAnswer;
    @FXML private TableColumn<Map<String, Object>, String> colStatus;
    @FXML private Label lblTotalCount;

    private final AdminDAO adminDAO = AdminDAO.getInstance();
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML
    public void initialize() {
        setupColumns();
        setupFilters();
        loadTransactions();
        subscribeToEvents();
    }

    private void setupColumns() {
        colDateTime.setCellValueFactory(data -> {
            Timestamp ts = (Timestamp) data.getValue().get("created_at");
            LocalDateTime dateTime = ts != null ? ts.toLocalDateTime() : LocalDateTime.now();
            return new SimpleStringProperty(dateTime.format(dateTimeFormatter));
        });

        colQuestion.setCellValueFactory(data -> {
            String title = (String) data.getValue().get("question_title");
            String text = (String) data.getValue().get("question_text");
            String display = title != null && !title.isEmpty() ? title : 
                            (text != null && text.length() > 50 ? text.substring(0, 50) + "..." : text);
            return new SimpleStringProperty(display != null ? display : "N/A");
        });

        colPoints.setCellValueFactory(data -> {
            Integer points = (Integer) data.getValue().get("points");
            return new SimpleIntegerProperty(points != null ? points : 0).asObject();
        });

        colFromStudent.setCellValueFactory(data -> {
            String name = (String) data.getValue().get("from_user_name");
            return new SimpleStringProperty(name != null ? name : "Unknown");
        });

        colToStudent.setCellValueFactory(data -> {
            String name = (String) data.getValue().get("to_user_name");
            return new SimpleStringProperty(name != null ? name : "Unknown");
        });

        colAnswer.setCellValueFactory(data -> {
            String answer = (String) data.getValue().get("answer_text");
            String display = answer != null && answer.length() > 60 ? answer.substring(0, 60) + "..." : answer;
            return new SimpleStringProperty(display != null ? display : "N/A");
        });

        colStatus.setCellValueFactory(data -> {
            String status = (String) data.getValue().get("status");
            return new SimpleStringProperty(status != null ? status : "Unknown");
        });

        // Style status column
        colStatus.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(status);
                    if ("COMPLETED".equals(status)) {
                        setStyle("-fx-text-fill: #22c55e; -fx-font-weight: bold;");
                    } else if ("ACCEPTED".equals(status)) {
                        setStyle("-fx-text-fill: #3b82f6; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #6b7280;");
                    }
                }
            }
        });
    }

    private void setupFilters() {
        statusFilter.setItems(FXCollections.observableArrayList("All", "COMPLETED", "ACCEPTED"));
        statusFilter.setValue("All");

        studentFilter.setItems(FXCollections.observableArrayList("All"));
        studentFilter.setValue("All");

        searchField.setOnAction(e -> handleSearch());
    }

    private void loadTransactions() {
        String search = searchField.getText();
        String status = statusFilter.getValue();
        Integer userId = null;

        if (!"All".equals(studentFilter.getValue()) && studentFilter.getValue() != null) {
            // Parse user ID from selection (format: "Name (ID)")
            String selection = studentFilter.getValue();
            if (selection.contains("(")) {
                try {
                    userId = Integer.parseInt(selection.substring(selection.indexOf("(") + 1, selection.indexOf(")")));
                } catch (Exception e) {
                    userId = null;
                }
            }
        }

        List<Map<String, Object>> transactions = adminDAO.getRewardTransactions(search, status, userId);
        ObservableList<Map<String, Object>> observableList = FXCollections.observableArrayList(transactions);
        transactionsTable.setItems(observableList);

        lblTotalCount.setText(transactions.size() + " transactions");
    }

    @FXML
    public void handleSearch() {
        loadTransactions();
    }

    @FXML
    public void handleRefresh() {
        loadTransactions();
    }

    @FXML
    public void handleClearFilters() {
        searchField.clear();
        statusFilter.setValue("All");
        studentFilter.setValue("All");
        loadTransactions();
    }

    @FXML
    public void handleExportCSV() {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Export Reward History to CSV");
        fileChooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("CSV files (*.csv)", "*.csv"));
        fileChooser.setInitialFileName("reward_history.csv");
        
        javafx.scene.Scene scene = transactionsTable.getScene();
        if (scene == null || scene.getWindow() == null) {
            return;
        }
        
        java.io.File file = fileChooser.showSaveDialog(scene.getWindow());
        if (file == null) {
            return;
        }

        try (java.io.PrintWriter writer = new java.io.PrintWriter(new java.io.FileWriter(file))) {
            // Write header
            writer.println("Date,Question,Points,From Student,To Student,Best Answer,Status");

            for (Map<String, Object> transaction : transactionsTable.getItems()) {
                Timestamp ts = (Timestamp) transaction.get("created_at");
                LocalDateTime dateTime = ts != null ? ts.toLocalDateTime() : LocalDateTime.now();
                String dateStr = dateTime.format(dateTimeFormatter);

                String qTitle = (String) transaction.get("question_title");
                String qText = (String) transaction.get("question_text");
                String qDisplay = qTitle != null && !qTitle.isEmpty() ? qTitle : qText;
                if (qDisplay != null) {
                    qDisplay = qDisplay.replace("\"", "\"\"");
                    qDisplay = "\"" + qDisplay + "\"";
                } else {
                    qDisplay = "\"\"";
                }

                Integer points = (Integer) transaction.get("points");
                String fromStud = (String) transaction.get("from_user_name");
                String toStud = (String) transaction.get("to_user_name");

                String answer = (String) transaction.get("answer_text");
                if (answer != null) {
                    answer = answer.replace("\"", "\"\"");
                    answer = "\"" + answer + "\"";
                } else {
                    answer = "\"\"";
                }

                String status = (String) transaction.get("status");

                writer.printf("%s,%s,%d,%s,%s,%s,%s%n",
                        dateStr,
                        qDisplay,
                        points != null ? points : 0,
                        fromStud != null ? fromStud : "Unknown",
                        toStud != null ? toStud : "Unknown",
                        answer,
                        status != null ? status : "Unknown"
                );
            }
            
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Reward history successfully exported to CSV.");
            alert.setHeaderText(null);
            alert.showAndWait();
        } catch (java.io.IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to export reward history: " + e.getMessage());
            alert.setHeaderText(null);
            alert.showAndWait();
        }
    }

    private void subscribeToEvents() {
        EventBus.getInstance().subscribe(EventBus.QuestionsChangedEvent.class, event -> {
            javafx.application.Platform.runLater(this::loadTransactions);
        });
    }
}
