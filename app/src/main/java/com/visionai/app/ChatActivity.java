package com.visionai.app;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import static com.visionai.app.AppConfig.*;

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
    private Button togglePanelButton;
    private Button toggleSingleButton;
    private LinearLayout singleMentorSelector;
    private Spinner mentorSpinner;

    private AppDatabase database;
    private GroqClient groqClient;
    private SharedPreferences prefs;

    private long conversationId = -1L;
    private String mentorName = "";
    private String chatMode = MODE_PANEL;
    private String selectedSingleMentor = MENTOR_ELON;
    private String userName = "User";

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
        togglePanelButton = findViewById(R.id.togglePanelButton);
        toggleSingleButton = findViewById(R.id.toggleSingleButton);
        singleMentorSelector = findViewById(R.id.singleMentorSelector);
        mentorSpinner = findViewById(R.id.mentorSpinner);

        chatAdapter = new ChatAdapter();
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(chatAdapter);

        database = AppDatabase.getInstance(this);
        prefs = getSharedPreferences("VisionAI", MODE_PRIVATE);
        
        userName = prefs.getString(PREF_USER_NAME, "User");
        if (TextUtils.isEmpty(userName)) userName = "User";
        
        chatMode = prefs.getString(PREF_CHAT_MODE, MODE_PANEL);
        selectedSingleMentor = prefs.getString(PREF_SELECTED_MENTOR, MENTOR_ELON);
        
        setupModeToggle();
        setupMentorSpinner();
        updateModeUI();

        String apiKey = BuildConfig.GROQ_API_KEY;
        if (TextUtils.isEmpty(apiKey) || apiKey.equals("your_groq_api_key_here")) {
            Toast.makeText(this, "⚠️ Please set your Groq API key in gradle.properties", Toast.LENGTH_LONG).show();
            apiKey = ""; // Set empty to prevent crashes
        }
        if (apiKey.length() > 0 && !apiKey.startsWith("gsk_")) {
            Toast.makeText(this, "⚠️ Invalid API key format. Should start with 'gsk_'", Toast.LENGTH_LONG).show();
        }
        groqClient = new GroqClient(apiKey);

        long passedConversationId = getIntent().getLongExtra("conversation_id", -1L);

        if (passedConversationId != -1L) {
            conversationId = passedConversationId;
        } else {
            createNewConversation();
        }

        updateTitle();

        loadExistingConversation();

        sendButton.setOnClickListener(v -> sendMessage());
    }

    private void setupModeToggle() {
        togglePanelButton.setOnClickListener(v -> {
            chatMode = MODE_PANEL;
            prefs.edit().putString(PREF_CHAT_MODE, chatMode).apply();
            updateModeUI();
            updateTitle();
        });
        
        toggleSingleButton.setOnClickListener(v -> {
            chatMode = MODE_SINGLE;
            prefs.edit().putString(PREF_CHAT_MODE, chatMode).apply();
            updateModeUI();
            updateTitle();
        });
    }
    
    private void setupMentorSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, 
            android.R.layout.simple_spinner_item, MENTOR_NAMES);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mentorSpinner.setAdapter(adapter);
        
        int position = 0;
        for (int i = 0; i < MENTOR_NAMES.length; i++) {
            if (MENTOR_NAMES[i].equals(selectedSingleMentor)) {
                position = i;
                break;
            }
        }
        mentorSpinner.setSelection(position);
        
        mentorSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedSingleMentor = MENTOR_NAMES[position];
                prefs.edit().putString(PREF_SELECTED_MENTOR, selectedSingleMentor).apply();
                updateTitle();
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }
    
    private void updateModeUI() {
        if (MODE_PANEL.equals(chatMode)) {
            togglePanelButton.setBackgroundTintList(getColorStateList(R.color.ios_primary));
            toggleSingleButton.setBackgroundTintList(getColorStateList(android.R.color.darker_gray));
            singleMentorSelector.setVisibility(View.GONE);
        } else {
            togglePanelButton.setBackgroundTintList(getColorStateList(android.R.color.darker_gray));
            toggleSingleButton.setBackgroundTintList(getColorStateList(R.color.ios_primary));
            singleMentorSelector.setVisibility(View.VISIBLE);
        }
    }
    
    private void updateTitle() {
        if (MODE_PANEL.equals(chatMode)) {
            mentorTitle.setText("VisionAI · 5 Mentors");
        } else {
            mentorTitle.setText(selectedSingleMentor);
        }
    }
    
    private void createNewConversation() {
        Executors.newSingleThreadExecutor().execute(() -> {
            Conversation c = new Conversation();
            c.title = MODE_PANEL.equals(chatMode) ? "Panel Session" : selectedSingleMentor + " Session";
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

    private String buildSystemPrompt() {
        if (MODE_PANEL.equals(chatMode)) {
            return "You are a single AI simulating a PANEL of five mentors: "
                + "Elon Musk, Tim Ferriss, Ilia Topuria, Steve Jobs, Kiyotaka Ayanokoji. "
                + "The user's name is " + userName + ".\n\n"
                + "RULES:\n"
                + "1) For every user message, respond with five labeled sections in this exact order:\n"
                + "   [Elon Musk]\n"
                + "   - (2-4 bullet points, absolute first-principles actions and moonshot mindset)\n"
                + "   [Tim Ferriss]\n"
                + "   - (2-4 bullet points, 80/20 experiments and quick wins)\n"
                + "   [Ilia Topuria]\n"
                + "   - (2-4 bullet points, champion mentality, discipline steps)\n"
                + "   [Steve Jobs]\n"
                + "   - (2-4 bullet points, product/focus/simplify approach)\n"
                + "   [Kiyotaka Ayanokoji]\n"
                + "   - (2-4 bullet points, analytic, strategic minimal-action plan)\n\n"
                + "2) Use short, actionable bullets. When appropriate, address " + userName + " directly by name. "
                + "3) No extra prefaces, no meta commentary, no internal thoughts. Just the five sections.";
        } else {
            return "You are simulating the mentor: " + selectedSingleMentor + ". "
                + "The user's name is " + userName + ".\n\n"
                + "RULES:\n"
                + "1) Answer only as this mentor. Use tone and approach faithful to that mentor's public persona. "
                + "2) Provide 4-6 short, actionable steps or suggestions for the user's situation. Address the user by name when appropriate. "
                + "3) No meta commentary, no multiple mentors. Keep it concise and practical.";
        }
    }
    
    private void callGroq(String userText) {
        setLoading(true);
        
        String systemPrompt = buildSystemPrompt();

        StringBuilder historyBuilder = new StringBuilder();
        for (int i = 0; i < cachedMessages.size() - 1; i++) {
            Message m = cachedMessages.get(i);
            historyBuilder.append(m.isUser ? "User: " : "AI: ");
            historyBuilder.append(m.content).append("\n");
        }

        Call<JsonObject> call = groqClient.sendChatRequest(
                DEFAULT_MODEL,
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
                } else if (response.code() == 401) {
                    addAiMessage("❌ Authentication Error (401): Invalid API key.\n\n" +
                        "Please check:\n" +
                        "1. Your API key in gradle.properties\n" +
                        "2. Key should start with 'gsk_'\n" +
                        "3. Get a valid key from https://console.groq.com/");
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
