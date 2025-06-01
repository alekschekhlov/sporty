package com.alexey.sporty.bets.kafka;

import com.alexey.sporty.bets.service.BetService;
import com.alexey.sporty.common.dto.BettingEventOutcome;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaBettingEventListener {
    
    private final BetService betService;
    
    /**
     * Receives events from event-outcomes topic
     *
     * @param eventOutcome
     */
    @KafkaListener(topics = "event-outcomes", groupId = "process-event-outcome")
    public void handleEventOutcome(BettingEventOutcome eventOutcome) {
        log.info("Received event outcome message from Kafka: {}", eventOutcome);
        betService.initiateBetsSettlement(eventOutcome.getEventId(), eventOutcome.getEventWinnerId());
    }
}
