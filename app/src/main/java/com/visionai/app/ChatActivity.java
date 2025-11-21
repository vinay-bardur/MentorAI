package com.visionai.app;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.JsonObject;
import com.visionai.app.adapters.ChatAdapter;
import com.visionai.app.api.GroqClient;
import com.visionai.app.database.AppDatabase;
import com.visionai.app.models.Conversation;
import com.visionai.app.models.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView chatRecyclerView;
    private ChatAdapter chatAdapter;
    private EditText inputMessage;
    private ImageButton sendButton;
    private ProgressBar loadingIndicator;
    private TextView mentorTitle;

    private AppDatabase database;
    private GroqClient groqClient;

    private long conversationId = -1L;
    private String systemPrompt = "";
    private String mentorName = "";
    private String customInstruction = "";

    private final List<Message> cachedMessages = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        inputMessage = findViewById(R.id.inputMessage);
        sendButton = findViewById(R.id.sendButton);
        loadingIndicator = findViewById(R.id.loadingIndicator);
        mentorTitle = findViewById(R.id.mentorTitle);

        chatAdapter = new ChatAdapter();
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(chatAdapter);

        database = AppDatabase.getInstance(this);

        String apiKey = BuildConfig.GROQ_API_KEY;
        if (TextUtils.isEmpty(apiKey) || apiKey.equals("your_groq_api_key_here")) {
            Toast.makeText(this, "Please set your Groq API key in gradle.properties", Toast.LENGTH_LONG).show();
        }
        groqClient = new GroqClient(apiKey);

        mentorName = getIntent().getStringExtra("mentor_name");
        systemPrompt = getIntent().getStringExtra("mentor_prompt");
        customInstruction = getIntent().getStringExtra("custom_instruction");
        long passedConversationId = getIntent().getLongExtra("conversation_id", -1L);

        if (passedConversationId != -1L) {
            conversationId = passedConversationId;
        } else {
            createNewConversation();
        }

        if (customInstruction != null && !customInstruction.isEmpty()) {
            systemPrompt = systemPrompt + "\nUser additional instructions: " + customInstruction;
        }

        mentorTitle.setText(mentorName != null ? mentorName : "VisionAI");

        loadExistingConversation();

        sendButton.setOnClickListener(v -> sendMessage());
    }

    private void createNewConversation() {
        Executors.newSingleThreadExecutor().execute(() -> {
            Conversation c = new Conversation();
            c.title = mentorName != null ? mentorName + " Session" : "New VisionAI Chat";
            long now = System.currentTimeMillis();
            c.createdAt = now;
            c.updatedAt = now;
            long id = database.conversationDao().insert(c);
            conversationId = id;
        });
    }

    private void loadExistingConversation() {
        Executors.newSingleThreadExecutor().execute(() -> {
            if (conversationId == -1L) return;
            List<Message> list = database.messageDao().getMessagesForConversation(conversationId);
            cachedMessages.clear();
            cachedMessages.addAll(list);
            runOnUiThread(() -> chatAdapter.setMessages(new ArrayList<>(cachedMessages)));
        });
    }

    private void sendMessage() {
        String text = inputMessage.getText().toString().trim();
        if (text.isEmpty()) return;

        inputMessage.setText("");

        Message userMsg = new Message();
        userMsg.conversationId = conversationId;
        userMsg.isUser = true;
        userMsg.content = text;
        userMsg.timestamp = System.currentTimeMillis();

        cachedMessages.add(userMsg);
        chatAdapter.addMessage(userMsg);
        scrollToBottom();

        saveMessage(userMsg);
        callGroq(text);
    }

    private void scrollToBottom() {
        chatRecyclerView.post(() -> chatRecyclerView.smoothScrollToPosition(chatAdapter.getItemCount() - 1));
    }

    private void setLoading(boolean loading) {
        loadingIndicator.setVisibility(loading ? View.VISIBLE : View.GONE);
        sendButton.setEnabled(!loading);
    }

    private void callGroq(String userText) {
        setLoading(true);

        StringBuilder historyBuilder = new StringBuilder();
        // Build history excluding the current message (last one in cachedMessages)
        for (int i = 0; i < cachedMessages.size() - 1; i++) {
            Message m = cachedMessages.get(i);
            historyBuilder.append(m.isUser ? "User: " : mentorName + ": ");
            historyBuilder.append(m.content).append("\n");
        }

        Call<JsonObject> call = groqClient.sendChatRequest(
                GroqClient.DEFAULT_MODEL,
                systemPrompt,
                historyBuilder.toString(),
                userText
        );

        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    String reply = GroqClient.extractMessageContent(response.body());
                    addAiMessage(reply);
                } else {
                    addAiMessage("Error: " + response.code() + " - " + response.message());
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                setLoading(false);
                addAiMessage("Request failed: " + t.getMessage());
            }
        });
    }

    private void addAiMessage(String text) {
        Message aiMsg = new Message();
        aiMsg.conversationId = conversationId;
        aiMsg.isUser = false;
        aiMsg.content = text;
        aiMsg.timestamp = System.currentTimeMillis();

        cachedMessages.add(aiMsg);
        chatAdapter.addMessage(aiMsg);
        scrollToBottom();

        saveMessage(aiMsg);
        touchConversationUpdatedAt();
    }

    private void saveMessage(Message message) {
        Executors.newSingleThreadExecutor().execute(() -> database.messageDao().insert(message));
    }

    private void touchConversationUpdatedAt() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Conversation> list = database.conversationDao().getAllConversations();
            for (Conversation c : list) {
                if (c.id == conversationId) {
                    c.updatedAt = System.currentTimeMillis();
                    database.conversationDao().update(c);
                    break;
                }
            }
        });
    }
}
