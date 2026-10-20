/**
 * This program is one successful mock Stripe charge. The idempotency
 * key is what a retried BK-03 confirm uses to find this charge again.
 *
 * File:   PaymentCharge.java
 * Course: AUCSC 325
 *
 */
package com.nicheflow.payment;

import java.math.BigDecimal;

public record PaymentCharge(String idempotencyKey, BigDecimal amount) {

    /**
     * PaymentCharge - This function stores the key and amount of one
     * charge the mock accepted.
     *
     * Parameters:
     * idempotencyKey - The key that must not be charged twice
     * amount - The listing price that was charged
     */
    public PaymentCharge {
    }
}
