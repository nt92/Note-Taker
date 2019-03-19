package com.nikhilthota.noteapp;

import java.util.Date;

public class Note {
    private String title;
    private String description;
    private Date updatedAt;
    private String user;

    public Note() { }

    public Note(String user, String title, String description, Date updatedAt) {
        this.title = title;
        this.user = user;
        this.description = description;
        this.updatedAt = updatedAt;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title){
        this.title = title;
    }
}
