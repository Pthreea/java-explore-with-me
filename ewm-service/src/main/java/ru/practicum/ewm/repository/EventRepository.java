package ru.practicum.ewm.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.ewm.model.Event;
import ru.practicum.ewm.model.EventState;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {

    Page<Event> findByInitiatorId(Long userId, Pageable pageable);

    Optional<Event> findByIdAndInitiatorId(Long id, Long userId);

    Optional<Event> findByIdAndState(Long id, EventState state);

    @Query("SELECT e FROM Event e " +
            "WHERE (:users IS NULL OR e.initiator.id IN :users) " +
            "AND (:states IS NULL OR e.state IN :states) " +
            "AND (:categories IS NULL OR e.category.id IN :categories) " +
            "AND (CAST(:rangeStart AS timestamp) IS NULL OR e.eventDate >= :rangeStart) " +
            "AND (CAST(:rangeEnd AS timestamp) IS NULL OR e.eventDate <= :rangeEnd)")
    Page<Event> findAdminEvents(@Param("users") List<Long> users,
                                @Param("states") List<EventState> states,
                                @Param("categories") List<Long> categories,
                                @Param("rangeStart") LocalDateTime rangeStart,
                                @Param("rangeEnd") LocalDateTime rangeEnd,
                                Pageable pageable);

    @Query(value = "SELECT * FROM events e WHERE e.state = 'PUBLISHED' " +
            "AND (CAST(:text AS TEXT) IS NULL OR " +
            "    LOWER(e.annotation) LIKE LOWER(CONCAT('%', CAST(:text AS TEXT), '%')) OR " +
            "    LOWER(e.description) LIKE LOWER(CONCAT('%', CAST(:text AS TEXT), '%'))) " +
            "AND (CAST(:categories AS TEXT) IS NULL OR e.category_id = ANY(CAST(:categories AS BIGINT[]))) " +
            "AND (CAST(:paid AS BOOLEAN) IS NULL OR e.paid = CAST(:paid AS BOOLEAN)) " +
            "AND e.event_date >= CAST(:rangeStart AS TIMESTAMP) " +  // ← Убрали проверку IS NULL
            "AND (CAST(:rangeEnd AS TIMESTAMP) IS NULL OR e.event_date <= CAST(:rangeEnd AS TIMESTAMP)) " +
            "ORDER BY e.event_date ASC",
            countQuery = "SELECT COUNT(*) FROM events e WHERE e.state = 'PUBLISHED' " +
                    "AND (CAST(:text AS TEXT) IS NULL OR " +
                    "    LOWER(e.annotation) LIKE LOWER(CONCAT('%', CAST(:text AS TEXT), '%')) OR " +
                    "    LOWER(e.description) LIKE LOWER(CONCAT('%', CAST(:text AS TEXT), '%'))) " +
                    "AND (CAST(:categories AS TEXT) IS NULL OR e.category_id = ANY(CAST(:categories AS BIGINT[]))) " +
                    "AND (CAST(:paid AS BOOLEAN) IS NULL OR e.paid = CAST(:paid AS BOOLEAN)) " +
                    "AND e.event_date >= CAST(:rangeStart AS TIMESTAMP) " +
                    "AND (CAST(:rangeEnd AS TIMESTAMP) IS NULL OR e.event_date <= CAST(:rangeEnd AS TIMESTAMP))",
            nativeQuery = true)
    Page<Event> searchPublicEvents(@Param("text") String text,
                                   @Param("categories") String categories,
                                   @Param("paid") Boolean paid,
                                   @Param("rangeStart") LocalDateTime rangeStart,
                                   @Param("rangeEnd") LocalDateTime rangeEnd,
                                   Pageable pageable);
}