package com.studybuddy.services;

import com.studybuddy.App;
import com.studybuddy.dao.QuestionDAO;
import com.studybuddy.models.Answer;
import com.studybuddy.models.Question;

import java.sql.SQLException;
import java.util.List;

public class QuestionService {

    private final QuestionDAO questionDAO = new QuestionDAO();

    public boolean saveQuestion(String text, String subject, int points, String attachment) throws SQLException {
        return questionDAO.createQuestion(getCurrentUserId(), text, subject, points, attachment);
    }

    public int getUserPoints() throws SQLException {
        return questionDAO.getUserPoints(getCurrentUserId());
    }

    public List<String> getAvailableSubjects() throws SQLException {
        return questionDAO.getAvailableSubjects();
    }

    public List<Question> getAllQuestions() throws SQLException {
        return questionDAO.getAllQuestions();
    }

    public List<Question> searchQuestions(String query, String subject) throws SQLException {
        return questionDAO.searchQuestions(query, subject);
    }

    public List<Question> getRelatedQuestions(int questionId, String subject, int limit) throws SQLException {
        return questionDAO.getRelatedQuestions(questionId, subject, limit);
    }

    public List<Answer> getAnswersByQuestionId(int questionId) throws SQLException {
        return questionDAO.getAnswersByQuestionId(questionId);
    }

    public boolean saveAnswer(int questionId, String answerText) throws SQLException {
        return questionDAO.createAnswer(questionId, getCurrentUserId(), answerText);
    }

    public boolean upvoteQuestion(int questionId) throws SQLException {
        return questionDAO.updateQuestionVotes(questionId, 1);
    }

    public boolean upvoteAnswer(int answerId) throws SQLException {
        return questionDAO.updateAnswerVotes(answerId, 1);
    }

    public int countQuestionsByUser(int userId) throws SQLException {
        return questionDAO.countQuestionsByUser(userId);
    }

    public int countAnswersByUser(int userId) throws SQLException {
        return questionDAO.countAnswersByUser(userId);
    }

    public boolean validateInputs(String q, String s, int p) {
        return q != null && !q.isEmpty()
                && s != null && !s.isEmpty()
                && p >= 5 && p <= 30;
    }

    private int getCurrentUserId() {
        return App.getCurrentUser() != null ? App.getCurrentUser().getId() : 0;
    }
}
