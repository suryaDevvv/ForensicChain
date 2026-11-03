package com.evidencechain.auth;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class BootstrapAdminInitializer implements ApplicationRunner {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final BootstrapAdminProperties properties;

    public BootstrapAdminInitializer(UserRepository users, PasswordEncoder passwordEncoder,
                                     BootstrapAdminProperties properties) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (users.existsByRole(Role.ADMIN)) {
            return;
        }
        if (properties.username() == null || properties.username().isBlank()
                || properties.password() == null || properties.password().length() < 12) {
            throw new IllegalStateException(
                    "BOOTSTRAP_ADMIN_USERNAME and a BOOTSTRAP_ADMIN_PASSWORD of at least 12 characters "
                            + "are required when no ADMIN user exists");
        }
        if (users.existsByUsername(properties.username())) {
            throw new IllegalStateException(
                    "The bootstrap username already belongs to a non-ADMIN user; choose another username");
        }
        users.save(User.create(properties.username(), passwordEncoder.encode(properties.password()), Role.ADMIN));
    }
}
