package com.brijesh.notify.notifications;


import com.brijesh.notify.user.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private final CurrentUserService currentUserService;

    @GetMapping()
    public List<Notification> myNotifications(@AuthenticationPrincipal UserDetails userDetails) {
        Long userId = currentUserService.resolveId(userDetails);
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @PatchMapping("/{id}/read")
    public void markRead(@AuthenticationPrincipal UserDetails userDetails, @PathVariable Long id) {
        Long userId = currentUserService.resolveId(userDetails);
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Notification not found"));

        if (!notification.getUserId().equals(userId)) {
            throw new IllegalStateException("Not your notification");
        }
        if (notification.getReadAt() == null) {
            notification.setReadAt(java.time.LocalDateTime.now());
            notificationRepository.save(notification);
        }
    }
}
