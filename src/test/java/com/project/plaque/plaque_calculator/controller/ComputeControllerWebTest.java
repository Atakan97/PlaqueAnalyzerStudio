package com.project.plaque.plaque_calculator.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.plaque.plaque_calculator.service.DecomposeService;
import com.project.plaque.plaque_calculator.service.FDService;
import com.project.plaque.plaque_calculator.service.RicService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ComputeController.class)
class ComputeControllerWebTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private FDService fdService;

    @MockitoBean
    private RicService ricService;

    @MockitoBean
    private DecomposeService decomposeService;

    @Test
    void streamInit_returnsToken_andStoresPayloadInSession() throws Exception {
        // This test checks that stream-init creates a token and saves request data in session
        MockHttpSession session = new MockHttpSession();

        MvcResult result = mockMvc.perform(post("/compute/stream-init")
                        .param("manualData", "a,b;c,d")
                        .param("fds", "1->2")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andReturn();

        Map<String, String> response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<Map<String, String>>() {}
        );
        String token = response.get("token");

        assertThat(token).isNotBlank();

        Object sessionAttr = session.getAttribute("computeRequests");
        assertThat(sessionAttr).isInstanceOf(Map.class);

        Map<?, ?> requests = (Map<?, ?>) sessionAttr;
        assertThat(requests.containsKey(token)).isTrue();

        Object payloadObj = requests.get(token);
        assertThat(payloadObj).isInstanceOf(Map.class);

        Map<?, ?> payload = (Map<?, ?>) payloadObj;
        assertThat(payload.get("originalManualData")).isEqualTo("a,b;c,d");
        assertThat(payload.get("fds")).isEqualTo("1->2");
        assertThat(payload.get("mode")).isEqualTo("enabled");
    }
}



