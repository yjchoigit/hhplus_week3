package com.hhplus.ecommerce.base.config.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hhplus.ecommerce.domain.order.event.dto.OrderPaymentCompleteForKafkaEvent;
import com.hhplus.ecommerce.domain.outbox.OutboxEnums;
import com.hhplus.ecommerce.domain.outbox.entity.Outbox;
import com.hhplus.ecommerce.domain.payment.entity.Payment;
import com.hhplus.ecommerce.service.outbox.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
@RequiredArgsConstructor
public class KafkaProducer {
    private static final String TOPIC = "order_events_topic";
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final OutboxService outboxService;

    public void sendMessage(Long buyerId, Payment payment) {
        // Outbox 이벤트 저장
        Outbox outbox = Outbox.builder()
                .aggregateType("Payment")
                .aggregateId(payment.getPaymentId())
                .eventType("orderPaymentComplete")
                .payload(serializeEvent(OrderPaymentCompleteForKafkaEvent.toPayload(buyerId, payment)))
                .status(OutboxEnums.Status.INIT)
                .build();
        outboxService.addOutbox(outbox);
    }

    // 이벤트를 JSON으로 직렬화
    private String serializeEvent(OrderPaymentCompleteForKafkaEvent event) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize event", e);
        }
    }

    public void sendMessageCallback(String message) {
        CompletableFuture<SendResult<String, String>> future = this.kafkaTemplate.send(TOPIC, message);
        future.whenComplete((result, ex) -> {
           if (ex == null) {
               log.info("Send message callback success");
           } else {
                log.error("Send message callback error", ex);
           }
        });
    }
}