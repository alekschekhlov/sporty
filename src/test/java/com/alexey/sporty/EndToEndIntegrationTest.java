package com.alexey.sporty;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.alexey.sporty.bets.model.BetEntity;
import com.alexey.sporty.bets.repository.BetsRepository;
import com.alexey.sporty.common.dto.BettingEventOutcome;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.utility.DockerImageName;

/**
 * Tests whole flow: prepopulates data, triggers endpoint to publish event, verifies that all bets where processed accordingly
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class EndToEndIntegrationTest {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private BetsRepository betsRepository;
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    private UUID eventId;
    private UUID[] winnerIds = new UUID[]{UUID.randomUUID(), UUID.randomUUID()};
    
    private final String settlementNotInitiatedBetsByWinnerSql = "SELECT COUNT(*) FROM bets WHERE event_id = ? AND event_winner_id = ? AND settlement_initiated_at IS NULL";
    
    private final String settlementNotInitiatedBetsByWinnerNotEqualSql = "SELECT COUNT(*) FROM bets WHERE event_id = ? AND event_winner_id != ? AND settlement_initiated_at IS NULL";
    
    private final String settlementNotCompletedBetsByWinnerSql = "SELECT COUNT(*) FROM bets WHERE event_id = ? AND event_winner_id = ? AND settlement_completed_at IS NULL";
    
    private final String settlementNotCompletedBetsByWinnerNotEqualSql = "SELECT COUNT(*) FROM bets WHERE event_id = ? AND event_winner_id != ? AND settlement_completed_at IS NULL";
    
    static final KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));
    
    static final Network rocketmqNetwork = Network.newNetwork();
    
    static final GenericContainer<?> rocketmqNamesrv =
        new GenericContainer<>(DockerImageName.parse("apache/rocketmq:5.1.4"))
            .withExposedPorts(9876)
            .withNetwork(rocketmqNetwork)
            .withNetworkAliases("rmq-namesrv")
            .withCommand("sh", "mqnamesrv");
    
    static final GenericContainer<?> rocketmqBroker =
        new GenericContainer<>(DockerImageName.parse("apache/rocketmq:5.1.4"))
            .withExposedPorts(10911, 10909)
            .withNetwork(rocketmqNetwork)
            .withNetworkAliases("broker-a")
            .withCommand("sh mqbroker -c /opt/rocketmq/conf/broker.conf -n rmq-namesrv:9876 ")
            .withFileSystemBind(
                "rocketmq-broker.conf",
                "/opt/rocketmq/conf/broker.conf",
                BindMode.READ_ONLY)
            .dependsOn(rocketmqNamesrv);
    
    static {
        kafka.start();
        rocketmqNamesrv.setPortBindings(List.of("9876:9876"));
        rocketmqBroker.setPortBindings(List.of("10911:10911", "10909:10909"));
        rocketmqNamesrv.start();
        rocketmqBroker.start();
        
        System.setProperty("spring.kafka.bootstrap-servers", kafka.getBootstrapServers());
    }
    
    @BeforeEach
    void setUp() {
        eventId = UUID.randomUUID();
        var marketId = UUID.randomUUID();
        
        List<BetEntity> bets = new ArrayList<>();
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
    
    
    @Test
    @SneakyThrows
    void testBetSettlementIntegration() {
        var winnerId = winnerIds[0];
        
        var settlementNotInitiatedOthersInitial = jdbcTemplate.queryForObject(
            settlementNotInitiatedBetsByWinnerNotEqualSql,
            Integer.class, eventId, winnerId
        );
        
        var settlementNotCompletedOthersInitial = jdbcTemplate.queryForObject(
            settlementNotCompletedBetsByWinnerNotEqualSql,
            Integer.class, eventId, winnerId
        );
        
        // 1. Trigger endpoint
        BettingEventOutcome outcome = new BettingEventOutcome();
        outcome.setEventId(eventId);
        outcome.setEventName("Test Match");
        outcome.setEventWinnerId(winnerId);
        
        //some gap to have kafka listener fully setup.
        Thread.sleep(1000);
        
        restTemplate.postForEntity("/api/events/result", outcome, Void.class);
        
        // Wait until all bets for eventId & winnerId1 have settlementInitiatedAt set
        await()
            .atMost(30, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                var unsettledWinner = jdbcTemplate.queryForObject(
                    settlementNotInitiatedBetsByWinnerSql,
                    Integer.class, eventId, winnerId
                );
                assertThat(unsettledWinner).isEqualTo(0);
            });
        
        // Wait until all bets for eventId & winnerId1 have settlementCompletedAt set
        await()
            .atMost(30, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                var unsettledWinner = jdbcTemplate.queryForObject(
                    settlementNotCompletedBetsByWinnerSql,
                    Integer.class, eventId, winnerId
                );
                assertThat(unsettledWinner).isEqualTo(0);
            });
        
        
        // Assert only winnerId1 bets are settled
        var notInitiatedOthers = jdbcTemplate.queryForObject(
            settlementNotInitiatedBetsByWinnerNotEqualSql,
            Integer.class, eventId, winnerId
        );
        
        assertThat(notInitiatedOthers).isEqualTo(settlementNotInitiatedOthersInitial);
        
        var notCompletesOthers = jdbcTemplate.queryForObject(
            settlementNotCompletedBetsByWinnerNotEqualSql,
            Integer.class, eventId, winnerId
        );
        
        assertThat(notCompletesOthers).isEqualTo(settlementNotCompletedOthersInitial);
    }
}
