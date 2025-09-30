package com.example.integration.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class UsageStatsDTO {
    private String userId;
    private String userType; // new_user, free_user, paid_user
    private List<NetworkUsage> networkUsage;
    
    @Data
    public static class NetworkUsage {
        private String networkName;
        private String period; // daily, monthly
        private Integer used;
        private Integer limit; // null = unlimited
        private Integer remaining; // null = unlimited
        private Integer tokensUsed;
    }
}

