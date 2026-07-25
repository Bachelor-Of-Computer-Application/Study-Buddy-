package com.studybuddy.controllers;

import com.studybuddy.App;
import com.studybuddy.dao.UserDAO;
import com.studybuddy.models.Task;
import com.studybuddy.models.User;
import com.studybuddy.services.TaskService;
import com.studybuddy.utils.EventBus;
import java.net.URL;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
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
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
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

public class TaskController implements Initializable {

    // ── Only FXML-injected field ──────────────────────────────────────────
    @FXML private VBox rootPane;

    // ── Services / state ─────────────────────────────────────────────────
    private final TaskService taskService = new TaskService();
    private final UserDAO userDAO = new UserDAO();
    private User currentUser;
    private final ObservableList<Task> allTasks    = FXCollections.observableArrayList();
    private final ObservableList<Task> visibleTasks = FXCollections.observableArrayList();

    // ── Programmatic UI references ────────────────────────────────────────
    private TextField   searchField;
    private ToggleGroup filterGroup;
    private VBox        tasksListBox;   // vertical list instead of FlowPane

    // Create-form fields
    private TextField   titleField;
    private TextField   subjectField;
    private TextArea    descriptionArea;
    private ComboBox<String> priorityComboBox;
    private ComboBox<String> statusComboBox;
    private DatePicker  dueDatePicker;
    private TextField   estimatedTimeField;

    // Stats labels (updated after every load)
    private Label lblPending;
    private Label lblInProgress;
    private Label lblCompleted;
    private Label lblOverdue;

    // Filter state
    private String activeFilter = "all";
    private boolean isInitialized = false;

    private EventBus.EventListener<EventBus.TasksChangedEvent>      tasksChangedListener;
    private EventBus.EventListener<EventBus.StatisticsChangedEvent> statsChangedListener;

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("MMM d, yyyy");

    // ══════════════════════════════════════════════════════════════════════
    // Lifecycle
    // ══════════════════════════════════════════════════════════════════════

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (isInitialized) return;
        currentUser = App.getCurrentUser();
        buildUi();
        loadTasks();

        tasksChangedListener = (_e) -> Platform.runLater(this::loadTasksInternal);
        statsChangedListener  = (_e) -> Platform.runLater(this::loadTasksInternal);

