package com.financemanager.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end tests that exercise the real HTTP layer, security session handling, and database,
 * mirroring the scenarios covered by the provided financial_manager_tests.sh script.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class FinanceManagerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void unauthenticatedRequest_isRejectedWith401() throws Exception {
        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void fullUserJourney_registerLoginTransactCategorizeGoalReport() throws Exception {
        String email = "journey_" + System.nanoTime() + "@example.com";

        // Register
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", email, "password", "password123",
                                "fullName", "Journey User", "phoneNumber", "+1234567890"))))
                .andExpect(status().isCreated());

        // Duplicate register -> 409
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", email, "password", "password123",
                                "fullName", "Journey User", "phoneNumber", "+1234567890"))))
                .andExpect(status().isConflict());

        // Login and capture session cookie
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", email, "password", "password123"))))
                .andExpect(status().isOk())
                .andReturn();
        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);

        // Wrong password -> 401
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", email, "password", "wrong"))))
                .andExpect(status().isUnauthorized());

        // View default categories
        mockMvc.perform(get("/api/categories").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categories", org.hamcrest.Matchers.hasSize(7)));

        // Create custom category
        mockMvc.perform(post("/api/categories").session(session).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "SideBusinessIncome", "type", "INCOME"))))
                .andExpect(status().isCreated());

        // Duplicate custom category -> 409
        mockMvc.perform(post("/api/categories").session(session).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "SideBusinessIncome", "type", "INCOME"))))
                .andExpect(status().isConflict());

        // Create income transaction
        MvcResult txResult = mockMvc.perform(post("/api/transactions").session(session).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "amount", 50000.00, "date", LocalDate.now().toString(),
                                "category", "Salary", "description", "January Salary"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("INCOME"))
                .andReturn();
        int txId = objectMapper.readTree(txResult.getResponse().getContentAsString()).get("id").asInt();

        // Future-dated transaction -> 400
        mockMvc.perform(post("/api/transactions").session(session).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "amount", 10.00, "date", LocalDate.now().plusDays(5).toString(),
                                "category", "Food"))))
                .andExpect(status().isBadRequest());

        // Invalid category -> 400
        mockMvc.perform(post("/api/transactions").session(session).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "amount", 10.00, "date", LocalDate.now().toString(),
                                "category", "NotACategory"))))
                .andExpect(status().isBadRequest());

        // Update transaction
        mockMvc.perform(put("/api/transactions/" + txId).session(session).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("amount", 60000.00))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(60000.00));

        // Create a savings goal
        mockMvc.perform(post("/api/goals").session(session).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "goalName", "Emergency Fund", "targetAmount", 5000.00,
                                "targetDate", LocalDate.now().plusMonths(6).toString()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.currentProgress").value(60000.00));

        // Monthly report reflects the transaction
        int y = LocalDate.now().getYear();
        int m = LocalDate.now().getMonthValue();
        mockMvc.perform(get("/api/reports/monthly/" + y + "/" + m).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome.Salary").value(60000.00));

        // Delete transaction, then report should no longer include it
        mockMvc.perform(delete("/api/transactions/" + txId).session(session))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/reports/monthly/" + y + "/" + m).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome.Salary").doesNotExist());

        // Deleting a default category is forbidden
        mockMvc.perform(delete("/api/categories/Food").session(session))
                .andExpect(status().isForbidden());

        // Logout invalidates the session
        mockMvc.perform(post("/api/auth/logout").session(session))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/transactions").session(session))
                .andExpect(status().isUnauthorized());
    }
}
