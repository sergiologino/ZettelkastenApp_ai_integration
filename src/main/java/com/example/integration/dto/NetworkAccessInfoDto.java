package com.example.integration.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NetworkAccessInfoDto {
    private UUID accessId;
    private UUID networkId;
    private String networkDisplayName;
    private String networkProvider;
    private String networkType;
    private Integer dailyRequestLimit;
    private Integer monthlyRequestLimit;
    private Integer priority;
}

