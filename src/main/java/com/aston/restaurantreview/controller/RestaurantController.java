package com.aston.restaurantreview.controller;

import com.aston.restaurantreview.dto.request.RestaurantRequest;
import com.aston.restaurantreview.dto.request.VoteRequest;
import com.aston.restaurantreview.dto.response.RestaurantResponse;
import com.aston.restaurantreview.dto.response.RestaurantSummaryResponse;
import com.aston.restaurantreview.dto.response.VoteResponse;
import com.aston.restaurantreview.service.RestaurantService;
import com.aston.restaurantreview.service.VoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/restaurants")
@RequiredArgsConstructor
public class RestaurantController {

    private final RestaurantService restaurantService;
    private final VoteService voteService;

    @GetMapping
    public List<RestaurantSummaryResponse> getAll() {
        return restaurantService.findAll();
    }

    @GetMapping("/{id}")
    public RestaurantResponse getById(@PathVariable Long id) {
        return restaurantService.findById(id);
    }

    /**
     * Returns restaurants for a given city ordered by averageRating (via JDBC).
     * {@code sort} accepts {@code rating_desc} (default) or {@code rating_asc};
     * any other value yields 400 Bad Request.
     */
    @GetMapping("/by-city/{cityName}")
    public List<RestaurantSummaryResponse> getByCity(
            @PathVariable String cityName,
            @RequestParam(defaultValue = "rating_desc") String sort) {
        return restaurantService.findByCityNameSortedByRating(cityName, sort);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RestaurantResponse create(@RequestBody @Valid RestaurantRequest request) {
        return restaurantService.create(request);
    }

    @PutMapping("/{id}")
    public RestaurantResponse update(@PathVariable Long id, @RequestBody @Valid RestaurantRequest request) {
        return restaurantService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        restaurantService.delete(id);
    }

    @PostMapping("/{id}/votes")
    @ResponseStatus(HttpStatus.CREATED)
    public VoteResponse addVote(@PathVariable Long id, @RequestBody @Valid VoteRequest request) {
        return voteService.addVote(id, request);
    }

    @GetMapping("/{id}/votes")
    public List<VoteResponse> getVotes(@PathVariable Long id) {
        return voteService.findByRestaurantId(id);
    }
}
