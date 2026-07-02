package com.aston.restaurantreview.controller;

import com.aston.restaurantreview.dto.request.CityRequest;
import com.aston.restaurantreview.dto.response.CityResponse;
import com.aston.restaurantreview.service.CityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cities")
@RequiredArgsConstructor
public class CityController {

    private final CityService cityService;

    @GetMapping
    public List<CityResponse> getAll() {
        return cityService.findAll();
    }

    @GetMapping("/{id}")
    public CityResponse getById(@PathVariable Long id) {
        return cityService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CityResponse create(@RequestBody @Valid CityRequest request) {
        return cityService.create(request);
    }

    @PutMapping("/{id}")
    public CityResponse update(@PathVariable Long id, @RequestBody @Valid CityRequest request) {
        return cityService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        cityService.delete(id);
    }
}
