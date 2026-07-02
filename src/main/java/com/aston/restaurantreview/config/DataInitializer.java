package com.aston.restaurantreview.config;

import com.aston.restaurantreview.dao.RestaurantJdbcDao;
import com.aston.restaurantreview.entity.City;
import com.aston.restaurantreview.entity.Restaurant;
import com.aston.restaurantreview.entity.Vote;
import com.aston.restaurantreview.repository.CityRepository;
import com.aston.restaurantreview.repository.RestaurantRepository;
import com.aston.restaurantreview.repository.VoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Populates the in-memory H2 database with demo data on startup so the API
 * can be explored immediately via Postman or the H2 console.
 *
 * Each repository call runs in its own transaction (no @Transactional here),
 * so the rows are committed before the JDBC recalculateAverageRating queries run.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final CityRepository cityRepository;
    private final RestaurantRepository restaurantRepository;
    private final VoteRepository voteRepository;
    private final RestaurantJdbcDao restaurantJdbcDao;

    @Override
    public void run(String... args) {
        if (cityRepository.count() > 0) {
            return; // already seeded (e.g. context restarted in tests)
        }

        City moscow = cityRepository.save(new City("Moscow"));
        City spb    = cityRepository.save(new City("Saint Petersburg"));
        City kazan  = cityRepository.save(new City("Kazan"));

        Restaurant whiteRabbit  = restaurant("White Rabbit",          moscow);
        Restaurant twinsGarden  = restaurant("Twins Garden",          moscow);
        Restaurant cococo        = restaurant("CoCoCo",               spb);
        Restaurant duoGastrobar = restaurant("Duo Gastrobar",         spb);
        Restaurant domTatarskoy = restaurant("Dom Tatarskoy Kukhni",  kazan);

        vote(whiteRabbit,  5, "Outstanding cuisine and view from the roof!");
        vote(whiteRabbit,  4, "Great food, a bit pricey but worth it.");
        vote(whiteRabbit,  5, "Best restaurant in Moscow, hands down.");

        vote(twinsGarden,  5, "Incredible tasting menu — pure creativity.");
        vote(twinsGarden,  4, "Very innovative dishes, loved every course.");

        vote(cococo,       4, "Loved the local-ingredients concept.");
        vote(cococo,       3, "Good, but slightly overrated.");
        vote(cococo,       5, "Absolutely stunning food and atmosphere.");

        vote(duoGastrobar, 5, "Hidden gem — book weeks in advance!");
        vote(duoGastrobar, 5, "Perfect dinner experience, will come back.");

        vote(domTatarskoy, 4, "Authentic Tatar food, very cosy place.");
        vote(domTatarskoy, 3, "Decent, nothing extraordinary.");

        // All votes are committed; now recalculate averages via JDBC AVG.
        restaurantRepository.findAll()
                .forEach(r -> restaurantJdbcDao.recalculateAverageRating(r.getId()));

        log.info("Demo data loaded: 3 cities, 5 restaurants, 12 votes.");
    }

    private Restaurant restaurant(String name, City city) {
        Restaurant r = new Restaurant();
        r.setName(name);
        r.setCity(city);
        r.setAverageRating(0.0);
        return restaurantRepository.save(r);
    }

    private void vote(Restaurant restaurant, int rating, String comment) {
        Vote v = new Vote();
        v.setRestaurant(restaurant);
        v.setRating(rating);
        v.setComment(comment);
        voteRepository.save(v);
    }
}
