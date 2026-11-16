/**
 * This program is a validation annotation that limits a string by
 * its UTF-8 byte length rather than its character count. BCrypt
 * hashes the UTF-8 bytes of a password and accepts at most 72, while
 * @Size counts characters, so the two only agree for ASCII input.
 *
 * File:   MaxUtf8Bytes.java
 * Course: AUCSC 325
 *
 * References:
 * https://jakarta.ee/specifications/bean-validation/3.0/jakarta-bean-validation-spec-3.0.html#constraintsdefinitionimplementation
 *     - Defining a custom constraint
 */
package com.nicheflow.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Documented
@Constraint(validatedBy = MaxUtf8BytesValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface MaxUtf8Bytes {

    /**
     * value - The largest allowed length in UTF-8 bytes.
     */
    int value();

    String message() default
            "must be at most {value} bytes; accented letters and emoji "
            + "count as more than one byte";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
