package com.aston.restaurantreview.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/** Full detail used for GET /api/restaurants/{id} — includes the vote list. */
@Getter
@AllArgsConstructor
public class RestaurantResponse {

    private Long id;
    private String name;
    private String cityName;
    private double averageRating;
    private List<VoteResponse> votes;
}
