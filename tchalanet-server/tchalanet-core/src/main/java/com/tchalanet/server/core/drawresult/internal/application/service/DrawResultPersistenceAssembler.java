package com.tchalanet.server.core.drawresult.internal.application.service;

import com.tchalanet.server.catalog.resultslot.api.ResultSlotView;
import com.tchalanet.server.common.json.utils.JsonUtils;
import com.tchalanet.server.core.drawresult.api.model.ResultQuality;
import com.tchalanet.server.core.drawresult.internal.application.port.out.external.ExternalResultItem;
import com.tchalanet.server.core.drawresult.internal.infra.util.SourceResultBuilder;
import java.time.Instant;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;
import tools.jackson.databind.node.ObjectNode;

@Service
@RequiredArgsConstructor
public class DrawResultPersistenceAssembler {

  private final JsonUtils jsonUtils;

  public DrawResultPersistPayload assemble(
      ResultSlotView slot,
      LocalDate date,
      Instant occurredAt,
      ResolvedExternalResults external,
      ResultSlotSourceConfig sourceCfg,
      HaitiProjectionResult projection,
      boolean includeRaw) {

    var sourceResult =
        SourceResultBuilder.build(
            jsonUtils,
            slot.provider(),
            slot.slotKey(),
            date,
            occurredAt,
            external.pick3(),
            external.pick4());

    var flags = JsonUtils.emptyObject();

    var sourceFlags = JsonUtils.emptyObject();
    if (external.pick3() != null) {
      sourceFlags.set("pick3", jsonUtils.toJsonNode(external.pick3().sourceFlags()));
    }
    if (external.pick4() != null) {
      sourceFlags.set("pick4", jsonUtils.toJsonNode(external.pick4().sourceFlags()));
    }

    flags.set("source", sourceFlags);
    flags.set("haiti", jsonUtils.toJsonNode(projection.flags()));

    return new DrawResultPersistPayload(
        sourceResult,
        projection.haitiResult(),
        includeRaw ? buildRawPayload(external) : null,
        flags,
        resolveQuality(sourceCfg, external, projection).name(),
        combinedSourceHash(slot.slotKey(), date, external.pick3(), external.pick4()));
  }

  private ObjectNode buildRawPayload(ResolvedExternalResults external) {
    var raw = JsonUtils.emptyObject();

    if (external.pick3() != null) {
      raw.set("pick3_raw", jsonUtils.toJsonNode(external.pick3().rawPayload()));
    }

    if (external.pick4() != null) {
      raw.set("pick4_raw", jsonUtils.toJsonNode(external.pick4().rawPayload()));
    }

    if (external.rawPayload() != null) {
      raw.set("provider_payload", jsonUtils.toJsonNode(external.rawPayload()));
    }

    return raw;
  }

  private ResultQuality resolveQuality(
      ResultSlotSourceConfig sourceCfg,
      ResolvedExternalResults external,
      HaitiProjectionResult projection) {

    var cfg = sourceCfg == null ? ResultSlotSourceConfig.empty() : sourceCfg;
    var quality = ResultQuality.COMPLETE;
    var expectedAny = false;

    if (cfg.activePick3().isPresent()) {
      expectedAny = true;
      quality = combine(quality, qualityForExpected(external.pick3()));
    }

    if (cfg.activePick4().isPresent()) {
      expectedAny = true;
      quality = combine(quality, qualityForExpected(external.pick4()));
    }

    if (!expectedAny) {
      quality = combine(quality, qualityForOptional(external.pick3()));
      quality = combine(quality, qualityForOptional(external.pick4()));
    }

    if (projection != null
        && projection.flags() != null
        && !projection.flags().projectionOk()
        && quality == ResultQuality.COMPLETE) {
      return ResultQuality.SUSPECT;
    }

    return quality;
  }

  private ResultQuality qualityForExpected(ExternalResultItem item) {
    if (item == null || !item.found()) {
      return ResultQuality.SUSPECT;
    }
    return item.quality() == null ? ResultQuality.SUSPECT : item.quality();
  }

  private ResultQuality qualityForOptional(ExternalResultItem item) {
    if (item == null || !item.found()) {
      return ResultQuality.COMPLETE;
    }
    return item.quality() == null ? ResultQuality.SUSPECT : item.quality();
  }

  private ResultQuality combine(ResultQuality left, ResultQuality right) {
    if (left == ResultQuality.INVALID || right == ResultQuality.INVALID) {
      return ResultQuality.INVALID;
    }
    if (left == ResultQuality.SUSPECT || right == ResultQuality.SUSPECT) {
      return ResultQuality.SUSPECT;
    }
    return ResultQuality.COMPLETE;
  }

  private String combinedSourceHash(
      String slotKey, LocalDate date, ExternalResultItem p3, ExternalResultItem p4) {

    String p3Hash = p3 == null || p3.sourceFlags() == null ? "" : p3.sourceFlags().sourceHash();
    String p4Hash = p4 == null || p4.sourceFlags() == null ? "" : p4.sourceFlags().sourceHash();

    return DigestUtils.sha256Hex(slotKey + "|" + date + "|p3=" + p3Hash + "|p4=" + p4Hash);
  }
}
