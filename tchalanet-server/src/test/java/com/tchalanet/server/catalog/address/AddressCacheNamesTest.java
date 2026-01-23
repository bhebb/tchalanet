package com.tchalanet.server.catalog.address;

import com.tchalanet.server.catalog.address.internal.cache.AddressCacheNames;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class AddressCacheNamesTest {
  @Test
  void constants_are_defined() {
    assertNotNull(AddressCacheNames.ACTIVE);
    assertFalse(AddressCacheNames.ACTIVE.isBlank());
    assertNotNull(AddressCacheNames.BY_ID);
  }
}
