package com.swipelab.integration.stardbi;

import com.swipelab.integration.stardbi.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Base64;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@Service
@Profile("e2e")
public class MockStardbiClient implements StardbiClientPort {

    @Override
    public StardbiAuthResponseDto login(StardbiAuthRequestDto request) {
        log.info("Mock Stardbi client login for user: {}", request.getUsername());
        StardbiAuthResponseDto response = new StardbiAuthResponseDto();
        response.setAccess("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ0b2tlbl90eXBlIjoiYWNjZXNzIiwiZXhwIjoxNzgxMjc3MzU2LCJpYXQiOjE3ODEyNzY3NTYsImp0aSI6IjQ1MjNlNzRjNGYzNTQ4ODBhY2Q0OGQxODNjNjY1MTVmIiwidXNlcl9pZCI6IjQ2In0.xYqdXSXfzuilZhz6Ln84_VIwO0iJfDQVA49KDK8zmrA");
        response.setRefresh("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ0b2tlbl90eXBlIjoicmVmcmVzaCIsImV4cCI6MTc4MTM2MzE1NiwiaWF0IjoxNzgxMjc2NzU2LCJqdGkiOiI4YjIwYmJhZjVhNjE0NmNjYmEwZmY1YmYxNDNjNmIwOSIsInVzZXJfaWQiOiI0NiJ9.jpZoEhd_VIje63sEpkGSQxncBtztwYMMXcL7i5PdOS4");
        response.setLifetime(600);
        response.setId(46L);
        response.setUsername(request.getUsername() != null ? request.getUsername() : "swipe_lab_test_user");
        response.setFirstName("swipe_lab");
        response.setLastName("test_user");
        response.setEmail("");
        return response;
    }

    @Override
    public boolean checkAuth(String accessToken) {
        log.info("Mock Stardbi client checkAuth");
        return true;
    }

    @Override
    public StardbiAuthResponseDto refreshToken(StardbiRefreshTokenRequestDto request) {
        log.info("Mock Stardbi client refreshToken");
        StardbiAuthResponseDto response = new StardbiAuthResponseDto();
        response.setAccess("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ0b2tlbl90eXBlIjoiYWNjZXNzIiwiZXhwIjoxNzgxMjc3MzU2LCJpYXQiOjE3ODEyNzY3NTYsImp0aSI6IjQ1MjNlNzRjNGYzNTQ4ODBhY2Q0OGQxODNjNjY1MTVmIiwidXNlcl9pZCI6IjQ2In0.xYqdXSXfzuilZhz6Ln84_VIwO0iJfDQVA49KDK8zmrA");
        response.setRefresh(request.getRefresh());
        return response;
    }

    @Override
    public void logout(StardbiLogoutRequestDto request, String accessToken) {
        log.info("Mock Stardbi client logout");
    }

    @Override
    public List<ExternalExperimentDto> getExperiments(String accessToken) {
        log.info("Mock Stardbi client getExperiments (with token)");
        return getMockExperiments();
    }

    @Override
    public List<ExternalExperimentDto> getExperiments() {
        log.info("Mock Stardbi client getExperiments");
        return getMockExperiments();
    }

    private List<ExternalExperimentDto> getMockExperiments() {
        ExternalExperimentDto exp1 = ExternalExperimentDto.builder()
                .id(1L)
                .name("Mock Experiment 1")
                .notes("Automatically synced by seeder on startup")
                .startDate("2025-01-01")
                .emdDate("2025-12-31")
                .build();
                
        ExternalExperimentDto exp2 = ExternalExperimentDto.builder()
                .id(2L)
                .name("Mock Experiment 2")
                .notes("Available for testing Add Task flows")
                .startDate("2026-01-01")
                .emdDate("2026-12-31")
                .build();
                
        return List.of(exp1, exp2);
    }

    @Override
    public List<ExternalCropDto> getUnclassifiedImageIds(Long experimentId) {
        log.info("Mock Stardbi client getUnclassifiedImageIds for exp: {}", experimentId);
        List<ExternalCropDto> crops = new ArrayList<>();
        java.io.File folder = new java.io.File("src/main/resources/e2e-crops");
        if (folder.exists() && folder.isDirectory()) {
            java.io.File[] files = folder.listFiles();
            if (files != null) {
                for (java.io.File file : files) {
                    if (file.isFile() && (file.getName().endsWith(".jpg") || file.getName().endsWith(".png"))) {
                        try {
                            String nameWithoutExt = file.getName().substring(0, file.getName().lastIndexOf('.'));
                            String[] parts = nameWithoutExt.split("_");
                            if (parts.length >= 3) {
                                Long boxId = Long.parseLong(parts[parts.length - 1]);
                                Long parentImageId = Long.parseLong(String.join("", java.util.Arrays.copyOfRange(parts, 1, parts.length - 1)));
                                
                                // Shift the boxId to guarantee global uniqueness across different mock experiments
                                Long uniqueBoxId = boxId + (experimentId * 1000);
                                
                                crops.add(ExternalCropDto.builder()
                                        .boxId(uniqueBoxId)
                                        .imageId(parentImageId)
                                        .speciesId(null)
                                        .build());
                            }
                        } catch (Exception e) {
                            log.warn("Could not parse image name: {}", file.getName());
                        }
                    }
                }
            }
        }
        
        // Fallback
        if (crops.isEmpty()) {
            for (long i = 1; i <= 5; i++) {
                crops.add(ExternalCropDto.builder()
                        .boxId(i * 10)
                        .imageId(i * 100)
                        .speciesId(null)
                        .build());
            }
        }
        return crops;
    }

