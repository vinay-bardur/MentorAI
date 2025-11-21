# 🎯 Diagnostic & Goal-Setting System

## ✅ Implementation Complete

### 🎯 **What's Been Implemented:**

---

## 1. **Two-Phase Chat Flow**

### **Phase 1: DIAGNOSTIC**
Triggered when user expresses emotional/ambiguous signals:
- "bored", "no purpose", "don't want", "lazy", "lost"
- "procrastin", "demotivat", "pointless", "waste time"

### **Phase 2: COACHING**
Only activated after:
- User answers diagnostic questions
- User confirms a goal
- Goal is stored in session

---

## 2. **Diagnostic Flow**

### **Trigger Detection:**
```java
private static final String[] DIAGNOSTIC_TRIGGERS = {
    "bored", "no purpose", "don't want", "lazy", "lost", 
    "procrastin", "demotivat", "pointless", "waste time"
};

private boolean needsDiagnostic(String userMessage) {
    if (confirmedGoal != null) return false;
    if (diagnosticAsked) return false;
    
    String lower = userMessage.toLowerCase();
    for (String trigger : DIAGNOSTIC_TRIGGERS) {
        if (lower.contains(trigger)) return true;
    }
    return false;
}
```

### **Diagnostic Message (Exact Format):**
```
Let me understand your situation better:

1) What is one specific task you should be doing right now (or intended to do)? 
   If none, reply "none".

2) What's the primary blocker stopping you from doing it? 
   (e.g., tired, unclear steps, no tools, fear).

3) Do you have a goal you care about? If not, reply "no goal" and I will propose 
   a practical starter goal (example: earn $1,000 in 30 days).
```

---

## 3. **Goal-Setting Subflow**

### **When User Replies "no goal":**
```
I can help you set a goal. Choose one:

💰 **Money Goal** - Earn $1,000 in 30 days (practical income plan)

🎯 **Lifestyle Goal** - Achieve world-class lifestyle (discipline + skills plan)

Reply with 'money' or 'lifestyle' to get started.
```

### **Goal Confirmation:**
```java
// User chooses "money"
confirmedGoal = "Earn $1,000 in 30 days";
addAiMessage("🎯 Goal set: " + confirmedGoal + 
    "\n\nGreat! Now let's get you there. What's your first question?");

// User chooses "lifestyle"
confirmedGoal = "Achieve world-class lifestyle through discipline and skills";
addAiMessage("🎯 Goal set: " + confirmedGoal + 
    "\n\nExcellent! Now let's build your path. What's your first question?");
```

---

## 4. **Enhanced System Prompts**

### **Panel Mode Prompt:**
```java
"SYSTEM: PANEL MENTORING (strict rules).\n"
+ "You simulate a PANEL of five mentors in this exact order: "
+ "Elon Musk, Tim Ferriss, Ilia Topuria, Steve Jobs, Kiyotaka Ayanokoji.\n"
+ "User's name: " + userName + "." + goalContext + "\n"
+ "MEMORY RULE: Session memory (diagnostic answers and confirmed goal) is provided. "
+ "Use it to reference prior facts and evolve advice.\n\n"
+ "DIAGNOSTIC RULE: If session memory does NOT contain a confirmed GOAL, "
+ "DO NOT produce mentor coaching. Instead, return ONLY diagnostic questions and STOP.\n\n"
+ "GOAL RULE: Only after user confirms a goal (stored in session memory) "
+ "you may produce coaching.\n\n"
+ "OUTPUT FORMAT: Produce exactly five labeled sections in this order: "
+ "[Elon Musk], [Tim Ferriss], [Ilia Topuria], [Steve Jobs], [Kiyotaka Ayanokoji]. "
+ "For each section give exactly 3 short actionable bullets (≤14 words each) "
+ "and 'Immediate action (30m):' with one concrete 30-minute step. "
+ "No extra paragraphs, intros, or meta commentary.\n\n"
+ "ANTI-REPEAT: Check last 3 assistant replies in memory. "
+ "If any proposed action repeats >60%, replace with 'Alternative action' "
+ "that is meaningfully different.\n\n"
+ "DOMAIN: Only discuss CAREER, SKILLS, PRODUCTIVITY, HABITS, and LIFE-CAREER STRATEGY. "
+ "For sexual, dating, illegal, or medical/therapy content, reply ONLY with: "
+ "'I can't assist with that. VisionAI focuses on career, skills, habits and discipline.'\n\n"
+ "PERSONA GUIDELINES: Elon=build/prototype, Tim=80/20 experiments, "
+ "Ilia=routine/discipline, Jobs=simplify/user-value, Ayanokoji=strategic minimalism.\n"
+ "END."
```

