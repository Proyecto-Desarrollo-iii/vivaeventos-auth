package co.empresa.vivaeventos.events.domain.model.Dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CreateEventRequest {

    // INFORMACION BASICA
    private String name;
    private String description;
    private String category;
    private LocalDateTime eventDateTime;

    // MATERIAL VISUAL
    private String bannerUrl;
    private String thumbnailUrl;

    // UBICACION
    private String venueName;
    private String address;
    private Double latitude;
    private Double longitude;
    private String mapsEmbedUrl;
    private String mapsLinkUrl;

    // ENTRADAS
    private List<TicketRequest> tickets;

    // ARTISTA Y RRSS
    private String artistName;
    private String spotifyUrl;
    private String instagramUrl;
    private String twitterUrl;

    @Data
    public static class TicketRequest {
        private String type;
        private String description;
        private java.math.BigDecimal price;
        private Integer capacity;
    }
}