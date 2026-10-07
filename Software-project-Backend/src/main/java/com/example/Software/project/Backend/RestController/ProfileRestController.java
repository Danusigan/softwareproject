package com.example.Software.project.Backend.RestController;

import com.example.Software.project.Backend.Model.Message;
import com.example.Software.project.Backend.Model.UserAccess;
import com.example.Software.project.Backend.Repository.MessageRepository;
import com.example.Software.project.Backend.Repository.UserAccessRepository;
import com.example.Software.project.Backend.Service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api")
public class ProfileRestController {

    private final UserAccessRepository accessRepo;
    private final MessageRepository messageRepo;
    private final UserService userService;

    public ProfileRestController(UserAccessRepository a, MessageRepository m, UserService userService) {
        this.accessRepo = a;
        this.messageRepo = m;
        this.userService = userService;
    }

    @PostMapping("/profile/change-password")
    @org.springframework.security.access.prepost.PreAuthorize("@accessPolicy.allow('ProfileRestController.changePassword', {'body': #p0, 'auth': #p1})")
    public ResponseEntity<?> changePassword(@RequestBody Map<String, String> body, Authentication auth) {
        try {
            userService.changePassword(auth.getName(), body.get("currentPassword"), body.get("newPassword"));
            return ResponseEntity.ok(Map.of("message", "Password changed.", "status", "SUCCESS"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage(), "status", "ERROR"));
        }
    }

    @GetMapping("/profile/login-activity")
    @org.springframework.security.access.prepost.PreAuthorize("@accessPolicy.allow('ProfileRestController.loginActivity', {'username': #p0, 'auth': #p1})")
    public ResponseEntity<?> loginActivity(@RequestParam(required = false) String username, Authentication auth) {
        String target = auth.getName();
        boolean isAdmin = auth.getAuthorities().stream()
            .anyMatch(g -> g.getAuthority().toUpperCase().contains("ADMIN"));
        if (isAdmin && username != null && !username.isBlank()) target = username;

        Map<String, Object> out = new HashMap<>();
        UserAccess ua = accessRepo.findById(target).orElse(null);
        out.put("firstAccess", ua != null ? withOffset(ua.getFirstAccess()) : null);
        out.put("lastAccess", ua != null ? withOffset(ua.getLastAccess()) : null);
        return ResponseEntity.ok(out);
    }

    // Timestamps are stored as server-local LocalDateTime; send an explicit offset so the
    // browser doesn't misread them in its own timezone.
    private static String withOffset(java.time.LocalDateTime t) {
        return t == null ? null : t.atZone(java.time.ZoneId.systemDefault()).toOffsetDateTime().toString();
    }

    @PostMapping("/messages")
    @org.springframework.security.access.prepost.PreAuthorize("@accessPolicy.allow('ProfileRestController.send', {'body': #p0, 'auth': #p1})")
    public ResponseEntity<?> send(@RequestBody Map<String, String> body, Authentication auth) {
        String recipient = body.get("recipient");
        String content = body.get("content");
        if (recipient == null || recipient.isBlank() || content == null || content.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Recipient and message are required."));
        }
        Message m = new Message();
        m.setSender(auth.getName());
        m.setRecipient(recipient.trim());
        m.setContent(content.trim());
        messageRepo.save(m);
        return ResponseEntity.ok(Map.of("message", "Message sent."));
    }

    @GetMapping("/messages/inbox")
    @org.springframework.security.access.prepost.PreAuthorize("@accessPolicy.allow('ProfileRestController.inbox', {'auth': #p0})")
    public ResponseEntity<List<Message>> inbox(Authentication auth) {
        return ResponseEntity.ok(messageRepo.findByRecipientOrderBySentAtDesc(auth.getName()));
    }

    @GetMapping("/messages/unread-count")
    @org.springframework.security.access.prepost.PreAuthorize("@accessPolicy.allow('ProfileRestController.unreadCount', {'auth': #p0})")
    public ResponseEntity<?> unreadCount(Authentication auth) {
        return ResponseEntity.ok(Map.of("count", messageRepo.countByRecipientAndReadFlagFalse(auth.getName())));
    }

    @PutMapping("/messages/{id}/read")
    @org.springframework.security.access.prepost.PreAuthorize("@accessPolicy.allow('ProfileRestController.markRead', {'id': #p0, 'auth': #p1})")
    public ResponseEntity<?> markRead(@PathVariable Long id, Authentication auth) {
        return messageRepo.findById(id)
            .filter(m -> m.getRecipient().equals(auth.getName()))
            .map(m -> {
                m.setReadFlag(true);
                messageRepo.save(m);
                return ResponseEntity.ok(Map.of("message", "ok"));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/messages/read-all")
    @org.springframework.security.access.prepost.PreAuthorize("@accessPolicy.allow('ProfileRestController.markAllRead', {'auth': #p0})")
    public ResponseEntity<?> markAllRead(Authentication auth) {
        List<Message> list = messageRepo.findByRecipientOrderBySentAtDesc(auth.getName());
        list.forEach(m -> m.setReadFlag(true));
        messageRepo.saveAll(list);
        return ResponseEntity.ok(Map.of("message", "ok"));
    }
}
