package com.evidencechain.cases;

import java.time.Instant;
import java.util.UUID;

public record CaseResponse(
        UUID id,
        String title,
        String description,
        CaseStatus status,
        UUID createdBy,
        Instant createdAt) {

    public static CaseResponse from(InvestigationCase investigationCase) {
        return new CaseResponse(investigationCase.getId(), investigationCase.getTitle(),
                investigationCase.getDescription(), investigationCase.getStatus(),
                investigationCase.getCreatedBy().getId(), investigationCase.getCreatedAt());
    }
}
