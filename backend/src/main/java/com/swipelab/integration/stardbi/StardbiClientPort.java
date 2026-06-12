package com.swipelab.integration.stardbi;

import com.swipelab.integration.stardbi.dto.*;

import java.util.List;

/**
 * Port for all interactions with the external StarDBI API.
 * Production implementation talks to the real StarDBI server via HTTP;
 * the E2E implementation returns deterministic mock data in-process.
 */
public interface StardbiClientPort {

    // ── Authentication ──────────────────────────────────────────────────────

    StardbiAuthResponseDto login(StardbiAuthRequestDto request);

    boolean checkAuth(String accessToken);

    StardbiAuthResponseDto refreshToken(StardbiRefreshTokenRequestDto request);

    void logout(StardbiLogoutRequestDto request, String accessToken);

    // ── Experiments ─────────────────────────────────────────────────────────

    List<ExternalExperimentDto> getExperiments(String accessToken);

    List<ExternalExperimentDto> getExperiments();

    // ── Images (Bounding Boxes / Crops) ─────────────────────────────────────

    List<ExternalCropDto> getUnclassifiedImageIds(Long experimentId);

    byte[] getImageBuffer(Long boxId);

    byte[] downloadExperimentCropsZip(Long experimentId, String accessToken);

    // ── Taxonomy ────────────────────────────────────────────────────────────

    List<ExternalTaxonomyDto> getTaxonomy();

    // ── Classification (Labels) ─────────────────────────────────────────────

    void postLabel(ExternalLabelDto label);
}
