package com.zuehlke.securesoftwaredevelopment.controller;

import com.zuehlke.securesoftwaredevelopment.config.AuditLogger;
import com.zuehlke.securesoftwaredevelopment.domain.*;
import com.zuehlke.securesoftwaredevelopment.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Controller
public class ReservationController {
    private static final Logger LOG = LoggerFactory.getLogger(ReservationController.class);
    private static final AuditLogger auditLogger = AuditLogger.getAuditLogger(ReservationController.class);

    private final ReservationRepository reservationRepository;
    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;
    private final RoleRepository roleRepository;

    public ReservationController(ReservationRepository reservationRepository, HotelRepository hotelRepository, RoomRepository roomRepository, RoleRepository roleRepository) {
        this.reservationRepository = reservationRepository;
        this.hotelRepository = hotelRepository;
        this.roomRepository = roomRepository;
        this.roleRepository = roleRepository;
    }

    @GetMapping("/reservations/view")
    @PreAuthorize("hasAuthority('VIEW_RESERVATION')")
    public String view(Model model, Authentication authentication) {
        try {
            if (authentication == null || !(authentication.getPrincipal() instanceof User)) {
                LOG.warn("Attempt to open reservations page without valid authentication");
                return "redirect:/login";
            }

            User user = (User) authentication.getPrincipal();
            Integer userId = user.getId();

            List<Reservation> userReservations = reservationRepository.forUser(userId);
            boolean isAdmin = roleRepository.isAdmin(userId);

            model.addAttribute("userReservations", userReservations);
            model.addAttribute("isAdmin", isAdmin);

            if (isAdmin) {
                List<Reservation> allReservations = reservationRepository.getAll();
                model.addAttribute("allReservations", allReservations);
            }

            LOG.info("Reservations page opened. userId={}, isAdmin={}", userId, isAdmin);
            return "reservations";
        } catch (RuntimeException e) {
            LOG.error("Unexpected error while opening reservations page", e);
            return "redirect:/?error=true";
        }
    }

    @GetMapping("/reservations/new/{id}")
    public String showReservation(
            @PathVariable int id,
            Model model,
            @RequestParam(value = "cityInvalid", required = false) Boolean cityInvalid,
            @RequestParam(value = "countryMissing", required = false) Boolean countryMissing,
            @RequestParam(value = "cityExists", required = false) Boolean cityExists
    ) {
        try {
            Hotel hotel = hotelRepository.get(id);
            if (hotel == null) {
                LOG.warn("Reservation page requested for non-existing hotel. hotelId={}", id);
                return "redirect:/?error=true";
            }

            List<RoomType> roomTypes = roomRepository.getAllRoomTypes(id);

            model.addAttribute("id", id);
            model.addAttribute("hotel", hotel);
            model.addAttribute("roomTypes", roomTypes);

            LOG.info("Reservation creation page opened. hotelId={}", id);
            return "reserve-hotel";
        } catch (RuntimeException e) {
            LOG.error("Unexpected error while opening reservation creation page. hotelId={}", id, e);
            return "redirect:/?error=true";
        }
    }

