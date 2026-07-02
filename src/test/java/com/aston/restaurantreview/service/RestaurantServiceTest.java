package com.aston.restaurantreview.service;

import com.aston.restaurantreview.dao.RestaurantJdbcDao;
import com.aston.restaurantreview.dto.request.RestaurantRequest;
import com.aston.restaurantreview.dto.response.RestaurantResponse;
import com.aston.restaurantreview.dto.response.RestaurantSummaryResponse;
import com.aston.restaurantreview.entity.City;
import com.aston.restaurantreview.entity.Restaurant;
import com.aston.restaurantreview.exception.EntityNotFoundException;
import com.aston.restaurantreview.repository.CityRepository;
import com.aston.restaurantreview.repository.RestaurantRepository;
import com.aston.restaurantreview.service.impl.RestaurantServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RestaurantServiceTest {

    @Mock private RestaurantRepository restaurantRepository;
    @Mock private CityRepository cityRepository;
    @Mock private RestaurantJdbcDao restaurantJdbcDao;

    @InjectMocks private RestaurantServiceImpl restaurantService;

    private City city;
    private Restaurant restaurant;

    @BeforeEach
    void setUp() {
        city = new City("Moscow");
        city.setId(1L);

        restaurant = new Restaurant();
        restaurant.setId(10L);
        restaurant.setName("White Rabbit");
        restaurant.setCity(city);
        restaurant.setAverageRating(4.5);
    }

    @Test
    void findAll_returnsAllRestaurantsAsSummaries() {
        when(restaurantRepository.findAllWithCity()).thenReturn(List.of(restaurant));

        List<RestaurantSummaryResponse> result = restaurantService.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("White Rabbit");
        assertThat(result.get(0).getCityName()).isEqualTo("Moscow");
    }

    @Test
    void findById_returnsFullResponse() {
        when(restaurantRepository.findByIdWithVotesAndCity(10L)).thenReturn(Optional.of(restaurant));

        RestaurantResponse response = restaurantService.findById(10L);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getName()).isEqualTo("White Rabbit");
        assertThat(response.getCityName()).isEqualTo("Moscow");
    }

    @Test
    void findById_whenNotFound_throwsEntityNotFoundException() {
        when(restaurantRepository.findByIdWithVotesAndCity(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> restaurantService.findById(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void create_savesRestaurantAndReturnsResponse() {
        RestaurantRequest request = new RestaurantRequest();
        request.setName("New Place");
        request.setCityId(1L);

        Restaurant saved = new Restaurant();
        saved.setId(20L);
        saved.setName("New Place");
        saved.setCity(city);
        saved.setAverageRating(0.0);

        when(cityRepository.findById(1L)).thenReturn(Optional.of(city));
        when(restaurantRepository.save(any())).thenReturn(saved);

        RestaurantResponse response = restaurantService.create(request);

        assertThat(response.getId()).isEqualTo(20L);
        assertThat(response.getName()).isEqualTo("New Place");
        assertThat(response.getAverageRating()).isEqualTo(0.0);
    }

    @Test
    void create_whenCityNotFound_throwsEntityNotFoundException() {
        RestaurantRequest request = new RestaurantRequest();
        request.setName("Place");
        request.setCityId(999L);

        when(cityRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> restaurantService.create(request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("999");
        verify(restaurantRepository, never()).save(any());
    }

    @Test
    void delete_whenFound_deletesRestaurant() {
        when(restaurantRepository.findById(10L)).thenReturn(Optional.of(restaurant));

        restaurantService.delete(10L);

        verify(restaurantRepository).delete(restaurant);
    }

    @Test
    void findByCityNameSortedByRating_desc_delegatesToJdbcDaoWithAscendingFalse() {
        List<RestaurantSummaryResponse> expected = List.of(
                new RestaurantSummaryResponse(1L, "A", "Moscow", 5.0),
                new RestaurantSummaryResponse(2L, "B", "Moscow", 3.0)
        );
        when(restaurantJdbcDao.findByCityNameOrderByRating("Moscow", false)).thenReturn(expected);

        List<RestaurantSummaryResponse> result = restaurantService.findByCityNameSortedByRating("Moscow", "rating_desc");

        assertThat(result).isEqualTo(expected);
        verify(restaurantJdbcDao).findByCityNameOrderByRating("Moscow", false);
    }

    @Test
    void findByCityNameSortedByRating_asc_delegatesToJdbcDaoWithAscendingTrue() {
        List<RestaurantSummaryResponse> expected = List.of(
                new RestaurantSummaryResponse(2L, "B", "Moscow", 3.0),
                new RestaurantSummaryResponse(1L, "A", "Moscow", 5.0)
        );
        when(restaurantJdbcDao.findByCityNameOrderByRating("Moscow", true)).thenReturn(expected);

        List<RestaurantSummaryResponse> result = restaurantService.findByCityNameSortedByRating("Moscow", "rating_asc");

        assertThat(result).isEqualTo(expected);
        verify(restaurantJdbcDao).findByCityNameOrderByRating("Moscow", true);
    }

    @Test
    void findByCityNameSortedByRating_invalidSort_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> restaurantService.findByCityNameSortedByRating("Moscow", "banana"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("banana");
        verify(restaurantJdbcDao, never()).findByCityNameOrderByRating(any(), anyBoolean());
    }
}
