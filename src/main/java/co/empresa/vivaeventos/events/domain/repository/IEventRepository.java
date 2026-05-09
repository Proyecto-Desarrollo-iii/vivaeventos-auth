package co.empresa.vivaeventos.events.domain.repository;

import co.empresa.vivaeventos.events.domain.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IEventRepository extends JpaRepository<Event, UUID> {

    List<Event> findByOrganizerId(UUID organizerId);

    List<Event> findByCategory(String category);

    List<Event> findByIsPublishedTrueAndIsActiveTrueOrderByEventDateTimeDesc();

    List<Event> findByIsPublishedTrueAndCategoryOrderByEventDateTimeDesc(String category);

    @Query("SELECT e FROM Event e WHERE e.isPublished = true AND e.isActive = true AND e.eventDateTime >= :now ORDER BY e.eventDateTime ASC")
    List<Event> findUpcomingEvents(LocalDateTime now);

    @Query("SELECT e FROM Event e WHERE e.isPublished = true AND e.isActive = true AND e.category = :category AND e.eventDateTime >= :now ORDER BY e.eventDateTime ASC")
    List<Event> findUpcomingEventsByCategory(String category, LocalDateTime now);
}