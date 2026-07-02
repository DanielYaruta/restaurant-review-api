package com.aston.restaurantreview.repository;

import com.aston.restaurantreview.entity.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {

    /** Avoids N+1 when mapping the summary list — fetches city in one query. */
    @Query("SELECT r FROM Restaurant r JOIN FETCH r.city")
    List<Restaurant> findAllWithCity();

    /** Fetches city and votes together to build the full RestaurantResponse. */
    @Query("SELECT DISTINCT r FROM Restaurant r JOIN FETCH r.city LEFT JOIN FETCH r.votes WHERE r.id = :id")
    Optional<Restaurant> findByIdWithVotesAndCity(@Param("id") Long id);
}
