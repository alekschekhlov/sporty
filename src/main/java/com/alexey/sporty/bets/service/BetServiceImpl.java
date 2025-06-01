package com.alexey.sporty.bets.service;

import com.alexey.sporty.bets.dto.BetSettleMessage;
import com.alexey.sporty.bets.repository.BetsRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class BetServiceImpl implements BetService {
    
    private final BetsRepository betsRepository;
    private final RocketMQTemplate rocketMQTemplate;
    private final PlatformTransactionManager transactionManager;
    
    @Value("${bet.initiate-settlement.batch.size}")
    private Integer initiateSettlementBatchSize;
    
    @Value("${bet.initiate-settlement.topic.name}")
    private String betsSettlementQueue;
    
    /**
     * Initiate settlements of winning bets. Loads batches of records to be initiated and sends individual message for each.
     * Each batch processed in separate transaction so if we fail in the middle already processed records will be not selected again.
     *
     * @param eventId
     * @param winnerId
     */
    @Override
    public void initiateBetsSettlement(UUID eventId, UUID winnerId) {
        
        log.info("Initiating bets settlement for eventId = {}, winnerId = {}", eventId, winnerId);
        var now = LocalDateTime.now();
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        int loadedRecords, processedRecords = 0;
        do {
            loadedRecords = txTemplate.execute(status -> {
                var bets = betsRepository.findBetsToInitiateSettlement(eventId, winnerId, PageRequest.of(0, initiateSettlementBatchSize));
                
                for (var bet : bets) {
                    bet.setSettlementInitiatedAt(now);
                    log.debug("Prepared settlement for bet {}", bet.getBetId());
                    rocketMQTemplate.convertAndSend(betsSettlementQueue, new BetSettleMessage(bet.getBetId()));
                }
                betsRepository.saveAll(bets);
                return bets.size();
            });
            processedRecords += loadedRecords;
        } while (loadedRecords > 0);
        
        log.info("Initiated settlement for {} records for eventId {}, winnerId {}", processedRecords, eventId, winnerId);
    }
    
    /**
     * Complete settlement for the bet. Idempotency achieved by locking record
     *
     * @param betId
     */
    @Transactional
    @Override
    public void completeBetSettlement(UUID betId) {
        log.info("Completing settlement for bet {}", betId);
        var maybeBet = betsRepository.findToCompleteSettlement(betId);
        
        if (maybeBet.isEmpty()) {
            log.warn("Bet with id = {} not exists or already settled");
            return;
        }
        
        var bet = maybeBet.get();
        bet.setSettlementCompletedAt(LocalDateTime.now());
        betsRepository.save(bet);
        
        log.info("Completed settlement for bet {}", betId);
    }
}
