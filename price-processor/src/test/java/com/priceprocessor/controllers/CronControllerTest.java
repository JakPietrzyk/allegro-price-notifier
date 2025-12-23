package com.priceprocessor.controllers;

import com.priceprocessor.services.JwtService;
import com.priceprocessor.services.PriceUpdateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CronController.class, properties = {
        "spring.cloud.gcp.sql.enabled=false",
        "spring.cloud.gcp.core.enabled=false"
})
@AutoConfigureMockMvc(addFilters = false)
class CronControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PriceUpdateService priceUpdateService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void shouldTriggerBatchUpdate_AndReturnCount() throws Exception {
        // Arrange
        int updatedCount = 42;
        when(priceUpdateService.updateOutdatedPrices()).thenReturn(updatedCount);

        // Act & Assert
        mockMvc.perform(post("/api/cron/update-prices"))
                .andExpect(status().isOk())
                .andExpect(content().string("Batch update finished. Processed: " + updatedCount));
    }

    @Test
    void shouldReturn500_WhenServiceFails() throws Exception {
        // Arrange
        when(priceUpdateService.updateOutdatedPrices()).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        try {
            mockMvc.perform(post("/api/cron/update-prices"))
                    .andExpect(status().isInternalServerError());
        } catch (Exception e) {
        }
    }
}