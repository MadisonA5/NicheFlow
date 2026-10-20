/**
 * This program is the mocked Stripe layer for BK-03. It keeps one
 * charge per idempotency key so a retried confirm does not
 * double-charge. No money moves.
 *
 * File:   MockStripePaymentClient.java
 * Course: AUCSC 325
 *
 * References:
 * https://docs.stripe.com/api/idempotent_requests
 *     - Stripe idempotency keys
 */
package com.nicheflow.payment;

import java.math.BigDecimal;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Component;

@Component
public class MockStripePaymentClient implements PaymentClient {

    private final ConcurrentMap<String, PaymentCharge> chargesByKey =
            new ConcurrentHashMap<>();

    private final AtomicInteger chargeCount = new AtomicInteger();

    /**
     * charge - This function stores a charge the first time a key is
     * seen and returns that same charge on every later call.
     *
     * Parameters:
     * amount - The listing price to charge
     * idempotencyKey - The key shared by retries of one confirm
     *
     * Return - The first charge stored for that key
     */
    @Override
    public PaymentCharge charge(BigDecimal amount, String idempotencyKey) {
        // To keep the first charge when two confirms race on one key
        return chargesByKey.computeIfAbsent(idempotencyKey, key -> {
            chargeCount.incrementAndGet();
            return new PaymentCharge(key, amount);
        });
    }

    /**
     * chargeCount - This function returns how many distinct keys have
     * been charged. A retry of a known key does not increase it.
     *
     * Return - The number of first-time charges
     */
    public int chargeCount() {
        return chargeCount.get();
    }

    /**
     * chargeFor - This function returns the charge stored for a key.
     *
     * Parameters:
     * idempotencyKey - The key to look up
     *
     * Return - The stored charge, or null when the key is new
     */
    public PaymentCharge chargeFor(String idempotencyKey) {
        return chargesByKey.get(idempotencyKey);
    }
}
