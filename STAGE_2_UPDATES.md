# 🎯 Stage 2 Updates - UI & Behavior Enhancements

## ✅ Changes Completed

### 1. Replaced Kobe Bryant with Kiyotaka Ayanokoji
**Files Modified:**
- `PersonaConfigActivity.java`
- `AppConfig.java` (new)

**Before:**
```java
mentors.add(new Mentor(
    "Kobe Bryant",
    "Mamba Mentality",
    ...
));
```

**After:**
```java
mentors.add(new Mentor(
    MENTOR_KIYOTAKA,  // "Kiyotaka Ayanokoji"
    "Strategic & Analytical",
    "Strategic, calm, analytic",
    "You are a Kiyotaka Ayanokoji–style mentor..."
));
```

**New Mentor Order:**
1. Elon Musk — First Principles & Moonshots
2. Tim Ferriss — Optimization & Experiments
3. Ilia Topuria — Champion Mindset & Discipline
4. Steve Jobs — Product & Design
5. Kiyotaka Ayanokoji — Strategic & Analytical

---

### 2. Centralized Configuration (AppConfig.java)
**New File:** `app/src/main/java/com/visionai/app/AppConfig.java`

```java
public class AppConfig {
    // Mentor names
    public static final String MENTOR_ELON = "Elon Musk";
    public static final String MENTOR_TIM = "Tim Ferriss";
    public static final String MENTOR_ILIA = "Ilia Topuria";
    public static final String MENTOR_STEVE = "Steve Jobs";
    public static final String MENTOR_KIYOTAKA = "Kiyotaka Ayanokoji";
    
    public static final String[] MENTOR_NAMES = {
        MENTOR_ELON, MENTOR_TIM, MENTOR_ILIA, 
        MENTOR_STEVE, MENTOR_KIYOTAKA
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
```

---

### 3. Chat Mode Toggle (Panel 1v5 / Single 1v1)
**Files Modified:**
- `ChatActivity.java`
- `activity_chat.xml`

**UI Added:**
- Two toggle buttons: "Panel (1v5)" and "Single (1v1)"
- Dropdown mentor selector (visible only in Single mode)
- Persists mode and selected mentor in SharedPreferences

**System Prompts:**

**Panel Mode (1v5):**
```java
"You are a single AI simulating a PANEL of five mentors: " +
"Elon Musk, Tim Ferriss, Ilia Topuria, Steve Jobs, and Kiyotaka Ayanokoji. " +
"The user is [userName]. " +
"For every user message respond with 5 labeled sections exactly in this order: " +
"[Elon Musk], [Tim Ferriss], [Ilia Topuria], [Steve Jobs], [Kiyotaka Ayanokoji]. " +
"Use bullet points and short actionable tasks per mentor. " +
"Do not include extra meta commentary."
```

**Single Mode (1v1):**
```java
"You are simulating the single mentor [selectedMentor]. " +
"The user is [userName]. " +
"Speak only as this mentor. Provide direct, actionable guidance in their style."
```

---

### 4. Username Integration
**Files Modified:**
- `LoginActivity.java`
- `ChatActivity.java`

**Login Changes:**
```java
// Save to SharedPreferences
SharedPreferences prefs = getSharedPreferences("VisionAI", MODE_PRIVATE);
prefs.edit()
    .putString(PREF_USER_NAME, name)
    .putString(PREF_USER_EMAIL, email)
    .apply();
```

**Chat Usage:**
```java
// Load username
userName = prefs.getString(PREF_USER_NAME, "User");
if (TextUtils.isEmpty(userName)) userName = "User";

// Inject into system prompt
"The user is " + userName + ". "
```

---

### 5. Centered Login Card
**File Modified:** `activity_login.xml`

**Before:**
```xml
<ScrollView
    android:padding="24dp">
    <LinearLayout
        android:gravity="center_horizontal">
```

**After:**
```xml
<ScrollView
    android:fillViewport="true">
    <LinearLayout
        android:gravity="center"
        android:padding="24dp"
        android:layout_gravity="center">
```

**Result:** Card is now vertically and horizontally centered

---

### 6. Email Validation
**File Modified:** `LoginActivity.java`

```java
private void goToMain() {
    String email = emailInput.getText().toString().trim();
    
    if (TextUtils.isEmpty(email)) {
        Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show();
        return;
    }
    // ... save and continue
}
```

---

## 🧪 How to Test (6 Quick Things)

### 1. Test Login & Username
- Open app → Enter name "John" and email
- Continue → Check SharedPreferences saved
- Start chat → Send message → AI should address you as "John"

### 2. Test Mentor Replacement
- Go to mentor selection
- Verify 5 mentors appear in order
- Confirm "Kiyotaka Ayanokoji" is 5th (not Kobe Bryant)

### 3. Test Panel Mode (Default)
- Start new chat
- Should show "Panel (1v5)" button highlighted
- Title shows "VisionAI · 5 Mentors"
- Send message → Get 5 labeled responses

### 4. Test Single Mode
- Tap "Single (1v1)" button
- Mentor dropdown appears
- Select "Tim Ferriss"
- Title changes to "Tim Ferriss"
- Send message → Get single mentor response

### 5. Test Mode Persistence
- Switch to Single mode, select "Steve Jobs"
- Close app
- Reopen same conversation
- Should remember Single mode + Steve Jobs

### 6. Test Centered Login
- Open login screen
- Card should be centered vertically
- Try on different screen sizes

---

## 📝 Files Changed Summary

### New Files:
- ✅ `AppConfig.java` - Centralized constants

### Modified Files:
- ✅ `PersonaConfigActivity.java` - Kiyotaka mentor, use constants
- ✅ `LoginActivity.java` - Save user data, email validation
- ✅ `ChatActivity.java` - Mode toggle, system prompt logic, username
- ✅ `activity_login.xml` - Centered card
- ✅ `activity_chat.xml` - Toggle buttons, mentor selector

### Unchanged (Safe):
- ✅ Network code (GroqClient.java)
- ✅ Model constant (llama-3.3-70b-versatile)
- ✅ Database schema
- ✅ Conversation/Message models

---

## ✅ Safety Checklist

- ✅ No network code changes
- ✅ No deprecated models used
- ✅ Database unchanged
- ✅ Existing conversations still work
- ✅ All mentor selection logic intact
- ✅ Delete functionality preserved
- ✅ Apple-style UI maintained

---

## 🚀 Ready to Build!

All changes are minimal, surgical, and safe. The app should build and run without issues.