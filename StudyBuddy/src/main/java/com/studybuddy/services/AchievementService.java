
package com.studybuddy.services;

import com.studybuddy.dao.*;
import com.studybuddy.models.Achievement;
import java.util.ArrayList;
import java.util.List;

/**
 * Service to calculate and track user achievements based on existing data.
 */
public class AchievementService {
    private final NoteDAO noteDAO = new NoteDAO();
    private final ResourceDAO resourceDAO = new ResourceDAO();
    private final QuestionDAO questionDAO = new QuestionDAO();
    private final TaskDAO taskDAO = new TaskDAO();
    private final StatisticsService statsService = new StatisticsService();
    
    // Singleton instance
    private static AchievementService instance;
    
    public static synchronized AchievementService getInstance() {
        if (instance == null) {
            instance = new AchievementService();
        }
        return instance;
    }
    
    private AchievementService() {}

    /**
     * Returns all available achievements for a specific user, with progress calculated.
     */
    public List<Achievement> getAchievementsForUser(int userId) {
        List<Achievement> achievements = new ArrayList<>();
        
        // 1. First Step (always unlocked if user exists)
        Achievement firstStep = new Achievement(
                "first-step",
                "First Step",
                "Registered an account on Study Buddy.",
                "🥇",
                1
        );
        firstStep.setCurrentProgress(1);
        firstStep.setUnlocked(true);
        achievements.add(firstStep);
        
        // 2. Note Scholar (upload first note)
        Achievement noteScholar = new Achievement(
                "note-scholar",
                "Note Scholar",
                "Created your first personal study notes.",
                "📝",
                1
        );
        try {
            int noteCount = statsService.getNotesUploaded(userId);
            noteScholar.setCurrentProgress(Math.min(noteCount, 1));
            noteScholar.setUnlocked(noteCount >= 1);
        } catch (Exception e) { e.printStackTrace(); }
        achievements.add(noteScholar);
        
        // 3. Curious Learner (ask first question)
        Achievement curiousLearner = new Achievement(
                "curious-learner",
                "Curious Learner",
                "Asked an academic question to the community.",
                "🙋",
                1
        );
        try {
            int questionCount = statsService.getQuestionsAsked(userId);
            curiousLearner.setCurrentProgress(Math.min(questionCount, 1));
            curiousLearner.setUnlocked(questionCount >= 1);
        } catch (Exception e) { e.printStackTrace(); }
        achievements.add(curiousLearner);
        
        // 4. Helpful Student (answer first question)
        Achievement helpfulStudent = new Achievement(
                "helpful-student",
                "Helpful Student",
                "Submitted your first answer to a question.",
                "💬",
                1
        );
        try {
            int answerCount = statsService.getAnswersSubmitted(userId);
            helpfulStudent.setCurrentProgress(Math.min(answerCount, 1));
            helpfulStudent.setUnlocked(answerCount >= 1);
        } catch (Exception e) { e.printStackTrace(); }
        achievements.add(helpfulStudent);
        
        // 5. Resource Contributor (upload first resource)
        Achievement resourceContributor = new Achievement(
                "resource-contributor",
                "Resource Contributor",
                "Shared your first resource with the community.",
                "📂",
                1
        );
        try {
            int resourceCount = statsService.getResourcesUploaded(userId);
            resourceContributor.setCurrentProgress(Math.min(resourceCount, 1));
            resourceContributor.setUnlocked(resourceCount >= 1);
        } catch (Exception e) { e.printStackTrace(); }
        achievements.add(resourceContributor);
        
        // 6. Task Master (complete 5 tasks)
        Achievement taskMaster = new Achievement(
                "task-master",
                "Task Master",
                "Completed 5 study tasks.",
                "✅",
                5
        );
        int completedTasks = taskDAO.getCompletedTaskCount(userId);
        taskMaster.setCurrentProgress(completedTasks);
        taskMaster.setUnlocked(completedTasks >= 5);
        achievements.add(taskMaster);
        
        // 7. Note Collector (upload 5 notes)
        Achievement noteCollector = new Achievement(
                "note-collector",
                "Note Collector",
                "Uploaded 5 study notes.",
                "📚",
                5
        );
        try {
            int noteCount = statsService.getNotesUploaded(userId);
            noteCollector.setCurrentProgress(noteCount);
            noteCollector.setUnlocked(noteCount >= 5);
        } catch (Exception e) { e.printStackTrace(); }
        achievements.add(noteCollector);
        
        // 8. Inquisitive Mind (ask 5 questions)
        Achievement inquisitiveMind = new Achievement(
                "inquisitive-mind",
                "Inquisitive Mind",
                "Asked 5 academic questions.",
                "🧠",
                5
        );
        try {
            int questionCount = statsService.getQuestionsAsked(userId);
            inquisitiveMind.setCurrentProgress(questionCount);
            inquisitiveMind.setUnlocked(questionCount >= 5);
        } catch (Exception e) { e.printStackTrace(); }
        achievements.add(inquisitiveMind);
        
        // 9. Consistent Learner (complete 10 tasks)
        Achievement consistentLearner = new Achievement(
                "consistent-learner",
                "Consistent Learner",
                "Completed 10 study tasks.",
                "📈",
                10
        );
        completedTasks = taskDAO.getCompletedTaskCount(userId);
        consistentLearner.setCurrentProgress(completedTasks);
        consistentLearner.setUnlocked(completedTasks >= 10);
        achievements.add(consistentLearner);
        
        // 10. Task Champion (complete 20 tasks)
        Achievement taskChampion = new Achievement(
                "task-champion",
                "Task Champion",
                "Completed 20 study tasks.",
                "🏆",
                20
        );
        taskChampion.setCurrentProgress(completedTasks);
        taskChampion.setUnlocked(completedTasks >= 20);
        achievements.add(taskChampion);
        
        // 11. Resource Provider (share 5 resources)
        Achievement resourceProvider = new Achievement(
                "resource-provider",
                "Resource Provider",
                "Shared 5 resources with the community.",
                "🎓",
                5
        );
        try {
            int resourceCount = statsService.getResourcesUploaded(userId);
            resourceProvider.setCurrentProgress(resourceCount);
            resourceProvider.setUnlocked(resourceCount >= 5);
        } catch (Exception e) { e.printStackTrace(); }
        achievements.add(resourceProvider);
        
        return achievements;
    }
    
    /**
     * Returns the number of unlocked achievements for a user
     */
    public int countUnlockedAchievements(int userId) {
        List<Achievement> achievements = getAchievementsForUser(userId);
        int count = 0;
        for (Achievement a : achievements) {
            if (a.isUnlocked()) count++;
        }
        return count;
    }
}
