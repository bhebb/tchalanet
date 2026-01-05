package com.tchalanet.server.core.draw.infra.persistence.repo;

import com.tchalanet.server.core.draw.infra.persistence.PublicDrawResultRow;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PublicDrawResultRepository
    extends JpaRepository<com.tchalanet.server.core.draw.infra.persistence.DrawJpaEntity, UUID> {

  @Query(
      nativeQuery = true,
      value =
          """
            SELECT
                dr.channel_code as channelCode,
                dc.name as channelName,
                dc.timezone as channelTimezone,
                dc.draw_time as channelDrawTime,
                dr.draw_date as drawDate,
                dr.occurred_at as occurredAt,
                dr.numbers_main as numbersMainJson,
                dr.numbers_extra as numbersExtraJson,
                dr.quality as quality,
                dr.source as source
            FROM draw_result dr
            JOIN draw_channel dc ON dr.channel_code = dc.code
            WHERE dr.status = 'VALID'
              AND (:channelCode IS NULL OR dr.channel_code = :channelCode)
              AND (:from IS NULL OR dr.draw_date >= :from)
              AND (:to IS NULL OR dr.draw_date <= :to)
            ORDER BY dr.draw_date DESC, dc.draw_time DESC
            """,
      countQuery =
          """
            SELECT count(*)
            FROM draw_result dr
            JOIN draw_channel dc ON dr.channel_code = dc.code
            WHERE dr.status = 'VALID'
              AND (:channelCode IS NULL OR dr.channel_code = :channelCode)
              AND (:from IS NULL OR dr.draw_date >= :from)
              AND (:to IS NULL OR dr.draw_date <= :to)
            """)
  Page<PublicDrawResultRow> search(
      @Param("channelCode") String channelCode,
      @Param("from") LocalDate from,
      @Param("to") LocalDate to,
      Pageable pageable);

  @Query(
      nativeQuery = true,
      value =
          """
            SELECT
                dr.channel_code as channelCode,
                dc.name as channelName,
                dc.timezone as channelTimezone,
                dc.draw_time as channelDrawTime,
                dr.draw_date as drawDate,
                dr.occurred_at as occurredAt,
                dr.numbers_main as numbersMainJson,
                dr.numbers_extra as numbersExtraJson,
                dr.quality as quality,
                dr.source as source
            FROM draw_result dr
            JOIN draw_channel dc ON dr.channel_code = dc.code
            WHERE dr.channel_code = :channelCode
              AND dr.draw_date = :drawDate
              AND dr.status = 'VALID'
            LIMIT 1
            """)
  Optional<PublicDrawResultRow> findOne(
      @Param("channelCode") String channelCode, @Param("drawDate") LocalDate drawDate);

  @Query(
      nativeQuery = true,
      value =
          """
            WITH ranked_results AS (
                SELECT
                    dr.channel_code as channelCode,
                    dc.name as channelName,
                    dc.timezone as channelTimezone,
                    dc.draw_time as channelDrawTime,
                    dr.draw_date as drawDate,
                    dr.occurred_at as occurredAt,
                    dr.numbers_main as numbersMainJson,
                    dr.numbers_extra as numbersExtraJson,
                    dr.quality as quality,
                    dr.source as source,
                    ROW_NUMBER() OVER (PARTITION BY dr.channel_code ORDER BY dr.draw_date DESC, dc.draw_time DESC) as rn
                FROM draw_result dr
                JOIN draw_channel dc ON dr.channel_code = dc.code
                WHERE dr.status = 'VALID'
            )
            SELECT *
            FROM ranked_results
            WHERE rn <= :limitPerChannel
            ORDER BY channelCode, drawDate DESC, channelDrawTime DESC
            """)
  List<PublicDrawResultRow> latest(@Param("limitPerChannel") int limitPerChannel);
}