        EventBus.getInstance().subscribe(EventBus.TasksChangedEvent.class,     tasksChangedListener);
        EventBus.getInstance().subscribe(EventBus.StatisticsChangedEvent.class, statsChangedListener);
        isInitialized = true;
    }

    public void cleanup() {
        if (tasksChangedListener != null)
            EventBus.getInstance().unsubscribe(EventBus.TasksChangedEvent.class, tasksChangedListener);
        if (statsChangedListener != null)
            EventBus.getInstance().unsubscribe(EventBus.StatisticsChangedEvent.class, statsChangedListener);
        isInitialized = false;
    }

    // ══════════════════════════════════════════════════════════════════════
    // UI Construction
    // ══════════════════════════════════════════════════════════════════════

    private void buildUi() {
        if (rootPane == null) return;

        // Outer scroll so the whole page scrolls
        ScrollPane pageScroll = new ScrollPane();
        pageScroll.setFitToWidth(true);
        pageScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        pageScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        pageScroll.getStyleClass().add("tasks-scroll");
        VBox.setVgrow(pageScroll, Priority.ALWAYS);

        VBox content = new VBox(24);
        content.setFillWidth(true);
        content.setPadding(new Insets(28, 28, 36, 28));
        pageScroll.setContent(content);

        content.getChildren().addAll(
            buildHeroBanner(),
            buildStatsRow(),
            buildCreateCard(),
            buildToolbar(),
            buildTaskListSection()
        );

        rootPane.getChildren().setAll(pageScroll);
        Platform.runLater(() -> { if (titleField != null) titleField.requestFocus(); });
    }

    // ── Hero Banner ───────────────────────────────────────────────────────

    private HBox buildHeroBanner() {
        HBox hero = new HBox();
        hero.getStyleClass().add("tasks-hero");
        hero.setAlignment(Pos.CENTER_LEFT);

        VBox left = new VBox(6);
        HBox.setHgrow(left, Priority.ALWAYS);

        Label title = new Label("📋  Study Tasks");
        title.getStyleClass().add("tasks-hero-title");

        Label sub = new Label("Plan your study schedule, organize assignments, and stay productive every day.");
        sub.getStyleClass().add("tasks-hero-subtitle");
        sub.setWrapText(true);

        left.getChildren().addAll(title, sub);

        Button newTaskBtn = new Button("＋  New Task");
        newTaskBtn.getStyleClass().add("tasks-hero-btn");
        newTaskBtn.setOnAction(e -> { if (titleField != null) titleField.requestFocus(); });

        hero.getChildren().addAll(left, newTaskBtn);
        return hero;
    }

    // ── Stats Row ─────────────────────────────────────────────────────────

    private HBox buildStatsRow() {
        HBox row = new HBox(16);
        row.setFillHeight(true);

        lblPending    = new Label("0");
        lblInProgress = new Label("0");
        lblCompleted  = new Label("0");
        lblOverdue    = new Label("0");

        row.getChildren().addAll(
            buildStatCard("⏳", lblPending,    "Pending",     "tasks-stat-icon-pending",   "tasks-stat-value-pending"),
            buildStatCard("🔄", lblInProgress, "In Progress", "tasks-stat-icon-progress",  "tasks-stat-value-progress"),
            buildStatCard("✅", lblCompleted,  "Completed",   "tasks-stat-icon-completed", "tasks-stat-value-completed"),
            buildStatCard("🚨", lblOverdue,    "Overdue",     "tasks-stat-icon-overdue",   "tasks-stat-value-overdue")
        );
        for (var child : row.getChildren()) HBox.setHgrow(child, Priority.ALWAYS);
        return row;
    }

    private VBox buildStatCard(String icon, Label valueLabel, String labelText,
                               String iconWrapStyle, String valueStyle) {
        VBox card = new VBox(10);
        card.getStyleClass().add("tasks-stat-card");
        card.setAlignment(Pos.TOP_LEFT);

        HBox top = new HBox(12);
        top.setAlignment(Pos.CENTER_LEFT);

        StackPane iconWrap = new StackPane();
        iconWrap.getStyleClass().addAll("tasks-stat-icon-wrap", iconWrapStyle);
        Label iconLbl = new Label(icon);
        iconLbl.getStyleClass().add("tasks-stat-icon");
        iconWrap.getChildren().add(iconLbl);

        VBox info = new VBox(2);
        valueLabel.getStyleClass().addAll("tasks-stat-value", valueStyle);
        Label nameLbl = new Label(labelText);
        nameLbl.getStyleClass().add("tasks-stat-label");
        info.getChildren().addAll(valueLabel, nameLbl);

        top.getChildren().addAll(iconWrap, info);
        card.getChildren().add(top);
        return card;
    }

    // ── Create Task Card ──────────────────────────────────────────────────

    private VBox buildCreateCard() {
        VBox card = new VBox(16);
        card.getStyleClass().add("tasks-create-card");

        Label cardTitle = new Label("Create New Task");
        cardTitle.getStyleClass().add("tasks-card-title");

        // Row 1: Title | Priority
        HBox row1 = new HBox(16);
        titleField = new TextField();
        titleField.setPromptText("Task title…");
        titleField.getStyleClass().add("tasks-input");
        titleField.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ENTER) handleAddTask(); });

        priorityComboBox = new ComboBox<>(
            FXCollections.observableArrayList("Low", "Medium", "High"));
        priorityComboBox.setValue("Medium");
        priorityComboBox.getStyleClass().add("tasks-combo");
        priorityComboBox.setMaxWidth(Double.MAX_VALUE);

        row1.getChildren().addAll(
            fieldBox("Task Title",   titleField,       true),
            fieldBox("Priority",     priorityComboBox, false)
        );

        // Row 2: Subject | Due Date
        HBox row2 = new HBox(16);
        subjectField = new TextField();
        subjectField.setPromptText("e.g. Mathematics");
        subjectField.getStyleClass().add("tasks-input");

        dueDatePicker = new DatePicker(LocalDate.now());
        dueDatePicker.getStyleClass().add("tasks-date");
        dueDatePicker.setMaxWidth(Double.MAX_VALUE);

        row2.getChildren().addAll(
            fieldBox("Subject",   subjectField,  true),
            fieldBox("Due Date",  dueDatePicker, false)
        );

        // Row 3: Estimated Time | Status
        HBox row3 = new HBox(16);
        estimatedTimeField = new TextField();
        estimatedTimeField.setPromptText("e.g. 2 hours");
        estimatedTimeField.getStyleClass().add("tasks-input");

        statusComboBox = new ComboBox<>(
            FXCollections.observableArrayList("Pending", "In Progress", "Completed"));
        statusComboBox.setValue("Pending");
        statusComboBox.getStyleClass().add("tasks-combo");
        statusComboBox.setMaxWidth(Double.MAX_VALUE);

        row3.getChildren().addAll(
            fieldBox("Estimated Time", estimatedTimeField, true),
            fieldBox("Status",         statusComboBox,     false)
        );

        // Row 4: Description (full width)
        VBox descBox = new VBox(6);
        Label descLbl = new Label("Description");
        descLbl.getStyleClass().add("tasks-field-label");
        descriptionArea = new TextArea();
        descriptionArea.setPromptText("Add details, links, or notes about this task…");
        descriptionArea.setPrefRowCount(3);
        descriptionArea.setWrapText(true);
        descriptionArea.getStyleClass().add("tasks-desc");
        descBox.getChildren().addAll(descLbl, descriptionArea);

        // Submit row
        HBox btnRow = new HBox();
        btnRow.setAlignment(Pos.CENTER_RIGHT);
        Button addBtn = new Button("✓  Add Task");
        addBtn.getStyleClass().add("tasks-btn-add");
        addBtn.setPrefHeight(44);
        addBtn.setOnAction(e -> handleAddTask());
        btnRow.getChildren().add(addBtn);

        card.getChildren().addAll(cardTitle, row1, row2, row3, descBox, btnRow);
        return card;
    }

    /** Wraps a control with a field label in a VBox, optionally growing horizontally. */
    private VBox fieldBox(String labelText, Control control, boolean grow) {
        VBox box = new VBox(6);
        Label lbl = new Label(labelText);
        lbl.getStyleClass().add("tasks-field-label");
        if (control instanceof TextField tf) tf.setMaxWidth(Double.MAX_VALUE);
        box.getChildren().addAll(lbl, control);
        if (grow) HBox.setHgrow(box, Priority.ALWAYS);
        else       HBox.setHgrow(box, Priority.ALWAYS);  // both grow equally
        return box;
    }

    // ── Search + Filter Toolbar ───────────────────────────────────────────

    private VBox buildToolbar() {
        VBox wrapper = new VBox(12);

        // Search row
        HBox searchRow = new HBox(16);
        searchRow.setAlignment(Pos.CENTER_LEFT);
        searchRow.getStyleClass().add("tasks-toolbar");

        HBox searchWrap = new HBox(8);
        searchWrap.setAlignment(Pos.CENTER_LEFT);
        searchWrap.getStyleClass().add("tasks-search-wrap");
        HBox.setHgrow(searchWrap, Priority.ALWAYS);

        Label searchIcon = new Label("🔍");
        searchIcon.setStyle("-fx-font-size: 14px;");
        searchField = new TextField();
        searchField.setPromptText("Search tasks…");
        searchField.getStyleClass().add("tasks-search-field");
        searchField.textProperty().addListener((obs, o, n) -> applyFiltersAndRender());
        HBox.setHgrow(searchField, Priority.ALWAYS);
        searchWrap.getChildren().addAll(searchIcon, searchField);
        searchRow.getChildren().add(searchWrap);
        wrapper.getChildren().add(searchRow);

        // Filter chips row
        HBox filterRow = new HBox(8);
        filterRow.setAlignment(Pos.CENTER_LEFT);
        filterGroup = new ToggleGroup();

        addFilterChip(filterRow, "All",         "all");
        addFilterChip(filterRow, "Pending",     "pending");
        addFilterChip(filterRow, "In Progress", "in-progress");
        addFilterChip(filterRow, "Completed",   "completed");
        addFilterChip(filterRow, "Overdue",     "overdue");
        addFilterChip(filterRow, "Today",       "today");
        addFilterChip(filterRow, "This Week",   "thisweek");

        wrapper.getChildren().add(filterRow);
        return wrapper;
    }

    private void addFilterChip(HBox container, String text, String value) {
        ToggleButton btn = new ToggleButton(text);
        btn.setUserData(value);
        btn.setToggleGroup(filterGroup);
        btn.getStyleClass().add("tasks-filter-pill");
        if (value.equals("all")) btn.setSelected(true);
        btn.setOnAction(e -> {
            activeFilter = (String) btn.getUserData();
            applyFiltersAndRender();
        });
        container.getChildren().add(btn);
    }

    // ── Task List Section ─────────────────────────────────────────────────

    private VBox buildTaskListSection() {
        VBox section = new VBox(12);
        tasksListBox = new VBox(12);
        tasksListBox.setFillWidth(true);
        section.getChildren().add(tasksListBox);
        return section;
    }

    // ══════════════════════════════════════════════════════════════════════
    // Data Loading & Filtering
    // ══════════════════════════════════════════════════════════════════════

    private void loadTasks() {
        if (Platform.isFxApplicationThread()) loadTasksInternal();
        else Platform.runLater(this::loadTasksInternal);
    }

    private void loadTasksInternal() {
        if (currentUser == null) {
            allTasks.clear();
            visibleTasks.clear();
            renderTasks();
            updateStats();
            return;
        }
        allTasks.setAll(taskService.getTasksForUser(currentUser.getId()));
        applyFiltersAndRender();
        updateStats();
    }

    private void updateStats() {
        if (lblPending == null) return;
        LocalDate today = LocalDate.now();
        long pending   = allTasks.stream().filter(t -> normalizeStatus(t.getStatus()).equals("pending")).count();
        long inProgress = allTasks.stream().filter(t -> normalizeStatus(t.getStatus()).equals("in-progress")).count();
        long completed = allTasks.stream().filter(t -> normalizeStatus(t.getStatus()).equals("completed")).count();
        long overdue   = allTasks.stream().filter(t -> isOverdue(t, today)).count();
        lblPending.setText(String.valueOf(pending));
        lblInProgress.setText(String.valueOf(inProgress));
        lblCompleted.setText(String.valueOf(completed));
        lblOverdue.setText(String.valueOf(overdue));
    }

    private boolean isOverdue(Task t, LocalDate today) {
        if (normalizeStatus(t.getStatus()).equals("completed")) return false;
        if (t.getDueDate() == null) return false;
        return t.getDueDate().toLocalDateTime().toLocalDate().isBefore(today);
    }

    private void applyFiltersAndRender() {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(this::applyFiltersAndRender);
            return;
        }
        String query  = searchField == null ? "" : searchField.getText().toLowerCase();
        LocalDate today = LocalDate.now();

        List<Task> filtered = allTasks.stream()
            .filter(t -> matchesFilter(t, activeFilter, today))
            .filter(t -> query.isBlank() || containsText(t, query))
            .sorted(Comparator.comparing(Task::getCreatedAt,
                    Comparator.nullsLast(Comparator.naturalOrder())).reversed())
            .collect(Collectors.toList());

        visibleTasks.setAll(filtered);
        renderTasks();
    }

    private boolean matchesFilter(Task t, String filter, LocalDate today) {
        return switch (filter) {
            case "pending"     -> normalizeStatus(t.getStatus()).equals("pending");
            case "in-progress" -> normalizeStatus(t.getStatus()).equals("in-progress");
            case "completed"   -> normalizeStatus(t.getStatus()).equals("completed");
            case "overdue"     -> isOverdue(t, today);
            case "today"       -> t.getDueDate() != null &&
                                   t.getDueDate().toLocalDateTime().toLocalDate().equals(today);
            case "thisweek"    -> t.getDueDate() != null &&
                                   !t.getDueDate().toLocalDateTime().toLocalDate().isBefore(today) &&
                                   t.getDueDate().toLocalDateTime().toLocalDate().isBefore(today.plusDays(7));
            default            -> true;
        };
    }

    private boolean containsText(Task t, String q) {
        return (t.getTitle()       != null && t.getTitle().toLowerCase().contains(q))
            || (t.getDescription() != null && t.getDescription().toLowerCase().contains(q))
            || (t.getSubject()     != null && t.getSubject().toLowerCase().contains(q))
            || (t.getStatus()      != null && t.getStatus().toLowerCase().contains(q));
    }

    // ══════════════════════════════════════════════════════════════════════
    // Render
    // ══════════════════════════════════════════════════════════════════════

    private void renderTasks() {
        if (tasksListBox == null) return;
        tasksListBox.getChildren().clear();

        if (visibleTasks.isEmpty()) {
            tasksListBox.getChildren().add(buildEmptyState());
            return;
        }

        for (Task task : visibleTasks) {
            tasksListBox.getChildren().add(buildTaskCard(task));
        }
    }

    private VBox buildEmptyState() {
        VBox empty = new VBox(16);
        empty.getStyleClass().add("tasks-empty");
        empty.setAlignment(Pos.CENTER);

        Label icon  = new Label("📋");
        icon.getStyleClass().add("tasks-empty-icon");
        Label title = new Label("No Tasks Yet");
        title.getStyleClass().add("tasks-empty-title");
        Label sub   = new Label("Create your first study task to stay organized.");
        sub.getStyleClass().add("tasks-empty-sub");

        Button btn  = new Button("＋  Create Task");
        btn.getStyleClass().add("tasks-btn-add");
        btn.setPrefHeight(42);
        btn.setOnAction(e -> { if (titleField != null) titleField.requestFocus(); });

        empty.getChildren().addAll(icon, title, sub, btn);
        return empty;
    }

    // ── Task Card ─────────────────────────────────────────────────────────

    private HBox buildTaskCard(Task task) {
        HBox card = new HBox(0);
        card.getStyleClass().add("tasks-card");
        card.setFillHeight(true);

        // Colored left priority strip
        Region strip = new Region();
        strip.getStyleClass().addAll("tasks-priority-strip", getPriorityStripClass(task.getPriority()));
        strip.setMinWidth(5); strip.setPrefWidth(5); strip.setMaxWidth(5);

        VBox body = new VBox(10);
        body.getStyleClass().add("tasks-card-body");
        HBox.setHgrow(body, Priority.ALWAYS);

        // Row 1: title + badges
        HBox titleRow = new HBox(10);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        boolean done = normalizeStatus(task.getStatus()).equals("completed");
        Label titleLbl = new Label(nvl(task.getTitle(), "Untitled"));
        titleLbl.getStyleClass().add(done ? "tasks-card-title-done" : "tasks-card-title");
        titleLbl.setWrapText(true);
        HBox.setHgrow(titleLbl, Priority.ALWAYS);

        Label statusBadge   = badge(getStatusLabel(task.getStatus()), getStatusBadgeClass(task.getStatus()));
        Label priorityBadge = badge(nvl(task.getPriority(), "Medium"), getPriorityBadgeClass(task.getPriority()));

        titleRow.getChildren().addAll(titleLbl, priorityBadge, statusBadge);

        // Row 2: subject chip + meta
        HBox metaRow = new HBox(14);
        metaRow.setAlignment(Pos.CENTER_LEFT);

        if (task.getSubject() != null && !task.getSubject().isBlank()) {
            Label subjectLbl = new Label("📚 " + task.getSubject());
            subjectLbl.getStyleClass().add("tasks-card-subject");
            metaRow.getChildren().add(subjectLbl);
        }

        LocalDate today = LocalDate.now();
        boolean overdue = isOverdue(task, today);
        if (task.getDueDate() != null) {
            Label dueLbl = new Label("📅 " + formatDate(task.getDueDate()));
            dueLbl.getStyleClass().add(overdue ? "tasks-card-meta-overdue" : "tasks-card-meta");
            metaRow.getChildren().add(dueLbl);
        }
        if (task.getEstimatedTime() != null && !task.getEstimatedTime().isBlank()) {
            Label timeLbl = new Label("⏱ " + task.getEstimatedTime());
            timeLbl.getStyleClass().add("tasks-card-meta");
            metaRow.getChildren().add(timeLbl);
        }
        if (overdue) {
            Label overdueTag = badge("Overdue", "tasks-badge-overdue");
            metaRow.getChildren().add(overdueTag);
        }

        // Row 3: description preview
        VBox descRow = new VBox();
        if (task.getDescription() != null && !task.getDescription().isBlank()) {
            Label desc = new Label(task.getDescription());
            desc.getStyleClass().add("tasks-card-desc");
            desc.setWrapText(true);
            desc.setMaxHeight(42);
            descRow.getChildren().add(desc);
        }

        // Row 4: action buttons
        HBox actionsRow = new HBox(8);
        actionsRow.setAlignment(Pos.CENTER_RIGHT);

        Button editBtn = new Button("✏  Edit");
        editBtn.getStyleClass().add("tasks-btn-edit");

        Button deleteBtn = new Button("🗑  Delete");
        deleteBtn.getStyleClass().add("tasks-btn-delete");
        deleteBtn.setOnAction(e -> confirmDelete(card, task));

        Button completeBtn = new Button("✅  Complete");
        completeBtn.getStyleClass().add("tasks-btn-complete");
        completeBtn.setDisable(done);
        completeBtn.setOnAction(e -> markComplete(task));

        // Edit mode toggling (lazy-build)
        VBox editPanel = new VBox();
        editPanel.setVisible(false);
        editPanel.setManaged(false);

        editBtn.setOnAction(e -> {
            boolean showing = editPanel.isVisible();
            if (!showing && editPanel.getChildren().isEmpty()) {
                editPanel.getChildren().setAll(buildEditPanel(task, body, editPanel, titleLbl));
            }
            editPanel.setVisible(!showing);
            editPanel.setManaged(!showing);
            editBtn.setText(showing ? "✏  Edit" : "✕  Cancel");
        });

        actionsRow.getChildren().addAll(completeBtn, editBtn, deleteBtn);
        body.getChildren().addAll(titleRow, metaRow, descRow, editPanel, actionsRow);

        card.getChildren().addAll(strip, body);

        // Hover animation
        card.setOnMouseEntered(e -> animateHover(card, true));
        card.setOnMouseExited(e  -> animateHover(card, false));

        // Fade-in on creation
        FadeTransition ft = new FadeTransition(Duration.millis(250), card);
        ft.setFromValue(0.0); ft.setToValue(1.0); ft.play();

        return card;
    }

    // ── Inline Edit Panel ─────────────────────────────────────────────────

    private VBox buildEditPanel(Task task, VBox body, VBox editPanel, Label titleLbl) {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(12, 0, 4, 0));

        // Row A: title | subject
        HBox rowA = new HBox(12);
        TextField eTitleField = new TextField(task.getTitle());
        eTitleField.getStyleClass().add("tasks-edit-input");
        eTitleField.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(eTitleField, Priority.ALWAYS);

        TextField eSubjectField = new TextField(nvl(task.getSubject(), ""));
        eSubjectField.setPromptText("Subject");
        eSubjectField.getStyleClass().add("tasks-edit-input");
        eSubjectField.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(eSubjectField, Priority.ALWAYS);

        rowA.getChildren().addAll(
            labeledControl("Title",   eTitleField,   true),
            labeledControl("Subject", eSubjectField, true)
        );

        // Row B: priority | status | due date | estimated time
        HBox rowB = new HBox(12);
        ComboBox<String> ePriority = new ComboBox<>(
            FXCollections.observableArrayList("Low", "Medium", "High"));
        ePriority.setValue(capitalize(nvl(task.getPriority(), "Medium")));
        ePriority.getStyleClass().add("tasks-edit-combo");
        ePriority.setMaxWidth(Double.MAX_VALUE);

        ComboBox<String> eStatus = new ComboBox<>(
            FXCollections.observableArrayList("Pending", "In Progress", "Completed"));
        eStatus.setValue(getStatusDisplayName(task.getStatus()));
        eStatus.getStyleClass().add("tasks-edit-combo");
        eStatus.setMaxWidth(Double.MAX_VALUE);

        DatePicker eDue = new DatePicker(
            task.getDueDate() != null
                ? task.getDueDate().toLocalDateTime().toLocalDate()
                : LocalDate.now());
        eDue.getStyleClass().add("tasks-edit-combo");
        eDue.setMaxWidth(Double.MAX_VALUE);

        TextField eTime = new TextField(nvl(task.getEstimatedTime(), ""));
        eTime.setPromptText("Est. time");
        eTime.getStyleClass().add("tasks-edit-input");
        eTime.setMaxWidth(Double.MAX_VALUE);

        rowB.getChildren().addAll(
            labeledControl("Priority",  ePriority, true),
            labeledControl("Status",    eStatus,   true),
            labeledControl("Due Date",  eDue,      true),
            labeledControl("Est. Time", eTime,     true)
        );

        // Row C: description
        TextArea eDesc = new TextArea(nvl(task.getDescription(), ""));
        eDesc.setPromptText("Description…");
        eDesc.setPrefRowCount(2);
        eDesc.setWrapText(true);
        eDesc.getStyleClass().add("tasks-edit-area");

        // Buttons
        HBox btnRow = new HBox(8);
        btnRow.setAlignment(Pos.CENTER_RIGHT);

        Button saveBtn = new Button("✓  Save");
        saveBtn.getStyleClass().add("tasks-btn-save");
        saveBtn.setOnAction(e -> {
            String t = eTitleField.getText().trim();
            if (t.isBlank()) { showAlert(Alert.AlertType.ERROR, "Title required", "Please enter a task title."); return; }
            task.setTitle(t);
            task.setSubject(eSubjectField.getText().trim());
            task.setDescription(eDesc.getText().trim());
            task.setPriority(ePriority.getValue() == null ? "medium" : ePriority.getValue().toLowerCase());
            task.setStatus(normalizeStatus(eStatus.getValue()));
            task.setDueDate(eDue.getValue() != null ? Timestamp.valueOf(eDue.getValue().atStartOfDay()) : null);
            task.setEstimatedTime(eTime.getText().trim());

            if (taskService.updateTask(task)) {
                boolean nowDone = normalizeStatus(task.getStatus()).equals("completed");
                titleLbl.getStyleClass().setAll(nowDone ? "tasks-card-title-done" : "tasks-card-title");
                titleLbl.setText(task.getTitle());
                editPanel.setVisible(false);
                editPanel.setManaged(false);
            } else {
                showAlert(Alert.AlertType.ERROR, "Save failed", "Could not update the task.");
            }
        });

        Button cancelBtn = new Button("✕  Cancel");
        cancelBtn.getStyleClass().add("tasks-btn-cancel");
        cancelBtn.setOnAction(e -> {
            editPanel.setVisible(false);
            editPanel.setManaged(false);
        });

        btnRow.getChildren().addAll(cancelBtn, saveBtn);
        panel.getChildren().addAll(rowA, rowB, eDesc, btnRow);
        return panel;
    }

    private VBox labeledControl(String labelText, Control control, boolean grow) {
        VBox box = new VBox(4);
        Label lbl = new Label(labelText);
        lbl.getStyleClass().add("tasks-field-label");
        box.getChildren().addAll(lbl, control);
        if (grow) HBox.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    // ══════════════════════════════════════════════════════════════════════
    // Actions
    // ══════════════════════════════════════════════════════════════════════

    private void handleAddTask() {
        if (currentUser == null) {
            showAlert(Alert.AlertType.ERROR, "Not logged in", "Please log in again.");
            return;
        }
        String title = titleField == null ? "" : titleField.getText().trim();
        if (title.isBlank()) {
            showAlert(Alert.AlertType.ERROR, "Title required", "Please enter a task title.");
            return;
        }
        Task task = new Task();
        task.setUserId(currentUser.getId());
        task.setTitle(title);
        task.setSubject(subjectField      != null ? subjectField.getText().trim()      : "");
        task.setDescription(descriptionArea != null ? descriptionArea.getText().trim() : "");
        task.setPriority((priorityComboBox != null && priorityComboBox.getValue() != null)
                ? priorityComboBox.getValue().toLowerCase() : "medium");
        task.setStatus(normalizeStatus(
                statusComboBox != null && statusComboBox.getValue() != null
                ? statusComboBox.getValue() : "Pending"));
        task.setDueDate(dueDatePicker != null && dueDatePicker.getValue() != null
                ? Timestamp.valueOf(dueDatePicker.getValue().atStartOfDay()) : null);
        task.setEstimatedTime(estimatedTimeField != null
                ? estimatedTimeField.getText().trim() : "");

        if (taskService.addTask(task)) {
            if (titleField        != null) titleField.clear();
            if (subjectField      != null) subjectField.clear();
            if (descriptionArea   != null) descriptionArea.clear();
            if (priorityComboBox  != null) priorityComboBox.setValue("Medium");
            if (statusComboBox    != null) statusComboBox.setValue("Pending");
            if (dueDatePicker     != null) dueDatePicker.setValue(LocalDate.now());
            if (estimatedTimeField != null) estimatedTimeField.clear();
            // EventBus triggers loadTasksInternal automatically
        } else {
            showAlert(Alert.AlertType.ERROR, "Add failed", "Could not create the task.");
        }
    }

    private void markComplete(Task task) {
        task.setStatus("completed");
        if (taskService.updateTask(task)) {
            // Refresh user's achievement points in session
            if (currentUser != null) {
                try {
                    int newPoints = userDAO.getAchievementPoints(currentUser.getId());
                    currentUser.setAchievementPoints(newPoints);
                } catch (Exception e) {
                    java.util.logging.Logger.getLogger(TaskController.class.getName()).warning("Failed to refresh achievement points: " + e.getMessage());
                }
            }
        } else {
            showAlert(Alert.AlertType.ERROR, "Update failed", "Could not mark task as complete.");
        }
        // EventBus will reload
    }

    private void confirmDelete(HBox card, Task task) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Task");
        confirm.setHeaderText("Delete \"" + task.getTitle() + "\"?");
        confirm.setContentText("This action cannot be undone.");
        confirm.getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);
        confirm.showAndWait().filter(b -> b == ButtonType.OK).ifPresent(b -> animateDelete(card, task));
    }

    private void animateDelete(HBox card, Task task) {
        FadeTransition fade   = new FadeTransition(Duration.millis(200), card);
        fade.setFromValue(1.0); fade.setToValue(0.0);
        TranslateTransition slide = new TranslateTransition(Duration.millis(200), card);
        slide.setFromX(0); slide.setToX(16);
        ParallelTransition pt = new ParallelTransition(fade, slide);
        pt.setOnFinished(e -> {
            if (!taskService.deleteTask(task.getId())) {
                Platform.runLater(() -> {
                    card.setOpacity(1.0); card.setTranslateX(0);
                    showAlert(Alert.AlertType.ERROR, "Delete failed", "Could not delete the task.");
                });
            }
        });
        pt.play();
    }

    private void animateHover(HBox card, boolean in) {
        ScaleTransition st = new ScaleTransition(Duration.millis(150), card);
        st.setToX(in ? 1.005 : 1.0);
        st.setToY(in ? 1.005 : 1.0);
        st.play();
    }

    // ══════════════════════════════════════════════════════════════════════
    // Helpers
    // ══════════════════════════════════════════════════════════════════════

    private Label badge(String text, String styleClass) {
        Label l = new Label(text);
        l.getStyleClass().addAll("tasks-badge", styleClass);
        return l;
    }

    private String normalizeStatus(String s) {
        if (s == null) return "pending";
        return switch (s.trim().toLowerCase()) {
            case "in progress", "in-progress", "in_progress" -> "in-progress";
            case "completed" -> "completed";
            default -> "pending";
        };
    }

    private String getStatusLabel(String s) {
        return switch (normalizeStatus(s)) {
            case "completed"  -> "Completed";
            case "in-progress"-> "In Progress";
            default           -> "Pending";
        };
    }

    private String getStatusDisplayName(String s) { return getStatusLabel(s); }

    private String getStatusBadgeClass(String s) {
        return switch (normalizeStatus(s)) {
            case "completed"   -> "tasks-badge-completed";
            case "in-progress" -> "tasks-badge-progress";
            default            -> "tasks-badge-pending";
        };
    }

    private String getPriorityBadgeClass(String p) {
        return switch (p == null ? "medium" : p.toLowerCase()) {
            case "high" -> "tasks-badge-high";
            case "low"  -> "tasks-badge-low";
            default     -> "tasks-badge-medium";
        };
    }

    private String getPriorityStripClass(String p) {
        return switch (p == null ? "medium" : p.toLowerCase()) {
            case "high" -> "tasks-strip-high";
            case "low"  -> "tasks-strip-low";
            default     -> "tasks-strip-medium";
        };
    }

    private String formatDate(Timestamp ts) {
        if (ts == null) return "";
        return ts.toLocalDateTime().toLocalDate().format(DATE_FMT);
    }

    private String capitalize(String s) {
        if (s == null || s.isBlank()) return "Medium";
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }

    private String nvl(String s, String fallback) {
        return (s == null || s.isBlank()) ? fallback : s;
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
