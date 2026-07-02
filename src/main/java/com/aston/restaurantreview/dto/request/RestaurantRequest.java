package com.aston.restaurantreview.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RestaurantRequest {

    @NotBlank(message = "Restaurant name must not be blank")
    @Size(max = 200, message = "Restaurant name must not exceed 200 characters")
    private String name;

    @NotNull(message = "City ID must not be null")
    private Long cityId;
}
