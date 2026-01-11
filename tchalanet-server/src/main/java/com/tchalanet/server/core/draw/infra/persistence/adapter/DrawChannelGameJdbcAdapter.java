package com.tchalanet.server.core.draw.infra.persistence.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.core.draw.application.port.out.DrawChannelGameWriterPort;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DrawChannelGameJdbcAdapter implements DrawChannelGameWriterPort {

  private final JdbcTemplate jdbc;

  @Override
  public void upsert(DrawChannelId channelId, String gameCode, boolean enabled, JsonNode flags) {
    if (channelId == null || gameCode == null) return;

    String sql =
        "INSERT INTO draw_channel_game (draw_channel_id, game_code, enabled, flags) "
            + "VALUES (?, ?, ?, ?::jsonb) "
            + "ON CONFLICT (tenant_id, draw_channel_id, game_id) DO UPDATE SET enabled = EXCLUDED.enabled, flags = EXCLUDED.flags";

    jdbc.update(sql, channelId.value(), gameCode, enabled, flags == null ? null : flags.toString());
  }
}
