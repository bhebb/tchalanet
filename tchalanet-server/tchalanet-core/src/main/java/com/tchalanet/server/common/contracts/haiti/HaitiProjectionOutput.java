package com.tchalanet.server.common.contracts.haiti;

public record HaitiProjectionOutput(HaitiResult result, HaitiFlags flags) {

  public static HaitiProjectionOutput ok(HaitiResult result, HaitiFlags flags) {
    return new HaitiProjectionOutput(result, flags);
  }

  public static HaitiProjectionOutput fail(HaitiFlags flags) {
    return new HaitiProjectionOutput(null, flags);
  }
}
