package com.aston.restaurantreview.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class VoteResponse {

    private Long id;
    private int rating;
    private String comment;
    private Long restaurantId;
}
