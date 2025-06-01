package com.alexey.sporty.events.api.controller;

import com.alexey.sporty.common.dto.BettingEventOutcome;
import com.alexey.sporty.events.api.service.BettingEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller to handle Evens through API
 */
@Slf4j
@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class BettingEventsController {
    
    private final BettingEventService betEventService;
    
    /**
     * Endpoint to publish Event result to the system
     *
     * @param eventOutcome
     */
    @PostMapping("/result")
    public void processEventResult(@RequestBody BettingEventOutcome eventOutcome) {
        log.info("Received event result to process: {}", eventOutcome);
        betEventService.processEventOutcome(eventOutcome);
    }
    
}
