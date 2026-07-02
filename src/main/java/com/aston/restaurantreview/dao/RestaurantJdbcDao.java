package com.aston.restaurantreview.dao;

import com.aston.restaurantreview.dto.response.RestaurantSummaryResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * JDBC-based data access for two operations that benefit from raw SQL:
 *
 * 1. Fetching restaurants by city with ORDER BY — expressed naturally in SQL
 *    without needing a separate JPA Specification or Sort object.
 *
 * 2. Recalculating averageRating via a single UPDATE … (SELECT AVG …) statement,
 *    which avoids loading all votes into memory just to compute an average.
 */
@Repository
public class RestaurantJdbcDao {

    private final JdbcTemplate jdbc;

    public RestaurantJdbcDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // Two static templates — direction is chosen in Java, never from user input,
    // which prevents SQL injection through the sort parameter.
    private static final String SQL_BY_CITY_DESC = """
            SELECT r.id, r.name, c.name AS city_name, r.average_rating
              FROM restaurants r
              JOIN cities c ON c.id = r.city_id
             WHERE LOWER(c.name) = LOWER(?)
             ORDER BY r.average_rating DESC
            """;

    private static final String SQL_BY_CITY_ASC = """
            SELECT r.id, r.name, c.name AS city_name, r.average_rating
              FROM restaurants r
              JOIN cities c ON c.id = r.city_id
             WHERE LOWER(c.name) = LOWER(?)
             ORDER BY r.average_rating ASC
            """;

    /**
     * Returns restaurants in the given city ordered by averageRating.
     * {@code ascending = true} → lowest first; {@code false} → highest first.
     * City name matching is case-insensitive.
     */
    public List<RestaurantSummaryResponse> findByCityNameOrderByRating(String cityName, boolean ascending) {
        String sql = ascending ? SQL_BY_CITY_ASC : SQL_BY_CITY_DESC;
        return jdbc.query(sql, RESTAURANT_ROW_MAPPER, cityName);
    }

    /**
     * Recalculates and persists the average rating of a restaurant
     * based on all its current votes.  COALESCE handles the zero-vote case.
     *
     * Called by VoteService after every add/delete to keep the denormalised
     * column consistent.
     */
    public void recalculateAverageRating(Long restaurantId) {
        String sql = """
                UPDATE restaurants
                   SET average_rating = (
                       SELECT COALESCE(AVG(rating), 0.0)
                         FROM votes
                        WHERE restaurant_id = ?
                   )
                 WHERE id = ?
                """;
        jdbc.update(sql, restaurantId, restaurantId);
    }

    private static final RowMapper<RestaurantSummaryResponse> RESTAURANT_ROW_MAPPER =
            new RowMapper<>() {
                @Override
                public RestaurantSummaryResponse mapRow(ResultSet rs, int rowNum) throws SQLException {
                    return new RestaurantSummaryResponse(
                            rs.getLong("id"),
                            rs.getString("name"),
                            rs.getString("city_name"),
                            rs.getDouble("average_rating")
                    );
                }
            };
}
