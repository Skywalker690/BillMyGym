package com.gymflow.api;

import com.gymflow.api.response.ApiResponse;
import com.gymflow.service.NotificationQueryService;
import com.gymflow.service.ReminderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reminders")
@RequiredArgsConstructor
public class ReminderController {

    private final ReminderService reminderService;
    private final NotificationQueryService notificationQueryService;

    @PostMapping("/run")
    public ResponseEntity<?> runReminders() {
        return ResponseEntity.ok(ApiResponse.success(
            "Reminder workflow executed successfully",
            reminderService.runRemindersForCurrentOrganization()
        ));
    }

    @GetMapping("/logs")
    public ResponseEntity<?> listReminderLogs() {
        return ResponseEntity.ok(ApiResponse.success(
            "Reminder logs fetched successfully",
            notificationQueryService.listReminderLogs()
        ));
    }
}
