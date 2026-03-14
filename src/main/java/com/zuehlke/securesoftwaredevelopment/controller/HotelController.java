package com.zuehlke.securesoftwaredevelopment.controller;

import com.zuehlke.securesoftwaredevelopment.config.AuditLogger;
import com.zuehlke.securesoftwaredevelopment.domain.*;
import com.zuehlke.securesoftwaredevelopment.repository.CityRepository;
import com.zuehlke.securesoftwaredevelopment.repository.HotelRepository;
import com.zuehlke.securesoftwaredevelopment.repository.RatingRepository;
import com.zuehlke.securesoftwaredevelopment.repository.RoomRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
public class HotelController {
    private static final Logger LOG = LoggerFactory.getLogger(HotelController.class);
    private static final AuditLogger auditLogger = AuditLogger.getAuditLogger(HotelController.class);

    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;
    private final CityRepository cityRepository;
    private final RatingRepository ratingRepository;

    public HotelController(RoomRepository roomRepository, CityRepository cityRepository, HotelRepository hotelRepository, RatingRepository ratingRepository) {
        this.roomRepository = roomRepository;
        this.cityRepository = cityRepository;
        this.hotelRepository = hotelRepository;
        this.ratingRepository = ratingRepository;
    }

    @GetMapping("/")
    public String showSearch(Model model) {
        try {
            model.addAttribute("hotels", hotelRepository.getAll());
            LOG.info("Opened hotel list page");
            return "hotels";
        } catch (RuntimeException e) {
            LOG.error("Unexpected error while opening hotel list page", e);
            return "redirect:/?error=true";
        }
    }

    @GetMapping("/hotels")
    public String showHotels(@RequestParam(name = "id", required = false) String id, Model model, Authentication authentication) {
        try {
            if (id == null) {
                model.addAttribute("hotels", hotelRepository.getAll());
                LOG.info("Opened hotel list page without hotelId");
                return "hotels";
            }

            if (authentication == null || !(authentication.getPrincipal() instanceof User)) {
                LOG.warn("Attempt to open hotel details without valid authentication. hotelId={}", id);
                return "redirect:/login";
            }

            User user = (User) authentication.getPrincipal();

            List<Rating> ratings = ratingRepository.getAll(id);
            Optional<Rating> userRating = ratings.stream()
                    .filter(rating -> rating.getUserId() == user.getId())
                    .findFirst();

            userRating.ifPresent(rating -> model.addAttribute("userRating", rating.getRating()));

            if (!ratings.isEmpty()) {
                Integer sumRating = ratings.stream()
                        .map(Rating::getRating)
                        .reduce(0, Integer::sum);
                Double avgRating = (double) sumRating / ratings.size();
                model.addAttribute("averageRating", avgRating);
            }

            Hotel hotel = hotelRepository.get(Integer.parseInt(id));
            if (hotel == null) {
                LOG.warn("Hotel details requested for non-existing hotel. hotelId={}, userId={}", id, user.getId());
                return "redirect:/?error=true";
            }

            model.addAttribute("hotel", hotel);

            LOG.info("Opened hotel details. hotelId={}, userId={}", id, user.getId());
            return "hotel";
        } catch (NumberFormatException e) {
            LOG.warn("Invalid hotel id format received. hotelId={}", id);
            return "redirect:/?error=true";
        } catch (RuntimeException e) {
            LOG.error("Unexpected error while opening hotel details. hotelId={}", id, e);
            return "redirect:/?error=true";
        }
    }