### **Single Mode Prompt:**
```java
"SYSTEM: SINGLE MENTOR (strict rules).\n"
+ "Simulate exactly one named mentor: " + selectedSingleMentor + ". "
+ "User's name: " + userName + "." + goalContext + "\n"
+ "Use session memory and confirmed goal. "
+ "If no goal confirmed, return diagnostic questions and stop.\n\n"
+ "OUTPUT: Provide 4-6 short action bullets (≤14 words) "
+ "and 'Immediate action (30m):' one concrete step. "
+ "Avoid repetition of last 3 assistant replies. Use persona voice.\n\n"
+ "DOMAIN and REFUSAL same as PANEL.\n"
+ "END."
```

---

## 5. **Complete Conversation Flow**

### **Example 1: Bored User → Diagnostic → Goal → Coaching**

```
User: "I'm bored and want to watch anime"

Assistant (DIAGNOSTIC):
Let me understand your situation better:

1) What is one specific task you should be doing right now (or intended to do)? 
   If none, reply "none".
2) What's the primary blocker stopping you from doing it?
3) Do you have a goal you care about? If not, reply "no goal" and I will propose 
   a practical starter goal.

---

User: "I should be studying coding but I'm tired. No goal."

Assistant (GOAL OFFER):
I can help you set a goal. Choose one:

💰 **Money Goal** - Earn $1,000 in 30 days (practical income plan)
🎯 **Lifestyle Goal** - Achieve world-class lifestyle (discipline + skills plan)

Reply with 'money' or 'lifestyle' to get started.

---

User: "money"

Assistant (GOAL CONFIRMATION):
🎯 Goal set: Earn $1,000 in 30 days

Great! Now let's get you there. What's your first question?

---

User: "How do I start?"

Assistant (COACHING - 5 Mentors):
[Elon Musk]
- Identify fastest income route: freelance coding, quick gigs, or sell a micro-product
- Build minimum viable offer today: one service you can deliver this week
- Price aggressively low for first 3 clients to build momentum fast
Immediate action (30m): List 3 skills you have, pick one to monetize

[Tim Ferriss]
- Apply 80/20: which 20% of your skills can generate 80% of $1k fastest?
- Run micro-test: post one service offer on Fiverr/Upwork today
- Automate outreach: template message to 10 potential clients daily
Immediate action (30m): Create one service listing on freelance platform

[Ilia Topuria]
- Daily discipline: 2 hours coding practice + 1 hour client outreach, no exceptions
- Build momentum: land first $50 gig this week, then scale
- Track progress: spreadsheet with daily earnings and hours worked
Immediate action (30m): Set up daily schedule, block 3 hours for income work

[Steve Jobs]
- Simplify offer: one clear service, one clear price, one clear deliverable
- Focus on value: what problem do you solve in 30 minutes for clients?
- Remove distractions: delete anime apps until you hit $1k milestone
Immediate action (30m): Write one-sentence value proposition for your service

[Kiyotaka Ayanokoji]
- Strategic analysis: which platform has lowest competition for your skill level?
- Minimal leverage: start with tasks requiring <2 hours to complete
- Optimize timing: work when clients are most active (evenings/weekends)
Immediate action (30m): Research 3 competitors, find pricing gap to exploit
```

