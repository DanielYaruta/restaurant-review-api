package com.aston.restaurantreview.service.impl;

import com.aston.restaurantreview.dto.request.CityRequest;
import com.aston.restaurantreview.dto.response.CityResponse;
import com.aston.restaurantreview.entity.City;
import com.aston.restaurantreview.exception.CityHasRestaurantsException;
import com.aston.restaurantreview.exception.DuplicateCityException;
import com.aston.restaurantreview.exception.EntityNotFoundException;
import com.aston.restaurantreview.repository.CityRepository;
import com.aston.restaurantreview.repository.RestaurantRepository;
import com.aston.restaurantreview.service.CityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CityServiceImpl implements CityService {

    private final CityRepository cityRepository;
    private final RestaurantRepository restaurantRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CityResponse> findAll() {
        return cityRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CityResponse findById(Long id) {
        return toResponse(getOrThrow(id));
    }

    @Override
    @Transactional
    public CityResponse create(CityRequest request) {
        if (cityRepository.existsByName(request.getName())) {
            throw new DuplicateCityException("City '" + request.getName() + "' already exists");
        }
        return toResponse(cityRepository.save(new City(request.getName())));
    }

    @Override
    @Transactional
    public CityResponse update(Long id, CityRequest request) {
        City city = getOrThrow(id);
        if (!city.getName().equals(request.getName()) && cityRepository.existsByName(request.getName())) {
            throw new DuplicateCityException("City '" + request.getName() + "' already exists");
        }
        city.setName(request.getName());
        return toResponse(cityRepository.save(city));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        City city = getOrThrow(id);
        long count = restaurantRepository.countByCityId(id);
        if (count > 0) {
            throw new CityHasRestaurantsException(
                    "City '" + city.getName() + "' cannot be deleted: it has " + count + " associated restaurant(s)"
            );
        }
        cityRepository.delete(city);
    }

    private City getOrThrow(Long id) {
        return cityRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("City with id=" + id + " not found"));
    }

    private CityResponse toResponse(City city) {
        return new CityResponse(city.getId(), city.getName());
    }
}
