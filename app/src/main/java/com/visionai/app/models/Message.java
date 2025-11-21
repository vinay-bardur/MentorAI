package com.visionai.app.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class Message {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public long conversationId;
    public boolean isUser;
    public String content;
    public long timestamp;
}
