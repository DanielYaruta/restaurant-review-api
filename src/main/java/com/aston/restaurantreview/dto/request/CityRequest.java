package com.aston.restaurantreview.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CityRequest {

    @NotBlank(message = "City name must not be blank")
    @Size(max = 100, message = "City name must not exceed 100 characters")
    private String name;
}
