package com.aston.restaurantreview.service;

import com.aston.restaurantreview.dto.request.CityRequest;
import com.aston.restaurantreview.dto.response.CityResponse;

import java.util.List;

public interface CityService {

    List<CityResponse> findAll();

    CityResponse findById(Long id);

    CityResponse create(CityRequest request);

    CityResponse update(Long id, CityRequest request);

    void delete(Long id);
}
