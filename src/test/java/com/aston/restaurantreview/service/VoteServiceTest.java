package com.aston.restaurantreview.service;

import com.aston.restaurantreview.dao.RestaurantJdbcDao;
import com.aston.restaurantreview.dto.request.VoteRequest;
import com.aston.restaurantreview.dto.response.VoteResponse;
import com.aston.restaurantreview.entity.City;
import com.aston.restaurantreview.entity.Restaurant;
import com.aston.restaurantreview.entity.Vote;
import com.aston.restaurantreview.exception.EntityNotFoundException;
import com.aston.restaurantreview.repository.RestaurantRepository;
import com.aston.restaurantreview.repository.VoteRepository;
import com.aston.restaurantreview.service.impl.VoteServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VoteServiceTest {

    @Mock private VoteRepository voteRepository;
    @Mock private RestaurantRepository restaurantRepository;
    @Mock private RestaurantJdbcDao restaurantJdbcDao;

    @InjectMocks private VoteServiceImpl voteService;

    private Restaurant restaurant;

    @BeforeEach
    void setUp() {
        City city = new City("Moscow");
        city.setId(1L);

        restaurant = new Restaurant();
        restaurant.setId(5L);
        restaurant.setName("White Rabbit");
        restaurant.setCity(city);
        restaurant.setAverageRating(0.0);
    }

    // ── addVote ──────────────────────────────────────────────────────────────

    @Test
    void addVote_savesVoteAndReturnsResponse() {
        when(restaurantRepository.findById(5L)).thenReturn(Optional.of(restaurant));
        when(voteRepository.save(any())).thenAnswer(inv -> {
            Vote v = inv.getArgument(0);
            v.setId(100L);
            return v;
        });

        VoteRequest request = voteRequest(5, "Excellent!");
        VoteResponse response = voteService.addVote(5L, request);

        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getRating()).isEqualTo(5);
        assertThat(response.getComment()).isEqualTo("Excellent!");
        assertThat(response.getRestaurantId()).isEqualTo(5L);
    }

    @Test
    void addVote_triggersRatingRecalculation() {
        when(restaurantRepository.findById(5L)).thenReturn(Optional.of(restaurant));
        when(voteRepository.save(any())).thenAnswer(inv -> {
            Vote v = inv.getArgument(0);
            v.setId(1L);
            return v;
        });

        voteService.addVote(5L, voteRequest(4, "Good"));

        verify(voteRepository).flush();
        verify(restaurantJdbcDao).recalculateAverageRating(5L);
    }

    /**
     * Key business-case test: adding three votes must trigger a recalculation
     * after each one so the rating is always kept in sync.
     */
    @Test
    void addVote_eachVoteTriggersSeparateRecalculation() {
        when(restaurantRepository.findById(5L)).thenReturn(Optional.of(restaurant));
        AtomicLong idGen = new AtomicLong(1);
        when(voteRepository.save(any())).thenAnswer(inv -> {
            Vote v = inv.getArgument(0);
            v.setId(idGen.getAndIncrement());
            return v;
        });

        voteService.addVote(5L, voteRequest(4, "Good"));
        voteService.addVote(5L, voteRequest(2, "Bad"));
        voteService.addVote(5L, voteRequest(5, "Excellent"));

        // The recalculation must fire once per addVote call (not batched).
        verify(restaurantJdbcDao, times(3)).recalculateAverageRating(5L);
    }

    @Test
    void addVote_whenRestaurantNotFound_throwsEntityNotFoundException() {
        when(restaurantRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> voteService.addVote(99L, voteRequest(3, "ok")))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("99");
        verify(voteRepository, never()).save(any());
        verify(restaurantJdbcDao, never()).recalculateAverageRating(any());
    }

    // ── delete ───────────────────────────────────────────────────────────────

    @Test
    void delete_removesVoteAndRecalculatesRating() {
        Vote vote = new Vote();
        vote.setId(7L);
        vote.setRestaurant(restaurant);
        vote.setRating(4);

        when(voteRepository.findById(7L)).thenReturn(Optional.of(vote));

        voteService.delete(7L);

        verify(voteRepository).delete(vote);
        verify(voteRepository).flush();
        verify(restaurantJdbcDao).recalculateAverageRating(5L);
    }

    @Test
    void delete_whenVoteNotFound_throwsEntityNotFoundException() {
        when(voteRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> voteService.delete(42L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("42");
        verify(restaurantJdbcDao, never()).recalculateAverageRating(any());
    }

    // ── findByRestaurantId ────────────────────────────────────────────────────

    @Test
    void findByRestaurantId_returnsMappedVotes() {
        Vote v1 = vote(1L, 5, "Great", restaurant);
        Vote v2 = vote(2L, 3, "Ok",    restaurant);

        when(restaurantRepository.findById(5L)).thenReturn(Optional.of(restaurant));
        when(voteRepository.findByRestaurantId(5L)).thenReturn(List.of(v1, v2));

        List<VoteResponse> result = voteService.findByRestaurantId(5L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getRating()).isEqualTo(5);
        assertThat(result.get(1).getRating()).isEqualTo(3);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static VoteRequest voteRequest(int rating, String comment) {
        VoteRequest r = new VoteRequest();
        r.setRating(rating);
        r.setComment(comment);
        return r;
    }

    private static Vote vote(Long id, int rating, String comment, Restaurant restaurant) {
        Vote v = new Vote();
        v.setId(id);
        v.setRating(rating);
        v.setComment(comment);
        v.setRestaurant(restaurant);
        return v;
    }
}
