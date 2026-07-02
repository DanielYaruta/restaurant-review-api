package com.aston.restaurantreview.repository;

import com.aston.restaurantreview.entity.City;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CityRepository extends JpaRepository<City, Long> {

    Optional<City> findByName(String name);

    boolean existsByName(String name);
}
