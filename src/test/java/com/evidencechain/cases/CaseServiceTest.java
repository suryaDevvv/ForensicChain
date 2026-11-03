package com.evidencechain.cases;

import com.evidencechain.auth.Role;
import com.evidencechain.auth.User;
import com.evidencechain.auth.UserRepository;
import com.evidencechain.common.ApiException;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CaseServiceTest {

    @Test
    void createTakesCreatorFromAuthenticatedUsernameAndStartsOpen() {
        CaseRepository cases = mock(CaseRepository.class);
        UserRepository users = mock(UserRepository.class);
        User investigator = User.create("investigator", "hash", Role.INVESTIGATOR);
        when(users.findByUsername("investigator")).thenReturn(Optional.of(investigator));
        when(cases.save(any(InvestigationCase.class))).thenAnswer(invocation -> invocation.getArgument(0));
        CaseService service = new CaseService(cases, users);

        CaseResponse response = service.create(new CreateCaseRequest("Example case", "Synthetic data"),
                "investigator");

        assertThat(response.status()).isEqualTo(CaseStatus.OPEN);
        assertThat(response.createdBy()).isEqualTo(investigator.getId());
    }

    @Test
    void anyUpdateToClosedCaseReturnsConflict() {
        CaseRepository cases = mock(CaseRepository.class);
        UserRepository users = mock(UserRepository.class);
        InvestigationCase closed = InvestigationCase.create("Closed case", null,
                User.create("investigator", "hash", Role.INVESTIGATOR));
        closed.update("Closed case", null, CaseStatus.CLOSED);
        when(cases.findById(any(UUID.class))).thenReturn(Optional.of(closed));
        CaseService service = new CaseService(cases, users);

        assertThatThrownBy(() -> service.update(closed.getId(),
                new UpdateCaseRequest("Changed title", null, CaseStatus.CLOSED)))
                .isInstanceOf(ApiException.class)
                .hasMessage("A CLOSED case cannot be changed");
        verify(cases, never()).save(any(InvestigationCase.class));
    }
}
