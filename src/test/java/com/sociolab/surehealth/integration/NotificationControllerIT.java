package com.sociolab.surehealth.integration;

import com.sociolab.surehealth.enums.CaseStatus;
import com.sociolab.surehealth.enums.NotificationEventType;
import com.sociolab.surehealth.enums.Role;
import com.sociolab.surehealth.model.Notification;
import com.sociolab.surehealth.model.User;
import com.sociolab.surehealth.repository.NotificationRepository;
import com.sociolab.surehealth.repository.UserRepository;
import com.sociolab.surehealth.security.UserPrincipal;
import com.sociolab.surehealth.testdata.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class NotificationControllerIT extends TestContainersConfig {

    private static final String BASE = "/api/v1/notifications";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestDataFactory testDataFactory;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    private User user;

    @BeforeEach
    void setup() {
        user = testDataFactory.createPatient("Pati3nt!pass");
    }

    private UserPrincipal mockUser() {
        return new UserPrincipal(user.getId(), user.getEmail(), Role.PATIENT, "");
    }

    private RequestPostProcessor userAuth() {
        var principal = mockUser();
        var auth = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + principal.role().name()))
        );
        return SecurityMockMvcRequestPostProcessors.authentication(auth);
    }

    private RequestPostProcessor unknownUserAuth() {
        var principal = new UserPrincipal(999999L, "missing@example.com", Role.PATIENT, "");
        var auth = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + principal.role().name()))
        );
        return SecurityMockMvcRequestPostProcessors.authentication(auth);
    }

    @Nested
    @DisplayName("UnreadNotifications")
    class UnreadNotificationsTests {

        @Test
        @DisplayName("shouldGetUnreadNotificationsSuccessfully")
        void shouldGetUnreadNotificationsSuccessfully() throws Exception {
            Notification notification = Notification.builder()
                    .user(user)
                    .message("Hello")
                    .newStatus(CaseStatus.ASSIGNED)
                    .readStatus(false)
                    .createdAt(LocalDateTime.now())
                    .build();
            notificationRepository.save(notification);

            mockMvc.perform(get(BASE ).with(userAuth())
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].message").value("Hello"));
        }

        @Test
        @DisplayName("shouldReturnUnauthorizedWhenNoAuth")
        void shouldReturnUnauthorizedWhenNoAuth() throws Exception {
            mockMvc.perform(get(BASE )
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("shouldReturnBadRequestWhenInvalidPage")
        void shouldReturnBadRequestWhenInvalidPage() throws Exception {
            mockMvc.perform(get(BASE).with(userAuth())
                            .param("page", "-1")
                            .param("size", "10"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("shouldReturnNotFoundWhenUserMissing")
        void shouldReturnNotFoundWhenUserMissing() throws Exception {
            mockMvc.perform(get(BASE ).with(unknownUserAuth())
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("ReadNotifications")
    class ReadNotificationsTests {

        @Test
        @DisplayName("shouldGetReadNotificationsSuccessfully")
        void shouldGetReadNotificationsSuccessfully() throws Exception {
            Notification notification = Notification.builder()
                    .user(user)
                    .message("Hello")
                    .newStatus(CaseStatus.ASSIGNED)
                    .readStatus(true)
                    .createdAt(LocalDateTime.now())
                    .build();
            notificationRepository.save(notification);

            mockMvc.perform(get(BASE ).with(userAuth())
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].message").value("Hello"));
        }

        @Test
        @DisplayName("shouldReturnUnauthorizedWhenNoAuth")
        void shouldReturnUnauthorizedWhenNoAuth() throws Exception {
            mockMvc.perform(get(BASE)
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("shouldReturnBadRequestWhenInvalidSize")
        void shouldReturnBadRequestWhenInvalidSize() throws Exception {
            mockMvc.perform(get(BASE).with(userAuth())
                            .param("page", "0")
                            .param("size", "0"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("shouldReturnNotFoundWhenUserMissing")
        void shouldReturnNotFoundWhenUserMissing() throws Exception {
            mockMvc.perform(get(BASE).with(unknownUserAuth())
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isNotFound());
        }
    }
}
