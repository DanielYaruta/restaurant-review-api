package com.aston.restaurantreview.service.impl;

import com.aston.restaurantreview.dao.RestaurantJdbcDao;
import com.aston.restaurantreview.dto.request.VoteRequest;
import com.aston.restaurantreview.dto.response.VoteResponse;
import com.aston.restaurantreview.entity.Restaurant;
import com.aston.restaurantreview.entity.Vote;
import com.aston.restaurantreview.exception.EntityNotFoundException;
import com.aston.restaurantreview.repository.RestaurantRepository;
import com.aston.restaurantreview.repository.VoteRepository;
import com.aston.restaurantreview.service.VoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VoteServiceImpl implements VoteService {

    private final VoteRepository voteRepository;
    private final RestaurantRepository restaurantRepository;
    private final RestaurantJdbcDao restaurantJdbcDao;

    @Override
    @Transactional(readOnly = true)
    public List<VoteResponse> findByRestaurantId(Long restaurantId) {
        getRestaurantOrThrow(restaurantId);
        return voteRepository.findByRestaurantId(restaurantId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public VoteResponse addVote(Long restaurantId, VoteRequest request) {
        Restaurant restaurant = getRestaurantOrThrow(restaurantId);

        Vote vote = new Vote();
        vote.setRestaurant(restaurant);
        vote.setRating(request.getRating());
        vote.setComment(request.getComment());
        Vote saved = voteRepository.save(vote);

        // Flush JPA writes so the JDBC AVG query sees the new row in the same transaction.
        voteRepository.flush();
        restaurantJdbcDao.recalculateAverageRating(restaurantId);

        return toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Long voteId) {
        Vote vote = voteRepository.findById(voteId)
                .orElseThrow(() -> new EntityNotFoundException("Vote with id=" + voteId + " not found"));
        Long restaurantId = vote.getRestaurant().getId();

        voteRepository.delete(vote);
        // Flush the DELETE before the JDBC AVG recalculation runs.
        voteRepository.flush();
        restaurantJdbcDao.recalculateAverageRating(restaurantId);
    }

    private Restaurant getRestaurantOrThrow(Long id) {
        return restaurantRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Restaurant with id=" + id + " not found"));
    }

    private VoteResponse toResponse(Vote vote) {
        return new VoteResponse(
                vote.getId(),
                vote.getRating(),
                vote.getComment(),
                vote.getRestaurant().getId()
        );
    }
}
