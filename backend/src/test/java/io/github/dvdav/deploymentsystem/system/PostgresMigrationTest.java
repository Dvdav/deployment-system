package io.github.dvdav.deploymentsystem.system;

import io.github.dvdav.deploymentsystem.DeploymentSystemApplication;
import org.flywaydb.core.internal.exception.FlywayMigrateException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
class PostgresMigrationTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:17.12");

    @Test
    void appliesBootstrapOnceAndReadsTechnicalDataThroughJdbc() {
        try (var application = start()) {
            var jdbc = application.getBean(JdbcTemplate.class);
            assertBootstrapAppliedOnce(jdbc);
            assertThat(jdbc.update("INSERT INTO system_info (id, value) VALUES (?, ?)", 1, "bootstrap"))
                    .isEqualTo(1);
        }

        try (var application = start()) {
            var jdbc = application.getBean(JdbcTemplate.class);
            assertBootstrapAppliedOnce(jdbc);
            assertThat(jdbc.queryForObject("SELECT value FROM system_info WHERE id = ?", String.class, 1))
                    .isEqualTo("bootstrap");
        }
    }

    @Test
    void preventsStartupWhenMigrationFails() {
        assertThatThrownBy(() -> {
            try (var application = start(
                    "--spring.flyway.default-schema=invalid_migration_test",
                    "--spring.flyway.locations=classpath:db/invalid-migration")) {
                assertThat(application.isActive()).isFalse();
            }
        }).hasCauseInstanceOf(FlywayMigrateException.class)
                .hasStackTraceContaining("V1__invalid.sql");
    }

    private ConfigurableApplicationContext start(String... arguments) {
        return new SpringApplicationBuilder(DeploymentSystemApplication.class)
                .initializers(context -> TestPropertyValues.of(
                        "spring.datasource.url=" + POSTGRES.getJdbcUrl(),
                        "spring.datasource.username=" + POSTGRES.getUsername(),
                        "spring.datasource.password=" + POSTGRES.getPassword()).applyTo(context))
                .web(WebApplicationType.NONE)
                .run(arguments);
    }

    private void assertBootstrapAppliedOnce(JdbcTemplate jdbc) {
        assertThat(jdbc.queryForList(
                "SELECT version FROM flyway_schema_history WHERE success AND version = '1'",
                String.class)).containsExactly("1");
    }
}
