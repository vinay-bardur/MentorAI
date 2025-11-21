package com.visionai.app;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.visionai.app.adapters.ConversationAdapter;
import com.visionai.app.database.AppDatabase;
import com.visionai.app.models.Conversation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements ConversationAdapter.OnConversationClickListener {

    private RecyclerView conversationsRecyclerView;
    private ConversationAdapter adapter;
    private AppDatabase database;
    private Button deleteAllButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        database = AppDatabase.getInstance(this);

        conversationsRecyclerView = findViewById(R.id.conversationsRecyclerView);
        FloatingActionButton fabNewChat = findViewById(R.id.fabNewChat);
        deleteAllButton = findViewById(R.id.deleteAllButton);

        conversationsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ConversationAdapter(new ArrayList<>(), this);
        conversationsRecyclerView.setAdapter(adapter);

        loadConversations();

        fabNewChat.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, PersonaConfigActivity.class);
            startActivity(intent);
        });
        
        deleteAllButton.setOnClickListener(v -> showDeleteAllDialog());
    }

    private void loadConversations() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Conversation> conversations = database.conversationDao().getAllConversations();
            runOnUiThread(() -> adapter.updateConversations(conversations));
        });
    }

    @Override
    public void onConversationClick(Conversation conversation) {
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("conversation_id", conversation.id);
        startActivity(intent);
    }
    
    @Override
    public void onDeleteConversation(Conversation conversation) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Conversation")
                .setMessage("Are you sure you want to delete this conversation?")
                .setPositiveButton("Delete", (dialog, which) -> deleteConversation(conversation))
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private void deleteConversation(Conversation conversation) {
        Executors.newSingleThreadExecutor().execute(() -> {
            database.messageDao().deleteMessagesByConversationId(conversation.id);
            database.conversationDao().delete(conversation);
            runOnUiThread(() -> {
                loadConversations();
                Toast.makeText(this, "Conversation deleted", Toast.LENGTH_SHORT).show();
            });
        });
    }
    
    private void showDeleteAllDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Delete All Conversations")
                .setMessage("Are you sure you want to delete all conversations? This cannot be undone.")
                .setPositiveButton("Delete All", (dialog, which) -> deleteAllConversations())
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private void deleteAllConversations() {
        Executors.newSingleThreadExecutor().execute(() -> {
            database.messageDao().deleteAllMessages();
            database.conversationDao().deleteAllConversations();
            runOnUiThread(() -> {
                loadConversations();
                Toast.makeText(this, "All conversations deleted", Toast.LENGTH_SHORT).show();
            });
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadConversations();
    }
}
