package com.thana.kafka_assignment_01.service;

import com.thana.kafka_assignment_01.model.Order;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
public class DeadLetterQueueService {

    // Thread-safe list to store failed order data for monitoring/UI
    private final List<FailedOrder> failedOrders =
        Collections.synchronizedList(new ArrayList<>());

    private static final DateTimeFormatter formatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Kafka listener for DLQ topic
     */
    @KafkaListener(
        topics = "${kafka.topics.orders-dlq}",
        groupId = "${spring.kafka.consumer.group-id}-dlq"
    )
    public void consumeDLQ(
        @Payload Order order,
        @Header(KafkaHeaders.RECEIVED_KEY) String key,
        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
        @Header(KafkaHeaders.OFFSET) long offset,
        Acknowledgment ack
    ) {

        String timestamp = LocalDateTime.now().format(formatter);

        log.error("======= DEAD LETTER MESSAGE RECEIVED =======");
        log.error("Timestamp : {}", timestamp);
        log.error("Order ID  : {}", order.getOrderId());
        log.error("Product   : {}", order.getProduct());
        log.error("Price     : {}", order.getPrice());
        log.error("Partition : {}", partition);
        log.error("Offset    : {}", offset);
        log.error("============================================");

        // Save failed message details for later inspection
        failedOrders.add(new FailedOrder(
            order.getOrderId().toString(),
            order.getProduct().toString(),
            order.getPrice(),
            timestamp,
            partition,
            offset
        ));

        // Acknowledge DLQ message to avoid reprocessing
        ack.acknowledge();
    }

    // Public helper methods (optional for UI or API exposure)
    public List<FailedOrder> getFailedOrders() {
        return new ArrayList<>(failedOrders);
    }

    public int getFailedOrderCount() {
        return failedOrders.size();
    }

    public void clearFailedOrders() {
        failedOrders.clear();
        log.info("DLQ records cleared successfully.");
    }

    /**
     * Record representing failed message metadata.
     */
    public record FailedOrder(
        String orderId,
        String product,
        float price,
        String timestamp,
        int partition,
        long offset
    ) {}
}
