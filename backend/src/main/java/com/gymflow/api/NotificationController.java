package com.gymflow.api;

import com.gymflow.api.response.ApiResponse;
import com.gymflow.service.NotificationQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationQueryService notificationQueryService;

    @GetMapping
    public ResponseEntity<?> listNotifications() {
        return ResponseEntity.ok(ApiResponse.success(
            "Notifications fetched successfully",
            notificationQueryService.listNotifications()
        ));
    }
}
