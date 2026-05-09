package co.empresa.vivaeventos.events.domain.service;

import co.empresa.vivaeventos.events.domain.model.Event;
import co.empresa.vivaeventos.events.domain.model.Ticket;
import co.empresa.vivaeventos.events.domain.model.Dto.CreateEventRequest;
import co.empresa.vivaeventos.events.domain.model.Dto.EventResponse;
import co.empresa.vivaeventos.events.domain.model.Dto.UpdateEventRequest;
import co.empresa.vivaeventos.events.domain.repository.IEventRepository;
import co.empresa.vivaeventos.events.domain.repository.ITicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class EventServiceImpl implements IEventService {

    private final IEventRepository eventRepository;
    private final ITicketRepository ticketRepository;

    public EventServiceImpl(IEventRepository eventRepository, ITicketRepository ticketRepository) {
        this.eventRepository = eventRepository;
        this.ticketRepository = ticketRepository;
    }

    @Override
    @Transactional
    public EventResponse createEvent(UUID organizerId, CreateEventRequest request) {
        Event event = new Event();
        event.setOrganizerId(organizerId);
        event.setName(request.getName());
        event.setDescription(request.getDescription());
        event.setCategory(request.getCategory());
        event.setEventDateTime(request.getEventDateTime());
        event.setBannerUrl(request.getBannerUrl());
        event.setThumbnailUrl(request.getThumbnailUrl());
        event.setVenueName(request.getVenueName());
        event.setAddress(request.getAddress());
        event.setLatitude(request.getLatitude());
        event.setLongitude(request.getLongitude());
        event.setMapsEmbedUrl(request.getMapsEmbedUrl());
        event.setMapsLinkUrl(request.getMapsLinkUrl());
        event.setArtistName(request.getArtistName());
        event.setSpotifyUrl(request.getSpotifyUrl());
        event.setInstagramUrl(request.getInstagramUrl());
        event.setTwitterUrl(request.getTwitterUrl());
        event.setIsPublished(false);
        event.setIsActive(true);

        Event savedEvent = eventRepository.save(event);

        // Crear tickets asociados
        if (request.getTickets() != null && !request.getTickets().isEmpty()) {
            request.getTickets().forEach(ticketReq -> {
                Ticket ticket = new Ticket();
                ticket.setEventId(savedEvent.getId());
                ticket.setType(ticketReq.getType());
                ticket.setDescription(ticketReq.getDescription());
                ticket.setPrice(ticketReq.getPrice());
                ticket.setCapacity(ticketReq.getCapacity());
                ticket.setSoldCount(0);
                ticket.setIsActive(true);
                ticketRepository.save(ticket);
            });
        }

        return mapEventToResponse(savedEvent);
    }

    @Override
    @Transactional(readOnly = true)
    public EventResponse getEventById(UUID eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Evento no encontrado: " + eventId));
        return mapEventToResponse(event);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventResponse> getEventsByOrganizer(UUID organizerId) {
        return eventRepository.findByOrganizerId(organizerId)
                .stream()
                .map(this::mapEventToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventResponse> getPublishedEvents() {
        return eventRepository.findByIsPublishedTrueAndIsActiveTrueOrderByEventDateTimeDesc()
                .stream()
                .map(this::mapEventToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventResponse> getPublishedEventsByCategory(String category) {
        return eventRepository.findByIsPublishedTrueAndCategoryOrderByEventDateTimeDesc(category)
                .stream()
                .map(this::mapEventToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventResponse> getUpcomingEvents() {
        return eventRepository.findUpcomingEvents(LocalDateTime.now())
                .stream()
                .map(this::mapEventToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventResponse> getUpcomingEventsByCategory(String category) {
        return eventRepository.findUpcomingEventsByCategory(category, LocalDateTime.now())
                .stream()
                .map(this::mapEventToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventResponse updateEvent(UUID eventId, UUID organizerId, UpdateEventRequest request) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Evento no encontrado: " + eventId));

        if (!event.getOrganizerId().equals(organizerId)) {
            throw new RuntimeException("No tienes permiso para actualizar este evento");
        }

        if (request.getName() != null) event.setName(request.getName());
        if (request.getDescription() != null) event.setDescription(request.getDescription());
        if (request.getCategory() != null) event.setCategory(request.getCategory());
        if (request.getEventDateTime() != null) event.setEventDateTime(request.getEventDateTime());
        if (request.getBannerUrl() != null) event.setBannerUrl(request.getBannerUrl());
        if (request.getThumbnailUrl() != null) event.setThumbnailUrl(request.getThumbnailUrl());
        if (request.getVenueName() != null) event.setVenueName(request.getVenueName());
        if (request.getAddress() != null) event.setAddress(request.getAddress());
        if (request.getLatitude() != null) event.setLatitude(request.getLatitude());
        if (request.getLongitude() != null) event.setLongitude(request.getLongitude());
        if (request.getMapsEmbedUrl() != null) event.setMapsEmbedUrl(request.getMapsEmbedUrl());
        if (request.getMapsLinkUrl() != null) event.setMapsLinkUrl(request.getMapsLinkUrl());
        if (request.getArtistName() != null) event.setArtistName(request.getArtistName());
        if (request.getSpotifyUrl() != null) event.setSpotifyUrl(request.getSpotifyUrl());
        if (request.getInstagramUrl() != null) event.setInstagramUrl(request.getInstagramUrl());
        if (request.getTwitterUrl() != null) event.setTwitterUrl(request.getTwitterUrl());
        if (request.getIsPublished() != null) event.setIsPublished(request.getIsPublished());

        // Actualizar tickets si se proporcionan
        if (request.getTickets() != null && !request.getTickets().isEmpty()) {
            // Eliminar tickets anteriores
            ticketRepository.findByEventId(eventId).forEach(ticketRepository::delete);

            // Crear nuevos tickets
            request.getTickets().forEach(ticketReq -> {
                Ticket ticket = new Ticket();
                ticket.setEventId(eventId);
                ticket.setType(ticketReq.getType());
                ticket.setDescription(ticketReq.getDescription());
                ticket.setPrice(ticketReq.getPrice());
                ticket.setCapacity(ticketReq.getCapacity());
                ticket.setSoldCount(0);
                ticket.setIsActive(true);
                ticketRepository.save(ticket);
            });
        }

        Event updatedEvent = eventRepository.save(event);
        return mapEventToResponse(updatedEvent);
    }

    @Override
    @Transactional
    public void publishEvent(UUID eventId, UUID organizerId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Evento no encontrado: " + eventId));

        if (!event.getOrganizerId().equals(organizerId)) {
            throw new RuntimeException("No tienes permiso para publicar este evento");
        }

        event.setIsPublished(true);
        eventRepository.save(event);
    }

    @Override
    @Transactional
    public void unpublishEvent(UUID eventId, UUID organizerId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Evento no encontrado: " + eventId));

        if (!event.getOrganizerId().equals(organizerId)) {
            throw new RuntimeException("No tienes permiso para despublicar este evento");
        }

        event.setIsPublished(false);
        eventRepository.save(event);
    }

    @Override
    @Transactional
    public void deleteEvent(UUID eventId, UUID organizerId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Evento no encontrado: " + eventId));

        if (!event.getOrganizerId().equals(organizerId)) {
            throw new RuntimeException("No tienes permiso para eliminar este evento");
        }

        // Eliminar tickets asociados
        ticketRepository.findByEventId(eventId).forEach(ticketRepository::delete);

        // Eliminar evento
        eventRepository.delete(event);
    }

    @Override
    @Transactional
    public void deactivateEvent(UUID eventId, UUID organizerId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Evento no encontrado: " + eventId));

        if (!event.getOrganizerId().equals(organizerId)) {
            throw new RuntimeException("No tienes permiso para desactivar este evento");
        }

        event.setIsActive(false);
        eventRepository.save(event);
    }

    private EventResponse mapEventToResponse(Event event) {
        List<Ticket> tickets = ticketRepository.findByEventId(event.getId());
        List<EventResponse.TicketResponse> ticketResponses = tickets.stream()
                .map(t -> new EventResponse.TicketResponse(
                        t.getId(),
                        t.getEventId(),
                        t.getType(),
                        t.getDescription(),
                        t.getPrice(),
                        t.getCapacity(),
                        t.getSoldCount(),
                        t.getIsActive()
                ))
                .collect(Collectors.toList());

        EventResponse response = new EventResponse();
        response.setId(event.getId());
        response.setOrganizerId(event.getOrganizerId());
        response.setName(event.getName());
        response.setDescription(event.getDescription());
        response.setCategory(event.getCategory());
        response.setEventDateTime(event.getEventDateTime());
        response.setBannerUrl(event.getBannerUrl());
        response.setThumbnailUrl(event.getThumbnailUrl());
        response.setVenueName(event.getVenueName());
        response.setAddress(event.getAddress());
        response.setLatitude(event.getLatitude());
        response.setLongitude(event.getLongitude());
        response.setMapsEmbedUrl(event.getMapsEmbedUrl());
        response.setMapsLinkUrl(event.getMapsLinkUrl());
        response.setArtistName(event.getArtistName());
        response.setSpotifyUrl(event.getSpotifyUrl());
        response.setInstagramUrl(event.getInstagramUrl());
        response.setTwitterUrl(event.getTwitterUrl());
        response.setIsPublished(event.getIsPublished());
        response.setIsActive(event.getIsActive());
        response.setCreatedAt(event.getCreatedAt());
        response.setUpdatedAt(event.getUpdatedAt());
        response.setTickets(ticketResponses);

        return response;
    }
}