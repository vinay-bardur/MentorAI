package com.visionai.app;

public class AppConfig {
    
    // Mentor names in order
    public static final String MENTOR_ELON = "Elon Musk";
    public static final String MENTOR_TIM = "Tim Ferriss";
    public static final String MENTOR_ILIA = "Ilia Topuria";
    public static final String MENTOR_STEVE = "Steve Jobs";
    public static final String MENTOR_KIYOTAKA = "Kiyotaka Ayanokoji";
    
    public static final String[] MENTOR_NAMES = {
        MENTOR_ELON,
        MENTOR_TIM,
        MENTOR_ILIA,
        MENTOR_STEVE,
        MENTOR_KIYOTAKA
    };
    
    // SharedPreferences keys
    public static final String PREF_USER_NAME = "user_name";
    public static final String PREF_USER_EMAIL = "user_email";
    public static final String PREF_CHAT_MODE = "chat_mode";
    public static final String PREF_SELECTED_MENTOR = "selected_mentor";
    
    // Chat modes
    public static final String MODE_PANEL = "PANEL";
    public static final String MODE_SINGLE = "SINGLE";
    
    // Default model
    public static final String DEFAULT_MODEL = "llama-3.3-70b-versatile";
}
