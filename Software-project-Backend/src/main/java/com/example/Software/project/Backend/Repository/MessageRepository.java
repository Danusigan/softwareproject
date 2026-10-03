package com.example.Software.project.Backend.Repository;

import com.example.Software.project.Backend.Model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByRecipientOrderBySentAtDesc(String recipient);
    long countByRecipientAndReadFlagFalse(String recipient);
}
