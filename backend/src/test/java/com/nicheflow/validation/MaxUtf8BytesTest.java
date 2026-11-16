/**
 * This program is the boundary-value suite for the @MaxUtf8Bytes(72)
 * rule on RegisterRequest.password. Each character width is tested
 * at the last length that fits in 72 bytes and the first that does
 * not: 1-byte ASCII, 2-byte é, 3-byte CJK and 4-byte emoji.
 *
 * File:   MaxUtf8BytesTest.java
 * Course: AUCSC 325
 */
package com.nicheflow.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.nicheflow.dto.RegisterRequest;
import com.nicheflow.model.Role;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class MaxUtf8BytesTest {

    private static ValidatorFactory factory;

    private static Validator validator;

    @BeforeAll
    static void createValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        factory.close();
    }

    /**
     * passwordAtTheByteLimitIsValid - This function repeats one
     * character until the password is exactly 72 bytes and expects
     * no password violation.
     *
     * Parameters:
     * character - The character to repeat
     * count - How many copies make 72 bytes
     */
    @ParameterizedTest(name = "{1} x {0} is 72 bytes and valid")
    @CsvSource({"a, 72", "é, 36", "日, 24", "😀, 18"})
    void passwordAtTheByteLimitIsValid(String character, int count) {
        assertThat(passwordViolations(character.repeat(count))).isEmpty();
    }

    /**
     * passwordOneCharacterOverTheByteLimitIsInvalid - This function
     * adds one more multi-byte character and expects the byte rule
     * to reject the password even though @Size still allows it.
     *
     * Parameters:
     * character - The character to repeat
     * count - How many copies go past 72 bytes
     */
    @ParameterizedTest(name = "{1} x {0} is over 72 bytes and invalid")
    @CsvSource({"é, 37", "日, 25", "😀, 19"})
    void passwordOneCharacterOverTheByteLimitIsInvalid(String character,
            int count) {
        assertThat(passwordViolations(character.repeat(count)))
                .extracting(ConstraintViolation::getMessage)
                .containsExactly("must be at most 72 bytes; accented "
                        + "letters and emoji count as more than one byte");
    }

    /**
     * missingPasswordIsLeftToNotBlank - This function checks that a
     * null password is reported once, by @NotBlank, and not again by
     * the byte rule.
     */
    @Test
    void missingPasswordIsLeftToNotBlank() {
        assertThat(passwordViolations(null))
                .extracting(violation -> violation.getConstraintDescriptor()
                        .getAnnotation().annotationType().getSimpleName())
                .containsExactly("NotBlank");
    }

    private Set<ConstraintViolation<RegisterRequest>> passwordViolations(
            String password) {
        RegisterRequest request = new RegisterRequest("ada@example.com",
                password, Role.CUSTOMER);
        return validator.validateProperty(request, "password");
    }
}
