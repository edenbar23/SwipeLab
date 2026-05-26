package com.swipelab.classification.domain;

/**
 * Escalation levels emitted by FraudDetectionService.
 * STRIKE    – silent accumulation, no user-visible effect.
 * WARNING_1 – first user-visible warning, −5% credibility penalty.
 * WARNING_2 – final warning before ban, −15% credibility penalty.
 * BAN       – automated ban threshold crossed.
 */
public enum WarningLevel {
    STRIKE,
    WARNING_1,
    WARNING_2,
    BAN
}
