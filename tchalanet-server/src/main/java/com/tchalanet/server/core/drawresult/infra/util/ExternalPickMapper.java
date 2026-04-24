package com.tchalanet.server.core.drawresult.infra.util;

import com.tchalanet.server.common.contracts.results.ExternalResultOutput;
import com.tchalanet.server.core.haiti.domain.lottery.model.ExternalPick;
import java.util.List;

public final class ExternalPickMapper {

  private ExternalPickMapper() {}

  public static ExternalPick fromExternal(ExternalResultOutput ext) {
    if (ext == null || ext.main() == null)
      throw new IllegalArgumentException("external result missing");

    // MVP: si main=3 => pick3, si main=4 => pick4, sinon on essaie via extra
    String pick3 = joinIfSize(ext.main(), 3);
    String pick4 = joinIfSize(ext.main(), 4);

    // fallback : certains providers peuvent exposer pick4 dans extra
    if (pick4 == null && ext.extra() != null) {
      pick4 = joinIfSize(ext.extra(), 4);
    }

    return new ExternalPick(pick3, pick4);
  }

  private static String joinIfSize(List<String> list, int size) {
    if (list == null || list.size() != size) return null;
    var sb = new StringBuilder();
    for (String s : list) {
      if (s == null || s.trim().isEmpty()) return null;
      sb.append(s.trim());
    }
    return sb.toString();
  }
}
