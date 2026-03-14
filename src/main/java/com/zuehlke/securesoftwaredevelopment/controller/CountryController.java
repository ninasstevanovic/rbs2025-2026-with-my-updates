package com.zuehlke.securesoftwaredevelopment.controller;

import com.zuehlke.securesoftwaredevelopment.config.AuditLogger;
import com.zuehlke.securesoftwaredevelopment.domain.Country;
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

@Controller
public class CountryController {
    private static final Logger LOG = LoggerFactory.getLogger(CountryController.class);
    private static final AuditLogger auditLogger = AuditLogger.getAuditLogger(CountryController.class);

    private final CountryRepository countryRepository;

    public CountryController(CountryRepository countryRepository) {
        this.countryRepository = countryRepository;
    }

    @GetMapping("/new-country")
    @PreAuthorize("hasAuthority('CREATE_COUNTRY')")
    public String newCountry(
            Model model,
            @RequestParam(value = "nameTaken", required = false) Boolean nameTaken,
            @RequestParam(value = "nameInvalid", required = false) Boolean nameInvalid
    ) {
        try {
            List<Country> countryList = countryRepository.getAll();
            model.addAttribute("countries", countryList);

            model.addAttribute("nameTaken", Boolean.TRUE.equals(nameTaken));
            model.addAttribute("nameInvalid", Boolean.TRUE.equals(nameInvalid));

            LOG.info("Opened new country page");
            return "new-country";
        } catch (RuntimeException e) {
            LOG.error("Unexpected error while opening new country page", e);
            return "redirect:/?error=true";
        }
    }

    @PostMapping("/countries/create")
    @PreAuthorize("hasAuthority('CREATE_COUNTRY')")
    public String create(@RequestParam String name) {
        try {
            if (name == null || name.length() < 2 || name.length() > 100) {
                LOG.warn("Country creation rejected because name is invalid. name={}", name);
                return "redirect:/new-country?nameInvalid=true";
            }

            if (!countryRepository.findByName(name).isEmpty()) {
                LOG.warn("Country creation rejected because name already exists. name={}", name);
                return "redirect:/new-country?nameTaken=true";
            }

            long countryId = countryRepository.create(new Country(name));

            auditLogger.audit("Created country via controller. countryId=" + countryId + ", name=" + name);
            LOG.info("Country created successfully via controller. countryId={}, name={}", countryId, name);

            return "redirect:/new-country";
        } catch (RuntimeException e) {
            LOG.error("Unexpected error while creating country. name={}", name, e);
            return "redirect:/new-country?error=true";
        }
    }
}