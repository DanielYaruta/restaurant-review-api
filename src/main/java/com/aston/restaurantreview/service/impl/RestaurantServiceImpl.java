package com.aston.restaurantreview.service.impl;

import com.aston.restaurantreview.dao.RestaurantJdbcDao;
import com.aston.restaurantreview.dto.request.RestaurantRequest;
import com.aston.restaurantreview.dto.response.RestaurantResponse;
import com.aston.restaurantreview.dto.response.RestaurantSummaryResponse;
import com.aston.restaurantreview.dto.response.VoteResponse;
import com.aston.restaurantreview.entity.City;
import com.aston.restaurantreview.entity.Restaurant;
import com.aston.restaurantreview.exception.EntityNotFoundException;
import com.aston.restaurantreview.repository.CityRepository;
import com.aston.restaurantreview.repository.RestaurantRepository;
import com.aston.restaurantreview.service.RestaurantService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RestaurantServiceImpl implements RestaurantService {

    private final RestaurantRepository restaurantRepository;
    private final CityRepository cityRepository;
    private final RestaurantJdbcDao restaurantJdbcDao;

    @Override
    @Transactional(readOnly = true)
    public List<RestaurantSummaryResponse> findAll() {
        return restaurantRepository.findAllWithCity().stream()
                .map(this::toSummary)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public RestaurantResponse findById(Long id) {
        Restaurant r = restaurantRepository.findByIdWithVotesAndCity(id)
                .orElseThrow(() -> new EntityNotFoundException("Restaurant with id=" + id + " not found"));
        return toFullResponse(r);
    }

    @Override
    public List<RestaurantSummaryResponse> findByCityNameSortedByRating(String cityName) {
        return restaurantJdbcDao.findByCityNameOrderByRatingDesc(cityName);
    }

    @Override
    @Transactional
    public RestaurantResponse create(RestaurantRequest request) {
        City city = getCityOrThrow(request.getCityId());
        Restaurant restaurant = new Restaurant();
        restaurant.setName(request.getName());
        restaurant.setCity(city);
        restaurant.setAverageRating(0.0);
        return toFullResponse(restaurantRepository.save(restaurant));
    }

    @Override
    @Transactional
    public RestaurantResponse update(Long id, RestaurantRequest request) {
        Restaurant restaurant = getOrThrow(id);
        restaurant.setName(request.getName());
        restaurant.setCity(getCityOrThrow(request.getCityId()));
        return toFullResponse(restaurantRepository.save(restaurant));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        restaurantRepository.delete(getOrThrow(id));
    }

    private Restaurant getOrThrow(Long id) {
        return restaurantRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Restaurant with id=" + id + " not found"));
    }

    private City getCityOrThrow(Long cityId) {
        return cityRepository.findById(cityId)
                .orElseThrow(() -> new EntityNotFoundException("City with id=" + cityId + " not found"));
    }

    private RestaurantSummaryResponse toSummary(Restaurant r) {
        return new RestaurantSummaryResponse(r.getId(), r.getName(), r.getCity().getName(), r.getAverageRating());
    }

    private RestaurantResponse toFullResponse(Restaurant r) {
        List<VoteResponse> votes = r.getVotes().stream()
                .map(v -> new VoteResponse(v.getId(), v.getRating(), v.getComment(), r.getId()))
                .collect(Collectors.toList());
        return new RestaurantResponse(r.getId(), r.getName(), r.getCity().getName(), r.getAverageRating(), votes);
    }
}
