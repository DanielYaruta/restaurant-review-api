package com.aston.restaurantreview.service;

import com.aston.restaurantreview.dto.request.VoteRequest;
import com.aston.restaurantreview.dto.response.VoteResponse;

import java.util.List;

public interface VoteService {

    List<VoteResponse> findByRestaurantId(Long restaurantId);

    /** Saves the vote and recalculates the restaurant's averageRating via JDBC. */
    VoteResponse addVote(Long restaurantId, VoteRequest request);

    /** Deletes the vote and recalculates the restaurant's averageRating via JDBC. */
    void delete(Long voteId);
}
