package com.tchalanet.server.architecture;

import com.tchalanet.server.app.TchalanetApplication;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/**
 * Spring Modulith verification — ensures package boundaries are respected
 * and all module dependencies are declared correctly.
 *
 * <p>Run: ./mvnw -pl tchalanet-app -am test -Dtest=ModulithVerificationTest
 */
class ModulithVerificationTest {

  ApplicationModules modules = ApplicationModules.of(TchalanetApplication.class);

  @Test
  void verifyModularStructure() {
    modules.verify();
  }
}
