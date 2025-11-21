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
    private String confirmedGoal = null;
    private boolean diagnosticAsked = false;

    private final List<Message> cachedMessages = new ArrayList<>();
    
    // Diagnostic trigger keywords
    private static final String[] DIAGNOSTIC_TRIGGERS = {
        "bored", "no purpose", "don't want", "lazy", "lost", 
        "procrastin", "demotivat", "pointless", "waste time"
    };

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
        
        // Check if diagnostic needed
        if (needsDiagnostic(text)) {
            diagnosticAsked = true;
            addAiMessage(getDiagnosticMessage());
            return;
        }
        
        // Check if user replied "no goal" after diagnostic
        if (diagnosticAsked && confirmedGoal == null && text.toLowerCase().contains("no goal")) {
            addAiMessage(getGoalOfferMessage());
            return;
        }
        
        // Check if user is choosing a goal
        if (diagnosticAsked && confirmedGoal == null) {
            String lower = text.toLowerCase();
            if (lower.contains("money") || lower.contains("$")) {
                confirmedGoal = "Earn $1,000 in 30 days";
                addAiMessage("🎯 Goal set: " + confirmedGoal + "\n\nGreat! Now let's get you there. What's your first question?");
                return;
            } else if (lower.contains("lifestyle") || lower.contains("world")) {
                confirmedGoal = "Achieve world-class lifestyle through discipline and skills";
                addAiMessage("🎯 Goal set: " + confirmedGoal + "\n\nExcellent! Now let's build your path. What's your first question?");
                return;
            }
        }
        
        callGroq(text);
    }

    private void scrollToBottom() {
        chatRecyclerView.post(() -> chatRecyclerView.smoothScrollToPosition(chatAdapter.getItemCount() - 1));
    }

    private void setLoading(boolean loading) {
        loadingIndicator.setVisibility(loading ? View.VISIBLE : View.GONE);
        sendButton.setEnabled(!loading);
    }

    private boolean needsDiagnostic(String userMessage) {
        if (confirmedGoal != null) return false;
        if (diagnosticAsked) return false;
        
        String lower = userMessage.toLowerCase();
        for (String trigger : DIAGNOSTIC_TRIGGERS) {
            if (lower.contains(trigger)) return true;
        }
        return false;
    }
    
    private String getDiagnosticMessage() {
        return "Let me understand your situation better:\n\n"
            + "1) What is one specific task you should be doing right now (or intended to do)? If none, reply \"none\".\n\n"
            + "2) What's the primary blocker stopping you from doing it? (e.g., tired, unclear steps, no tools, fear).\n\n"
            + "3) Do you have a goal you care about? If not, reply \"no goal\" and I will propose a practical starter goal (example: earn $1,000 in 30 days).";
    }
    
    private String getGoalOfferMessage() {
        return "I can help you set a goal. Choose one:\n\n"
            + "💰 **Money Goal** - Earn $1,000 in 30 days (practical income plan)\n\n"
            + "🎯 **Lifestyle Goal** - Achieve world-class lifestyle (discipline + skills plan)\n\n"
            + "Reply with 'money' or 'lifestyle' to get started.";
    }
    
    private String buildSystemPrompt() {
        String goalContext = confirmedGoal != null ? "\nCONFIRMED GOAL: " + confirmedGoal + "\n" : "";
        
        if (MODE_PANEL.equals(chatMode)) {
            return "SYSTEM: PANEL MENTORING (strict rules).\n"
                + "You simulate a PANEL of five mentors in this exact order: Elon Musk, Tim Ferriss, Ilia Topuria, Steve Jobs, Kiyotaka Ayanokoji.\n"
                + "User's name: " + userName + "." + goalContext + "\n"
                + "MEMORY RULE: Session memory (diagnostic answers and confirmed goal) is provided. Use it to reference prior facts and evolve advice.\n\n"
                + "DIAGNOSTIC RULE: If session memory does NOT contain a confirmed GOAL, DO NOT produce mentor coaching. "
                + "Instead, return ONLY diagnostic questions and STOP.\n\n"
                + "GOAL RULE: Only after user confirms a goal (stored in session memory) you may produce coaching.\n\n"
                + "OUTPUT FORMAT: Produce exactly five labeled sections in this order: [Elon Musk], [Tim Ferriss], [Ilia Topuria], [Steve Jobs], [Kiyotaka Ayanokoji]. "
                + "For each section give exactly 3 short actionable bullets (≤14 words each) and 'Immediate action (30m):' with one concrete 30-minute step. "
                + "No extra paragraphs, intros, or meta commentary.\n\n"
                + "ANTI-REPEAT: Check last 3 assistant replies in memory. If any proposed action repeats >60%, replace with 'Alternative action' that is meaningfully different.\n\n"
                + "DOMAIN: Only discuss CAREER, SKILLS, PRODUCTIVITY, HABITS, and LIFE-CAREER STRATEGY. "
                + "For sexual, dating, illegal, or medical/therapy content, reply ONLY with: 'I can't assist with that. VisionAI focuses on career, skills, habits and discipline.'\n\n"
                + "PERSONA GUIDELINES: Elon=build/prototype, Tim=80/20 experiments, Ilia=routine/discipline, Jobs=simplify/user-value, Ayanokoji=strategic minimalism.\n"
                + "END.";
        } else {
            return "SYSTEM: SINGLE MENTOR (strict rules).\n"
                + "Simulate exactly one named mentor: " + selectedSingleMentor + ". "
                + "User's name: " + userName + "." + goalContext + "\n"
                + "Use session memory and confirmed goal. If no goal confirmed, return diagnostic questions and stop.\n\n"
                + "OUTPUT: Provide 4-6 short action bullets (≤14 words) and 'Immediate action (30m):' one concrete step. "
                + "Avoid repetition of last 3 assistant replies. Use persona voice.\n\n"
                + "DOMAIN and REFUSAL same as PANEL.\n"
                + "END.";
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
