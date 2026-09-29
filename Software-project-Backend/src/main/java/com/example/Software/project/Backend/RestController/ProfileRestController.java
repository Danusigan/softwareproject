package com.example.Software.project.Backend.RestController;

import com.example.Software.project.Backend.Model.Message;
import com.example.Software.project.Backend.Model.UserAccess;
import com.example.Software.project.Backend.Repository.MessageRepository;
import com.example.Software.project.Backend.Repository.UserAccessRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api")
public class ProfileRestController {

    private final UserAccessRepository accessRepo;
    private final MessageRepository messageRepo;

    public ProfileRestController(UserAccessRepository a, MessageRepository m) {
        this.accessRepo = a;
        this.messageRepo = m;
    }

    @GetMapping("/profile/login-activity")
    public ResponseEntity<?> loginActivity(@RequestParam(required = false) String username, Authentication auth) {
        String target = auth.getName();
        boolean isAdmin = auth.getAuthorities().stream()
            .anyMatch(g -> g.getAuthority().toUpperCase().contains("ADMIN"));
        if (isAdmin && username != null && !username.isBlank()) target = username;

        Map<String, Object> out = new HashMap<>();
        UserAccess ua = accessRepo.findById(target).orElse(null);
        out.put("firstAccess", ua != null ? ua.getFirstAccess() : null);
        out.put("lastAccess", ua != null ? ua.getLastAccess() : null);
        return ResponseEntity.ok(out);
    }

    @PostMapping("/messages")
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
    public ResponseEntity<List<Message>> inbox(Authentication auth) {
        return ResponseEntity.ok(messageRepo.findByRecipientOrderBySentAtDesc(auth.getName()));
    }

    // ---- NEW: notifications ----

    @GetMapping("/messages/unread-count")
    public ResponseEntity<?> unreadCount(Authentication auth) {
        return ResponseEntity.ok(Map.of("count", messageRepo.countByRecipientAndReadFlagFalse(auth.getName())));
    }

    @PutMapping("/messages/{id}/read")
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
    public ResponseEntity<?> markAllRead(Authentication auth) {
        List<Message> list = messageRepo.findByRecipientOrderBySentAtDesc(auth.getName());
        list.forEach(m -> m.setReadFlag(true));
        messageRepo.saveAll(list);
        return ResponseEntity.ok(Map.of("message", "ok"));
    }
}
