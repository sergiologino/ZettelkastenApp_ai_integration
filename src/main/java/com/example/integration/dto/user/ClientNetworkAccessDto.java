package com.example.integration.dto.user;

import lombok.Data;
import java.util.UUID;

@Data
public class ClientNetworkAccessDto {
    private UUID networkId;
    private String networkName;
    private String networkDisplayName;
    private String provider;
    private String networkType;
    private Integer priority;
}

