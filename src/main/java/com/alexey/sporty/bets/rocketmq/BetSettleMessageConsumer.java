package com.alexey.sporty.bets.rocketmq;

import com.alexey.sporty.bets.dto.BetSettleMessage;
import com.alexey.sporty.bets.service.BetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RocketMQMessageListener(
    topic = "${bet.initiate-settlement.topic.name}",
    consumerGroup = "${rocketmq.consumer.group}"
)
@RequiredArgsConstructor
public class BetSettleMessageConsumer implements RocketMQListener<BetSettleMessage> {
    
    private final BetService betService;
    
    @Override
    public void onMessage(BetSettleMessage message) {
        log.info("Received SettleBetMessage: {}", message);
        
        betService.completeBetSettlement(message.getBetId());
        
        log.info("SettleBetMessage {} processed", message);
    }
}