---

## 6. **Flow Control Logic**

### **Decision Tree:**
```
User Message
    ↓
Contains trigger word? (bored, lazy, etc.)
    ↓ YES
Has confirmed goal?
    ↓ NO
Send DIAGNOSTIC message → STOP
    ↓
User replies with answers
    ↓
Contains "no goal"?
    ↓ YES
Send GOAL OFFER message → STOP
    ↓
User chooses "money" or "lifestyle"
    ↓
Store confirmed goal
Send GOAL CONFIRMATION → STOP
    ↓
User asks question
    ↓
Build Groq request with:
- System prompt (with goal context)
- Session memory (last 12 messages)
- Current user message
    ↓
Get mentor coaching (5 mentors or 1 mentor)
```

---

## 7. **Memory & State Management**

### **State Variables:**
```java
private String confirmedGoal = null;        // Stores user's confirmed goal
private boolean diagnosticAsked = false;    // Tracks if diagnostic was shown
```

### **Goal Context in Prompts:**
```java
String goalContext = confirmedGoal != null 
    ? "\nCONFIRMED GOAL: " + confirmedGoal + "\n" 
    : "";
```

### **Session Memory:**
- Diagnostic answers stored as user messages
- Goal confirmation stored as assistant message
- All messages available in session history
- Mentors can reference: "Previously you said..."

---

## 8. **Test Checklist**

### **Test 1: Diagnostic Trigger** ✅
```
User: "I'm bored"
Expected: Diagnostic message with 3 questions (no coaching)
```

### **Test 2: Goal Offer** ✅
```
User: [Answers diagnostic] "no goal"
Expected: Goal offer message (money vs lifestyle)
```

### **Test 3: Goal Confirmation** ✅
```
User: "money"
Expected: "🎯 Goal set: Earn $1,000 in 30 days"
```

### **Test 4: Coaching After Goal** ✅
```
User: "How do I start?"
Expected: 5 mentor responses tied to $1k goal
```

### **Test 5: No Diagnostic if Goal Exists** ✅
```
[Goal already set]
User: "I'm bored"
Expected: Mentor coaching (not diagnostic)
```

### **Test 6: Domain Lock** ✅
```
User: "How do I get a girlfriend?"
Expected: Refusal message about career/skills focus
```

---

## 9. **Key Features**

### **Diagnostic Phase:**
- ✅ Detects emotional/ambiguous signals
- ✅ Asks 3 specific questions
- ✅ Waits for user answers
- ✅ No coaching until goal confirmed

### **Goal-Setting:**
- ✅ Offers 2 practical goals
- ✅ Money goal: $1,000 in 30 days
- ✅ Lifestyle goal: World-class discipline
- ✅ Stores confirmed goal in session

### **Coaching Phase:**
- ✅ Only activates after goal confirmation
- ✅ All advice tied to confirmed goal
- ✅ References diagnostic answers
- ✅ Progressive, non-repetitive guidance

### **Memory:**
- ✅ Remembers diagnostic answers
- ✅ Remembers confirmed goal
- ✅ References prior conversations
- ✅ Builds progressive coaching

---

## 10. **Benefits**

✅ **Contextual**: Understands user's situation before advising  
✅ **Goal-Oriented**: All advice tied to specific goal  
✅ **Progressive**: Builds on diagnostic insights  
✅ **Non-Repetitive**: Forces novel actions each turn  
✅ **Focused**: Career/skills/productivity only  
✅ **Actionable**: Concrete steps, not generic advice  

---

## 🚀 **Result:**

**VisionAI now provides:**
- Diagnostic understanding before coaching
- Goal-oriented, contextual advice
- Progressive guidance tied to user's situation
- No more generic "just do it" responses
- Practical paths from boredom to action

**This transforms VisionAI from reactive advice to proactive, goal-driven coaching! 🎯✨**