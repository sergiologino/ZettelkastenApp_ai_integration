package com.example.integration.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@Data
@AllArgsConstructor
public class SocialPostStatsDTO {
    private long totalPosts;
    private long successfulPosts;
    private long failedPosts;
    private Map<String, Long> postsByPlatform;
    private Map<String, Long> successfulPostsByPlatform;
    private Map<String, Long> failedPostsByPlatform;
}
