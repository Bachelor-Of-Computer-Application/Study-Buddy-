
package com.studybuddy.controllers;

import com.studybuddy.models.Task;
import com.studybuddy.services.TaskService;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class EditTaskDialogController {

    @FXML private TextField titleField;
    @FXML private TextArea descriptionField;
    
    private Task task;
    private final TaskService taskService = new TaskService();

    public void initData(Task task) {
        this.task = task;
        titleField.setText(task.getTitle());
        descriptionField.setText(task.getDescription());
    }

    @FXML
    private void handleSave() {
        if (titleField.getText().trim().isEmpty()) {
            return;
        }
        
        task.setTitle(titleField.getText().trim());
        task.setDescription(descriptionField.getText().trim());
        
        taskService.updateTask(task);
        
        // Close dialog
        ((Stage) titleField.getScene().getWindow()).close();
    }
    
    @FXML
    private void handleCancel() {
        ((Stage) titleField.getScene().getWindow()).close();
    }
}
