package com.zuehlke.securesoftwaredevelopment.controller;

import com.zuehlke.securesoftwaredevelopment.config.AuditLogger;
import com.zuehlke.securesoftwaredevelopment.domain.Rating;
import com.zuehlke.securesoftwaredevelopment.domain.User;
import com.zuehlke.securesoftwaredevelopment.repository.RatingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Controller
public class RatingsController {
    private static final Logger LOG = LoggerFactory.getLogger(RatingsController.class);
    private static final AuditLogger auditLogger = AuditLogger.getAuditLogger(RatingsController.class);

    private final RatingRepository ratingRepository;

    public RatingsController(RatingRepository ratingRepository) {
        this.ratingRepository = ratingRepository;
    }

    @PostMapping(value = "/ratings", consumes = "application/json")
    @PreAuthorize("hasAuthority('RATE_HOTEL')")
    public String createOrUpdateRating(@RequestBody Rating rating, Authentication authentication) {
        try {
            if (authentication == null || !(authentication.getPrincipal() instanceof User)) {
                LOG.warn("Attempt to create or update rating without valid authentication");
                return "redirect:/login";
            }

            if (rating == null) {
                LOG.warn("Rating create/update rejected because request body is invalid");
                return "redirect:/?error=true";
            }

            User user = (User) authentication.getPrincipal();
            rating.setUserId(user.getId());

            ratingRepository.createOrUpdate(rating);

            auditLogger.audit("Created or updated hotel rating. hotelId=" + rating.getHotelId() + ", rating=" + rating.getRating());
            LOG.info("Rating created or updated successfully. hotelId={}, userId={}, rating={}",
                    rating.getHotelId(), user.getId(), rating.getRating());

            return "redirect:/hotels?id=" + rating.getHotelId();
        } catch (RuntimeException e) {
            LOG.error("Unexpected error while creating/updating rating. hotelId={}",
                    rating != null ? rating.getHotelId() : null, e);
            return "redirect:/?error=true";
        }
    }
}