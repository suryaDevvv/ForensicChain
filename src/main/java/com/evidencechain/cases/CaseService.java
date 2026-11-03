package com.evidencechain.cases;

import com.evidencechain.auth.User;
import com.evidencechain.auth.UserRepository;
import com.evidencechain.common.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CaseService {

    private final CaseRepository cases;
    private final UserRepository users;

    public CaseService(CaseRepository cases, UserRepository users) {
        this.cases = cases;
        this.users = users;
    }

    @Transactional
    public CaseResponse create(CreateCaseRequest request, String authenticatedUsername) {
        User creator = users.findByUsername(authenticatedUsername)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED",
                        "Authentication is required"));
        return CaseResponse.from(cases.save(InvestigationCase.create(
                request.title(), request.description(), creator)));
    }

    @Transactional(readOnly = true)
    public List<CaseResponse> findAll() {
        return cases.findAllByOrderByCreatedAtDesc().stream().map(CaseResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public CaseResponse findById(UUID id) {
        return CaseResponse.from(findCase(id));
    }

    @Transactional
    public CaseResponse update(UUID id, UpdateCaseRequest request) {
        InvestigationCase investigationCase = findCase(id);
        if (investigationCase.getStatus() == CaseStatus.CLOSED) {
            throw new ApiException(HttpStatus.CONFLICT, "CASE_CLOSED", "A CLOSED case cannot be changed");
        }
        investigationCase.update(request.title(), request.description(), request.status());
        return CaseResponse.from(cases.save(investigationCase));
    }

    private InvestigationCase findCase(UUID id) {
        return cases.findById(id).orElseThrow(() -> new ApiException(
                HttpStatus.NOT_FOUND, "CASE_NOT_FOUND", "Case was not found"));
    }
}
