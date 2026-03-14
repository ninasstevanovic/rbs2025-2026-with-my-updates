package com.zuehlke.securesoftwaredevelopment.service;

import com.zuehlke.securesoftwaredevelopment.domain.User;
import com.zuehlke.securesoftwaredevelopment.repository.RoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service("personSecurityService")
public class PersonSecurityService {

    private static final Logger LOG = LoggerFactory.getLogger(PersonSecurityService.class);
    private RoleRepository roleRepository;

    public PersonSecurityService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    public boolean canUpdatePerson(String personId, Authentication authentication) {
        try {
            if (authentication == null || authentication.getPrincipal() == null) {
                LOG.warn("Authorization denied for person update because authentication is missing. targetPersonId={}", personId);
                return false;
            }

            Object principal = authentication.getPrincipal();
            if (!(principal instanceof User)) {
                LOG.warn("Authorization denied for person update because principal is invalid. targetPersonId={}, principalType={}",
                        personId, principal.getClass().getName());
                return false;
            }

            User currentUser = (User) principal;

            boolean isAdmin = roleRepository.isAdmin(currentUser.getId());
            if (isAdmin) {
                LOG.info("Authorization granted for person update because user is admin. actorUserId={}, targetPersonId={}",
                        currentUser.getId(), personId);
                return true;
            }

            try {
                int requestedPersonId = Integer.parseInt(personId);
                boolean allowed = currentUser.getId() == requestedPersonId;

                if (allowed) {
                    LOG.info("Authorization granted for person update because user accesses own profile. actorUserId={}, targetPersonId={}",
                            currentUser.getId(), personId);
                } else {
                    LOG.warn("Authorization denied for person update. actorUserId={}, targetPersonId={}",
                            currentUser.getId(), personId);
                }

                return allowed;
            } catch (NumberFormatException e) {
                LOG.warn("Authorization denied for person update because targetPersonId is invalid. actorUserId={}, targetPersonId={}",
                        currentUser.getId(), personId);
                return false;
            }
        } catch (RuntimeException e) {
            LOG.error("Unexpected error while checking authorization for person update. targetPersonId={}", personId, e);
            return false;
        }
    }
}
