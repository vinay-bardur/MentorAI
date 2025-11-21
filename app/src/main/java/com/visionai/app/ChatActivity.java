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
            return "SYSTEM INSTRUCTIONS FOR PANEL (strict):\n"
                + "You are a single AI that simulates a PANEL of five mentors in the exact order: "
                + "Elon Musk, Tim Ferriss, Ilia Topuria, Steve Jobs, Kiyotaka Ayanokoji.\n"
                + "The user's name is " + userName + ".\n\n"
                + "SESSION MEMORY: You must read and use the recent conversation history provided. "
                + "Reference prior user statements where relevant (e.g., 'Previously you said...'). Evolve advice based on that memory.\n\n"
                + "DOMAIN LOCK: You only answer topics about CAREER, SKILLS, PRODUCTIVITY, HABITS, MINDSET, and LIFE-CAREER STRATEGY. "
                + "If a user asks about sexual content, dating advice outside career, illegal activity, or medical/therapy diagnosis, reply ONLY with: "
                + "'I can't assist with that. VisionAI focuses on career, skills, habits and discipline. If you need help in those areas, tell me more about your work or goals.'\n\n"
                + "OUTPUT FORMAT (REQUIRED): For every user message return FIVE labeled sections in this exact order: "
                + "[Elon Musk], [Tim Ferriss], [Ilia Topuria], [Steve Jobs], [Kiyotaka Ayanokoji]. "
                + "For each section provide EXACTLY: three (3) short action bullets (each ≤14 words) followed by one single-line 'Immediate action (30m): ...'. "
                + "Do NOT add any other text or headings.\n\n"
                + "ANTI-REPEAT RULE: Before emitting actions, check the last 3 assistant outputs in session memory; "
                + "if any proposed action repeats >60% in meaning or wording, replace it with a novel alternative and label it 'Alternative action'. \n\n"
                + "PROGRESSIVE COACHING: At least one bullet per mentor must reference a prior user fact or previous mentor instruction (if applicable), "
                + "and propose a clear next step that builds on it.\n\n"
                + "PERSONA GUIDE: Use each mentor's voice and tactical lens:\n"
                + "- Elon Musk: first principles, build/prototype quickly, technical moonshots\n"
                + "- Tim Ferriss: 80/20 rule, experiments, micro-tests, automate\n"
                + "- Ilia Topuria: routine/discipline/training, incremental overload, momentum\n"
                + "- Steve Jobs: simplify ruthlessly, focus on product essence, remove features\n"
                + "- Kiyotaka Ayanokoji: strategic minimalism, leverage, calm analysis\n\n"
                + "BE CONCISE: Bullets only, no long paragraphs, no intros, no meta commentary, no apologies. "
                + "Use the user's name when appropriate.\n\n"
                + "TEMPERATURE: keep responses practical; prefer concrete short steps, not philosophical essays.\n"
                + "END OF INSTRUCTIONS.";
        } else {
            return "SYSTEM INSTRUCTIONS FOR SINGLE MENTOR (strict):\n"
                + "You are simulating exactly one mentor: " + selectedSingleMentor + ". "
                + "The user's name is " + userName + ".\n\n"
                + "SESSION MEMORY: Use session memory to reference past user messages. "
                + "If the user asks follow-ups, explicitly reference the last relevant user message and propose the next progressive action.\n\n"
                + "DOMAIN LOCK: You only answer topics about CAREER, SKILLS, PRODUCTIVITY, HABITS, MINDSET, and LIFE-CAREER STRATEGY. "
                + "If a user asks about sexual content, dating advice outside career, illegal activity, or medical/therapy diagnosis, reply ONLY with: "
                + "'I can't assist with that. VisionAI focuses on career, skills, habits and discipline. If you need help in those areas, tell me more about your work or goals.'\n\n"
                + "OUTPUT FORMAT: Provide 4-6 short actionable steps (each ≤14 words) and one 'Immediate action (30m): ...' line. \n\n"
                + "ANTI-REPEAT RULE: Do not repeat suggestions that appear in the last 3 assistant replies; provide novel, prioritized steps.\n\n"
                + "PERSONA: Use a voice faithful to the chosen mentor: specific tactics, prioritized sequence, and clear immediate action. \n\n"
                + "BE CONCISE: No meta commentary, no filler, no apologies. Keep it clean, crisp, concise and actionable.\n"
                + "END OF INSTRUCTIONS.";
        }
    }
    
    private String getRecentHistory(int maxTurns) {
        // Get last N messages (excluding the current user message)
        int totalMessages = cachedMessages.size() - 1;
        int startIndex = Math.max(0, totalMessages - maxTurns);
        
        StringBuilder history = new StringBuilder();
        for (int i = startIndex; i < totalMessages; i++) {
            Message m = cachedMessages.get(i);
            history.append(m.isUser ? "User: " : "Assistant: ");
            history.append(m.content).append("\n\n");
        }
        return history.toString();
    }
    
    private void callGroq(String userText) {
        setLoading(true);
        
        String systemPrompt = buildSystemPrompt();
        
        // Get last 12 messages (6 user + 6 assistant turns) for context
        String recentHistory = getRecentHistory(12);
        
        // Build full context: system prompt + history + current message
        String fullContext = systemPrompt + "\n\n--- RECENT CONVERSATION HISTORY ---\n" + recentHistory;

        Call<JsonObject> call = groqClient.sendChatRequest(
                DEFAULT_MODEL,
                fullContext,
                "",  // History already included in system prompt
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
