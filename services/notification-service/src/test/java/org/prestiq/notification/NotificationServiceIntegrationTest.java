package org.prestiq.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.prestiq.notification.client.ApplicationServiceClient;
import org.prestiq.notification.client.dto.ApplicationClientDto;
import org.prestiq.notification.domain.entity.DeadlineReminder;
import org.prestiq.notification.domain.entity.Notification;
import org.prestiq.notification.domain.entity.NotificationPreferences;
import org.prestiq.notification.domain.entity.NotificationType;
import org.prestiq.notification.domain.entity.ReminderStatus;
import org.prestiq.notification.dto.request.ApplicationStatusNotificationRequest;
import org.prestiq.notification.dto.request.CancelDeadlineReminderRequest;
import org.prestiq.notification.dto.request.ScheduleDeadlineReminderRequest;
import org.prestiq.notification.dto.request.UpdatePreferencesRequest;
import org.prestiq.notification.repository.DeadlineReminderRepository;
import org.prestiq.notification.repository.NotificationPreferencesRepository;
import org.prestiq.notification.repository.NotificationRepository;
import org.prestiq.notification.service.DeadlineReminderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class NotificationServiceIntegrationTest {

    private static final String TEST_JWT_SECRET = "0123456789012345678901234567890123456789012345678901234567890123";
    private static final String INTERNAL_SERVICE_KEY = "prestiq-internal-test-key-2026";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationPreferencesRepository preferencesRepository;

    @Autowired
    private DeadlineReminderRepository reminderRepository;

    @Autowired
    private DeadlineReminderService reminderService;

    @MockBean
    private ApplicationServiceClient applicationServiceClient;

    @MockBean
    private Clock clock;

    private UUID studentAId;
    private UUID studentBId;
    private String studentAToken;
    private String studentBToken;
    private Instant fixedNow;

    @BeforeEach
    void setUp() {
        reminderRepository.deleteAll();
        notificationRepository.deleteAll();
        preferencesRepository.deleteAll();

        studentAId = UUID.randomUUID();
        studentBId = UUID.randomUUID();

        studentAToken = generateToken(studentAId, "student.a@example.com", List.of("ROLE_STUDENT"));
        studentBToken = generateToken(studentBId, "student.b@example.com", List.of("ROLE_STUDENT"));

        // Fixed baseline: 2026-10-01 12:00:00 UTC
        fixedNow = Instant.parse("2026-10-01T12:00:00Z");
        when(clock.instant()).thenReturn(fixedNow);
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);
    }

    private String generateToken(UUID userId, String email, List<String> roles) {
        SecretKey key = Keys.hmacShaKeyFor(TEST_JWT_SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("roles", roles)
                .issuer("prestiq-auth-service")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(key)
                .compact();
    }

    @Test
    @DisplayName("1. User can list own notifications with pagination and unread filter")
    void testGetNotificationsPaginationAndFilter() throws Exception {
        // Create 3 notifications for Student A, 1 for Student B
        Notification n1 = new Notification(UUID.randomUUID(), studentAId, NotificationType.DEADLINE_REMINDER,
                "Reminder 1", "Message 1", "APPLICATION", UUID.randomUUID().toString(), null);
        n1.setCreatedAt(fixedNow.minusSeconds(300));

        Notification n2 = new Notification(UUID.randomUUID(), studentAId, NotificationType.APPLICATION_STATUS_UPDATE,
                "Status 1", "Message 2", "APPLICATION", UUID.randomUUID().toString(), null);
        n2.setCreatedAt(fixedNow.minusSeconds(200));
        n2.markAsRead(fixedNow.minusSeconds(100));

        Notification n3 = new Notification(UUID.randomUUID(), studentAId, NotificationType.DEADLINE_REMINDER,
                "Reminder 2", "Message 3", "APPLICATION", UUID.randomUUID().toString(), null);
        n3.setCreatedAt(fixedNow.minusSeconds(100));

        Notification nOther = new Notification(UUID.randomUUID(), studentBId, NotificationType.SYSTEM,
                "Other User", "Message", null, null, null);

        notificationRepository.saveAll(List.of(n1, n2, n3, nOther));

        // Query all notifications for Student A (should return 3)
        mockMvc.perform(get("/api/v1/notifications")
                        .header("Authorization", "Bearer " + studentAToken)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(3)))
                .andExpect(jsonPath("$.content", hasSize(3)));

        // Query only unread notifications for Student A (should return 2: n3 and n1)
        mockMvc.perform(get("/api/v1/notifications")
                        .header("Authorization", "Bearer " + studentAToken)
                        .param("unreadOnly", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(2)))
                .andExpect(jsonPath("$.content", hasSize(2)));

        // Query filtered by type APPLICATION_STATUS_UPDATE
        mockMvc.perform(get("/api/v1/notifications")
                        .header("Authorization", "Bearer " + studentAToken)
                        .param("type", "APPLICATION_STATUS_UPDATE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].title", is("Status 1")));
    }

    @Test
    @DisplayName("2. Unread count query returns accurate count for user")
    void testGetUnreadCount() throws Exception {
        Notification n1 = new Notification(UUID.randomUUID(), studentAId, NotificationType.DEADLINE_REMINDER,
                "Title 1", "Msg 1", null, null, null);
        Notification n2 = new Notification(UUID.randomUUID(), studentAId, NotificationType.DEADLINE_REMINDER,
                "Title 2", "Msg 2", null, null, null);
        notificationRepository.saveAll(List.of(n1, n2));

        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .header("Authorization", "Bearer " + studentAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount", is(2)));
    }

    @Test
    @DisplayName("3. Mark single notification as read enforces ownership and updates read status")
    void testMarkAsRead() throws Exception {
        Notification n = new Notification(UUID.randomUUID(), studentAId, NotificationType.DEADLINE_REMINDER,
                "Title", "Msg", null, null, null);
        Notification saved = notificationRepository.save(n);

        // Student A marks as read -> 200 OK
        mockMvc.perform(patch("/api/v1/notifications/" + saved.getId() + "/read")
                        .header("Authorization", "Bearer " + studentAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isRead", is(true)))
                .andExpect(jsonPath("$.readAt").isNotEmpty());

        Notification reloaded = notificationRepository.findById(saved.getId()).orElseThrow();
        assertTrue(reloaded.isRead());

        // Student B attempts to mark Student A's notification as read -> 404 Not Found
        mockMvc.perform(patch("/api/v1/notifications/" + saved.getId() + "/read")
                        .header("Authorization", "Bearer " + studentBToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("4. Mark all as read updates all unread notifications for authenticated user")
    void testMarkAllAsRead() throws Exception {
        Notification n1 = new Notification(UUID.randomUUID(), studentAId, NotificationType.DEADLINE_REMINDER,
                "Title 1", "Msg 1", null, null, null);
        Notification n2 = new Notification(UUID.randomUUID(), studentAId, NotificationType.DEADLINE_REMINDER,
                "Title 2", "Msg 2", null, null, null);
        Notification nB = new Notification(UUID.randomUUID(), studentBId, NotificationType.DEADLINE_REMINDER,
                "Title B", "Msg B", null, null, null);
        notificationRepository.saveAll(List.of(n1, n2, nB));

        mockMvc.perform(post("/api/v1/notifications/mark-all-read")
                        .header("Authorization", "Bearer " + studentAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.markedCount", is(2)));

        assertEquals(0, notificationRepository.countByUserIdAndIsReadFalse(studentAId));
        assertEquals(1, notificationRepository.countByUserIdAndIsReadFalse(studentBId));
    }

    @Test
    @DisplayName("5. Delete notification removes entity and denies cross-user deletion")
    void testDeleteNotification() throws Exception {
        Notification n = new Notification(UUID.randomUUID(), studentAId, NotificationType.DEADLINE_REMINDER,
                "Title", "Msg", null, null, null);
        Notification saved = notificationRepository.save(n);

        // Student B tries to delete Student A's notification -> 404
        mockMvc.perform(delete("/api/v1/notifications/" + saved.getId())
                        .header("Authorization", "Bearer " + studentBToken))
                .andExpect(status().isNotFound());

        // Student A deletes own notification -> 204 No Content
        mockMvc.perform(delete("/api/v1/notifications/" + saved.getId())
                        .header("Authorization", "Bearer " + studentAToken))
                .andExpect(status().isNoContent());

        assertFalse(notificationRepository.existsById(saved.getId()));
    }

    @Test
    @DisplayName("6. User notification preferences creates defaults on first access")
    void testGetDefaultPreferences() throws Exception {
        mockMvc.perform(get("/api/v1/notifications/preferences")
                        .header("Authorization", "Bearer " + studentAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId", is(studentAId.toString())))
                .andExpect(jsonPath("$.inAppEnabled", is(true)))
                .andExpect(jsonPath("$.deadlineRemindersEnabled", is(true)))
                .andExpect(jsonPath("$.applicationStatusEnabled", is(true)))
                .andExpect(jsonPath("$.emailEnabled", is(false)));

        assertTrue(preferencesRepository.existsByUserId(studentAId));
    }

    @Test
    @DisplayName("7. Update user notification preferences successfully")
    void testUpdatePreferences() throws Exception {
        UpdatePreferencesRequest updateReq = new UpdatePreferencesRequest(
                true,   // emailEnabled
                true,   // inAppEnabled
                false,  // deadlineRemindersEnabled
                true,   // applicationStatusEnabled
                false   // marketingAnnouncementsEnabled
        );

        mockMvc.perform(put("/api/v1/notifications/preferences")
                        .header("Authorization", "Bearer " + studentAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailEnabled", is(true)))
                .andExpect(jsonPath("$.deadlineRemindersEnabled", is(false)))
                .andExpect(jsonPath("$.marketingAnnouncementsEnabled", is(false)));

        NotificationPreferences entity = preferencesRepository.findByUserId(studentAId).orElseThrow();
        assertFalse(entity.isDeadlineRemindersEnabled());
        assertTrue(entity.isEmailEnabled());
    }

    @Test
    @DisplayName("8. Schedule deadline reminders calculates 30d, 7d, 1d windows and rejects duplicates")
    void testScheduleDeadlineRemindersAndDeduplication() throws Exception {
        UUID appId = UUID.randomUUID();
        // Deadline is 45 days in the future from 2026-10-01 (approx 2026-11-15)
        LocalDate deadline = LocalDate.of(2026, 11, 15);
        ScheduleDeadlineReminderRequest req = new ScheduleDeadlineReminderRequest(appId, studentAId, deadline, "STEM Leaders Scholarship");

        // 1. Initial schedule call -> 201 Created (should schedule 30, 7, 1 days before)
        mockMvc.perform(post("/internal/v1/notifications/deadlines")
                        .header("X-Internal-Service-Key", INTERNAL_SERVICE_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasSize(3)));

        List<DeadlineReminder> reminders = reminderRepository.findByApplicationId(appId);
        assertEquals(3, reminders.size());

        // 2. Duplicate schedule call with same deadline -> Idempotent, no duplicates created
        mockMvc.perform(post("/internal/v1/notifications/deadlines")
                        .header("X-Internal-Service-Key", INTERNAL_SERVICE_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasSize(3)));

        List<DeadlineReminder> remindersAfterDuplicate = reminderRepository.findByApplicationId(appId);
        assertEquals(3, remindersAfterDuplicate.size());
    }

    @Test
    @DisplayName("9. Schedule deadline reminders skips past windows when deadline is imminent")
    void testScheduleDeadlineRemindersImminentDeadline() throws Exception {
        UUID appId = UUID.randomUUID();
        // Deadline is 5 days from now (2026-10-06).
        // 30 days before is 2026-09-06 (past).
        // 7 days before is 2026-09-29 (past).
        // 1 day before is 2026-10-05 (future).
        LocalDate deadline = LocalDate.of(2026, 10, 6);
        ScheduleDeadlineReminderRequest req = new ScheduleDeadlineReminderRequest(appId, studentAId, deadline, "Quick Grant");

        mockMvc.perform(post("/internal/v1/notifications/deadlines")
                        .header("X-Internal-Service-Key", INTERNAL_SERVICE_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].reminderWindow", is("1_DAY_BEFORE")));
    }

    @Test
    @DisplayName("10. Cancel pending deadline reminders marks status as CANCELLED")
    void testCancelDeadlineReminders() throws Exception {
        UUID appId = UUID.randomUUID();
        LocalDate deadline = LocalDate.of(2026, 12, 1);
        ScheduleDeadlineReminderRequest req = new ScheduleDeadlineReminderRequest(appId, studentAId, deadline, "Grant");

        reminderService.scheduleReminders(req);
        assertEquals(3, reminderRepository.findByApplicationIdAndStatus(appId, ReminderStatus.PENDING).size());

        CancelDeadlineReminderRequest cancelReq = new CancelDeadlineReminderRequest(appId);
        mockMvc.perform(post("/internal/v1/notifications/deadlines/cancel")
                        .header("X-Internal-Service-Key", INTERNAL_SERVICE_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cancelReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cancelledCount", is(3)));

        assertEquals(0, reminderRepository.findByApplicationIdAndStatus(appId, ReminderStatus.PENDING).size());
    }

    @Test
    @DisplayName("11. Deadline countdown engine dispatches in-app notification when clock advances")
    void testProcessDueRemindersWithControllableClock() throws Exception {
        // Initial time at 08:00 UTC so that 09:00 UTC reminder is in the future
        when(clock.instant()).thenReturn(Instant.parse("2026-10-01T08:00:00Z"));

        UUID appId = UUID.randomUUID();
        LocalDate deadline = LocalDate.of(2026, 10, 8); // 7 days from fixedNow (2026-10-01)
        ScheduleDeadlineReminderRequest scheduleReq = new ScheduleDeadlineReminderRequest(
                appId, studentAId, deadline, "Global STEM Award"
        );
        reminderService.scheduleReminders(scheduleReq);

        // Mock ApplicationServiceClient to return title
        ApplicationClientDto appDto = new ApplicationClientDto(
                appId, studentAId, 101L, "Global STEM Award", "STEM Org", "IN_PROGRESS", deadline
        );
        when(applicationServiceClient.getApplicationById(appId)).thenReturn(Optional.of(appDto));

        // Advance clock to reminder day (2026-10-01 09:30:00 UTC)
        Instant reminderTime = Instant.parse("2026-10-01T10:00:00Z");
        when(clock.instant()).thenReturn(reminderTime);

        // Trigger processing engine
        mockMvc.perform(post("/internal/v1/notifications/process-reminders")
                        .header("X-Internal-Service-Key", INTERNAL_SERVICE_KEY)
                        .param("batchSize", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processedCount", is(1)));

        // Verify in-app notification created
        List<Notification> notifs = notificationRepository.findAll();
        assertEquals(1, notifs.size());
        assertEquals("Deadline Reminder: Global STEM Award", notifs.get(0).getTitle());
        assertEquals(NotificationType.DEADLINE_REMINDER, notifs.get(0).getType());
        assertEquals(studentAId, notifs.get(0).getUserId());

        // Verify reminder status transitioned to DISPATCHED
        DeadlineReminder reminder = reminderRepository.findByApplicationId(appId).stream()
                .filter(r -> "7_DAYS_BEFORE".equals(r.getReminderWindow()))
                .findFirst()
                .orElseThrow();
        assertEquals(ReminderStatus.DISPATCHED, reminder.getStatus());
        assertNotNull(reminder.getDispatchedAt());
    }

    @Test
    @DisplayName("12. Application status update notification creates in-app notification with idempotency")
    void testApplicationStatusNotificationIdempotency() throws Exception {
        UUID appId = UUID.randomUUID();
        ApplicationStatusNotificationRequest statusReq = new ApplicationStatusNotificationRequest(
                appId,
                studentAId,
                "National Merit Scholarship",
                "IN_PROGRESS",
                "SUBMITTED",
                "STATUS:" + appId + ":SUBMITTED"
        );

        // 1. First invocation -> 201 Created
        mockMvc.perform(post("/internal/v1/notifications/application-status")
                        .header("X-Internal-Service-Key", INTERNAL_SERVICE_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title", is("Application Status: National Merit Scholarship")))
                .andExpect(jsonPath("$.type", is("APPLICATION_STATUS_UPDATE")));

        assertEquals(1, notificationRepository.count());

        // 2. Second invocation with same idempotencyKey -> 200 OK (skipped, no duplicate)
        mockMvc.perform(post("/internal/v1/notifications/application-status")
                        .header("X-Internal-Service-Key", INTERNAL_SERVICE_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("SKIPPED_OR_DUPLICATE")));

        assertEquals(1, notificationRepository.count());
    }

    @Test
    @DisplayName("13. Notifications suppressed when user preferences disable category")
    void testNotificationsSuppressedByPreferences() throws Exception {
        // Disable application status notifications in preferences
        NotificationPreferences prefs = new NotificationPreferences(studentAId);
        prefs.setApplicationStatusEnabled(false);
        preferencesRepository.save(prefs);

        UUID appId = UUID.randomUUID();
        ApplicationStatusNotificationRequest statusReq = new ApplicationStatusNotificationRequest(
                appId, studentAId, "Merit", "PLANNING", "IN_PROGRESS", null
        );

        mockMvc.perform(post("/internal/v1/notifications/application-status")
                        .header("X-Internal-Service-Key", INTERNAL_SERVICE_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("SKIPPED_OR_DUPLICATE")));

        assertEquals(0, notificationRepository.count());
    }

    @Test
    @DisplayName("14. Validation errors return 400 Bad Request with field details")
    void testValidationErrors() throws Exception {
        // Missing required fields
        ScheduleDeadlineReminderRequest invalidReq = new ScheduleDeadlineReminderRequest(null, null, null, null);

        mockMvc.perform(post("/internal/v1/notifications/deadlines")
                        .header("X-Internal-Service-Key", INTERNAL_SERVICE_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Validation Failed")))
                .andExpect(jsonPath("$.invalidFields.applicationId").isNotEmpty())
                .andExpect(jsonPath("$.invalidFields.deadline").isNotEmpty());
    }

    @Test
    @DisplayName("15. Unauthenticated request to /api/v1/notifications is rejected with 401")
    void testUnauthenticatedAccess() throws Exception {
        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("16. Internal endpoints reject unauthorized callers missing internal key")
    void testInternalEndpointsProtected() throws Exception {
        ScheduleDeadlineReminderRequest req = new ScheduleDeadlineReminderRequest(
                UUID.randomUUID(), studentAId, LocalDate.of(2026, 12, 1), "Test"
        );

        // Missing key -> 401 Unauthorized
        mockMvc.perform(post("/internal/v1/notifications/deadlines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());

        // Invalid key -> 401 Unauthorized
        mockMvc.perform(post("/internal/v1/notifications/deadlines")
                        .header("X-Internal-Service-Key", "wrong-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }
}
