package com.visionai.app.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.visionai.app.models.Message;

import java.util.List;

@Dao
public interface MessageDao {

    @Insert
    long insert(Message message);

    @Query("SELECT * FROM Message WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    List<Message> getMessagesForConversation(long conversationId);
    
    @Query("DELETE FROM Message WHERE conversationId = :conversationId")
    void deleteMessagesByConversationId(long conversationId);
    
    @Query("DELETE FROM Message")
    void deleteAllMessages();
}
