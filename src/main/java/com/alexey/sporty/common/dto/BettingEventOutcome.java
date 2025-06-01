package com.alexey.sporty.common.dto;

import java.util.UUID;
import lombok.Data;

@Data
public class BettingEventOutcome {
    private UUID eventId;
    private String eventName;
    private UUID eventWinnerId;
}
