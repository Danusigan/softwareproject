package com.example.Software.project.Backend.Service;

import com.example.Software.project.Backend.Model.Message;
import com.example.Software.project.Backend.Repository.MessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;

@Service
public class NotificationService {

    public static final String SYSTEM_SENDER = "System";

    @Autowired
    private MessageRepository messageRepository;

    public void notifyUser(String recipient, String content) {
        if (recipient == null || recipient.isBlank() || content == null || content.isBlank()) return;
        try {
            Message m = new Message();
            m.setSender(SYSTEM_SENDER);
            m.setRecipient(recipient.trim());
            m.setContent(content.length() > 2000 ? content.substring(0, 2000) : content);
            messageRepository.save(m);
        } catch (Exception e) {
            System.err.println("Notification failed for " + recipient + ": " + e.getMessage());
        }
    }

    public void notifyUsers(Collection<String> recipients, String content) {
        if (recipients == null) return;
        recipients.stream().filter(r -> r != null && !r.isBlank()).distinct()
            .forEach(r -> notifyUser(r, content));
    }
}
