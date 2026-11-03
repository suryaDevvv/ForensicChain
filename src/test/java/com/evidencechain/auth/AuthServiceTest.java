package com.evidencechain.auth;

import com.evidencechain.common.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private final UserRepository users = mock(UserRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
    private final JwtService jwtService = mock(JwtService.class);
    private final AuthService authService = new AuthService(users, passwordEncoder, authenticationManager, jwtService);

    @Test
    void registrationAlwaysCreatesInvestigatorAndHashesPassword() {
        when(users.existsByUsername("new-investigator")).thenReturn(false);
        when(passwordEncoder.encode("a-very-long-test-password")).thenReturn("bcrypt-hash");
        when(users.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = authService.register(
                new RegisterRequest("new-investigator", "a-very-long-test-password"));

        assertThat(response.role()).isEqualTo(Role.INVESTIGATOR);
        verify(users).save(any(User.class));
        verify(passwordEncoder).encode("a-very-long-test-password");
    }

    @Test
    void duplicateRegistrationReturnsConflict() {
        when(users.existsByUsername("existing-user")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("existing-user", "a-very-long-test-password")))
                .isInstanceOf(ApiException.class)
                .satisfies(exception -> assertThat(((ApiException) exception).getStatus())
                        .isEqualTo(HttpStatus.CONFLICT));
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void loginMapsAuthenticationFailuresToOneGenericUnauthorizedError() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("internal authentication detail"));

        assertThatThrownBy(() -> authService.login(new LoginRequest("unknown", "incorrect-password")))
                .isInstanceOf(ApiException.class)
                .satisfies(exception -> {
                    ApiException apiException = (ApiException) exception;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(apiException.getCode()).isEqualTo("INVALID_CREDENTIALS");
                    assertThat(apiException.getMessage()).isEqualTo("Invalid username or password");
                });
        verify(users, never()).findByUsername("unknown");
    }
}
