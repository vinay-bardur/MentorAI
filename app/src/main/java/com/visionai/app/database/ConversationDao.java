package com.visionai.app.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.visionai.app.models.Conversation;

import java.util.List;

@Dao
public interface ConversationDao {

    @Insert
    long insert(Conversation conversation);

    @Update
    void update(Conversation conversation);

    @Query("SELECT * FROM Conversation ORDER BY updatedAt DESC")
    List<Conversation> getAllConversations();
    
    @Delete
    void delete(Conversation conversation);
    
    @Query("DELETE FROM Conversation")
    void deleteAllConversations();
}
