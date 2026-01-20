package com.example.Software.project.Backend.Repository;

import com.example.Software.project.Backend.Model.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, String> {
    
    // Since Assignment doesn't have a direct "LosPos" field (LosPos has the Assignment),
    // we need to find assignments by looking at the LosPos table.
    // However, usually the relationship is bidirectional or we query LosPos.
    
    // Let's check the LosPos model again.
    // LosPos has: @ManyToOne Assignment assignment;
    // This means LosPos OWNS the relationship. One LosPos has One Assignment?
    // Or One LosPos has Many Assignments?
    
    // In your LosPos.java:
    // @ManyToOne @JoinColumn(name = "assignment_id") private Assignment assignment;
    
    // This implies MANY LosPos can share ONE Assignment.
    // BUT usually, it's the other way around: One LosPos has MANY Assignments.
    // If One LosPos has Many Assignments, then Assignment should have "private LosPos losPos".
    
    // If the current design is: LosPos -> Assignment (Many-to-One), then one LosPos can only have ONE assignment.
    // If that is the case, finding "Assignments for a LosPos" just means getting THE assignment for that LosPos.
    
    // Let's assume you want One LosPos to have MANY Assignments.
    // If so, the Foreign Key should be in the Assignment table (pointing to LosPos).
    
    // CURRENT STATE CHECK:
    // LosPos.java: private Assignment assignment; (FK is in LosPos table)
    // This means a LosPos can only have ONE assignment.
    
    // If you want multiple assignments per LosPos, we need to change the relationship.
    // But based on your previous requests ("add assignment to LosPos"), it seems you might want 1-to-1 or Many-to-1.
    
    // If 1 LosPos has 1 Assignment:
    // We can find the assignment by finding the LosPos first, then getting .getAssignment().
    
    // I will implement the Service method to find the assignment via LosPos repository.
}