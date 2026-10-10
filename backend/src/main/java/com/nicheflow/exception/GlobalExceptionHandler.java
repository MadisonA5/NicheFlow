/**
 * This program turns auth, listing, and validation failures into HTTP
 * problem details so register, login, and LST-01 return stable status
 * codes and bodies.
 *
 * File:   GlobalExceptionHandler.java
 * Course: AUCSC 325
 *
 */
package com.nicheflow.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * handleDuplicateEmail - This function maps a taken email to HTTP
     * 409 so the client can ask the user to pick another address.
     *
     * Parameters:
     * exception - The duplicate-email error from registration
     *
     * Return - A conflict problem detail
     */
    @ExceptionHandler(DuplicateEmailException.class)
    public ProblemDetail handleDuplicateEmail(
            DuplicateEmailException exception) {
        return problem(HttpStatus.CONFLICT, "Email already registered",
                exception.getMessage());
    }

    /**
     * handleInvalidCredentials - This function maps a failed login to
     * HTTP 401 with the shared "invalid email or password" message.
     *
     * Parameters:
     * exception - The failed-login error
     *
     * Return - An unauthorized problem detail
     */
    @ExceptionHandler(InvalidCredentialsException.class)
    public ProblemDetail handleInvalidCredentials(
            InvalidCredentialsException exception) {
        return problem(HttpStatus.UNAUTHORIZED, "Authentication failed",
                exception.getMessage());
    }

    /**
     * handleRoleNotSelfAssignable - This function maps an ADMIN
     * self-registration attempt to HTTP 400.
     *
     * Parameters:
     * exception - The rejected-role error
     *
     * Return - A bad-request problem detail
     */
    @ExceptionHandler(RoleNotSelfAssignableException.class)
    public ProblemDetail handleRoleNotSelfAssignable(
            RoleNotSelfAssignableException exception) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid role",
                exception.getMessage());
    }

    /**
     * handleListingAccessDenied - This function maps a listing
     * privilege or ownership failure to HTTP 403.
     *
     * Parameters:
     * exception - The denied listing write
     *
     * Return - A forbidden problem detail
     */
    @ExceptionHandler(ListingAccessDeniedException.class)
    public ProblemDetail handleListingAccessDenied(
            ListingAccessDeniedException exception) {
        return problem(HttpStatus.FORBIDDEN, "Listing access denied",
                exception.getMessage());
    }

    /**
     * handleListingNotFound - This function maps a missing listing id
     * to HTTP 404.
     *
     * Parameters:
     * exception - The missing-listing error
     *
     * Return - A not-found problem detail
     */
    @ExceptionHandler(ListingNotFoundException.class)
    public ProblemDetail handleListingNotFound(
            ListingNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, "Listing not found",
                exception.getMessage());
    }

    /**
     * handleValidationFailure - This function maps bean-validation
     * errors to HTTP 400 and attaches the first message per field.
     *
     * Parameters:
     * exception - The Spring validation failure
     *
     * Return - A bad-request problem detail with an errors map
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationFailure(
            MethodArgumentNotValidException exception) {
        // To collect the first message for each invalid field
        Map<String, String> fieldErrors = collectFieldErrors(exception);

        // To return a 400 with that field map attached
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST,
                "Validation failed", "One or more fields are invalid");
        problem.setProperty("errors", fieldErrors);
        return problem;
    }

    /**
     * collectFieldErrors - This function copies the first validation
     * message for each field so the client can highlight inputs.
     *
     * Parameters:
     * exception - The Spring validation failure
     *
     * Return - A field-name to message map
     */
    private Map<String, String> collectFieldErrors(
            MethodArgumentNotValidException exception) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(error -> fieldErrors.putIfAbsent(error.getField(),
                        error.getDefaultMessage()));
        return fieldErrors;
    }

    /**
     * problem - This function builds a ProblemDetail with a status,
     * title, and detail string used by every handler above.
     *
     * Parameters:
     * status - The HTTP status to return
     * title - The short problem title
     * detail - The longer explanation
     *
     * Return - The filled problem detail
     */
    private static ProblemDetail problem(HttpStatus status, String title,
            String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status,
                detail);
        problem.setTitle(title);
        return problem;
    }
}
