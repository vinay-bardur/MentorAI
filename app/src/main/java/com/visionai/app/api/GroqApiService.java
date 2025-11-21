package com.visionai.app.api;

import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface GroqApiService {

    @Headers({
            "Content-Type: application/json"
    })
    @POST("openai/v1/chat/completions")
    Call<JsonObject> createCompletion(@Body JsonObject body);
}
