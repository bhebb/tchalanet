package com.tchalanet.server.core.game.web;

import com.tchalanet.server.core.game.domain.model.Game;
import com.tchalanet.server.core.game.domain.usecase.GameCrudUseCase;
import com.tchalanet.server.core.game.web.dto.GameDto;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/games")
@RequiredArgsConstructor
public class GameController {

  private final GameCrudUseCase useCase;

  @GetMapping
  public List<GameDto> list() {
    return useCase.listActive().stream().map(this::toDto).collect(Collectors.toList());
  }

  @GetMapping("/{code}")
  public ResponseEntity<GameDto> get(@PathVariable String code) {
    return useCase
        .get(code)
        .map(g -> ResponseEntity.ok(toDto(g)))
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  @PostMapping
  public ResponseEntity<GameDto> create(@RequestBody GameDto dto) {
    Game g = fromDto(dto);
    Game saved = useCase.create(g);
    return ResponseEntity.ok(toDto(saved));
  }

  @PutMapping("/{code}")
  public ResponseEntity<GameDto> update(@PathVariable String code, @RequestBody GameDto dto) {
    Game g = fromDto(dto);
    Game updated = useCase.update(code, g);
    return ResponseEntity.ok(toDto(updated));
  }

  @DeleteMapping("/{code}")
  public ResponseEntity<Void> delete(@PathVariable String code) {
    useCase.delete(code);
    return ResponseEntity.noContent().build();
  }

  private GameDto toDto(Game g) {
    return GameDto.builder()
        .id(g.getId() == null ? null : g.getId().value())
        .code(g.getCode())
        .name(g.getName())
        .category(g.getCategory())
        .minDigits(g.getMinDigits())
        .maxDigits(g.getMaxDigits())
        .combination(g.getCombination())
        .description(g.getDescription())
        .active(g.getActive())
        .sortOrder(g.getSortOrder())
        .createdAt(g.getCreatedAt())
        .updatedAt(g.getUpdatedAt())
        .build();
  }

  private Game fromDto(GameDto d) {
    return Game.builder()
        .id(d.getId() == null ? null : new Game.GameId(d.getId()))
        .code(d.getCode())
        .name(d.getName())
        .category(d.getCategory())
        .minDigits(d.getMinDigits())
        .maxDigits(d.getMaxDigits())
        .combination(d.getCombination())
        .description(d.getDescription())
        .active(d.getActive())
        .sortOrder(d.getSortOrder())
        .build();
  }
}
