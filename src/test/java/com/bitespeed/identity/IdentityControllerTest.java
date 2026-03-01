package com.bitespeed.identity;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class IdentityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @Test
    void identifyEndpointWorks() throws Exception {
        IdentifyRequest req = new IdentifyRequest();
        req.setEmail("test@api.com");
        req.setPhoneNumber("999");

        mockMvc.perform(
                post("/identify")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(req))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.contact.primaryContactId").exists())
            .andExpect(jsonPath("$.contact.emails[0]").value("test@api.com"));
    }
}
