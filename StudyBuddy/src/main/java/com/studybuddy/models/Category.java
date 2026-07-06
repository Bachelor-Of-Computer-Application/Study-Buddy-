package com.studybuddy.models;

public class Category {
    private Integer id;
    private String name;
    private String icon;
    private String color;
    private Integer noteCount;

    public Category() {
    }

    public Category(Integer id, String name, String icon, String color, Integer noteCount) {
        this.id = id;
        this.name = name;
        this.icon = icon;
        this.color = color;
        this.noteCount = noteCount;
    }

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public Integer getNoteCount() {
        return noteCount;
    }

    public void setNoteCount(Integer noteCount) {
        this.noteCount = noteCount;
    }

    @Override
    public String toString() {
        return name;
    }
}