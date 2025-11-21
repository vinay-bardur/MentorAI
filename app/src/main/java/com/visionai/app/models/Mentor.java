package com.visionai.app.models;

public class Mentor {
    public String name;
    public String role;
    public String style;
    public String systemPrompt;

    public Mentor(String name, String role, String style, String systemPrompt) {
        this.name = name;
        this.role = role;
        this.style = style;
        this.systemPrompt = systemPrompt;
    }

    @Override
    public String toString() {
        return name + " - " + role;
    }
}
