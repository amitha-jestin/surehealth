package com.sociolab.surehealth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sociolab.surehealth.security.JwtAuthenticationFilter;
import com.sociolab.surehealth.service.AdminService;
import com.sociolab.surehealth.dto.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

@WebMvcTest(controllers = AdminController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminService adminService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setupFilter() throws Exception {
        // Ensure the mocked filter delegates to the filter chain so requests reach the controller
        doAnswer(invocation -> {
            ServletRequest req = invocation.getArgument(0);
            ServletResponse res = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(req, res);
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(ServletRequest.class), any(ServletResponse.class), any(FilterChain.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void approveDoctor_callsServiceAndReturnsOk() throws Exception {

        String expectedJson = objectMapper.writeValueAsString(ApiResponse.success("Doctor approved successfully"));

        Long doctorId = 1L;
        doNothing().when(adminService).approveDoctor(doctorId);

        mockMvc.perform(patch("/api/v1/admin/doctors/{doctorId}/approve", doctorId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(content().json("""
                        {
                            "status": "success",
                            "message": "Doctor approved successfully"
                        }
                        """));

        verify(adminService).approveDoctor(doctorId);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void blockUser_callsServiceAndReturnsOk() throws Exception {
        Long userId = 2L;
        doNothing().when(adminService).blockUser(userId);

        mockMvc.perform(patch("/api/v1/admin/user/{userId}/block", userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                            "status": "success",
                            "message": "User blocked successfully"
                        }
                        """));

        verify(adminService).blockUser(userId);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void unblockUser_callsServiceAndReturnsOk() throws Exception {
        Long userId = 3L;
        doNothing().when(adminService).unblockUser(userId);

        mockMvc.perform(patch("/api/v1/admin/user/{userId}/unblock", userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                            "status": "success",
                            "message": "User unblocked successfully"
                        }
                        """));

        verify(adminService).unblockUser(userId);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllPatients_returnsOk() throws Exception {
        when(adminService.getAllPatients(0, 20))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(Collections.emptyList()));

        mockMvc.perform(get("/api/v1/admin/patients")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk());

        verify(adminService).getAllPatients(0, 20);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllDoctors_returnsOk() throws Exception {
        when(adminService.getAllDoctors(0, 20))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(Collections.emptyList()));

        mockMvc.perform(get("/api/v1/admin/doctors")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk());

        verify(adminService).getAllDoctors(0, 20);
    }
}
