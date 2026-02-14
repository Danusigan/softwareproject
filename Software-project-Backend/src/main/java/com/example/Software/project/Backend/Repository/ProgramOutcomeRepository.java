package com.example.Software.project.Backend.Repository;

import com.example.Software.project.Backend.Model.ProgramOutcome;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProgramOutcomeRepository extends JpaRepository<ProgramOutcome, Long> {
}