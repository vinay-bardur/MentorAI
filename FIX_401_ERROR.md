# 🔧 Fix 401 Authentication Error

## Problem
Getting "401 Unauthorized" error when sending messages to AI mentors.

## Cause
Invalid or missing Groq API key.

## Solution

### Step 1: Get Your API Key
1. Go to https://console.groq.com/
2. Sign up or log in
3. Navigate to "API Keys" section
4. Click "Create API Key"
5. Copy the key (starts with `gsk_`)

### Step 2: Add API Key to Project
1. Open `gradle.properties` in your project root
2. Find this line:
   ```
   GROQ_API_KEY=your_groq_api_key_here
   ```
3. Replace with your actual key:
   ```
   GROQ_API_KEY=gsk_YOUR_ACTUAL_KEY_HERE
   ```
4. Save the file

### Step 3: Rebuild Project
1. In Android Studio: **Build → Clean Project**
2. Then: **Build → Rebuild Project**
3. Wait for build to complete
4. Run the app

### Step 4: Verify
1. Open the app
2. Start a chat
3. Send a message
4. You should get responses from mentors (no 401 error)

## Common Issues

### Issue: Still getting 401 after adding key
**Solution**: 
- Make sure there are NO spaces around the `=` sign
- Make sure the key starts with `gsk_`
- Rebuild the project completely
- Uninstall and reinstall the app

### Issue: Key not being read
**Solution**:
```bash
# Clean everything
./gradlew clean
# Or on Windows:
gradlew.bat clean

# Then rebuild in Android Studio
```

### Issue: "Please set your Groq API key" message
**Solution**:
- The key in gradle.properties is still the placeholder
- Replace `your_groq_api_key_here` with your actual key
- Rebuild

## Verification Checklist
- [ ] API key starts with `gsk_`
- [ ] No spaces in gradle.properties line
- [ ] Project rebuilt after adding key
- [ ] App reinstalled (not just rerun)
- [ ] Key is valid (test at https://console.groq.com/)

## Example gradle.properties
```properties
org.gradle.jvmargs=-Xmx4096m -Dfile.encoding=UTF-8
android.useAndroidX=true
android.enableJetifier=true
android.nonTransitiveRClass=true

# Add your Groq API key here (no quotes)
GROQ_API_KEY=gsk_abcdefghijklmnopqrstuvwxyz1234567890ABCDEFGH
```

## Still Not Working?
1. Check Groq console for API usage limits
2. Verify your account is active
3. Try generating a new API key
4. Check if your IP is blocked

---

**Note**: Never commit your actual API key to Git!