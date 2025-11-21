package com.thana.kafka_assignment_01.service;

import com.thana.kafka_assignment_01.model.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderProcessingService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final PriceAnalyticsService aggregationService;

    @Value("${kafka.topics.orders-dlq}")
    private String dlqTopic;

    @Value("${kafka.topics.orders-retry}")
    private String retryTopic;

    @Value("${kafka.consumer.max-retry-attempts}")
    private int maxRetryAttempts;

    @Value("${kafka.consumer.retry-delay-ms}")
    private long retryDelayMs;

    /**
     * Main Kafka consumer for processing new orders.
     */
    @KafkaListener(topics = "${kafka.topics.orders}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeOrder(
        @Payload Order order,
        @Header(KafkaHeaders.RECEIVED_KEY) String key,
        @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
        @Header(KafkaHeaders.OFFSET) long offset,
        Acknowledgment ack
    ) {
        try {
            log.info("Received order: {} | Product: {} | Price: {} | Partition: {} | Offset: {}",
                order.getOrderId(), order.getProduct(), order.getPrice(), partition, offset);

            processOrder(order);
            aggregationService.addPrice(order.getPrice());

            ack.acknowledge();
            log.info("Order processed successfully: {}", order.getOrderId());

        } catch (Exception e) {
            log.error("Error processing order: {} — switching to retry handler", order.getOrderId(), e);
            handleFailure(order, 0, ack);
        }
    }

    /**
     * Kafka consumer for retry topic.
     */
    @KafkaListener(topics = "${kafka.topics.orders-retry}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeRetryOrder(
        @Payload Order order,
        @Header(value = "retry-count", defaultValue = "0") int retryCount,
        Acknowledgment ack
    ) {
        try {
            log.info("Retry attempt {} for order {}", retryCount, order.getOrderId());

            Thread.sleep(retryDelayMs);

            processOrder(order);
            aggregationService.addPrice(order.getPrice());

            ack.acknowledge();
            log.info("Order recovered after {} retries: {}", retryCount, order.getOrderId());

        } catch (Exception e) {
            log.error("Retry {} failed for order {}", retryCount, order.getOrderId(), e);
            handleFailure(order, retryCount, ack);
        }
    }

    /**
     * Business logic: simple validation.
     */
    private void processOrder(Order order) throws Exception {

        log.debug("Validating order {} | Product: {} | Price: {}",
            order.getOrderId(), order.getProduct(), order.getPrice());

        if (order.getOrderId() == null || order.getOrderId().toString().isEmpty()) {
            throw new IllegalArgumentException("Order ID cannot be empty");
        }
        if (order.getProduct() == null || order.getProduct().toString().isEmpty()) {
            throw new IllegalArgumentException("Product name cannot be empty");
        }
        if (order.getPrice() <= 0) {
            throw new IllegalArgumentException("Price must be greater than zero");
        }

        log.info("Order validated: {}", order.getOrderId());
    }

    /**
     * Retry handling.
     */
    private void handleFailure(Order order, int retryCount, Acknowledgment ack) {
        retryCount++;
        final int count = retryCount;

        if (retryCount < maxRetryAttempts) {
            log.warn("Retrying order {} (attempt {})", order.getOrderId(), retryCount);

            kafkaTemplate.send(retryTopic, order.getOrderId().toString(), order)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish to retry topic {}", order.getOrderId(), ex);
                        sendToDLQ(order, count, "Failed to publish to retry topic");
                    }
                });

        } else {
            log.error("Maximum retries exceeded for order {} — sending to DLQ", order.getOrderId());
            sendToDLQ(order, retryCount, "Max retry attempts exceeded");
        }

        ack.acknowledge();
    }

    /**
     * Send message to DLQ with reason.
     */
    private void sendToDLQ(Order order, int retryCount, String reason) {

        log.error("Sending order {} to DLQ | Reason: {} | Retries: {}",
            order.getOrderId(), reason, retryCount);

        kafkaTemplate.send(dlqTopic, order.getOrderId().toString(), order)
            .whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Order sent to DLQ successfully: {}", order.getOrderId());
                } else {
                    log.error("Failed to send order to DLQ: {}", order.getOrderId(), ex);
                }
            });
    }
}
