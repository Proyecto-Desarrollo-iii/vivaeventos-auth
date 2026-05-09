package co.empresa.vivaeventos.events.domain.model.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EventResponse {

    private UUID id;
    private UUID organizerId;
    private String name;
    private String description;
    private String category;
    private LocalDateTime eventDateTime;
    private String bannerUrl;
    private String thumbnailUrl;
    private String venueName;
    private String address;
    private Double latitude;
    private Double longitude;
    private String mapsEmbedUrl;
    private String mapsLinkUrl;
    private String artistName;
    private String spotifyUrl;
    private String instagramUrl;
    private String twitterUrl;
    private Boolean isPublished;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<TicketResponse> tickets;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TicketResponse {
        private UUID id;
        private UUID eventId;
        private String type;
        private String description;
        private BigDecimal price;
        private Integer capacity;
        private Integer soldCount;
        private Boolean isActive;
    }
}