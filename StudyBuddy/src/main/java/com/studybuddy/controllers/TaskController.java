
package com.studybuddy.controllers;

import com.studybuddy.App;
import com.studybuddy.models.Task;
import com.studybuddy.models.User;
import com.studybuddy.services.TaskService;
import com.studybuddy.utils.EventBus;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.net.URL;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;

public class TaskController implements Initializable {

    @FXML private VBox rootPane;

    private final TaskService taskService = new TaskService();
    private User currentUser;
    private final ObservableList<Task> allTasks = FXCollections.observableArrayList();
    private final ObservableList<Task> visibleTasks = FXCollections.observableArrayList();

    private TextField searchField;
    private ComboBox<String> sortComboBox;
    private ToggleGroup filterGroup;
    private FlowPane tasksFlowPane;
    private TextField titleField;
    private TextArea descriptionArea;
    private ComboBox<String> priorityComboBox;
    private DatePicker dueDatePicker;
    private TextField estimatedTimeField;
    private String activeFilter = "all";
    private String activeSort = "newest";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM d, yyyy");
    
    private boolean isInitialized = false;
    private EventBus.EventListener<EventBus.TasksChangedEvent> tasksChangedListener;
    private EventBus.EventListener<EventBus.StatisticsChangedEvent> statisticsChangedListener;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (isInitialized) {
            return;
        }
        
        currentUser = App.getCurrentUser();
        buildUi();
        loadTasks();
        
        // Store listeners to enable unsubscription later
        tasksChangedListener = (_event) -> Platform.runLater(this::loadTasksInternal);
        statisticsChangedListener = (_event) -> Platform.runLater(this::loadTasksInternal);
        
        EventBus.getInstance().subscribe(EventBus.TasksChangedEvent.class, tasksChangedListener);
        EventBus.getInstance().subscribe(EventBus.StatisticsChangedEvent.class, statisticsChangedListener);
        
