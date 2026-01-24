package com.tchalanet.server.core.tenantgame.application.command.handler;

import static org.junit.jupiter.api.Assertions.*;

import com.tchalanet.server.catalog.game.api.GameCatalog;
import com.tchalanet.server.core.tenantgame.application.command.model.EnableTenantGameCommand;
import com.tchalanet.server.core.tenantgame.application.port.TenantGamePersistencePort;
import com.tchalanet.server.core.tenantgame.domain.TenantGame;
import com.tchalanet.server.common.types.id.TenantId;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for EnableTenantGameCommandHandler.
 * Validates idempotency, game validation, and persistence logic.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EnableTenantGameCommandHandler")
class EnableTenantGameCommandHandlerTest {

  @Mock private GameCatalog gameCatalog;
  @Mock private TenantGamePersistencePort persistencePort;

  @InjectMocks private EnableTenantGameCommandHandler handler;

  private TenantId tenantId;
  private String gameCode;
  private UUID gameUuid;

  @BeforeEach
  void setUp() {
    tenantId = TenantId.of(UUID.randomUUID());
    gameCode = "HT_BOLET";
    gameUuid = UUID.randomUUID();
  }

  @Nested
  @DisplayName("Validation")
  class Validation {

    @Test
    @DisplayName("should reject if game not found in catalog")
    void shouldRejectIfGameNotFound() {
      var command = new EnableTenantGameCommand(tenantId, gameCode, null, null);

      // Mock game not found
      org.mockito.Mockito.when(gameCatalog.findByCode(gameCode)).thenReturn(Optional.empty());

      // Should throw exception
      assertThrows(IllegalArgumentException.class, () -> handler.handle(command));

      // Should not persist
      org.mockito.Mockito.verify(persistencePort, org.mockito.Mockito.never()).save(org.mockito.ArgumentMatchers.any());
    }
  }

  @Nested
  @DisplayName("Idempotency")
  class Idempotency {

    @Test
    @DisplayName("should idempotently enable game (idempotency)")
    void shouldIdempotentlyEnable() {
      var command1 = new EnableTenantGameCommand(tenantId, gameCode, null, "key1");
      var command2 = new EnableTenantGameCommand(tenantId, gameCode, null, "key1");

      // Mock game found
      org.mockito.Mockito.when(gameCatalog.findByCode(gameCode))
          .thenReturn(Optional.of(createGameView()));

      // First call - new entry
      org.mockito.Mockito.when(persistencePort.findByTenantIdAndGameCode(tenantId, gameCode))
          .thenReturn(Optional.empty());
      handler.handle(command1);

      // Verify first save
      org.mockito.Mockito.verify(persistencePort, org.mockito.Mockito.times(1))
          .save(org.mockito.ArgumentMatchers.any());

      // Second call - should update existing (no new insert)
      org.mockito.Mockito.reset(persistencePort);
      var existingGame = TenantGame.builder()
          .tenantId(tenantId)
          .gameCode(gameCode)
          .enabled(true)
          .version(1L)
          .build();
      org.mockito.Mockito.when(persistencePort.findByTenantIdAndGameCode(tenantId, gameCode))
          .thenReturn(Optional.of(existingGame));

      handler.handle(command2);

      // Should still persist (update case)
      org.mockito.Mockito.verify(persistencePort, org.mockito.Mockito.times(1))
          .save(org.mockito.ArgumentMatchers.any());
    }
  }

  private com.tchalanet.server.catalog.game.api.GameView createGameView() {
    return new com.tchalanet.server.catalog.game.api.GameView(
        com.tchalanet.server.common.types.id.GameId.of(gameUuid),
        gameCode,
        "Bolet",
        "HAITI",
        "3D",
        3,
        3,
        "Test game",
        true,
        0,
        java.time.Instant.now(),
        java.time.Instant.now()
    );
  }
}