    @GetMapping("/hotels/new-hotel")
    @PreAuthorize("hasAuthority('CREATE_HOTEL')")
    public String newHotel(
            Model model,
            @RequestParam(value = "hotelInvalid", required = false) Boolean hotelInvalid,
            @RequestParam(value = "hotelExists", required = false) Boolean hotelExists,
            @RequestParam(value = "cityMissing", required = false) Boolean cityMissing
    ) {
        try {
            List<City> cityList = cityRepository.getAll();
            List<Hotel> hotelList = hotelRepository.getAll();

            model.addAttribute("cities", cityList);
            model.addAttribute("hotels", hotelList);

            model.addAttribute("hotelInvalid", Boolean.TRUE.equals(hotelInvalid));
            model.addAttribute("hotelExists", Boolean.TRUE.equals(hotelExists));
            model.addAttribute("cityMissing", Boolean.TRUE.equals(cityMissing));

            LOG.info("Opened new hotel page");
            return "new-hotel";
        } catch (RuntimeException e) {
            LOG.error("Unexpected error while opening new hotel page", e);
            return "redirect:/?error=true";
        }
    }

    @PostMapping("/hotels/create")
    @PreAuthorize("hasAuthority('CREATE_HOTEL')")
    public String createHotel(
            @RequestParam Integer cityId,
            @RequestParam String name,
            @RequestParam String description,
            @RequestParam String address
    ) {
        try {
            if (cityId == null || cityId <= 0) {
                LOG.warn("Hotel creation rejected because cityId is invalid. cityId={}, name={}", cityId, name);
                return "redirect:/hotels/new-hotel?cityMissing=true";
            }

            City city = cityRepository.findById(cityId);
            if (city == null) {
                LOG.warn("Hotel creation rejected because city does not exist. cityId={}, name={}", cityId, name);
                return "redirect:/hotels/new-hotel?cityMissing=true";
            }

            if (name == null || name.length() < 2 || name.length() > 200) {
                LOG.warn("Hotel creation rejected because hotel name is invalid. cityId={}, name={}", cityId, name);
                return "redirect:/hotels/new-hotel?hotelInvalid=true";
            }

            if (description == null || description.length() < 10 || description.length() > 511) {
                LOG.warn("Hotel creation rejected because description is invalid. cityId={}, name={}", cityId, name);
                return "redirect:/hotels/new-hotel?hotelInvalid=true";
            }

            if (address != null && address.length() > 255) {
                LOG.warn("Hotel creation rejected because address is invalid. cityId={}, name={}, addressLength={}",
                        cityId, name, address.length());
                return "redirect:/hotels/new-hotel?hotelInvalid=true";
            }

            Hotel hotel = new Hotel(cityId, name, description, address);
            long hotelId = hotelRepository.create(hotel);

            auditLogger.audit("Created hotel via controller. hotelId=" + hotelId + ", cityId=" + cityId + ", name=" + name);
            LOG.info("Hotel created successfully via controller. hotelId={}, cityId={}, name={}", hotelId, cityId, name);

            return "redirect:/hotels/new-hotel";
        } catch (RuntimeException e) {
            LOG.error("Unexpected error while creating hotel. cityId={}, name={}", cityId, name, e);
            return "redirect:/hotels/new-hotel?error=true";
        }
    }

    @GetMapping("/api/hotels/{hotelId}/room-types")
    public ResponseEntity<List<RoomType>> getRoomTypesForHotel(@PathVariable Integer hotelId) {
        try {
            List<RoomType> result = roomRepository.getAllRoomTypes(hotelId);
            LOG.info("Fetched room types for hotelId={}", hotelId);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            LOG.error("Unexpected error while fetching room types for hotelId={}", hotelId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/api/hotels/search", produces = "application/json")
    @ResponseBody
    public List<Hotel> search(@RequestParam("query") String query) {
        try {
            if (query == null || query.trim().isEmpty()) {
                LOG.warn("Hotel search rejected because query is empty");
                return new ArrayList<>();
            }

            LOG.info("Hotel search requested. query={}", query);
            return hotelRepository.search(query);
        } catch (RuntimeException e) {
            LOG.error("Unexpected error while searching hotels. query={}", query, e);
            throw e;
        } catch (Exception e) {
            LOG.error("Unexpected checked exception while searching hotels. query={}", query, e);
            throw new RuntimeException("Failed to search hotels", e);
        }
    }
}