package com.studybuddy.admin.controllers;

import com.studybuddy.admin.services.AdminService;
import com.studybuddy.models.Note;
import com.studybuddy.models.Question;
import com.studybuddy.models.Resource;
import com.studybuddy.models.User;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.List;

public class AdminGlobalSearchController {

    @FXML private TextField searchField;
    @FXML private Label resultSummaryLabel;
    @FXML private TableView<User> usersTable;
    @FXML private TableColumn<User, String> colUserName;
    @FXML private TableColumn<User, String> colUserEmail;
    @FXML private TableColumn<User, String> colUserRole;
    @FXML private TableView<Note> notesTable;
    @FXML private TableColumn<Note, String> colNoteTitle;
    @FXML private TableColumn<Note, String> colNoteSubject;
    @FXML private TableColumn<Note, String> colNoteStatus;
    @FXML private TableView<Resource> resourcesTable;
    @FXML private TableColumn<Resource, String> colResTitle;
    @FXML private TableColumn<Resource, String> colResSubject;
    @FXML private TableColumn<Resource, String> colResStatus;
    @FXML private TableView<Question> questionsTable;
    @FXML private TableColumn<Question, String> colQuestionTitle;
    @FXML private TableColumn<Question, String> colQuestionSubject;
    @FXML private TableColumn<Question, String> colQuestionAuthor;

    private final AdminService adminService = AdminService.getInstance();
    private String pendingQuery = "";

    @FXML
    public void initialize() {
        setupTables();
        if (pendingQuery != null && !pendingQuery.isBlank()) {
            searchField.setText(pendingQuery);
            runSearch(pendingQuery);
        }
    }

    public void setSearchQuery(String query) {
        pendingQuery = query != null ? query.trim() : "";
        if (searchField != null) {
            searchField.setText(pendingQuery);
            runSearch(pendingQuery);
        }
    }

    @FXML
    public void handleSearch() {
        runSearch(searchField.getText());
    }

    private void runSearch(String query) {
        String q = query == null ? "" : query.trim();
        if (q.isEmpty()) {
            clearResults();
            if (resultSummaryLabel != null) {
                resultSummaryLabel.setText("Enter a search term above.");
            }
            return;
        }

        List<User> users = adminService.globalSearchUsers(q);
        List<Note> notes = adminService.globalSearchNotes(q);
        List<Resource> resources = adminService.globalSearchResources(q);
        List<Question> questions = adminService.globalSearchQuestions(q);

        usersTable.setItems(FXCollections.observableArrayList(users));
        notesTable.setItems(FXCollections.observableArrayList(notes));
        resourcesTable.setItems(FXCollections.observableArrayList(resources));
        questionsTable.setItems(FXCollections.observableArrayList(questions));

        if (resultSummaryLabel != null) {
            resultSummaryLabel.setText(String.format(
                    "Results for \"%s\" — %d users, %d notes, %d resources, %d questions",
                    q, users.size(), notes.size(), resources.size(), questions.size()));
        }
    }

    private void clearResults() {
        usersTable.setItems(FXCollections.emptyObservableList());
        notesTable.setItems(FXCollections.emptyObservableList());
        resourcesTable.setItems(FXCollections.emptyObservableList());
        questionsTable.setItems(FXCollections.emptyObservableList());
    }

    private void setupTables() {
        if (colUserName != null) colUserName.setCellValueFactory(new PropertyValueFactory<>("username"));
        if (colUserEmail != null) colUserEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        if (colUserRole != null) colUserRole.setCellValueFactory(new PropertyValueFactory<>("role"));

        if (colNoteTitle != null) colNoteTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        if (colNoteSubject != null) colNoteSubject.setCellValueFactory(new PropertyValueFactory<>("subject"));
        if (colNoteStatus != null) colNoteStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        if (colResTitle != null) colResTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        if (colResSubject != null) colResSubject.setCellValueFactory(new PropertyValueFactory<>("subject"));
        if (colResStatus != null) colResStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        if (colQuestionTitle != null) {
            colQuestionTitle.setCellValueFactory(cell ->
                    new javafx.beans.property.SimpleStringProperty(
                            cell.getValue().getTitle() != null ? cell.getValue().getTitle()
                                    : cell.getValue().getQuestionText()));
        }
        if (colQuestionSubject != null) colQuestionSubject.setCellValueFactory(new PropertyValueFactory<>("subject"));
        if (colQuestionAuthor != null) colQuestionAuthor.setCellValueFactory(new PropertyValueFactory<>("authorName"));
    }
}
