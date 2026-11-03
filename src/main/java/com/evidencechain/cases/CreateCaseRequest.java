package com.evidencechain.cases;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCaseRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 4000) String description) {
}
