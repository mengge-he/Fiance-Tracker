package com.Mengge.finance_tracker.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CoreApiIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc;

    @BeforeEach
    void setUpMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
            .apply(springSecurity())
            .build();
    }

    @Test
    void health_returnsOk() throws Exception {
        mockMvc.perform(get("/api/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void openApiDocs_arePublic() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.openapi").exists());
    }

    @Test
    void protectedRoutes_rejectWithoutToken() throws Exception {
        int code = mockMvc.perform(get("/api/transactions"))
            .andReturn()
            .getResponse()
            .getStatus();
        assertThat(code).isIn(401, 403);
    }

    @Test
    void register_thenTransactionsAndDashboard() throws Exception {
        String email = "user-" + UUID.randomUUID() + "@example.com";
        MvcResult reg = mockMvc.perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of("name", "Integration User", "email", email, "password", "secret12")
                    )
                )
        )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.token").exists())
            .andReturn();

        JsonNode body = objectMapper.readTree(reg.getResponse().getContentAsString());
        String token = body.get("token").asText();

        mockMvc.perform(
            post("/api/transactions")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"type":"EXPENSE","amount":42.50,"category":"Food","date":"2026-05-15","note":"groceries"}
                    """
                )
        )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.category").value("Food"))
            .andExpect(jsonPath("$.amount").value(42.5));

        mockMvc.perform(
            get("/api/transactions").header("Authorization", "Bearer " + token)
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.paged").value(false))
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content[0].category").value("Food"));

        mockMvc.perform(
            get("/api/dashboard").param("year", "2026").param("month", "5").header("Authorization", "Bearer " + token)
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.year").value(2026))
            .andExpect(jsonPath("$.month").value(5))
            .andExpect(jsonPath("$.totalSpentThisMonth").value(42.5));

        MvcResult budgetCreate = mockMvc.perform(
            post("/api/budgets")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"year":2026,"month":5,"category":"Food","limitAmount":30.00}
                    """
                )
        )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.overBudget").value(true))
            .andReturn();

        long budgetId = objectMapper.readTree(budgetCreate.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(
            get("/api/budgets").param("year", "2026").param("month", "5").header("Authorization", "Bearer " + token)
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.budgets[0].category").value("Food"));

        mockMvc.perform(
            delete("/api/budgets/" + budgetId).header("Authorization", "Bearer " + token)
        )
            .andExpect(status().isNoContent());
    }

    @Test
    void login_acceptsDifferentEmailCasingThanRegister() throws Exception {
        String local = "case-" + UUID.randomUUID();
        String registeredAs = local + "@EXAMPLE.COM";
        String loginAs = local + "@example.com";

        mockMvc.perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of("name", "Case Test", "email", registeredAs, "password", "secret12")
                    )
                )
        )
            .andExpect(status().isCreated());

        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of("email", loginAs, "password", "secret12")
                    )
                )
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").exists());
    }
}
