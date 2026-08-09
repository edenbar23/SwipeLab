package com.swipelab.common.dto.response;

import lombok.Data;
import java.util.List;

@Data
public class PlayTaskResponse {
    private Long taskId;
    private List<SpeciesRefDto> species;
    private List<ImageToClassifyDto> images;

    @Data
    public static class SpeciesRefDto {
        private String scientificName;
        private String commonName;
        private List<RefImageDto> referenceImages;
    }

    @Data
    public static class RefImageDto {
        private String imageUrl;
        private String caption;
    }

    @Data
    public static class ImageToClassifyDto {
        private Long imageId;
        private String imageBuffer; // Base64
        private String contentType;
        private String question;
        private Long taskId;
        private String species;
    }
}
