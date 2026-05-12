package com.tchalanet.server.core.haiti.internal.domain.tchala.service;

import com.tchalanet.server.core.haiti.domain.tchala.model.MergePolicy;
import com.tchalanet.server.core.haiti.domain.tchala.model.TchalaNumber;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class TchalaMerge {

  private TchalaMerge() {}

  public static List<TchalaNumber> mergeNumbers(
      List<TchalaNumber> canonical, List<TchalaNumber> pending, MergePolicy policy) {
    Objects.requireNonNull(canonical);
    Objects.requireNonNull(pending);
    Objects.requireNonNull(policy);

    return switch (policy) {
      case UNION_NUMBERS -> union(canonical, pending);
      case CANONICAL_WINS -> canonicalOnly(canonical);
      case PENDING_WINS -> pendingOnly(pending);
    };
  }

  private static List<TchalaNumber> union(List<TchalaNumber> a, List<TchalaNumber> b) {
    // preserve numeric order by value
    var merged = new ArrayList<TchalaNumber>();
    merged.addAll(a);
    merged.addAll(b);

    return merged.stream().sorted(Comparator.comparingInt(TchalaNumber::value)).distinct().toList();
  }

  private static List<TchalaNumber> canonicalOnly(List<TchalaNumber> canonical) {
    return canonical.stream().sorted(Comparator.comparingInt(TchalaNumber::value)).toList();
  }

  private static List<TchalaNumber> pendingOnly(List<TchalaNumber> pending) {
    return pending.stream().sorted(Comparator.comparingInt(TchalaNumber::value)).toList();
  }
}
