package com.visionai.app.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class Conversation {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public String title;
    public long createdAt;
    public long updatedAt;
}
