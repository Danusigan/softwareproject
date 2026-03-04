package com.example.Software.project.Backend.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.List;

@Component
public class LegacySchemaFixService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void applyLegacyFixes() {
        makeAssessmentIdNullableForLegacyStudentMark();
        reconcileLoPoProgramOutcomeForeignKey();
    }

    private void makeAssessmentIdNullableForLegacyStudentMark() {
        try {
            List<String> fkNames = jdbcTemplate.queryForList(
                    "SELECT CONSTRAINT_NAME FROM information_schema.KEY_COLUMN_USAGE " +
                            "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'StudentMark' " +
                            "AND COLUMN_NAME = 'assessment_id' AND REFERENCED_TABLE_NAME IS NOT NULL",
                    String.class
            );

            for (String fkName : fkNames) {
                try {
                    jdbcTemplate.execute("ALTER TABLE StudentMark DROP FOREIGN KEY " + fkName);
                } catch (Exception ignored) {
                }
            }

            jdbcTemplate.execute("ALTER TABLE StudentMark MODIFY COLUMN assessment_id VARCHAR(255) NULL");
        } catch (Exception ignored) {
        }
    }

    private void reconcileLoPoProgramOutcomeForeignKey() {
        try {
            List<String> fkNames = jdbcTemplate.queryForList(
                    "SELECT CONSTRAINT_NAME FROM information_schema.KEY_COLUMN_USAGE " +
                            "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'lo_po_mappings' " +
                            "AND COLUMN_NAME = 'program_outcome_id' AND REFERENCED_TABLE_NAME IS NOT NULL",
                    String.class
            );

            for (String fkName : fkNames) {
                try {
                    jdbcTemplate.execute("ALTER TABLE lo_po_mappings DROP FOREIGN KEY " + fkName);
                } catch (Exception ignored) {
                }
            }

            jdbcTemplate.execute("ALTER TABLE lo_po_mappings MODIFY COLUMN program_outcome_id VARCHAR(255) NOT NULL");
        } catch (Exception ignored) {
        }
    }
}
