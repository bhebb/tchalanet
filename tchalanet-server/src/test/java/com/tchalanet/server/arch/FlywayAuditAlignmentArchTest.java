package com.tchalanet.server.arch;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.regex.Pattern;
import org.hibernate.envers.Audited;
import org.junit.jupiter.api.Test;

class FlywayAuditAlignmentArchTest {

  private static final Path MIGRATIONS_DIR =
      Path.of("src/main/resources/db/migration");

  @Test
  void auditedEntitiesMustHaveAnEnversAuditTableInFlyway() throws IOException {
    String migrationsSql = readMigrationsSql();

    var missingTables =
        new ClassFileImporter().importPackages("com.tchalanet.server").stream()
            .filter(javaClass -> javaClass.isAnnotatedWith(Entity.class))
            .filter(javaClass -> javaClass.isAnnotatedWith(Audited.class))
            .map(FlywayAuditAlignmentArchTest::tableName)
            .map(tableName -> tableName + "_aud")
            .filter(auditTable -> !containsCreateTable(migrationsSql, auditTable))
            .toList();

    assertThat(missingTables)
        .as("Every @Audited JPA entity must have a matching Envers *_aud table in Flyway")
        .isEmpty();
  }

  private static String readMigrationsSql() throws IOException {
    var sql = new StringBuilder();
    try (var files = Files.list(MIGRATIONS_DIR)) {
      for (Path file : files.filter(path -> path.toString().endsWith(".sql")).sorted().toList()) {
        sql.append(Files.readString(file)).append('\n');
      }
    }
    return sql.toString().toLowerCase(Locale.ROOT);
  }

  private static String tableName(JavaClass javaClass) {
    return javaClass
        .tryGetAnnotationOfType(Table.class)
        .map(Table::name)
        .filter(name -> !name.isBlank())
        .orElseGet(() -> toSnakeCase(javaClass.getSimpleName().replaceAll("(Jpa)?Entity$", "")));
  }

  private static boolean containsCreateTable(String migrationsSql, String tableName) {
    var pattern = Pattern.compile("\\bcreate\\s+table\\s+(?:public\\.)?" + tableName + "\\b");
    return pattern.matcher(migrationsSql).find();
  }

  private static String toSnakeCase(String value) {
    return value
        .replaceAll("([a-z0-9])([A-Z])", "$1_$2")
        .replaceAll("([A-Z]+)([A-Z][a-z])", "$1_$2")
        .toLowerCase(Locale.ROOT);
  }
}
