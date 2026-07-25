package com.tchalanet.server.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class ProblemRestMessageFirstGuardTest {

  private static final Pattern MESSAGE_FIRST_FACTORY =
      Pattern.compile(
          "ProblemRest\\.(?:badRequest|conflict|forbidden|notFound|internal|unauthorized|unprocessable)\\s*\\(\\s*\"");

  private static final Pattern MESSAGE_FIRST_STATUS_FACTORY =
      Pattern.compile("ProblemRest\\.of\\s*\\(\\s*HttpStatus\\.[A-Z_]+\\s*,\\s*\"");

  @Test
  void featuresAndPlatformDoNotIntroduceMessageFirstProblemRestProducers() throws IOException {
    Path serverRoot = serverRoot();
    List<String> violations = new ArrayList<>();

    for (Path sourceRoot :
        List.of(
            serverRoot.resolve("tchalanet-features/src/main/java"),
            serverRoot.resolve("tchalanet-platform/src/main/java"))) {
      for (Path javaFile : javaFiles(sourceRoot)) {
        violations.addAll(violations(javaFile));
      }
    }

    assertThat(violations)
        .as(
            "Use ProblemRest.of(ErrorDescriptor...) or a typed domain exception; do not add"
                + " message-first ProblemRest producers in features/platform.")
        .isEmpty();
  }

  private static Path serverRoot() {
    Path current = Path.of("").toAbsolutePath();
    for (Path candidate = current; candidate != null; candidate = candidate.getParent()) {
      if (Files.isDirectory(candidate.resolve("tchalanet-features/src/main/java"))
          && Files.isDirectory(candidate.resolve("tchalanet-platform/src/main/java"))) {
        return candidate;
      }
    }
    throw new IllegalStateException("Cannot locate tchalanet-server root from " + current);
  }

  private static List<Path> javaFiles(Path root) throws IOException {
    try (var paths = Files.walk(root)) {
      return paths
          .filter(Files::isRegularFile)
          .filter(path -> path.toString().endsWith(".java"))
          .toList();
    }
  }

  private static List<String> violations(Path javaFile) {
    try {
      List<String> lines = Files.readAllLines(javaFile);
      List<String> violations = new ArrayList<>();

      for (int index = 0; index < lines.size(); index++) {
        String line = lines.get(index);
        if (MESSAGE_FIRST_FACTORY.matcher(line).find()
            || MESSAGE_FIRST_STATUS_FACTORY.matcher(line).find()) {
          violations.add(javaFile + ":" + (index + 1) + " -> " + line.trim());
        }
      }

      return violations;
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }
}
