package com.aston.restaurantreview.controller;

import com.aston.restaurantreview.dto.request.CityRequest;
import com.aston.restaurantreview.dto.request.RestaurantRequest;
import com.aston.restaurantreview.dto.request.VoteRequest;
import com.aston.restaurantreview.repository.CityRepository;
import com.aston.restaurantreview.repository.RestaurantRepository;
import com.aston.restaurantreview.repository.VoteRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class RestaurantControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Autowired private VoteRepository voteRepository;
    @Autowired private RestaurantRepository restaurantRepository;
    @Autowired private CityRepository cityRepository;

    @BeforeEach
    void cleanDatabase() {
        voteRepository.deleteAll();
        restaurantRepository.deleteAll();
        cityRepository.deleteAll();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Adding a vote updates averageRating
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void addVote_updatesAverageRatingCorrectly() throws Exception {
        long cityId = createCity("TestCity");
        long restaurantId = createRestaurant("Test Restaurant", cityId);

        // ratings: 4, 2, 5  →  AVG = 11/3 ≈ 3.6667
        addVote(restaurantId, 4, "Good");
        addVote(restaurantId, 2, "Bad");
        addVote(restaurantId, 5, "Excellent");

        mockMvc.perform(get("/api/restaurants/" + restaurantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageRating", closeTo(11.0 / 3.0, 0.01)))
                .andExpect(jsonPath("$.votes", hasSize(3)));
    }

    @Test
    void addVote_withInvalidRating_returns400() throws Exception {
        long cityId = createCity("CityX");
        long restaurantId = createRestaurant("Rest X", cityId);

        VoteRequest bad = new VoteRequest();
        bad.setRating(10); // > 5
        bad.setComment("Way too high");

        mockMvc.perform(post("/api/restaurants/" + restaurantId + "/votes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bad)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // By-city sorted by rating (JDBC path)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void getByCity_returnsSortedByRatingDesc() throws Exception {
        long cityId = createCity("Moscow");
        long rest1Id = createRestaurant("Low Rated",  cityId);
        long rest2Id = createRestaurant("High Rated", cityId);

        addVote(rest1Id, 2, "Not great");
        addVote(rest2Id, 5, "Fantastic");

        mockMvc.perform(get("/api/restaurants/by-city/Moscow?sort=rating_desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name").value("High Rated"))
                .andExpect(jsonPath("$[1].name").value("Low Rated"))
                .andExpect(jsonPath("$[0].averageRating", closeTo(5.0, 0.01)))
                .andExpect(jsonPath("$[1].averageRating", closeTo(2.0, 0.01)));
    }

    @Test
    void getByCity_whenNoCityMatch_returnsEmptyList() throws Exception {
        mockMvc.perform(get("/api/restaurants/by-city/NonExistentCity"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // City duplicate → 409
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void createCity_withDuplicateName_returns409() throws Exception {
        createCity("UniqueCity");

        CityRequest dup = new CityRequest();
        dup.setName("UniqueCity");

        mockMvc.perform(post("/api/cities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dup)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Delete vote recalculates rating
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void deleteVote_recalculatesAverageRating() throws Exception {
        long cityId = createCity("CityDel");
        long restaurantId = createRestaurant("Rest Del", cityId);

        long vote5Id = addVoteAndGetId(restaurantId, 5, "Great");
        addVote(restaurantId, 3, "Meh");
        // average = (5+3)/2 = 4.0

        // delete the 5-star vote → average should become 3.0
        mockMvc.perform(delete("/api/votes/" + vote5Id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/restaurants/" + restaurantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageRating", closeTo(3.0, 0.01)));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 404 for unknown resources
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void getRestaurant_whenNotFound_returns404() throws Exception {
        mockMvc.perform(get("/api/restaurants/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private long createCity(String name) throws Exception {
        CityRequest req = new CityRequest();
        req.setName(name);
        MvcResult result = mockMvc.perform(post("/api/cities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();
        return id(result);
    }

    private long createRestaurant(String name, long cityId) throws Exception {
        RestaurantRequest req = new RestaurantRequest();
        req.setName(name);
        req.setCityId(cityId);
        MvcResult result = mockMvc.perform(post("/api/restaurants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();
        return id(result);
    }

    private void addVote(long restaurantId, int rating, String comment) throws Exception {
        VoteRequest req = new VoteRequest();
        req.setRating(rating);
        req.setComment(comment);
        mockMvc.perform(post("/api/restaurants/" + restaurantId + "/votes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    private long addVoteAndGetId(long restaurantId, int rating, String comment) throws Exception {
        VoteRequest req = new VoteRequest();
        req.setRating(rating);
        req.setComment(comment);
        MvcResult result = mockMvc.perform(post("/api/restaurants/" + restaurantId + "/votes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();
        return id(result);
    }

    private long id(MvcResult result) throws Exception {
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get("id").asLong();
    }
}
