package com.example.Software.project.Backend.Security;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import static org.assertj.core.api.Assertions.assertThat;

class SecretConfigurationTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(SecretConfiguration.class)
            .withPropertyValues("spring.datasource.url=jdbc:mysql://localhost/unused",
                    "spring.datasource.username=local-user");

    @Test void missingPasswordFailsBeforeDatabaseCreation() {
        runner.run(context -> assertThat(context).hasFailed());
    }
    @Test void blankPasswordFails() {
        runner.withPropertyValues("spring.datasource.password=   ")
                .run(context -> assertThat(context).hasFailed());
    }
    @Test void missingUsernameFails() {
        runner.withPropertyValues("spring.datasource.username=", "spring.datasource.password=test-value")
                .run(context -> assertThat(context).hasFailed());
    }
    @Test void unresolvedValueFailsWithoutLeakingContents() {
        runner.withPropertyValues("spring.datasource.password=${UNSET_TEST_CREDENTIAL}private-suffix")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasMessage("Set spring.datasource.password through the environment.")
                            .hasNoCause();
                });
    }
    @Test void configuredCredentialsAreAccepted() {
        runner.withPropertyValues("spring.datasource.password=test-only-value")
                .run(context -> assertThat(context).hasNotFailed());
    }
    @Test void isolatedH2TestsRemainSupported() {
        runner.withPropertyValues("spring.datasource.url=jdbc:h2:mem:config-test", "spring.datasource.password=")
                .run(context -> assertThat(context).hasNotFailed());
    }
}