        isInitialized = true;
    }
    
    /**
     * Cleanup method to prevent memory leaks.
     * Call this when the controller is being destroyed.
     */
    public void cleanup() {
        if (tasksChangedListener != null) {
            EventBus.getInstance().unsubscribe(EventBus.TasksChangedEvent.class, tasksChangedListener);
        }
        if (statisticsChangedListener != null) {
            EventBus.getInstance().unsubscribe(EventBus.StatisticsChangedEvent.class, statisticsChangedListener);
        }
        isInitialized = false;
    }

    private void buildUi() {
        if (rootPane == null) {
            return;
        }

        VBox content = new VBox(24);
        content.setFillWidth(true);
        content.setPadding(new Insets(32));
        content.getStyleClass().add("task-content-container");

        // ── HEADER ──────────────────────────────────────────────────
        VBox headerBox = new VBox(8);
        Label titleLabel = new Label("Study Tasks");
        titleLabel.getStyleClass().add("task-page-title");
        Label subtitleLabel = new Label("Plan your study flow, track momentum, and stay on top of every deadline.");
        subtitleLabel.getStyleClass().add("task-page-subtitle");
        headerBox.getChildren().addAll(titleLabel, subtitleLabel);
        content.getChildren().add(headerBox);

        // ── CREATE TASK CARD ────────────────────────────────────────
        VBox addCard = new VBox(20);
        addCard.getStyleClass().add("task-create-card");
        addCard.setPadding(new Insets(24));
        addCard.setMaxWidth(1200);

        Label addTitle = new Label("Create a new task");
        addTitle.getStyleClass().add("task-section-title");

        // Main form fields (Title + Description)
        HBox mainRow = new HBox(16);
        mainRow.setAlignment(Pos.TOP_LEFT);

        VBox titleBox = new VBox(8);
        Label titleLabelField = new Label("Task title");
        titleLabelField.getStyleClass().add("task-field-label");
        titleField = new TextField();
        titleField.setPromptText("e.g. Review JavaFX layouts for midterm exam");
        titleField.getStyleClass().add("task-input-field");
        titleField.setPrefHeight(48);
        titleBox.getChildren().addAll(titleLabelField, titleField);
        HBox.setHgrow(titleBox, Priority.ALWAYS);

        VBox descriptionBox = new VBox(8);
        Label descLabel = new Label("Description");
        descLabel.getStyleClass().add("task-field-label");
        descriptionArea = new TextArea();
        descriptionArea.setPromptText("Add details about your study goal or task requirements");
        descriptionArea.setPrefHeight(120);
        descriptionArea.setWrapText(true);
        descriptionArea.getStyleClass().add("task-input-area");
        descriptionBox.getChildren().addAll(descLabel, descriptionArea);
        HBox.setHgrow(descriptionBox, Priority.ALWAYS);

        mainRow.getChildren().addAll(titleBox, descriptionBox);

        // Metadata row (Priority, Date, Time) - 3 columns now
        HBox metadataRow = new HBox(12);
        metadataRow.setAlignment(Pos.CENTER_LEFT);

        VBox priorityBox = new VBox(8);
        Label priorityLabel = new Label("Priority");
        priorityLabel.getStyleClass().add("task-field-label");
        priorityComboBox = new ComboBox<>(FXCollections.observableArrayList("Low", "Medium", "High"));
        priorityComboBox.setPromptText("Set priority");
        priorityComboBox.setValue("Medium");
        priorityComboBox.getStyleClass().add("task-input-combo");
        priorityComboBox.setPrefHeight(48);
        priorityComboBox.setMaxWidth(Double.MAX_VALUE);
        priorityBox.getChildren().addAll(priorityLabel, priorityComboBox);
        HBox.setHgrow(priorityBox, Priority.ALWAYS);

        VBox dateBox = new VBox(8);
        Label dateLabel = new Label("Due Date");
        dateLabel.getStyleClass().add("task-field-label");
        dueDatePicker = new DatePicker(LocalDate.now());
        dueDatePicker.setPromptText("Select due date");
        dueDatePicker.getStyleClass().add("task-input-date");
        dueDatePicker.setPrefHeight(48);
        dueDatePicker.setMaxWidth(Double.MAX_VALUE);
        dateBox.getChildren().addAll(dateLabel, dueDatePicker);
        HBox.setHgrow(dateBox, Priority.ALWAYS);

        VBox timeBox = new VBox(8);
        Label timeLabel = new Label("Estimated Time");
        timeLabel.getStyleClass().add("task-field-label");
        estimatedTimeField = new TextField();
        estimatedTimeField.setPromptText("e.g. 2 hours");
        estimatedTimeField.getStyleClass().add("task-input-field");
        estimatedTimeField.setPrefHeight(48);
        estimatedTimeField.setMaxWidth(Double.MAX_VALUE);
        timeBox.getChildren().addAll(timeLabel, estimatedTimeField);
        HBox.setHgrow(timeBox, Priority.ALWAYS);

        metadataRow.getChildren().addAll(priorityBox, dateBox, timeBox);

        // Action button
        HBox buttonRow = new HBox();
        buttonRow.setAlignment(Pos.CENTER_RIGHT);
        Button addTaskButton = new Button("✓ Add Task");
        addTaskButton.getStyleClass().add("task-btn-add");
        addTaskButton.setPrefHeight(48);
        addTaskButton.setOnAction(e -> handleAddTask());
        buttonRow.getChildren().add(addTaskButton);

        addCard.getChildren().addAll(addTitle, mainRow, metadataRow, buttonRow);
        content.getChildren().add(addCard);

        // ── TOOLBAR (Search + Sort) ─────────────────────────────────
        HBox toolbar = new HBox(16);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setMaxWidth(1200);

        // Search box
        HBox searchBox = new HBox(10);
        searchBox.setAlignment(Pos.CENTER_LEFT);
        searchBox.getStyleClass().add("task-search-container");
        searchBox.setPadding(new Insets(0, 16, 0, 16));
        searchBox.setPrefHeight(48);

        Label searchIcon = new Label("🔎");
        searchIcon.setStyle("-fx-font-size: 16px;");
        searchField = new TextField();
        searchField.setPromptText("Search tasks by title, description, or status...");
        searchField.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
        searchField.getStyleClass().add("task-search-field");
        searchField.textProperty().addListener((obs, oldValue, newValue) -> applyFiltersAndRender());
        HBox.setHgrow(searchField, Priority.ALWAYS);
        searchBox.getChildren().addAll(searchIcon, searchField);
        HBox.setHgrow(searchBox, Priority.ALWAYS);

        // Sort dropdown
        VBox sortBox = new VBox(4);
        Label sortLabel = new Label("Sort by");
        sortLabel.getStyleClass().add("task-sort-label");
        sortComboBox = new ComboBox<>(FXCollections.observableArrayList("Newest", "Oldest", "Due Date", "Priority", "Alphabetical"));
        sortComboBox.setValue("Newest");
        sortComboBox.getStyleClass().add("task-sort-combo");
        sortComboBox.setPrefHeight(48);
        sortComboBox.setPrefWidth(160);
        sortComboBox.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null) {
                activeSort = normalizeSortValue(newValue);
            }
            applyFiltersAndRender();
        });
        sortBox.getChildren().addAll(sortLabel, sortComboBox);

        toolbar.getChildren().addAll(searchBox, sortBox);
        content.getChildren().add(toolbar);

        // ── FILTER CHIPS ────────────────────────────────────────────
        HBox filterBar = new HBox(10);
        filterBar.setAlignment(Pos.CENTER_LEFT);
        filterBar.setMaxWidth(1200);
        filterGroup = new ToggleGroup();
        addFilterChip(filterBar, "All Tasks", "all");
        addFilterChip(filterBar, "Pending", "pending");
        addFilterChip(filterBar, "In Progress", "in-progress");
        addFilterChip(filterBar, "Completed", "completed");
        content.getChildren().add(filterBar);

        // ── TASK GRID ───────────────────────────────────────────────
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.getStyleClass().add("task-scroll-pane");

        tasksFlowPane = new FlowPane();
        tasksFlowPane.setHgap(20);
        tasksFlowPane.setVgap(20);
        tasksFlowPane.setPrefWrapLength(1200);
        tasksFlowPane.setPadding(new Insets(8));
        tasksFlowPane.setMinHeight(300);
        scrollPane.setContent(tasksFlowPane);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        content.getChildren().add(scrollPane);

        rootPane.getChildren().setAll(content);
        Platform.runLater(() -> {
            if (titleField != null) {
                titleField.requestFocus();
            }
        });
    }

    private void addFilterChip(HBox container, String text, String value) {
        ToggleButton button = new ToggleButton(text);
        button.setUserData(value);
        button.setToggleGroup(filterGroup);
        button.getStyleClass().add("task-filter-pill");
        button.setPrefHeight(40);
        button.setPadding(new Insets(0, 20, 0, 20));
        if (value.equals("all")) {
            button.setSelected(true);
        }
        button.setOnAction(e -> {
            activeFilter = (String) button.getUserData();
            applyFiltersAndRender();
        });
        container.getChildren().add(button);
    }

    /**
     * Public method for external callers to trigger a task reload.
     * Ensures thread safety by running on JavaFX Application Thread.
     */
    private void loadTasks() {
        if (Platform.isFxApplicationThread()) {
            loadTasksInternal();
        } else {
            Platform.runLater(this::loadTasksInternal);
        }
    }
    
    /**
     * Internal method that actually loads tasks from database.
     * Must only be called on JavaFX Application Thread.
     */
    private void loadTasksInternal() {
        if (currentUser == null) {
            allTasks.clear();
            visibleTasks.clear();
            renderTasks();
            return;
        }

        // Load tasks from database (runs synchronously on FX thread - acceptable for small datasets)
        List<Task> tasks = taskService.getTasksForUser(currentUser.getId());
        allTasks.setAll(tasks);
        applyFiltersAndRender();
    }

    private void applyFiltersAndRender() {
        // Ensure we're on FX Application Thread for UI operations
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(this::applyFiltersAndRender);
            return;
        }
        
        // Null safety check for UI components
        if (searchField == null) {
            renderTasks();
            return;
        }
        
        List<Task> filtered = new ArrayList<>();
        String query = searchField.getText();
        String loweredQuery = query == null ? "" : query.toLowerCase();

        for (Task task : allTasks) {
            boolean matchesFilter = activeFilter.equals("all") || normalizeStatus(task.getStatus()).equals(activeFilter);
            boolean matchesSearch = loweredQuery.isBlank() || containsText(task, loweredQuery);
            if (matchesFilter && matchesSearch) {
                filtered.add(task);
            }
        }

        filtered.sort(getComparator());
        visibleTasks.setAll(filtered);
        renderTasks();
    }

    private Comparator<Task> getComparator() {
        return switch (activeSort) {
            case "oldest" -> Comparator.comparing(Task::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()));
            case "due date" -> Comparator.comparing(Task::getDueDate, Comparator.nullsLast(Comparator.naturalOrder()));
            case "priority" -> Comparator.comparing(this::getPriorityWeight).reversed().thenComparing(Task::getTitle, String.CASE_INSENSITIVE_ORDER);
            case "alphabetical" -> Comparator.comparing(Task::getTitle, String.CASE_INSENSITIVE_ORDER);
            default -> Comparator.comparing(Task::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed();
        };
    }

    private String normalizeSortValue(String value) {
        if (value == null) {
            return "newest";
        }
        String normalized = value.trim().toLowerCase();
        return switch (normalized) {
            case "oldest" -> "oldest";
            case "due date" -> "due date";
            case "priority" -> "priority";
            case "alphabetical" -> "alphabetical";
            default -> "newest";
        };
    }

    private boolean containsText(Task task, String query) {
        return (task.getTitle() != null && task.getTitle().toLowerCase().contains(query))
                || (task.getDescription() != null && task.getDescription().toLowerCase().contains(query))
                || (task.getStatus() != null && task.getStatus().toLowerCase().contains(query));
    }

    private int getPriorityWeight(Task task) {
        return switch (task.getPriority() == null ? "medium" : task.getPriority().toLowerCase()) {
            case "high" -> 3;
            case "medium" -> 2;
            case "low" -> 1;
            default -> 0;
        };
    }

    private void renderTasks() {
        if (tasksFlowPane == null) {
            return;
        }

        tasksFlowPane.getChildren().clear();
        if (visibleTasks.isEmpty()) {
            VBox emptyState = new VBox(16);
            emptyState.getStyleClass().add("task-empty-card");
            emptyState.setAlignment(Pos.CENTER);
            emptyState.setPadding(new Insets(48));
            emptyState.setPrefWidth(600);
            emptyState.setMaxWidth(600);

            Label icon = new Label("📋");
            icon.setStyle("-fx-font-size: 48px;");
            Label title = new Label("No tasks found");
            title.getStyleClass().add("task-empty-title");
            Label subtitle = new Label("Create your first study task to get started.");
            subtitle.getStyleClass().add("task-empty-subtitle");
            Button createButton = new Button("+ Create Task");
            createButton.getStyleClass().add("task-btn-add");
            createButton.setPrefHeight(44);
            createButton.setOnAction(e -> {
                if (titleField != null) {
                    titleField.requestFocus();
                }
            });
            emptyState.getChildren().addAll(icon, title, subtitle, createButton);
            tasksFlowPane.getChildren().add(emptyState);
            return;
        }

        for (Task task : visibleTasks) {
            VBox card = createTaskCard(task);
            tasksFlowPane.getChildren().add(card);
        }
    }

    private VBox createTaskCard(Task task) {
        VBox card = new VBox(16);
        card.getStyleClass().add("task-grid-card");
        card.setPadding(new Insets(20));
        card.setMinWidth(360);
        card.setPrefWidth(360);
        card.setMaxWidth(420);
        card.setMinHeight(280);
        card.setPrefHeight(280);

        card.setOnMouseEntered(e -> animateCardHover(card, true));
        card.setOnMouseExited(e -> animateCardHover(card, false));

        // Use StackPane to overlay view/edit modes without resizing
        StackPane contentStack = new StackPane();
        contentStack.setAlignment(Pos.TOP_LEFT);
        VBox.setVgrow(contentStack, Priority.ALWAYS);

        // View mode content
        VBox viewMode = createCardViewContent(card, task);
        viewMode.setVisible(true);
        viewMode.setManaged(true);

        // Edit mode content (created on demand)
        VBox editMode = new VBox();
        editMode.setVisible(false);
        editMode.setManaged(false);

        contentStack.getChildren().addAll(viewMode, editMode);
        card.getChildren().add(contentStack);

        // Store references in card's properties for toggle
        card.getProperties().put("viewMode", viewMode);
        card.getProperties().put("editMode", editMode);
        card.getProperties().put("task", task);

        return card;
    }

    private VBox createCardViewContent(VBox card, Task task) {
        VBox content = new VBox(14);
        content.setFillWidth(true);

        // Title
        Label titleLabel = new Label(task.getTitle() == null || task.getTitle().isBlank() ? "Untitled task" : task.getTitle());
        titleLabel.getStyleClass().add("task-grid-title");
        titleLabel.setWrapText(true);
        titleLabel.setMaxHeight(50);

        // Description
        Label descriptionLabel = new Label(task.getDescription() == null || task.getDescription().isBlank() ? "No description provided." : task.getDescription());
        descriptionLabel.getStyleClass().add("task-grid-description");
        descriptionLabel.setWrapText(true);
        descriptionLabel.setMaxHeight(60);
        VBox.setVgrow(descriptionLabel, Priority.ALWAYS);

        // Priority badge only
        HBox badgesRow = new HBox(8);
        badgesRow.setAlignment(Pos.CENTER_LEFT);
        Label priorityBadge = createBadge(getDisplayText(task.getPriority(), "Medium"), getPriorityClass(task.getPriority()));
        badgesRow.getChildren().add(priorityBadge);

        // Dates row
        HBox datesRow = new HBox(12);
        datesRow.setAlignment(Pos.CENTER_LEFT);
        Label dueLabel = new Label("📅 " + formatDueDate(task.getDueDate()));
        dueLabel.getStyleClass().add("task-grid-meta");
        Label createdLabel = new Label("🕒 " + formatDate(task.getCreatedAt()));
        createdLabel.getStyleClass().add("task-grid-meta");
        datesRow.getChildren().addAll(dueLabel, createdLabel);

        // Footer (Status + Actions)
        HBox footerRow = new HBox(10);
        footerRow.setAlignment(Pos.CENTER_LEFT);
        
        Label statusBadge = createBadge(getStatusLabel(task.getStatus()), getStatusClass(task.getStatus()));
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button editButton = new Button("✏ Edit");
        editButton.getStyleClass().add("task-btn-edit");
        editButton.setOnAction(e -> switchToEditMode(card, task));

        Button deleteButton = new Button("🗑 Delete");
        deleteButton.getStyleClass().add("task-btn-delete");
        deleteButton.setOnAction(e -> confirmDelete(card, task));

        footerRow.getChildren().addAll(statusBadge, spacer, editButton, deleteButton);

        content.getChildren().addAll(titleLabel, descriptionLabel, badgesRow, datesRow, footerRow);
        return content;
    }

    private void switchToEditMode(VBox card, Task task) {
        VBox viewMode = (VBox) card.getProperties().get("viewMode");
        VBox editMode = (VBox) card.getProperties().get("editMode");

        if (editMode.getChildren().isEmpty()) {
            // Build edit mode UI (first time)
            editMode.getChildren().setAll(createEditModeContent(card, task));
        }

        viewMode.setVisible(false);
        viewMode.setManaged(false);
        editMode.setVisible(true);
        editMode.setManaged(true);
    }

    private void switchToViewMode(VBox card, Task task) {
        VBox viewMode = (VBox) card.getProperties().get("viewMode");
        VBox editMode = (VBox) card.getProperties().get("editMode");

        // Refresh view mode with updated task data
        viewMode.getChildren().setAll(createCardViewContent(card, task).getChildren());

        editMode.setVisible(false);
        editMode.setManaged(false);
        viewMode.setVisible(true);
        viewMode.setManaged(true);
    }

    private VBox createEditModeContent(VBox card, Task task) {
        VBox content = new VBox(12);
        content.setFillWidth(true);

        // Title field
        TextField editTitle = new TextField(task.getTitle());
        editTitle.getStyleClass().add("task-edit-field");
        editTitle.setPrefHeight(42);
        editTitle.setPromptText("Task title");

        // Description area
        TextArea editDescription = new TextArea(task.getDescription());
        editDescription.getStyleClass().add("task-edit-area");
        editDescription.setPrefHeight(70);
        editDescription.setWrapText(true);
        editDescription.setPromptText("Description");

        // Compact metadata row
        HBox metaRow = new HBox(8);
        metaRow.setAlignment(Pos.CENTER_LEFT);

        ComboBox<String> editPriority = new ComboBox<>(FXCollections.observableArrayList("Low", "Medium", "High"));
        editPriority.setValue(task.getPriority() == null || task.getPriority().isBlank() ? "Medium" : capitalize(task.getPriority()));
        editPriority.getStyleClass().add("task-edit-combo");
        editPriority.setPrefHeight(40);
        HBox.setHgrow(editPriority, Priority.ALWAYS);

        ComboBox<String> editStatus = new ComboBox<>(FXCollections.observableArrayList("Pending", "In Progress", "Completed"));
        editStatus.setValue(getStatusDisplayName(task.getStatus()));
        editStatus.getStyleClass().add("task-edit-combo");
        editStatus.setPrefHeight(40);
        HBox.setHgrow(editStatus, Priority.ALWAYS);

        metaRow.getChildren().addAll(editPriority, editStatus);

        // Date/Time row
        HBox dateRow = new HBox(8);
        dateRow.setAlignment(Pos.CENTER_LEFT);

        DatePicker editDueDate = new DatePicker();
        editDueDate.setValue(task.getDueDate() != null ? task.getDueDate().toLocalDateTime().toLocalDate() : LocalDate.now());
        editDueDate.getStyleClass().add("task-edit-date");
        editDueDate.setPrefHeight(40);
        HBox.setHgrow(editDueDate, Priority.ALWAYS);

        TextField editEstimatedTime = new TextField(task.getEstimatedTime());
        editEstimatedTime.getStyleClass().add("task-edit-field");
        editEstimatedTime.setPromptText("Est. time");
        editEstimatedTime.setPrefHeight(40);
        HBox.setHgrow(editEstimatedTime, Priority.ALWAYS);

        dateRow.getChildren().addAll(editDueDate, editEstimatedTime);

        // Action buttons
        HBox actionsRow = new HBox(8);
        actionsRow.setAlignment(Pos.CENTER_RIGHT);

        Button saveButton = new Button("✓ Save");
        saveButton.getStyleClass().add("task-btn-save");
        saveButton.setPrefHeight(38);
        saveButton.setOnAction(e -> {
            saveTaskFromEdit(task, editTitle, editDescription, editPriority, editStatus, editDueDate, editEstimatedTime);
            switchToViewMode(card, task);
        });

        Button cancelButton = new Button("✕ Cancel");
        cancelButton.getStyleClass().add("task-btn-cancel");
        cancelButton.setPrefHeight(38);
        cancelButton.setOnAction(e -> switchToViewMode(card, task));

        actionsRow.getChildren().addAll(cancelButton, saveButton);

        // Keyboard shortcuts
        editTitle.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                switchToViewMode(card, task);
            } else if (e.getCode() == KeyCode.ENTER && e.isControlDown()) {
                saveTaskFromEdit(task, editTitle, editDescription, editPriority, editStatus, editDueDate, editEstimatedTime);
                switchToViewMode(card, task);
            }
        });

        content.getChildren().addAll(editTitle, editDescription, metaRow, dateRow, actionsRow);
        return content;
    }

    private void saveTaskFromEdit(Task task, TextField titleField, TextArea descriptionArea, 
                                   ComboBox<String> priorityBox, 
                                   ComboBox<String> statusBox, DatePicker dueDatePicker, 
                                   TextField estimatedTimeField) {
        String title = titleField != null ? titleField.getText().trim() : task.getTitle();
        if (title == null || title.isBlank()) {
            showAlert(Alert.AlertType.ERROR, "Task title required", "Please enter a title before saving.");
            return;
        }

        task.setTitle(title);
        task.setDescription(descriptionArea != null ? descriptionArea.getText().trim() : task.getDescription());
        task.setPriority(priorityBox != null ? priorityBox.getValue().toLowerCase() : task.getPriority());
        
        String newStatus = statusBox != null ? normalizeStatus(statusBox.getValue()) : task.getStatus();
        task.setStatus(newStatus);
        
        task.setDueDate(dueDatePicker != null && dueDatePicker.getValue() != null ? 
            Timestamp.valueOf(dueDatePicker.getValue().atStartOfDay()) : task.getDueDate());
        task.setEstimatedTime(estimatedTimeField != null ? estimatedTimeField.getText().trim() : task.getEstimatedTime());

        if (!taskService.updateTask(task)) {
            showAlert(Alert.AlertType.ERROR, "Save failed", "The task could not be updated right now.");
        }
    }

    private void confirmDelete(VBox card, Task task) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete task?");
        confirm.setHeaderText("This action cannot be undone.");
        confirm.setContentText("Delete \"" + task.getTitle() + "\"?");
        confirm.getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);
        if (confirm.showAndWait().filter(button -> button == ButtonType.OK).isPresent()) {
            animateDelete(card, task);
        }
    }

    private void handleAddTask() {
        if (currentUser == null) {
            showAlert(Alert.AlertType.ERROR, "Login required", "Please log in again before creating a task.");
            return;
        }

        String title = titleField != null ? titleField.getText().trim() : "";
        if (title == null || title.isBlank()) {
            showAlert(Alert.AlertType.ERROR, "Task title required", "Please enter a task title first.");
            return;
        }

        Task task = new Task();
        task.setUserId(currentUser.getId());
        task.setTitle(title);
        task.setDescription(descriptionArea != null ? descriptionArea.getText().trim() : "");
        task.setPriority(priorityComboBox != null ? priorityComboBox.getValue().toLowerCase() : "medium");
        task.setStatus("pending");
        task.setDueDate(dueDatePicker != null && dueDatePicker.getValue() != null ? Timestamp.valueOf(dueDatePicker.getValue().atStartOfDay()) : null);
        task.setEstimatedTime(estimatedTimeField != null ? estimatedTimeField.getText().trim() : "");

        if (taskService.addTask(task)) {
            // Clear form fields
            if (titleField != null) titleField.clear();
            if (descriptionArea != null) descriptionArea.clear();
            if (priorityComboBox != null) priorityComboBox.setValue("Medium");
            if (dueDatePicker != null) dueDatePicker.setValue(LocalDate.now());
            if (estimatedTimeField != null) estimatedTimeField.clear();
            
            // Refresh will happen via EventBus subscription - no need to call refreshTasks() here
        } else {
            showAlert(Alert.AlertType.ERROR, "Add task failed", "The task could not be created right now.");
        }
    }

    private void refreshTasks() {
        // Deprecated - EventBus handles refresh automatically
        // Kept for backward compatibility but intentionally empty
    }

    private Label createBadge(String text, String styleClass) {
        Label badge = new Label(text);
        badge.getStyleClass().addAll("task-badge-base", styleClass);
        return badge;
    }

    private String getStatusLabel(String status) {
        return switch (normalizeStatus(status)) {
            case "completed" -> "Completed";
            case "in-progress" -> "In Progress";
            default -> "Pending";
        };
    }

    private String getPriorityClass(String priority) {
        return switch (priority == null ? "medium" : priority.toLowerCase()) {
            case "high" -> "task-badge-priority-high";
            case "medium" -> "task-badge-priority-med";
            case "low" -> "task-badge-priority-low";
            default -> "task-badge-priority-med";
        };
    }

    private String getStatusClass(String status) {
        return switch (normalizeStatus(status)) {
            case "completed" -> "task-badge-status-done";
            case "in-progress" -> "task-badge-status-progress";
            default -> "task-badge-status-pending";
        };
    }

    private String getStatusStyle(String status) {
        return switch (normalizeStatus(status)) {
            case "completed" -> "status-completed";
            case "in-progress" -> "status-progress";
            default -> "status-pending";
        };
    }

    private String getPriorityStyle(String priority) {
        return switch (priority == null ? "medium" : priority.toLowerCase()) {
            case "high" -> "priority-high";
            case "medium" -> "priority-medium";
            case "low" -> "priority-low";
            default -> "priority-medium";
        };
    }

    private String normalizeStatus(String status) {
        if (status == null) {
            return "pending";
        }
        String normalized = status.trim().toLowerCase();
        return switch (normalized) {
            case "in progress", "in-progress", "in_progress" -> "in-progress";
            case "completed" -> "completed";
            default -> "pending";
        };
    }

    private String getDisplayText(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }

    private String capitalize(String value) {
        if (value == null || value.isBlank()) {
            return "Medium";
        }
        return value.substring(0, 1).toUpperCase() + value.substring(1).toLowerCase();
    }

    private String formatDueDate(Timestamp timestamp) {
        if (timestamp == null) {
            return "No due date";
        }
        return timestamp.toLocalDateTime().toLocalDate().format(DATE_FORMATTER);
    }

    private String formatDate(Timestamp timestamp) {
        if (timestamp == null) {
            return "Just now";
        }
        return timestamp.toLocalDateTime().toLocalDate().format(DATE_FORMATTER);
    }

    private String getStatusDisplayName(String status) {
        return switch (normalizeStatus(status)) {
            case "completed" -> "Completed";
            case "in-progress" -> "In Progress";
            default -> "Pending";
        };
    }

    private void animateCardHover(VBox card, boolean hovered) {
        ScaleTransition transition = new ScaleTransition(Duration.millis(180), card);
        transition.setToX(hovered ? 1.02 : 1.0);
        transition.setToY(hovered ? 1.02 : 1.0);
        transition.play();
    }

    private void animateSuccess(VBox card) {
        ScaleTransition scale = new ScaleTransition(Duration.millis(140), card);
        scale.setFromX(1.0);
        scale.setFromY(1.0);
        scale.setToX(1.01);
        scale.setToY(1.01);
        scale.setAutoReverse(true);
        scale.setCycleCount(2);
        scale.play();
    }

    private void animateDelete(VBox card, Task task) {
        FadeTransition fade = new FadeTransition(Duration.millis(180), card);
        fade.setFromValue(1.0);
        fade.setToValue(0.0);
        TranslateTransition slide = new TranslateTransition(Duration.millis(180), card);
        slide.setFromX(0);
        slide.setToX(12);
        ParallelTransition transition = new ParallelTransition(fade, slide);
        transition.setOnFinished(e -> {
            if (taskService.deleteTask(task.getId())) {
                // Refresh will happen via EventBus subscription - no need to call refreshTasks() here
            } else {
                // Restore card visibility on failure
                Platform.runLater(() -> {
                    fade.stop();
                    slide.stop();
                    card.setOpacity(1.0);
                    card.setTranslateX(0);
                    showAlert(Alert.AlertType.ERROR, "Delete failed", "The task could not be deleted right now.");
                });
            }
        });
        transition.play();
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