    @PostMapping("/reservations/create")
    public String createReservation(
            @RequestParam Integer hotelId,
            @RequestParam Integer roomTypeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam Integer roomsCount,
            @RequestParam Integer guestsCount,
            Authentication authentication
    ) {
        String redirectPage = "redirect:/reservations/new/" + hotelId;

        try {
            if (authentication == null || !(authentication.getPrincipal() instanceof User)) {
                LOG.warn("Attempt to create reservation without valid authentication. hotelId={}", hotelId);
                return "redirect:/login";
            }

            if (hotelId == null || hotelId <= 0) {
                LOG.warn("Reservation creation rejected because hotelId is invalid. hotelId={}", hotelId);
                return redirectPage + "?createError=true";
            }
            if (roomTypeId == null || roomTypeId <= 0) {
                LOG.warn("Reservation creation rejected because roomTypeId is invalid. hotelId={}, roomTypeId={}", hotelId, roomTypeId);
                return redirectPage + "?createError=true";
            }
            if (roomsCount == null || roomsCount <= 0) {
                LOG.warn("Reservation creation rejected because roomsCount is invalid. hotelId={}, roomsCount={}", hotelId, roomsCount);
                return redirectPage + "?createError=true";
            }
            if (guestsCount == null || guestsCount <= 0) {
                LOG.warn("Reservation creation rejected because guestsCount is invalid. hotelId={}, guestsCount={}", hotelId, guestsCount);
                return redirectPage + "?createError=true";
            }
            if (startDate == null || endDate == null || !endDate.isAfter(startDate)) {
                LOG.warn("Reservation creation rejected because dates are invalid. hotelId={}, startDate={}, endDate={}",
                        hotelId, startDate, endDate);
                return redirectPage + "?dateError=true";
            }

            if (!hotelRepository.existsById(hotelId)) {
                LOG.warn("Reservation creation rejected because hotel does not exist. hotelId={}", hotelId);
                return redirectPage + "?hotelError=true";
            }

            RoomType roomType = roomRepository.findByIdAndHotelId(roomTypeId, hotelId);
            if (roomType == null) {
                LOG.warn("Reservation creation rejected because room type does not exist. hotelId={}, roomTypeId={}", hotelId, roomTypeId);
                return redirectPage + "?roomTypeError=true";
            }

            long nights = ChronoUnit.DAYS.between(startDate, endDate);
            BigDecimal totalPrice = roomType.getPricePerNight()
                    .multiply(BigDecimal.valueOf(nights))
                    .multiply(BigDecimal.valueOf(roomsCount));

            int maxGuests = roomType.getCapacity() * roomsCount;
            if (guestsCount > maxGuests) {
                LOG.warn("Reservation creation rejected because guestsCount exceeds capacity. hotelId={}, roomTypeId={}, guestsCount={}, maxGuests={}",
                        hotelId, roomTypeId, guestsCount, maxGuests);
                return redirectPage + "?createError=true";
            }

            User user = (User) authentication.getPrincipal();
            Integer userId = user.getId();

            Reservation r = new Reservation();
            r.setUserId(userId);
            r.setHotelId(hotelId);
            r.setRoomTypeId(roomTypeId);
            r.setStartDate(startDate);
            r.setEndDate(endDate);
            r.setRoomsCount(roomsCount);
            r.setGuestsCount(guestsCount);
            r.setTotalPrice(totalPrice);

            long reservationId = reservationRepository.create(r);

            auditLogger.audit(
                    "Created reservation via controller. reservationId=" + reservationId
                            + ", hotelId=" + hotelId
                            + ", roomTypeId=" + roomTypeId
                            + ", startDate=" + startDate
                            + ", endDate=" + endDate
                            + ", roomsCount=" + roomsCount
                            + ", guestsCount=" + guestsCount
                            + ", totalPrice=" + totalPrice
            );

            LOG.info("Reservation created successfully. reservationId={}, userId={}, hotelId={}",
                    reservationId, userId, hotelId);

            return redirectPage + "?created=true";
        } catch (RuntimeException e) {
            LOG.error("Unexpected error while creating reservation. hotelId={}, roomTypeId={}", hotelId, roomTypeId, e);
            return redirectPage + "?error=true";
        }
    }

    @PostMapping("/reservations/delete")
    public String delete(@RequestParam Integer id, Authentication authentication) {
        try {
            if (authentication == null || !(authentication.getPrincipal() instanceof User)) {
                LOG.warn("Attempt to delete reservation without valid authentication. reservationId={}", id);
                return "redirect:/login";
            }

            User user = (User) authentication.getPrincipal();
            boolean isAdmin = roleRepository.isAdmin(user.getId());
            boolean isUsersReservation = reservationRepository.forUser(user.getId())
                    .stream()
                    .anyMatch(r -> r.getId().equals(id));

            if (!isAdmin && !isUsersReservation) {
                LOG.warn("Unauthorized reservation delete attempt. reservationId={}, userId={}", id, user.getId());
                return "redirect:/reservations/view";
            }

            reservationRepository.deleteById(id);

            auditLogger.audit("Deleted reservation via controller. reservationId=" + id);
            LOG.info("Reservation deleted successfully. reservationId={}, userId={}, isAdmin={}", id, user.getId(), isAdmin);

            return "redirect:/reservations/view";
        } catch (RuntimeException e) {
            LOG.error("Unexpected error while deleting reservation. reservationId={}", id, e);
            return "redirect:/reservations/view?error=true";
        }
    }
}