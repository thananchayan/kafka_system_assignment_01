package com.thana.kafka_assignment_01.service;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
public class PriceAnalyticsService {

    // Running average of all processed order prices
    @Getter
    private final AtomicReference<Double> runningAverage = new AtomicReference<>(0.0);

    // Total number of processed orders
    @Getter
    private final AtomicInteger totalOrders = new AtomicInteger(0);

    // Total sum of prices for computing averages
    private final AtomicReference<Double> totalSum = new AtomicReference<>(0.0);

    // Min/max tracking
    @Getter
    private volatile double minPrice = Double.MAX_VALUE;

    @Getter
    private volatile double maxPrice = Double.MIN_VALUE;

    /**
     * Add a new price entry and update stats.
     */
    public synchronized void addPrice(float price) {

        int currentCount = totalOrders.get();
        double currentSum = totalSum.get();

        double newSum = currentSum + price;
        int newCount = currentCount + 1;

        totalSum.set(newSum);
        totalOrders.set(newCount);

        double newAverage = newSum / newCount;
        runningAverage.set(newAverage);

        // Track min/max
        if (price < minPrice) minPrice = price;
        if (price > maxPrice) maxPrice = price;

        log.info(
            "Price Update -> Price: {} | Avg: {} | Count: {} | Min: {} | Max: {}",
            price, newAverage, newCount, minPrice, maxPrice
        );
    }

    /**
     * Get snapshot object containing all computed stats.
     */
    public AggregationStats getStats() {
        return new AggregationStats(
            runningAverage.get(),
            totalOrders.get(),
            minPrice == Double.MAX_VALUE ? 0.0 : minPrice,
            maxPrice == Double.MIN_VALUE ? 0.0 : maxPrice,
            totalSum.get()
        );
    }

    /**
     * Reset all metrics.
     */
    public void reset() {
        runningAverage.set(0.0);
        totalOrders.set(0);
        totalSum.set(0.0);
        minPrice = Double.MAX_VALUE;
        maxPrice = Double.MIN_VALUE;

        log.info("Price analytics statistics have been reset");
    }

    /**
     * Immutable record used as API response.
     */
    public record AggregationStats(
        double runningAverage,
        int totalOrders,
        double minPrice,
        double maxPrice,
        double totalSum
    ) {}
}
