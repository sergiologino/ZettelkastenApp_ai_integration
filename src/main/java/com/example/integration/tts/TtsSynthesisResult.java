package com.example.integration.tts;

public record TtsSynthesisResult(int sampleRate, String format, String audioBase64) {}
