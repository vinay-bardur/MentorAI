package com.visionai.app.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.IOException;

public class GroqClient {

    private static final String BASE_URL = "https://api.groq.com/";
    public static final String DEFAULT_MODEL = "llama-3.3-70b-versatile";
    
    private final GroqApiService apiService;
    private final String apiKey;

    public GroqClient(final String apiKey) {
        this.apiKey = apiKey;
        
        if (apiKey != null && apiKey.length() > 4) {
            System.out.println("API Key loaded: " + apiKey.substring(0, 4) + "...");
        }
        
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(new Interceptor() {
                    @Override
                    public Response intercept(Chain chain) throws IOException {
                        Request original = chain.request();
                        Request request = original.newBuilder()
                                .header("Authorization", "Bearer " + apiKey)
                                .build();
                        return chain.proceed(request);
                    }
                })
                .addInterceptor(logging)
                .build();

        Gson gson = new GsonBuilder().create();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        apiService = retrofit.create(GroqApiService.class);
    }

    public Call<JsonObject> sendChatRequest(String model, String systemPrompt, String history, String userMessage) {
        JsonObject body = new JsonObject();
        body.addProperty("model", model);
        body.addProperty("temperature", 0.7);
        body.addProperty("max_tokens", 1024);
        
        JsonArray messages = new JsonArray();
        
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            JsonObject sys = new JsonObject();
            sys.addProperty("role", "system");
            sys.addProperty("content", systemPrompt);
            messages.add(sys);
        }
        
        JsonObject user = new JsonObject();
        user.addProperty("role", "user");
        user.addProperty("content", userMessage);
        messages.add(user);
        
        body.add("messages", messages);
        
        System.out.println("Request: " + body.toString());
        
        return apiService.createCompletion(body);
    }

    public static String extractMessageContent(JsonObject response) {
        try {
            JsonArray choices = response.getAsJsonArray("choices");
            if (choices != null && choices.size() > 0) {
                JsonObject choice0 = choices.get(0).getAsJsonObject();
                JsonObject message = choice0.getAsJsonObject("message");
                JsonElement content = message.get("content");
                if (content != null) {
                    return content.getAsString();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "Error parsing response";
    }
}
