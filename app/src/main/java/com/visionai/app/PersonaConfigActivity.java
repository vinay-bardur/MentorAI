package com.visionai.app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.visionai.app.adapters.MentorAdapter;
import com.visionai.app.models.Mentor;

import java.util.ArrayList;
import java.util.List;

public class PersonaConfigActivity extends AppCompatActivity implements MentorAdapter.OnMentorClickListener {

    private RecyclerView mentorRecyclerView;
    private Button startChatButton;
    private MentorAdapter mentorAdapter;

    private List<Mentor> mentors;
    private Mentor selectedMentor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_persona_config);

        mentorRecyclerView = findViewById(R.id.mentorRecyclerView);
        startChatButton = findViewById(R.id.startChatButton);

        setupMentors();
        setupRecyclerView();

        startChatButton.setOnClickListener(v -> startChat());
    }

    private void setupMentors() {
        mentors = new ArrayList<>();
        mentors.add(new Mentor(
                "Elon Musk",
                "First Principles & Moonshots",
                "Ruthlessly logical, ambitious, technical",
                "You are an Elon Musk–style mentor. Use first principles, be direct, focus on bold solutions."
        ));
        mentors.add(new Mentor(
                "Tim Ferriss",
                "Optimization & Experiments",
                "Curious, systems thinker, practical",
                "You are a Tim Ferriss–style mentor. Focus on 80/20, experiments, and practical steps."
        ));
        mentors.add(new Mentor(
                "Ilia Topuria",
                "Champion Mindset & Discipline",
                "Focused, disciplined, champion mentality",
                "You are an Ilia Topuria–style mentor. Focus on discipline, mental toughness, and champion mindset."
        ));
        mentors.add(new Mentor(
                "Kobe Bryant",
                "Mamba Mentality",
                "Brutally honest, discipline-focused",
                "You are a Kobe Bryant–style mentor. Emphasize discipline, work ethic, and competitive mindset."
        ));
        mentors.add(new Mentor(
                "Steve Jobs",
                "Product & Design",
                "High standards, product-obsessed",
                "You are a Steve Jobs–style mentor. Focus on simplicity, product excellence, and user experience."
        ));


    }

    private void setupRecyclerView() {
        mentorRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mentorAdapter = new MentorAdapter(mentors, this);
        mentorRecyclerView.setAdapter(mentorAdapter);
    }

    @Override
    public void onMentorClick(Mentor mentor, int position) {
        selectedMentor = mentor;
        mentorAdapter.setSelectedPosition(position);
        startChatButton.setEnabled(true);
    }

    private void startChat() {
        if (selectedMentor == null) return;

        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("mentor_name", selectedMentor.name);
        intent.putExtra("mentor_prompt", selectedMentor.systemPrompt);
        startActivity(intent);
        finish();
    }
}
