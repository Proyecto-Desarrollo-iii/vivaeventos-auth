package co.empresa.vivaeventos.events.domain.model.Dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class UpdateEventRequest {

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
    private List<CreateEventRequest.TicketRequest> tickets;
    private String artistName;
    private String spotifyUrl;
    private String instagramUrl;
    private String twitterUrl;
    private Boolean isPublished;
}