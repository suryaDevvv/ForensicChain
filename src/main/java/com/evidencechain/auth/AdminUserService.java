package com.evidencechain.auth;

import com.evidencechain.common.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminUserService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    public AdminUserService(UserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (users.existsByUsername(request.username())) {
            throw new ApiException(HttpStatus.CONFLICT, "USERNAME_TAKEN", "Username is already in use");
        }
        User user = User.create(request.username(), passwordEncoder.encode(request.password()), request.role());
        return UserResponse.from(users.save(user));
    }
}
