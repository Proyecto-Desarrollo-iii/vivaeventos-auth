package co.empresa.vivaeventos.events.domain.service;

import co.empresa.vivaeventos.events.domain.model.Dto.CreateEventRequest;
import co.empresa.vivaeventos.events.domain.model.Dto.EventResponse;
import co.empresa.vivaeventos.events.domain.model.Dto.UpdateEventRequest;

import java.util.List;
import java.util.UUID;

public interface IEventService {

    EventResponse createEvent(UUID organizerId, CreateEventRequest request);

    EventResponse getEventById(UUID eventId);

    List<EventResponse> getEventsByOrganizer(UUID organizerId);

    List<EventResponse> getPublishedEvents();

    List<EventResponse> getPublishedEventsByCategory(String category);

    List<EventResponse> getUpcomingEvents();

    List<EventResponse> getUpcomingEventsByCategory(String category);

    EventResponse updateEvent(UUID eventId, UUID organizerId, UpdateEventRequest request);

    void publishEvent(UUID eventId, UUID organizerId);

    void unpublishEvent(UUID eventId, UUID organizerId);

    void deleteEvent(UUID eventId, UUID organizerId);

    void deactivateEvent(UUID eventId, UUID organizerId);
}