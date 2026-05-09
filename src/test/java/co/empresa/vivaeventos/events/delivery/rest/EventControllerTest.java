package co.empresa.vivaeventos.events.delivery.rest;

import co.empresa.vivaeventos.events.domain.model.Dto.CreateEventRequest;
import co.empresa.vivaeventos.events.domain.model.Dto.EventResponse;
import co.empresa.vivaeventos.events.domain.service.IEventService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = EventController.class,
        excludeAutoConfiguration = {org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class})
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IEventService eventService;

    @Test
    void shouldGetPublishedEvents() throws Exception {
        EventResponse event = new EventResponse();
        event.setId(UUID.randomUUID());
        event.setName("Test Event");
        event.setIsPublished(true);

        when(eventService.getPublishedEvents()).thenReturn(Collections.singletonList(event));

        mockMvc.perform(get("/api/v1/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.eventos[0].name").value("Test Event"));
    }

    @Test
    void shouldGetEventById() throws Exception {
        UUID eventId = UUID.randomUUID();
        EventResponse event = new EventResponse();
        event.setId(eventId);
        event.setName("Test Event");

        when(eventService.getEventById(eventId)).thenReturn(event);

        mockMvc.perform(get("/api/v1/events/" + eventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.evento.name").value("Test Event"));
    }

    @Test
    void shouldGetUpcomingEvents() throws Exception {
        EventResponse event = new EventResponse();
        event.setId(UUID.randomUUID());
        event.setName("Upcoming Event");
        event.setEventDateTime(LocalDateTime.now().plusDays(7));

        when(eventService.getUpcomingEvents()).thenReturn(Collections.singletonList(event));

        mockMvc.perform(get("/api/v1/events/upcoming"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1));
    }

    @Test
    void shouldFilterEventsByCategory() throws Exception {
        EventResponse event = new EventResponse();
        event.setId(UUID.randomUUID());
        event.setName("Music Event");
        event.setCategory("musica");

        when(eventService.getPublishedEventsByCategory("musica")).thenReturn(Collections.singletonList(event));

        mockMvc.perform(get("/api/v1/events")
                        .param("category", "musica"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.eventos[0].category").value("musica"));
    }
}