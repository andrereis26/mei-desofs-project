package com.desofs.project.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.FileSystemResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class SchemaMigrationConfigurationTest {

    private static final Pattern FLYWAY_VERSIONED_MIGRATION =
            Pattern.compile("V(?<version>\\d+)__[-_a-zA-Z0-9]+\\.sql");

    @Test
    void productionConfiguration_UsesFlywayAndSchemaValidation() {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new FileSystemResource("src/main/resources/application.yml"));

        Properties properties = yaml.getObject();

        assertThat(properties).isNotNull();
        assertThat(properties.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
        assertThat(properties.getProperty("spring.flyway.enabled")).isEqualTo("true");
        assertThat(properties.getProperty("spring.flyway.locations")).isEqualTo("classpath:db/migration");
    }

    @Test
    void migrationFiles_AreVersionedAndUnique() throws IOException {
        Path migrationDirectory = Path.of("src/main/resources/db/migration");

        List<String> migrationFiles;
        try (var paths = Files.list(migrationDirectory)) {
            migrationFiles = paths
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith(".sql"))
                    .sorted()
                    .toList();
        }

        assertThat(migrationFiles).isNotEmpty();
        assertThat(migrationFiles).allMatch(name -> FLYWAY_VERSIONED_MIGRATION.matcher(name).matches());

        List<String> versions = migrationFiles.stream()
                .map(name -> name.substring(1, name.indexOf("__")))
                .toList();
        assertThat(versions).doesNotHaveDuplicates();
    }
}
