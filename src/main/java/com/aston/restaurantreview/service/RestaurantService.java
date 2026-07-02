package com.aston.restaurantreview.service;

import com.aston.restaurantreview.dto.request.RestaurantRequest;
import com.aston.restaurantreview.dto.response.RestaurantResponse;
import com.aston.restaurantreview.dto.response.RestaurantSummaryResponse;

import java.util.List;

public interface RestaurantService {

    List<RestaurantSummaryResponse> findAll();

    RestaurantResponse findById(Long id);

    /** Implemented via JDBC — see RestaurantJdbcDao. */
    List<RestaurantSummaryResponse> findByCityNameSortedByRating(String cityName);

    RestaurantResponse create(RestaurantRequest request);

    RestaurantResponse update(Long id, RestaurantRequest request);

    void delete(Long id);
}
