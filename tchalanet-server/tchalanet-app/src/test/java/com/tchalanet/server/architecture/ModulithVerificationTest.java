package com.tchalanet.server.architecture;

import com.tchalanet.server.TchalanetApiApplication;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/**
 * Spring Modulith verification — ensures package boundaries are respected
 * and all module dependencies are declared correctly.
 *
 * <p>Run: ./mvnw -pl tchalanet-app -am test -Dtest=ModulithVerificationTest
 */
class ModulithVerificationTest {

  ApplicationModules modules = ApplicationModules.of(TchalanetApiApplication.class);

  @Test
  void verifyModularStructure() {
    modules.verify();
  }

  @Test
  void writeDocumentationSnippets() {
    new Documenter(modules).writeModulesAsPlantUml();
  }
}
