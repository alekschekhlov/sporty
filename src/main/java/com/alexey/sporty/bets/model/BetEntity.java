package com.alexey.sporty.bets.model;


import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Data
@Table(name = "bets")
public class BetEntity {
    
    @Id
    private UUID betId;
    private UUID userId;
    private UUID eventId;
    private UUID eventMarketId;
    private UUID eventWinnerId;
    private Double betAmount;
    private LocalDateTime settlementInitiatedAt;
    private LocalDateTime settlementCompletedAt;
}
