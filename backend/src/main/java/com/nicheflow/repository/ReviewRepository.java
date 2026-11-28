/**
 * This program is the Spring Data access point for the reviews table
 * so ADM-02 can store a review, list the reviews on a booking or a
 * listing, and summarize a listing's average rating.
 *
 * File:   ReviewRepository.java
 * Course: AUCSC 325
 *
 */
package com.nicheflow.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.nicheflow.model.Review;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    /**
     * findByBookingId - This function returns every review stored for
     * one booking, earliest first. A booking may carry up to two
     * reviews, one from each party.
     *
     * Parameters:
     * bookingId - The booking whose reviews to load
     *
     * Return - The reviews for that booking, possibly empty
     */
    @Query("""
            SELECT review FROM Review review
            WHERE review.booking.id = :bookingId
            ORDER BY review.createdAt
            """)
    List<Review> findByBookingId(@Param("bookingId") Long bookingId);

    /**
     * findByBookingListingId - This function returns every review a
     * listing's supplier has received across that listing's
     * bookings, earliest first. A supplier's own review of a
     * customer is excluded, so a supplier cannot inflate a listing's
     * public reputation by rating their own customers well.
     *
     * Parameters:
     * listingId - The listing whose reviews to load
     *
     * Return - The reviews about that listing's supplier, possibly
     *     empty
     */
    @Query("""
            SELECT review FROM Review review
            WHERE review.booking.listing.id = :listingId
              AND review.reviewee.id = review.booking.listing.supplier.id
            ORDER BY review.createdAt
            """)
    List<Review> findByBookingListingId(@Param("listingId") Long listingId);

    /**
     * summarizeByListingIds - This function returns the average
     * rating and review count for every listing in the given ids
     * whose supplier has received at least one review. A supplier's
     * own review of a customer is excluded from the average, the
     * same way it is excluded from findByBookingListingId. A listing
     * with no qualifying reviews has no row in the result.
     *
     * Parameters:
     * listingIds - The listings to summarize
     *
     * Return - One summary per listing with at least one review of
     *     its supplier
     */
    @Query("""
            SELECT review.booking.listing.id AS listingId,
                   AVG(review.rating) AS averageRating,
                   COUNT(review) AS reviewCount
            FROM Review review
            WHERE review.booking.listing.id IN :listingIds
              AND review.reviewee.id = review.booking.listing.supplier.id
            GROUP BY review.booking.listing.id
            """)
    List<ListingRatingSummary> summarizeByListingIds(
            @Param("listingIds") List<Long> listingIds);

    /**
     * summarizeByRevieweeIds - This function returns the average
     * rating and review count received by every user in the given
     * ids who has been reviewed at least once, excluding any review
     * left by excludedReviewerId. Passed a list of customer ids and
     * the viewing supplier's own id, this reports each customer's
     * standing as rated by every *other* supplier they booked with,
     * so a supplier cannot see their own past review reflected back
     * at them as if it were independent corroboration. A user with
     * no qualifying reviews has no row in the result.
     *
     * Parameters:
     * userIds - The users to summarize
     * excludedReviewerId - The reviewer whose reviews should not
     *     count toward the average, typically the caller
     *
     * Return - One summary per user who has received at least one
     *     qualifying review
     */
    @Query("""
            SELECT review.reviewee.id AS userId,
                   AVG(review.rating) AS averageRating,
                   COUNT(review) AS reviewCount
            FROM Review review
            WHERE review.reviewee.id IN :userIds
              AND review.reviewer.id <> :excludedReviewerId
            GROUP BY review.reviewee.id
            """)
    List<UserRatingSummary> summarizeByRevieweeIds(
            @Param("userIds") List<Long> userIds,
            @Param("excludedReviewerId") Long excludedReviewerId);

    /**
     * findReviewedBookingIds - This function returns, among the
     * given booking ids, those the given reviewer has already left
     * a review on. Used to show "Review" or "See reviews" on the
     * account page without a per-booking round trip.
     *
     * Parameters:
     * bookingIds - The bookings to check
     * reviewerId - The caller whose own reviews are being looked up
     *
     * Return - The booking ids the reviewer has already reviewed
     */
    @Query("""
            SELECT review.booking.id FROM Review review
            WHERE review.booking.id IN :bookingIds
              AND review.reviewer.id = :reviewerId
            """)
    List<Long> findReviewedBookingIds(
            @Param("bookingIds") List<Long> bookingIds,
            @Param("reviewerId") Long reviewerId);

    /**
     * ListingRatingSummary - This interface projects one listing's
     * average rating and review count out of a GROUP BY query without
     * loading every Review row.
     */
    interface ListingRatingSummary {

        /**
         * getListingId - This function returns the summarized
         * listing's id.
         *
         * Return - The listing id
         */
        Long getListingId();

        /**
         * getAverageRating - This function returns the mean of every
         * stored rating for the listing.
         *
         * Return - The average rating
         */
        Double getAverageRating();

        /**
         * getReviewCount - This function returns how many reviews
         * were averaged.
         *
         * Return - The review count
         */
        Long getReviewCount();
    }

    /**
     * UserRatingSummary - This interface projects one user's average
     * rating received and review count out of a GROUP BY query
     * without loading every Review row.
     */
    interface UserRatingSummary {

        /**
         * getUserId - This function returns the summarized user's
         * id.
         *
         * Return - The user id
         */
        Long getUserId();

        /**
         * getAverageRating - This function returns the mean of
         * every rating this user has received.
         *
         * Return - The average rating
         */
        Double getAverageRating();

        /**
         * getReviewCount - This function returns how many reviews
         * were averaged.
         *
         * Return - The review count
         */
        Long getReviewCount();
    }
}
