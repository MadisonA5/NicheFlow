/**
 * This program checks the @MaxUtf8Bytes limit. A null value passes,
 * so @NotBlank stays the only rule that reports a missing value.
 *
 * File:   MaxUtf8BytesValidator.java
 * Course: AUCSC 325
 */
package com.nicheflow.validation;

import java.nio.charset.StandardCharsets;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class MaxUtf8BytesValidator
        implements ConstraintValidator<MaxUtf8Bytes, String> {

    private int maxBytes;

    /**
     * initialize - This function reads the byte limit from the
     * annotation.
     *
     * Parameters:
     * annotation - The @MaxUtf8Bytes on the validated element
     */
    @Override
    public void initialize(MaxUtf8Bytes annotation) {
        maxBytes = annotation.value();
    }

    /**
     * isValid - This function accepts a value whose UTF-8 encoding is
     * at most the limit.
     *
     * Parameters:
     * value - The string being validated, possibly null
     * context - The validation context (unused)
     *
     * Return - True if the value is null or within the limit
     */
    @Override
    public boolean isValid(String value,
            ConstraintValidatorContext context) {
        return value == null
                || value.getBytes(StandardCharsets.UTF_8).length <= maxBytes;
    }
}
