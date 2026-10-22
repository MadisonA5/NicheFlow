/**
 * This program is the BK-03 payment port. Confirm charges through it
 * before a booking leaves pending. A second call with the same
 * idempotency key must not create a second charge.
 *
 * File:   PaymentClient.java
 * Course: AUCSC 325
 *
 */
package com.nicheflow.payment;

import java.math.BigDecimal;

/**
* charge - This function takes the listing price once for an
* idempotency key. A repeat of that key returns the first charge.
*
* Parameters:
* amount - The listing price to charge
* idempotencyKey - The key shared by retries of one confirm
*
* Return - The charge stored for that key
*/
public interface PaymentClient {
    PaymentCharge charge(BigDecimal amount, String idempotencyKey);
}
