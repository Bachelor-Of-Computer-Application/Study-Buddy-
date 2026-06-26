module StudyBuddy {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires javafx.base;
    requires java.sql;
    
    exports com.studybuddy;
    exports com.studybuddy.controllers;
    exports com.studybuddy.models;
    exports com.studybuddy.services;
    exports com.studybuddy.dao;
    exports com.studybuddy.utils;
    exports com.studybuddy.admin;
    exports com.studybuddy.admin.controllers;
    exports com.studybuddy.admin.utils;
    exports com.studybuddy.admin.services;
    exports com.studybuddy.admin.dao;
    
    opens com.studybuddy to javafx.fxml;
    opens com.studybuddy.controllers to javafx.fxml;
    opens com.studybuddy.admin to javafx.fxml;
    opens com.studybuddy.admin.controllers to javafx.fxml;
}
