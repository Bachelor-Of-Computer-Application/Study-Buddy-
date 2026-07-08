
package com.studybuddy.utils;

import java.util.*;
import java.util.concurrent.*;

public class EventBus {
    private static EventBus instance;
    private final Map<Class<?>, List<EventListener<?>>> listeners = new ConcurrentHashMap<>();

    private EventBus() {}

    public static synchronized EventBus getInstance() {
        if (instance == null) {
            instance = new EventBus();
        }
        return instance;
    }

    @SuppressWarnings("unchecked")
    public <T> void subscribe(Class<T> eventType, EventListener<T> listener) {
        listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(listener);
    }

    @SuppressWarnings("unchecked")
    public <T> void unsubscribe(Class<T> eventType, EventListener<T> listener) {
        List<EventListener<?>> eventListeners = listeners.get(eventType);
        if (eventListeners != null) {
            eventListeners.remove(listener);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> void publish(T event) {
        List<EventListener<?>> eventListeners = listeners.get(event.getClass());
        if (eventListeners != null) {
            for (EventListener<?> listener : eventListeners) {
                try {
                    ((EventListener<T>) listener).onEvent(event);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @FunctionalInterface
    public interface EventListener<T> {
        void onEvent(T event);
    }

    // Event types
    public static class NotesChangedEvent {}
    public static class ResourcesChangedEvent {}
    public static class QuestionsChangedEvent {}
    public static class ProfileChangedEvent {}
    public static class AdminChangesEvent {}
    public static class StatisticsChangedEvent {}
}
