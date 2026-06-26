package com.studybuddy.admin.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.*;

public class AdminReportsController {

    @FXML private LineChart<String, Number> userGrowthChart;
    @FXML private BarChart<String, Number> notesUploadedChart;
    @FXML private BarChart<String, Number> questionsAskedChart;
    @FXML private BarChart<String, Number> resourcesSharedChart;
    @FXML private PieChart monthlyStatsChart;

    @FXML
    public void initialize() {
        populateUserGrowth();
        populateNotesUploaded();
        populateQuestionsAsked();
        populateResourcesShared();
        populateMonthlyStats();
    }

    private void populateUserGrowth() {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.getData().add(new XYChart.Data<>("Jan", 120));
        series.getData().add(new XYChart.Data<>("Feb", 180));
        series.getData().add(new XYChart.Data<>("Mar", 340));
        series.getData().add(new XYChart.Data<>("Apr", 560));
        series.getData().add(new XYChart.Data<>("May", 890));
        series.getData().add(new XYChart.Data<>("Jun", 1248));
        userGrowthChart.getData().add(series);
    }

    private void populateNotesUploaded() {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.getData().add(new XYChart.Data<>("Week 1", 45));
        series.getData().add(new XYChart.Data<>("Week 2", 60));
        series.getData().add(new XYChart.Data<>("Week 3", 85));
        series.getData().add(new XYChart.Data<>("Week 4", 110));
        notesUploadedChart.getData().add(series);
    }

    private void populateQuestionsAsked() {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.getData().add(new XYChart.Data<>("Mon", 14));
        series.getData().add(new XYChart.Data<>("Tue", 22));
        series.getData().add(new XYChart.Data<>("Wed", 19));
        series.getData().add(new XYChart.Data<>("Thu", 35));
        series.getData().add(new XYChart.Data<>("Fri", 28));
        series.getData().add(new XYChart.Data<>("Sat", 12));
        series.getData().add(new XYChart.Data<>("Sun", 8));
        questionsAskedChart.getData().add(series);
    }

    private void populateResourcesShared() {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.getData().add(new XYChart.Data<>("Week 1", 12));
        series.getData().add(new XYChart.Data<>("Week 2", 18));
        series.getData().add(new XYChart.Data<>("Week 3", 25));
        series.getData().add(new XYChart.Data<>("Week 4", 39));
        resourcesSharedChart.getData().add(series);
    }

    private void populateMonthlyStats() {
        ObservableList<PieChart.Data> data = FXCollections.observableArrayList(
                new PieChart.Data("Downloads", 4500),
                new PieChart.Data("Uploads", 1200),
                new PieChart.Data("Q&A Threads", 850)
        );
        monthlyStatsChart.setData(data);
    }
}
