package com.tchalanet.server.core.draw.application.command.handler;

import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.draw.application.command.model.UpsertDrawChannelGamesCommand;
import com.tchalanet.server.core.draw.application.port.out.DrawChannelGameWriterPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class UpsertDrawChannelGamesCommandHandler
    implements VoidCommandHandler<UpsertDrawChannelGamesCommand> {

  private final DrawChannelGameWriterPort writer;

  @Override
  public void handle(UpsertDrawChannelGamesCommand command) {
    var channelId = command.channelId();
    var codes = command.gameCodes();
    if (codes == null || codes.isEmpty()) return;

    for (String code : codes) {
      writer.upsert(channelId, code, command.enabled(), null);
    }

    log.info(
        "Upserted {} games for channel {} enabled={}", codes.size(), channelId, command.enabled());
  }
}
