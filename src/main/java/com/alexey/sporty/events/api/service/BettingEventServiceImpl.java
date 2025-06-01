package com.alexey.sporty.events.api.service;

import com.alexey.sporty.common.dto.BettingEventOutcome;
import java.util.concurrent.ExecutionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class BettingEventServiceImpl implements BettingEventService {
    
    private final KafkaTemplate<String, BettingEventOutcome> kafkaTemplate;
    
    @Value("${kafka.event.outcome.topic}")
    private String eventOutcomesTopic;
    
    /**
     * Sends outcome event received through API to Kafka
     *
     * @param outcome
     */
    @Override
    public void processEventOutcome(BettingEventOutcome outcome) {
        log.info("Processing event outcome {}", outcome);
        try {
            kafkaTemplate.send(eventOutcomesTopic, outcome).get();
            log.info("Sent outcome for event {} to topic {}", outcome.getEventId(), eventOutcomesTopic);
        } catch (InterruptedException | ExecutionException e) {
            log.error("Failed to process outcome for event {}", outcome.getEventId(), e);
            throw new RuntimeException(e);
        }
    }
}
