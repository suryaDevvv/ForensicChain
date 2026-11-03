package com.evidencechain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.evidencechain.cases.CreateCaseRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ApiIntegrationTest {

    private static final String BOOTSTRAP_PASSWORD = "test-bootstrap-" + UUID.randomUUID();
    private static final String JWT_SECRET = randomSecret();

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("evidencechain.jwt.secret-base64", () -> JWT_SECRET);
        registry.add("evidencechain.jwt.expiration-seconds", () -> "3600");
        registry.add("evidencechain.bootstrap-admin.username", () -> "integration-admin");
        registry.add("evidencechain.bootstrap-admin.password", () -> BOOTSTRAP_PASSWORD);
    }

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void registersLogsInAndProtectsCaseEndpoints() throws Exception {
        MvcResult registration = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"investigator-one","password":"investigator-test-password"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("INVESTIGATOR"))
                .andReturn();
        JsonNode registeredUser = objectMapper.readTree(registration.getResponse().getContentAsString());

        String investigatorToken = login("investigator-one", "investigator-test-password");
        mvc.perform(get("/api/cases").header("Authorization", bearer(investigatorToken)))
                .andExpect(status().isOk());

        String body = """
                {"title":"Synthetic evidence review","description":"Public test data","createdBy":"%s"}
                """.formatted(UUID.randomUUID());
        MvcResult created = mvc.perform(post("/api/cases")
                        .header("Authorization", bearer(investigatorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andReturn();
        JsonNode createdCase = objectMapper.readTree(created.getResponse().getContentAsString());
        assertThat(createdCase.path("createdBy").asText()).isEqualTo(registeredUser.path("id").asText());

        mvc.perform(get("/api/cases/{id}", createdCase.path("id").asText())
                        .header("Authorization", bearer(investigatorToken)))
                .andExpect(status().isOk());

        mvc.perform(get("/api/cases"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void adminCanCreateAuditorWhoCanReadButCannotCreateCases() throws Exception {
        String adminToken = login("integration-admin", BOOTSTRAP_PASSWORD);
        MvcResult createdUser = mvc.perform(post("/api/admin/users")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"auditor-one","password":"auditor-test-password","role":"AUDITOR"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("AUDITOR"))
                .andReturn();
        assertThat(createdUser.getResponse().getContentAsString()).doesNotContain("password");

        String auditorToken = login("auditor-one", "auditor-test-password");
        mvc.perform(post("/api/cases")
                        .header("Authorization", bearer(auditorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Should be denied\",\"description\":\"Synthetic\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        mvc.perform(get("/api/cases").header("Authorization", bearer(auditorToken)))
                .andExpect(status().isOk());

        String investigatorToken = loginAfterRegistration("investigator-two");
        mvc.perform(post("/api/admin/users")
                        .header("Authorization", bearer(investigatorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"blocked-user","password":"blocked-test-password","role":"AUDITOR"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void loginFailureIsGenericAndClosedCasesRejectEveryUpdate() throws Exception {
        String unknownBody = """
                {"username":"does-not-exist","password":"wrong-password-123"}
                """;
        String wrongPasswordBody = """
                {"username":"integration-admin","password":"wrong-password-123"}
                """;
        MvcResult unknown = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(unknownBody))
                .andExpect(status().isUnauthorized()).andReturn();
        MvcResult wrongPassword = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(wrongPasswordBody))
                .andExpect(status().isUnauthorized()).andReturn();
        JsonNode unknownError = objectMapper.readTree(unknown.getResponse().getContentAsString());
        JsonNode passwordError = objectMapper.readTree(wrongPassword.getResponse().getContentAsString());
        assertThat(unknownError.path("code").asText()).isEqualTo(passwordError.path("code").asText());
        assertThat(unknownError.path("message").asText()).isEqualTo(passwordError.path("message").asText());

        String investigatorToken = loginAfterRegistration("closing-investigator");
        MvcResult created = mvc.perform(post("/api/cases")
                        .header("Authorization", bearer(investigatorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Case to close\",\"description\":\"Synthetic\"}"))
                .andExpect(status().isCreated()).andReturn();
        String caseId = objectMapper.readTree(created.getResponse().getContentAsString()).path("id").asText();

        mvc.perform(put("/api/cases/{id}", caseId)
                        .header("Authorization", bearer(investigatorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Case to close\",\"description\":\"Synthetic\",\"status\":\"CLOSED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));
        mvc.perform(put("/api/cases/{id}", caseId)
                        .header("Authorization", bearer(investigatorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Attempted change\",\"description\":\"Synthetic\",\"status\":\"CLOSED\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CASE_CLOSED"));
    }

    @Test
    void validatesRegistrationAndTitleLengths() throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"x\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest());

        String investigatorToken = loginAfterRegistration("validation-investigator");
        mvc.perform(post("/api/cases")
                        .header("Authorization", bearer(investigatorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\",\"description\":\"Synthetic\"}"))
                .andExpect(status().isBadRequest());

        String tooLongTitle = objectMapper.writeValueAsString(
                new CreateCaseRequest("t".repeat(201), "Synthetic"));
        mvc.perform(post("/api/cases")
                        .header("Authorization", bearer(investigatorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tooLongTitle))
                .andExpect(status().isBadRequest());
    }

    private String loginAfterRegistration(String username) throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"a-valid-test-password"}
                                """.formatted(username)))
                .andExpect(status().isCreated());
        return login(username, "a-valid-test-password");
    }

    private String login(String username, String password) throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginPayload(username, password))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("accessToken").asText();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private static String randomSecret() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    private record LoginPayload(String username, String password) {
    }
}
