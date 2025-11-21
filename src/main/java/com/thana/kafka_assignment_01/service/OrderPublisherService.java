package com.thana.kafka_assignment_01.service;

import com.thana.kafka_assignment_01.model.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderPublisherService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.orders}")
    private String ordersTopic;

    /**
     * Publish a new order to the main orders topic.
     */
    public void sendOrder(String orderId, String product, float price) {

        // Basic input validation before publishing
        if (orderId == null || orderId.isBlank()) {
            log.warn("Skipping publish: orderId is empty");
            return;
        }
        if (product == null || product.isBlank()) {
            log.warn("Skipping publish: product name is empty for order {}", orderId);
            return;
        }
        if (price <= 0) {
            log.warn("Skipping publish: price must be > 0 for order {}", orderId);
            return;
        }

        try {
            Order order = Order.newBuilder()
                .setOrderId(orderId)
                .setProduct(product)
                .setPrice(price)
                .build();

            CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(ordersTopic, orderId, order);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Order published: {} | Topic: {} | Partition: {} | Offset: {}",
                        orderId,
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
                } else {
                    log.error("Failed to publish order {}", orderId, ex);
                }
            });

        } catch (Exception ex) {
            // Extra safety in case Avro building or send() throws synchronously
            log.error("Unexpected error while building or sending order {}", orderId, ex);
        }
    }
}
