package co.empresa.vivaeventos.events.delivery.rest;

import co.empresa.vivaeventos.events.domain.model.Dto.CreateEventRequest;
import co.empresa.vivaeventos.events.domain.model.Dto.EventResponse;
import co.empresa.vivaeventos.events.domain.model.Dto.UpdateEventRequest;
import co.empresa.vivaeventos.events.domain.service.IEventService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/events")
public class EventController {

    private final IEventService eventService;

    public EventController(IEventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createEvent(
            Authentication authentication,
            @RequestBody CreateEventRequest request) {
        try {
            String organizerId = authentication.getName();
            UUID orgId = UUID.fromString(extractUserIdFromToken(authentication));

            EventResponse eventResponse = eventService.createEvent(orgId, request);

            Map<String, Object> response = new HashMap<>();
            response.put("mensaje", "Evento creado exitosamente");
            response.put("evento", eventResponse);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping("/{eventId}")
    public ResponseEntity<Map<String, Object>> getEvent(@PathVariable UUID eventId) {
        try {
            EventResponse eventResponse = eventService.getEventById(eventId);

            Map<String, Object> response = new HashMap<>();
            response.put("evento", eventResponse);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    @GetMapping("/organizer/{organizerId}")
    public ResponseEntity<Map<String, Object>> getEventsByOrganizer(@PathVariable UUID organizerId) {
        try {
            List<EventResponse> events = eventService.getEventsByOrganizer(organizerId);

            Map<String, Object> response = new HashMap<>();
            response.put("eventos", events);
            response.put("total", events.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getPublishedEvents(
            @RequestParam(required = false) String category) {
        try {
            List<EventResponse> events;

            if (category != null && !category.isEmpty()) {
                events = eventService.getPublishedEventsByCategory(category);
            } else {
                events = eventService.getPublishedEvents();
            }

            Map<String, Object> response = new HashMap<>();
            response.put("eventos", events);
            response.put("total", events.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping("/upcoming")
    public ResponseEntity<Map<String, Object>> getUpcomingEvents(
            @RequestParam(required = false) String category) {
        try {
            List<EventResponse> events;

            if (category != null && !category.isEmpty()) {
                events = eventService.getUpcomingEventsByCategory(category);
            } else {
                events = eventService.getUpcomingEvents();
            }

            Map<String, Object> response = new HashMap<>();
            response.put("eventos", events);
            response.put("total", events.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PutMapping("/{eventId}")
    public ResponseEntity<Map<String, Object>> updateEvent(
            Authentication authentication,
            @PathVariable UUID eventId,
            @RequestBody UpdateEventRequest request) {
        try {
            UUID organizerId = UUID.fromString(extractUserIdFromToken(authentication));

            EventResponse eventResponse = eventService.updateEvent(eventId, organizerId, request);

            Map<String, Object> response = new HashMap<>();
            response.put("mensaje", "Evento actualizado exitosamente");
            response.put("evento", eventResponse);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
        }
    }

    @PostMapping("/{eventId}/publish")
    public ResponseEntity<Map<String, Object>> publishEvent(
            Authentication authentication,
            @PathVariable UUID eventId) {
        try {
            UUID organizerId = UUID.fromString(extractUserIdFromToken(authentication));

            eventService.publishEvent(eventId, organizerId);

            Map<String, Object> response = new HashMap<>();
            response.put("mensaje", "Evento publicado exitosamente");

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
        }
    }

    @PostMapping("/{eventId}/unpublish")
    public ResponseEntity<Map<String, Object>> unpublishEvent(
            Authentication authentication,
            @PathVariable UUID eventId) {
        try {
            UUID organizerId = UUID.fromString(extractUserIdFromToken(authentication));

            eventService.unpublishEvent(eventId, organizerId);

            Map<String, Object> response = new HashMap<>();
            response.put("mensaje", "Evento despublicado exitosamente");

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
        }
    }

    @PostMapping("/{eventId}/deactivate")
    public ResponseEntity<Map<String, Object>> deactivateEvent(
            Authentication authentication,
            @PathVariable UUID eventId) {
        try {
            UUID organizerId = UUID.fromString(extractUserIdFromToken(authentication));

            eventService.deactivateEvent(eventId, organizerId);

            Map<String, Object> response = new HashMap<>();
            response.put("mensaje", "Evento desactivado exitosamente");

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
        }
    }

    @DeleteMapping("/{eventId}")
    public ResponseEntity<Map<String, Object>> deleteEvent(
            Authentication authentication,
            @PathVariable UUID eventId) {
        try {
            UUID organizerId = UUID.fromString(extractUserIdFromToken(authentication));

            eventService.deleteEvent(eventId, organizerId);

            Map<String, Object> response = new HashMap<>();
            response.put("mensaje", "Evento eliminado exitosamente");

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
        }
    }

    private String extractUserIdFromToken(Authentication authentication) {
        // Extraer el ID del usuario desde el token JWT
        // Este método debería ser implementado según tu JWT structure
        // Por ahora, usamos el nombre de usuario como fallback
        Object principal = authentication.getPrincipal();
        if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
            // Aquí deberías extraer el ID real del token
            // Esto es un placeholder
            return "00000000-0000-0000-0000-000000000000";
        }
        return "00000000-0000-0000-0000-000000000000";
    }
}