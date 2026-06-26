package com.studybuddy.admin.services;

import com.studybuddy.admin.dao.NotificationDAO;
import com.studybuddy.models.Notification;
import com.studybuddy.utils.SessionManager;

import java.util.List;

/**
 * Service for notification management: sending, retrieving, deleting.
 */
public class NotificationService {

    private static NotificationService instance;
    private final NotificationDAO notificationDAO = NotificationDAO.getInstance();

    private NotificationService() {}

    public static synchronized NotificationService getInstance() {
        if (instance == null) instance = new NotificationService();
        return instance;
    }

    /**
     * Send a notification. The current admin is automatically set as sender.
     *
     * @param title          notification title
     * @param message        full message body
     * @param recipientType  one of: ALL, DEPARTMENT, SEMESTER, USER
     * @param recipientValue the department name, semester, or user email (null for ALL)
     * @param priority       LOW | NORMAL | HIGH | URGENT
     * @return true on success
     */
    public boolean sendNotification(String title, String message, String recipientType,
                                    String recipientValue, String priority) {
        int adminId = 0;
        if (SessionManager.getCurrentAdmin() != null) {
            adminId = SessionManager.getCurrentAdmin().getId();
        }
        Notification n = new Notification(title, message, recipientType, recipientValue, priority, adminId);
        return notificationDAO.sendNotification(n);
    }

    public List<Notification> getNotifications() {
        return notificationDAO.getAllNotifications();
    }

    public int getUnreadCount() {
        return notificationDAO.getUnreadCount();
    }

    public boolean deleteNotification(int id) {
        return notificationDAO.deleteNotification(id);
    }

    public boolean markAsRead(int id) {
        return notificationDAO.markAsRead(id);
    }
}
