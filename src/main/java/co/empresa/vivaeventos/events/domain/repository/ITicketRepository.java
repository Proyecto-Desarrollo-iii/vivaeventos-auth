package co.empresa.vivaeventos.events.domain.repository;

import co.empresa.vivaeventos.events.domain.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ITicketRepository extends JpaRepository<Ticket, UUID> {

    List<Ticket> findByEventId(UUID eventId);

    List<Ticket> findByEventIdAndIsActiveTrue(UUID eventId);
}