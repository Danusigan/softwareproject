package com.example.Software.project.Backend.Reporting.Progress;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

@Configuration("progressMigrationConfiguration")
public class ProgressMigrations {
    public record Applied(int migrations) {}
    // Existing tables are still owned by the application's Hibernate bootstrap. Only
    // the new reporting tables are migration-owned; no baseline of legacy data is taken.
    @Bean("progressMigrations")
    @DependsOn("entityManagerFactory")
    public Applied progressMigrations(DataSource source) {
        Flyway flyway = Flyway.configure().dataSource(source)
                .locations("classpath:db/progress").table("progress_schema_history")
                .baselineOnMigrate(true).baselineVersion("0").load();
        return new Applied(flyway.migrate().migrationsExecuted);
    }
}
