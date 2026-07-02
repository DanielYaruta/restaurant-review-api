package com.aston.restaurantreview.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** Lightweight projection used in list endpoints (no votes included). */
@Getter
@AllArgsConstructor
public class RestaurantSummaryResponse {

    private Long id;
    private String name;
    private String cityName;
    private double averageRating;
}
