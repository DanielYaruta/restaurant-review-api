package com.aston.restaurantreview.service;

import com.aston.restaurantreview.dto.request.CityRequest;
import com.aston.restaurantreview.dto.response.CityResponse;
import com.aston.restaurantreview.entity.City;
import com.aston.restaurantreview.exception.CityHasRestaurantsException;
import com.aston.restaurantreview.exception.DuplicateCityException;
import com.aston.restaurantreview.exception.EntityNotFoundException;
import com.aston.restaurantreview.repository.CityRepository;
import com.aston.restaurantreview.repository.RestaurantRepository;
import com.aston.restaurantreview.service.impl.CityServiceImpl;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CityServiceTest {

    @Mock private CityRepository cityRepository;
    @Mock private RestaurantRepository restaurantRepository;

    @InjectMocks private CityServiceImpl cityService;

    private City moscow;

    @BeforeEach
    void setUp() {
        moscow = new City("Moscow");
        moscow.setId(1L);
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Test
    void create_savesAndReturnsResponse() {
        CityRequest request = new CityRequest();
        request.setName("Moscow");

        when(cityRepository.existsByName("Moscow")).thenReturn(false);
        when(cityRepository.save(any())).thenReturn(moscow);

        CityResponse response = cityService.create(request);

        assertThat(response.getName()).isEqualTo("Moscow");
        verify(cityRepository).save(any(City.class));
    }

    @Test
    void create_whenDuplicate_throwsDuplicateCityException() {
        CityRequest request = new CityRequest();
        request.setName("Moscow");

        when(cityRepository.existsByName("Moscow")).thenReturn(true);

        assertThatThrownBy(() -> cityService.create(request))
                .isInstanceOf(DuplicateCityException.class)
                .hasMessageContaining("Moscow");
        verify(cityRepository, never()).save(any());
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Test
    void delete_whenCityHasRestaurants_throwsCityHasRestaurantsException() {
        when(cityRepository.findById(1L)).thenReturn(Optional.of(moscow));
        when(restaurantRepository.countByCityId(1L)).thenReturn(3L);

        assertThatThrownBy(() -> cityService.delete(1L))
                .isInstanceOf(CityHasRestaurantsException.class)
                .hasMessageContaining("Moscow")
                .hasMessageContaining("3");

        verify(cityRepository, never()).delete(any());
    }

    @Test
    void delete_whenCityHasNoRestaurants_deletesSuccessfully() {
        when(cityRepository.findById(1L)).thenReturn(Optional.of(moscow));
        when(restaurantRepository.countByCityId(1L)).thenReturn(0L);

        cityService.delete(1L);

        verify(cityRepository).delete(moscow);
    }

    @Test
    void delete_whenCityNotFound_throwsEntityNotFoundException() {
        when(cityRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cityService.delete(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("99");
    }

    // ── findAll ───────────────────────────────────────────────────────────────

    @Test
    void findAll_returnsMappedList() {
        when(cityRepository.findAll()).thenReturn(List.of(moscow));

        List<CityResponse> result = cityService.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Moscow");
    }
}
