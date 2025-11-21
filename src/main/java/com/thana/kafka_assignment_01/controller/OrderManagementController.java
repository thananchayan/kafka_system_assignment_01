package com.thana.kafka_assignment_01.controller;

import java.util.List;
import java.util.Map;

import com.thana.kafka_assignment_01.service.DeadLetterQueueService;
import com.thana.kafka_assignment_01.service.OrderPublisherService;
import com.thana.kafka_assignment_01.service.PriceAnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderManagementController {

    private final OrderPublisherService producerService;
    private final PriceAnalyticsService aggregationService;
    private final DeadLetterQueueService deadLetterQueueService;

    @PostMapping("/send")
    public ResponseEntity<Map<String, Object>> sendOrder(@RequestBody OrderRequest request) {
        try {
            // Basic validation
            if (request.orderId() == null || request.orderId().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Order ID is required"
                ));
            }
            if (request.product() == null || request.product().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Product name is required"
                ));
            }
            if (request.price() <= 0) {
                return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Price must be greater than zero"
                ));
            }

            log.info("REST API: Sending order - ID: {}, Product: {}, Price: {}",
                request.orderId(), request.product(), request.price());

            producerService.sendOrder(request.orderId(), request.product(), request.price());

            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Order sent to Kafka",
                "orderId", request.orderId(),
                "product", request.product(),
                "price", request.price()
            ));
        } catch (Exception e) {
            log.error("Error sending order", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "error",
                "message", "Failed to send order: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/send-batch")
    public ResponseEntity<Map<String, Object>> sendBatchOrders(@RequestBody List<OrderRequest> orders) {
        try {
            if (orders == null || orders.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Order list cannot be empty"
                ));
            }

            log.info("REST API: Sending batch of {} orders", orders.size());
            int successCount = 0;
            int failCount = 0;

            for (OrderRequest order : orders) {
                try {
                    if (order.orderId() != null && !order.orderId().trim().isEmpty()
                        && order.product() != null && !order.product().trim().isEmpty()
                        && order.price() > 0) {
                        producerService.sendOrder(order.orderId(), order.product(), order.price());
                        successCount++;
                    } else {
                        log.warn("Skipping invalid order: {}", order);
                        failCount++;
                    }
                } catch (Exception e) {
                    log.error("Failed to send order: {}", order.orderId(), e);
                    failCount++;
                }
            }

            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Batch processing complete",
                "totalOrders", orders.size(),
                "successCount", successCount,
                "failCount", failCount
            ));
        } catch (Exception e) {
            log.error("Error sending batch orders", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "error",
                "message", "Failed to send batch orders: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        log.info("REST API: Fetching aggregation stats");
        PriceAnalyticsService.AggregationStats stats = aggregationService.getStats();
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "stats", stats
        ));
    }

    @PostMapping("/stats/reset")
    public ResponseEntity<Map<String, String>> resetStats() {
        log.info("REST API: Resetting aggregation stats");
        aggregationService.reset();

        return ResponseEntity.ok(Map.of(
            "status", "success",
            "message", "Aggregation statistics reset"
        ));
    }

    @GetMapping("/failed")
    public ResponseEntity<Map<String, Object>> getFailedOrders() {
        log.info("REST API: Fetching failed orders from DLQ");
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "failedOrderCount", deadLetterQueueService.getFailedOrderCount(),
            "failedOrders", deadLetterQueueService.getFailedOrders()
        ));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "Kafka Order System",
            "totalOrdersProcessed", aggregationService.getTotalOrders().get(),
            "currentAverage", aggregationService.getRunningAverage().get(),
            "failedOrders", deadLetterQueueService.getFailedOrderCount()
        ));
    }

    // Request DTO
    public record OrderRequest(
        String orderId,
        String product,
        float price
    ) {}
}
