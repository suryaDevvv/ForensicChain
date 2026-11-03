package com.evidencechain.cases;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CaseRepository extends JpaRepository<InvestigationCase, UUID> {
    List<InvestigationCase> findAllByOrderByCreatedAtDesc();
}
