package com.alexey.sporty.config;

import com.alexey.sporty.bets.model.BetEntity;
import com.alexey.sporty.bets.repository.BetsRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
@Profile("dev")
public class SampleDataInitializer implements CommandLineRunner {
    
    private final UUID eventId = UUID.fromString("273654f0-acb5-4136-b326-e95e2db550b1");
    private UUID[] winnerIds = new UUID[]{UUID.fromString("e9652be9-a6da-4a2e-8fbc-50b4e3c9ef55"),
        UUID.fromString("c8f74a34-44e3-41aa-9bea-34e18d51ebba")};
    
    private final BetsRepository betsRepository;
    
    @Override
    public void run(String... args) {
        List<BetEntity> bets = new ArrayList<>();
        var marketId = UUID.randomUUID();
        var random = new Random();
        for (int i = 0; i < 1000; i++) {
            bets.add(BetEntity.builder()
                         .betId(UUID.randomUUID())
                         .eventId(eventId)
                         .eventWinnerId(winnerIds[random.nextInt(2)])
                         .userId(UUID.randomUUID())
                         .eventMarketId(marketId)
                         .betAmount(random.nextDouble(1000))
                         .build());
        }
        betsRepository.saveAll(bets);
    }
}
