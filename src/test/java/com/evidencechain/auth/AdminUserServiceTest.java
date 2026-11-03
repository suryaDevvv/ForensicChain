package com.evidencechain.auth;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminUserServiceTest {

    @Test
    void adminCanCreateAUserWithTheRequestedRoleAndHashedPassword() {
        UserRepository users = mock(UserRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(users.existsByUsername("auditor-one")).thenReturn(false);
        when(encoder.encode("a-very-long-test-password")).thenReturn("bcrypt-hash");
        when(users.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        AdminUserService service = new AdminUserService(users, encoder);

        UserResponse response = service.createUser(
                new CreateUserRequest("auditor-one", "a-very-long-test-password", Role.AUDITOR));

        assertThat(response.role()).isEqualTo(Role.AUDITOR);
        verify(encoder).encode("a-very-long-test-password");
        verify(users).save(any(User.class));
    }
}
