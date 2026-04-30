package com.tchalanet.server.arch;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
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

  @Test
  void auditedEntityColumnsMustExistInMatchingEnversAuditTable() throws IOException {
    String migrationsSql = readMigrationsSql();

    var missingColumns = new java.util.LinkedHashMap<String, Set<String>>();

    new ClassFileImporter().importPackages("com.tchalanet.server").stream()
        .filter(javaClass -> javaClass.isAnnotatedWith(Entity.class))
        .filter(javaClass -> javaClass.isAnnotatedWith(Audited.class))
        .forEach(
            javaClass -> {
              String auditTable = tableName(javaClass) + "_aud";
              Set<String> auditColumns = auditTableColumns(migrationsSql, auditTable);
              Set<String> expectedColumns = persistentAuditedColumns(javaClass.reflect());
              expectedColumns.removeAll(auditColumns);
              if (!expectedColumns.isEmpty()) {
                missingColumns.put(auditTable, expectedColumns);
              }
            });

    assertThat(missingColumns)
        .as("Every audited persistent entity column must be mirrored in its Flyway *_aud table")
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

  private static Set<String> auditTableColumns(String migrationsSql, String tableName) {
    var pattern =
        Pattern.compile(
            "\\bcreate\\s+table\\s+(?:public\\.)?"
                + tableName
                + "\\s*\\((.*?)\\);",
            Pattern.DOTALL);
    var matcher = pattern.matcher(migrationsSql);
    if (!matcher.find()) {
      return Set.of();
    }

    var columns = new HashSet<String>();
    for (String part : matcher.group(1).split(",")) {
      String token = part.stripLeading().split("\\s+", 2)[0].replace("\"", "");
      if (!token.isBlank() && !isConstraintToken(token)) {
        columns.add(token);
      }
    }
    return columns;
  }

  private static boolean isConstraintToken(String token) {
    return Set.of("constraint", "primary", "foreign", "unique", "check").contains(token);
  }

  private static Set<String> persistentAuditedColumns(Class<?> type) {
    var columns = new HashSet<String>();
    Class<?> current = type;
    while (current != null && current != Object.class) {
      for (Field field : current.getDeclaredFields()) {
        columnName(field).ifPresent(columns::add);
      }
      current = current.getSuperclass();
    }
    return columns;
  }

  private static java.util.Optional<String> columnName(Field field) {
    if (Modifier.isStatic(field.getModifiers())
        || field.isAnnotationPresent(Transient.class)
        || field.isAnnotationPresent(NotAudited.class)
        || field.isAnnotationPresent(OneToMany.class)
        || field.isAnnotationPresent(ManyToMany.class)) {
      return java.util.Optional.empty();
    }

    var column = field.getAnnotation(jakarta.persistence.Column.class);
    if (column != null && !column.name().isBlank()) {
      return java.util.Optional.of(column.name().toLowerCase(Locale.ROOT));
    }

    var joinColumn = field.getAnnotation(JoinColumn.class);
    if (joinColumn != null && !joinColumn.name().isBlank()) {
      return java.util.Optional.of(joinColumn.name().toLowerCase(Locale.ROOT));
    }

    if (field.isSynthetic()) {
      return java.util.Optional.empty();
    }
    return java.util.Optional.of(toSnakeCase(field.getName()));
  }

  private static String toSnakeCase(String value) {
    return value
        .replaceAll("([a-z0-9])([A-Z])", "$1_$2")
        .replaceAll("([A-Z]+)([A-Z][a-z])", "$1_$2")
        .toLowerCase(Locale.ROOT);
  }
}
