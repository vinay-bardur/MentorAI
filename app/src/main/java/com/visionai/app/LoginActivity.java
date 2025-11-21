package com.visionai.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import static com.visionai.app.AppConfig.*;

public class LoginActivity extends AppCompatActivity {

    private EditText nameInput;
    private EditText emailInput;
    private Button continueButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        nameInput = findViewById(R.id.nameInput);
        emailInput = findViewById(R.id.emailInput);
        continueButton = findViewById(R.id.continueButton);

        continueButton.setOnClickListener(v -> goToMain());
    }

    private void goToMain() {
        String name = nameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        
        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show();
            return;
        }
        
        SharedPreferences prefs = getSharedPreferences("VisionAI", MODE_PRIVATE);
        prefs.edit()
            .putString(PREF_USER_NAME, name)
            .putString(PREF_USER_EMAIL, email)
            .apply();
        
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
