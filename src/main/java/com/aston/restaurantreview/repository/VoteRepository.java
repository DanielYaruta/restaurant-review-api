package com.aston.restaurantreview.repository;

import com.aston.restaurantreview.entity.Vote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VoteRepository extends JpaRepository<Vote, Long> {

    List<Vote> findByRestaurantId(Long restaurantId);
}
