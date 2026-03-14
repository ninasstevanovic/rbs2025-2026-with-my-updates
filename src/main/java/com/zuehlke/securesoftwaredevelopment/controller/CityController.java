package com.zuehlke.securesoftwaredevelopment.controller;

import com.zuehlke.securesoftwaredevelopment.config.AuditLogger;
import com.zuehlke.securesoftwaredevelopment.domain.City;
import com.zuehlke.securesoftwaredevelopment.domain.Country;
import com.zuehlke.securesoftwaredevelopment.repository.CityRepository;
import com.zuehlke.securesoftwaredevelopment.repository.CountryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Objects;

@Controller
public class CityController {
    private static final Logger LOG = LoggerFactory.getLogger(CityController.class);
    private static final AuditLogger auditLogger = AuditLogger.getAuditLogger(CityController.class);

    private final CityRepository cityRepository;
    private final CountryRepository countryRepository;

    public CityController(CountryRepository countryRepository, CityRepository cityRepository) {
        this.countryRepository = countryRepository;
        this.cityRepository = cityRepository;
    }

    @GetMapping("/new-city")
    @PreAuthorize("hasAuthority('CREATE_CITY')")
    public String newCity(
            Model model,
            @RequestParam(value = "cityInvalid", required = false) Boolean cityInvalid,
            @RequestParam(value = "countryMissing", required = false) Boolean countryMissing,
            @RequestParam(value = "cityExists", required = false) Boolean cityExists
    ) {
        try {
            List<Country> countryList = countryRepository.getAll();
            List<City> cityList = cityRepository.getAll();

            model.addAttribute("countries", countryList);
            model.addAttribute("cities", cityList);

            model.addAttribute("cityInvalid", Boolean.TRUE.equals(cityInvalid));
            model.addAttribute("countryMissing", Boolean.TRUE.equals(countryMissing));
            model.addAttribute("cityExists", Boolean.TRUE.equals(cityExists));

            LOG.info("Opened new city page");
            return "new-city";
        } catch (RuntimeException e) {
            LOG.error("Unexpected error while opening new city page", e);
            return "redirect:/?error=true";
        }
    }

    @PostMapping("/cities/create")
    @PreAuthorize("hasAuthority('CREATE_CITY')")
    public String createCity(
            @RequestParam Integer countryId,
            @RequestParam String name
    ) {
        try {
            if (countryId == null || countryId <= 0) {
                LOG.warn("City creation rejected because countryId is invalid. countryId={}, name={}", countryId, name);
                return "redirect:/new-city?countryMissing=true";
            }

            Country country = countryRepository.findById(countryId);
            if (country == null) {
                LOG.warn("City creation rejected because country was not found. countryId={}, name={}", countryId, name);
                return "redirect:/new-city?countryMissing=true";
            }

        if (name.length() < 2 || name.length() > 200) {
            return "redirect:/new-city?cityInvalid=true";
        }

            if (cityRepository.findByName(name).stream().anyMatch(x -> Objects.equals(x.getCountryId(), countryId))) {
                LOG.warn("City creation rejected because city already exists. countryId={}, name={}", countryId, name);
                return "redirect:/new-city?cityExists=true";
            }

            City city = new City();
            city.setName(name);
            city.setCountryId(countryId);
            long cityId = cityRepository.create(city);

            auditLogger.audit("Created city via controller. cityId=" + cityId + ", countryId=" + countryId + ", name=" + name);
            LOG.info("City created successfully via controller. cityId={}, countryId={}, name={}", cityId, countryId, name);

            return "redirect:/new-city";
        } catch (RuntimeException e) {
            LOG.error("Unexpected error while creating city. countryId={}, name={}", countryId, name, e);
            return "redirect:/new-city?error=true";
        }
    }
}