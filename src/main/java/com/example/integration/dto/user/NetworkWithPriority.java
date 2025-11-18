package com.example.integration.dto.user;

import lombok.Data;
import java.util.UUID;

@Data
public class NetworkWithPriority {
    private UUID networkId;
    private Integer priority; // Приоритет (меньше = выше приоритет, по умолчанию 100)
}