    private static final String DUMMY_PNG_B64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==";

    @Override
    public byte[] getImageBuffer(Long boxId) {
        log.info("Mock Stardbi client getImageBuffer for boxId: {}", boxId);
        // Returning a 1x1 transparent valid PNG byte array
        return Base64.getDecoder().decode(DUMMY_PNG_B64);
    }

    @Override
    public byte[] downloadExperimentCropsZip(Long experimentId, String accessToken) {
        log.info("Mock Stardbi client downloadExperimentCropsZip for exp: {}", experimentId);
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos)) {
            
            java.io.File folder = new java.io.File("src/main/resources/e2e-crops");
            boolean filesAdded = false;
            if (folder.exists() && folder.isDirectory()) {
                java.io.File[] files = folder.listFiles();
                if (files != null) {
                    for (java.io.File file : files) {
                        if (file.isFile() && (file.getName().endsWith(".jpg") || file.getName().endsWith(".png"))) {
                            try {
                                String nameWithoutExt = file.getName().substring(0, file.getName().lastIndexOf('.'));
                                String[] parts = nameWithoutExt.split("_");
                                if (parts.length >= 3) {
                                    Long boxId = Long.parseLong(parts[parts.length - 1]);
                                    Long parentImageId = Long.parseLong(String.join("", java.util.Arrays.copyOfRange(parts, 1, parts.length - 1)));
                                    
                                    // Shift the boxId with a random value to guarantee global uniqueness every time the ZIP is downloaded
                                    long randomShift = (long)(Math.random() * 1000000);
                                    Long uniqueBoxId = boxId + (experimentId * 1000) + randomShift;
                                    
                                    String ext = file.getName().substring(file.getName().lastIndexOf('.'));
                                    ZipEntry entry = new ZipEntry(parentImageId + "_" + uniqueBoxId + ext);
                                    zos.putNextEntry(entry);
                                    zos.write(java.nio.file.Files.readAllBytes(file.toPath()));
                                    zos.closeEntry();
                                    filesAdded = true;
                                }
                            } catch (Exception e) {
                                log.warn("Could not parse image name for zip: {}", file.getName());
                            }
                        }
                    }
                }
            }

            if (!filesAdded) {
                for (long i = 1; i <= 5; i++) {
                    ZipEntry entry = new ZipEntry((i * 100) + "_" + (i * 10) + ".png");
                    zos.putNextEntry(entry);
                    zos.write(getImageBuffer(i * 10));
                    zos.closeEntry();
                }
            }
            zos.finish();
            return baos.toByteArray();
        } catch (IOException e) {
            log.error("Error generating mock zip", e);
            return new byte[0];
        }
    }

    @Override
    public List<ExternalTaxonomyDto> getTaxonomy() {
        log.info("Mock Stardbi client getTaxonomy");
        // returning subset of species provided
        return List.of(
                ExternalTaxonomyDto.builder().speciesId(null).clazz("insecta").order("hemiptera").family("thaumastocoridae").genus("thaumastocoris").species("thaumastocoris peregrinus").build(),
                ExternalTaxonomyDto.builder().speciesId(null).clazz("insecta").order("hemiptera").family("thaumastocoridae").genus("thaumastocoris").species("thaumastocoris peregrinos").build(),
                ExternalTaxonomyDto.builder().speciesId(null).clazz("insecta").order("hemiptera").family("thaumastocoridae").genus("thaumastocoris").species("thaumastocoris sp.").build(),
                ExternalTaxonomyDto.builder().speciesId(null).clazz("insecta").order("hemiptera").family("thaumastocoridae").genus("thaumastocoridae sp.").species("thaumastocoridae sp.").build(),
                ExternalTaxonomyDto.builder().speciesId(null).clazz("insecta").order("hemiptera").family("aphalaridae").genus("glycaspis").species("glycaspis brimblecombei").build()
        );
    }

    @Override
    public void postLabel(ExternalLabelDto label) {
        log.info("Mock Stardbi client postLabel: {}", label);
    }
}
