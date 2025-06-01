package com.alexey.sporty.events.api.service;

import com.alexey.sporty.common.dto.BettingEventOutcome;

/**
 * Service to process events received through API
 */
public interface BettingEventService {
    void processEventOutcome(BettingEventOutcome outcome);
}
