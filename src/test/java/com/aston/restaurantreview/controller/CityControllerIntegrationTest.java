package com.aston.restaurantreview.controller;

import com.aston.restaurantreview.dto.request.CityRequest;
import com.aston.restaurantreview.dto.request.RestaurantRequest;
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

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class CityControllerIntegrationTest {

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

    @Test
    void deleteCity_withRestaurants_returns409WithMessage() throws Exception {
        long cityId = createCity("CityWithRestaurants");
        createRestaurant("Some Restaurant", cityId);

        mockMvc.perform(delete("/api/cities/" + cityId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message", containsString("CityWithRestaurants")))
                .andExpect(jsonPath("$.message", containsString("1")));
    }

    @Test
    void deleteCity_withoutRestaurants_returns204() throws Exception {
        long cityId = createCity("EmptyCity");

        mockMvc.perform(delete("/api/cities/" + cityId))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteCity_afterRemovingRestaurants_returns204() throws Exception {
        long cityId = createCity("CityToClean");
        long restaurantId = createRestaurant("Restaurant To Delete", cityId);

        // first deletion fails — city still has a restaurant
        mockMvc.perform(delete("/api/cities/" + cityId))
                .andExpect(status().isConflict());

        // remove the restaurant, then retry
        mockMvc.perform(delete("/api/restaurants/" + restaurantId))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/cities/" + cityId))
                .andExpect(status().isNoContent());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

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

    private long id(MvcResult result) throws Exception {
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get("id").asLong();
    }
}
