package ru.practicum.ewm.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.ewm.model.Location;

import java.util.Optional;

public interface LocationRepository extends JpaRepository<Location, Long> {
    Optional<Location> findByName(String name);

    boolean existsByName(String name);

    @Query("SELECT l FROM Location l WHERE " +
            "LOWER(l.name) LIKE LOWER(CONCAT('%', :text, '%'))")
    Page<Location> searchByName(@Param("text") String text, Pageable pageable);
}
