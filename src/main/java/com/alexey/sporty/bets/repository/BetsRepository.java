package com.alexey.sporty.bets.repository;

import com.alexey.sporty.bets.model.BetEntity;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface BetsRepository extends JpaRepository<BetEntity, UUID> {
    
    /**
     * Find all bets by eventId and winnerId and locks records to prevent modifications be made by other transactions.
     *
     * @param eventId
     * @param eventWinnerId
     * @param pageable
     * @return
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM BetEntity b WHERE b.eventId = :eventId AND b.eventWinnerId = :eventWinnerId AND b.settlementInitiatedAt IS NULL")
    List<BetEntity> findBetsToInitiateSettlement(UUID eventId, UUID eventWinnerId, Pageable pageable);
    
    /**
     * Find unsettled bet by id. Locks the record to prevent modification by other transaction.
     *
     * @param betId
     * @return
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM BetEntity b WHERE b.betId = :betId AND b.settlementCompletedAt IS NULL")
    Optional<BetEntity> findToCompleteSettlement(UUID betId);
    
}
