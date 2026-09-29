package com.example.Software.project.Backend.Model;

import jakarta.persistence.*;   // if red underlines, change to javax.persistence.*
import java.time.LocalDateTime;

@Entity
@Table(name = "messages")
public class Message {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String sender;
    private String recipient;
    @Column(length = 2000)
    private String content;
    private LocalDateTime sentAt = LocalDateTime.now();
    private boolean readFlag = false;

    public Long getId() { return id; }
    public String getSender() { return sender; }
    public void setSender(String s) { this.sender = s; }
    public String getRecipient() { return recipient; }
    public void setRecipient(String r) { this.recipient = r; }
    public String getContent() { return content; }
    public void setContent(String c) { this.content = c; }
    public LocalDateTime getSentAt() { return sentAt; }
    public boolean isReadFlag() { return readFlag; }
    public void setReadFlag(boolean r) { this.readFlag = r; }
}
