package com.zuehlke.securesoftwaredevelopment.controller;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.GoogleAuthenticatorQRGenerator;
import com.zuehlke.securesoftwaredevelopment.config.AuditLogger;
import com.zuehlke.securesoftwaredevelopment.domain.HashedUser;
import com.zuehlke.securesoftwaredevelopment.repository.HashedUserRepository;
import org.slf4j.Logger;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.slf4j.LoggerFactory;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LoginController {

    private static final Logger LOG = LoggerFactory.getLogger(LoginController.class);
    private static final AuditLogger auditLogger = AuditLogger.getAuditLogger(LoginController.class);

    private final HashedUserRepository repository;

    LoginController(HashedUserRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/login")
    public String showLoginForm() {
        LOG.info("Login page opened");
        return "login";
    }

    @GetMapping("/register-totp")
    public String showRegisterTotp(Model model, Authentication authentication) {
        try {
            if (authentication == null || !(authentication.getPrincipal() instanceof HashedUser)) {
                LOG.warn("Attempt to open TOTP registration page without valid authentication");
                return "redirect:/login";
            }

            GoogleAuthenticator gAuth = new GoogleAuthenticator();
            GoogleAuthenticatorKey key = gAuth.createCredentials();

            model.addAttribute("totpKey", key.getKey());

            HashedUser user = (HashedUser) authentication.getPrincipal();
            String totpUrl = GoogleAuthenticatorQRGenerator.getOtpAuthTotpURL(
                    "RBS Secure Travel Agency",
                    user.getUsername(),
                    key
            );
            model.addAttribute("totpUrl", totpUrl);

            LOG.info("TOTP registration page opened for username={}", user.getUsername());
            return "register-totp";
        } catch (RuntimeException e) {
            LOG.error("Unexpected error while opening TOTP registration page", e);
            return "redirect:/login?error=true";
        }
    }

    @PostMapping("/register-totp")
    public String registerTotp(@RequestParam() String totpKey, Model model, Authentication authentication) {
        try {
            if (authentication == null || !(authentication.getPrincipal() instanceof HashedUser)) {
                LOG.warn("Attempt to register TOTP without valid authentication");
                return "redirect:/login";
            }

            HashedUser user = (HashedUser) authentication.getPrincipal();

            if (totpKey == null || totpKey.isEmpty()) {
                LOG.warn("TOTP registration rejected because key is missing. username={}", user.getUsername());
                model.addAttribute("registered", false);
                return "register-totp";
            }

            repository.saveTotpKey(user.getUsername(), totpKey);

            auditLogger.audit("Registered TOTP for username=" + user.getUsername());
            LOG.info("TOTP registered successfully for username={}", user.getUsername());

            model.addAttribute("registered", true);
            return "register-totp";
        } catch (RuntimeException e) {
            LOG.error("Unexpected error while registering TOTP", e);
            model.addAttribute("registered", false);
            return "register-totp";
        }
    }
}
