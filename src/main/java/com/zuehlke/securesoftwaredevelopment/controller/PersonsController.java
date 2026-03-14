package com.zuehlke.securesoftwaredevelopment.controller;

import com.zuehlke.securesoftwaredevelopment.config.AuditLogger;
import com.zuehlke.securesoftwaredevelopment.domain.Person;
import com.zuehlke.securesoftwaredevelopment.domain.User;
import com.zuehlke.securesoftwaredevelopment.repository.PersonRepository;
import com.zuehlke.securesoftwaredevelopment.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Controller

public class PersonsController {

    private static final Logger LOG = LoggerFactory.getLogger(PersonsController.class);
    private static final AuditLogger auditLogger = AuditLogger.getAuditLogger(PersonRepository.class);

    private final PersonRepository personRepository;
    private final UserRepository userRepository;

    public PersonsController(PersonRepository personRepository, UserRepository userRepository) {
        this.personRepository = personRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/persons/{id}")
    // treba da obezbedim da view profile- detalje user-a moze da vidi samo admin, ALI da ako dodjem na svoj profil onda mogu da ga vidim
    // tj ako je id moj id onda imam dozvolu
    @PreAuthorize("hasAuthority('VIEW_PERSON') or authentication.name == @userRepository.findUsername(#id)")
    public String person(@PathVariable int id, Model model) {
        try {
            Person person = personRepository.get("" + id);
            String username = userRepository.findUsername(id);

            if (person == null || username == null) {
                LOG.warn("Person profile requested for non-existing user. personId={}", id);
                return "redirect:/persons?error=true";
            }

            model.addAttribute("person", person);
            model.addAttribute("username", username);

            return "person";
        } catch (RuntimeException e) {
            LOG.error("Unexpected error while opening person profile. personId={}", id, e);
            return "redirect:/persons?error=true";
        }
    }

    @GetMapping("/myprofile")
    public String self(Model model, Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            Person person = personRepository.get("" + user.getId());
            String username = userRepository.findUsername(user.getId());

            if (person == null || username == null) {
                LOG.warn("Own profile could not be loaded. userId={}", user.getId());
                return "redirect:/?error=true";
            }

            model.addAttribute("person", person);
            model.addAttribute("username", username);

            LOG.info("Own profile opened. userId={}", user.getId());
            return "person";
        } catch (RuntimeException e) {
            LOG.error("Unexpected error while opening own profile", e);
            return "redirect:/?error=true";
        }
    }

    @DeleteMapping("/persons/{id}")
    // treba da obezbedim da admin moze da update-uje bilo kog user-a, dok menadzer i cutomer mogu samo sebe
    @PreAuthorize("@personSecurityService.canUpdatePerson(#id, authentication)")
    public ResponseEntity<Void> person(@PathVariable int id) {
        try {
            Person existingPerson = personRepository.get("" + id);
            String existingUsername = userRepository.findUsername(id);

            if (existingPerson == null && existingUsername == null) {
                LOG.warn("Attempt to delete non-existing person. personId={}", id);
                return ResponseEntity.notFound().build();
            }

            personRepository.delete(id);
            userRepository.delete(id);

            auditLogger.audit("Deleted person. personId=" + id + ", username=" + existingUsername);
            LOG.info("Person deleted successfully. personId={}", id);

            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            LOG.error("Unexpected error while deleting person. personId={}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/update-person")
    // treba da obezbedim da admin moze da update-uje bilo kog user-a, dok menadzer i cutomer mogu samo sebe
    @PreAuthorize("@personSecurityService.canUpdatePerson(#person.id, authentication)")
    public String updatePerson(Person person, String username) {
        try {
            if (person == null || person.getId() == null) {
                LOG.warn("Person update rejected because person or personId is missing");
                return "redirect:/persons?error=true";
            }

            Person oldPerson = personRepository.get(person.getId());
            String oldUsername = userRepository.findUsername(Integer.parseInt(person.getId()));

            personRepository.update(person);
            userRepository.updateUsername(Integer.parseInt(person.getId()), username);

            auditLogger.audit(
                    "Updated person and username. personId=" + person.getId()
                            + ", oldUsername=" + oldUsername
                            + ", newUsername=" + username
                            + ", oldFirstName=" + oldPerson.getFirstName()
                            + ", newFirstName=" + person.getFirstName()
                            + ", oldLastName=" + oldPerson.getLastName()
                            + ", newLastName=" + person.getLastName()
                            + ", oldEmail=" + oldPerson.getEmail()
                            + ", newEmail=" + person.getEmail()
            );

            LOG.info("Person updated successfully. personId={}", person.getId());
            return "redirect:/persons/" + person.getId();
        } catch (RuntimeException e) {
            LOG.error("Unexpected error while updating person. personId={}", person != null ? person.getId() : null, e);
            return "redirect:/persons?error=true";
        }
    }

    @GetMapping("/persons")
    @PreAuthorize("hasAuthority('VIEW_PERSONS_LIST')")
    public String persons(Model model) {
        try {
            model.addAttribute("persons", personRepository.getAll());
            LOG.info("Persons list opened");
            return "persons";
        } catch (RuntimeException e) {
            LOG.error("Unexpected error while opening persons list", e);
            return "redirect:/?error=true";
        }
    }

    @GetMapping(value = "/persons/search", produces = "application/json")
    @ResponseBody
    @PreAuthorize("hasAuthority('VIEW_PERSONS_LIST')")
    public List<Person> searchPersons(@RequestParam String searchTerm) throws SQLException {
        try {
            if (searchTerm == null || searchTerm.trim().isEmpty()) {
                LOG.warn("Persons search rejected because searchTerm is empty");
                return new ArrayList<>();
            }

            LOG.info("Persons search requested. searchTerm={}", searchTerm);
            return personRepository.search(searchTerm);
        } catch (RuntimeException e) {
            LOG.error("Unexpected error while searching persons. searchTerm={}", searchTerm, e);
            throw e;
        } catch (Exception e) {
            LOG.error("Unexpected checked exception while searching persons. searchTerm={}", searchTerm, e);
            throw new RuntimeException("Failed to search persons", e);
        }
    }
}
