package com.studybuddy.services;

import com.studybuddy.dao.ResourceDAO;
import com.studybuddy.models.Note;
import com.studybuddy.models.Resource;

import java.sql.SQLException;
import java.util.List;

public class ResourceService {

    private final ResourceDAO resourceDAO = new ResourceDAO();

    public void shareAsResource(Note note, String filePath) throws SQLException {
        resourceDAO.shareAsResource(note, filePath);
    }

    public List<Resource> getAllActiveResources() throws SQLException {
        return resourceDAO.getAllActiveResources();
    }

    public List<Resource> getResourcesByUser(String userId) throws SQLException {
        return resourceDAO.getResourcesByUser(userId);
    }

    public boolean deleteResource(String id) throws SQLException {
        return resourceDAO.deleteResource(id);
    }

    public int countActiveResources() throws SQLException {
        return resourceDAO.countActiveResources();
    }

    public int countResourcesByUser(int userId) throws SQLException {
        return resourceDAO.countResourcesByUser(userId);
    }
}
