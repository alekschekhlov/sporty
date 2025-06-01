package com.alexey.sporty.bets.service;

import java.util.UUID;

public interface BetService {
    void initiateBetsSettlement(UUID eventId, UUID winnerId);
    
    void completeBetSettlement(UUID betId);
}
