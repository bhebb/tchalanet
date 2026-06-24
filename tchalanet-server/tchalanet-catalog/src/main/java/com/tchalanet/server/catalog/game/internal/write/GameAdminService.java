package com.tchalanet.server.catalog.game.internal.write;

import com.tchalanet.server.catalog.game.api.model.GameView;
import com.tchalanet.server.catalog.game.internal.cache.GameCacheNames;
import com.tchalanet.server.catalog.game.internal.mapper.GameMapper;
import com.tchalanet.server.catalog.game.internal.persistence.GameJpaEntity;
import com.tchalanet.server.catalog.game.internal.persistence.GameJpaRepository;
import com.tchalanet.server.common.types.id.GameId;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageMapper;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.common.web.paging.TchSearchQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class GameAdminService {

    private final GameJpaRepository repository;
    private final GameMapper mapper;
    private final JdbcTemplate jdbc;

    @Transactional(readOnly = true)
    public TchPage<GameView> search(Boolean active, TchSearchQuery search, TchPageRequest pageReq) {
        var page = repository.searchLive(active, search.likePattern(), pageReq.pageable());
        return TchPageMapper.map(page, mapper::toView);
    }

    @Transactional
    @CacheEvict(cacheNames = {GameCacheNames.ACTIVE_GAMES, GameCacheNames.GAME_BY_CODE, GameCacheNames.GAME_BY_ID}, allEntries = true)
    public GameView create(CreateCommand req) {
        var entity = new GameJpaEntity();
        entity.setCode(req.code().trim().toUpperCase());
        entity.setName(req.name());
        entity.setCategory(req.category());
        entity.setCombination(req.combination());
        entity.setMinDigits(req.minDigits());
        entity.setMaxDigits(req.maxDigits());
        entity.setDescription(req.description());
        entity.setActive(req.active() != null ? req.active() : true);
        entity.setSortOrder(req.sortOrder() != null ? req.sortOrder() : 0);
        return mapper.toView(repository.save(entity));
    }

    @Transactional
    @CacheEvict(cacheNames = {GameCacheNames.ACTIVE_GAMES, GameCacheNames.GAME_BY_CODE, GameCacheNames.GAME_BY_ID}, allEntries = true)
    public GameView update(GameId id, UpdateCommand req) {
        var entity = repository.findById(id.value())
            .orElseThrow(() -> new IllegalArgumentException("Game not found: " + id));

        boolean structuralChange = req.category() != null || req.combination() != null
            || req.minDigits() != null || req.maxDigits() != null;

        if (structuralChange && isGameInUse(id)) {
            throw new IllegalStateException(
                "Cannot update structural fields (category, combination, minDigits, maxDigits) "
                + "on game '" + entity.getCode() + "' — it is referenced by tenant configuration.");
        }

        if (req.name() != null) entity.setName(req.name());
        if (req.category() != null) entity.setCategory(req.category());
        if (req.combination() != null) entity.setCombination(req.combination());
        if (req.minDigits() != null) entity.setMinDigits(req.minDigits());
        if (req.maxDigits() != null) entity.setMaxDigits(req.maxDigits());
        if (req.description() != null) entity.setDescription(req.description());
        if (req.active() != null) entity.setActive(req.active());
        if (req.sortOrder() != null) entity.setSortOrder(req.sortOrder());

        return mapper.toView(repository.save(entity));
    }

    @Transactional
    @CacheEvict(cacheNames = {GameCacheNames.ACTIVE_GAMES, GameCacheNames.GAME_BY_CODE, GameCacheNames.GAME_BY_ID}, allEntries = true)
    public void deactivate(GameId id) {
        var entity = repository.findById(id.value())
            .orElseThrow(() -> new IllegalArgumentException("Game not found: " + id));
        entity.setActive(false);
        repository.save(entity);
    }

    @Transactional
    @CacheEvict(cacheNames = {GameCacheNames.ACTIVE_GAMES, GameCacheNames.GAME_BY_CODE, GameCacheNames.GAME_BY_ID}, allEntries = true)
    public void softDelete(GameId id) {
        if (isGameInUse(id)) {
            throw new IllegalStateException(
                "Cannot delete game — it is referenced by tenant configuration.");
        }
        var entity = repository.findById(id.value())
            .orElseThrow(() -> new IllegalArgumentException("Game not found: " + id));
        entity.setDeletedAt(Instant.now());
        entity.setActive(false);
        repository.save(entity);
    }

    private boolean isGameInUse(GameId id) {
        Integer count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM tenant_game WHERE game_id = ? AND deleted_at IS NULL",
            Integer.class, id.value());
        return count != null && count > 0;
    }

    public record CreateCommand(
        String code, String name, String category, String combination,
        Integer minDigits, Integer maxDigits, String description,
        Boolean active, Integer sortOrder) {}

    public record UpdateCommand(
        String name, String category, String combination,
        Integer minDigits, Integer maxDigits, String description,
        Boolean active, Integer sortOrder) {}
}
