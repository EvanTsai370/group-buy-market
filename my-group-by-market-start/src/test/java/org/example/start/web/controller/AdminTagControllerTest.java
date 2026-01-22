package org.example.start.web.controller;

import org.example.interfaces.web.dto.admin.CreateTagRequest;
import org.example.interfaces.web.dto.admin.UpdateTagRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class AdminTagControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(username = "admin", roles = { "ADMIN" })
    public void testListTags_shouldReturnPageResult() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/admin/tags")
                .param("page", "1")
                .param("size", "10")
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data.list").isArray());
    }

    @Test
    @WithMockUser(username = "admin", roles = { "ADMIN" })
    public void testCreateTag_shouldSucceed() throws Exception {
        CreateTagRequest request = new CreateTagRequest();
        request.setTagName("Test Tag");
        request.setTagRule("{\"age\": {\"$gte\": 18}}");

        String content = com.alibaba.fastjson2.JSON.toJSONString(request);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/admin/tags")
                .content(content)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"));
    }
}
