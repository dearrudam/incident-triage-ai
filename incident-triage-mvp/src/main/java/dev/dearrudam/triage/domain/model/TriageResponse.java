package dev.dearrudam.triage.domain.model;

/**
 * Structured triage decision produced by the LLM.
 * No framework dependencies — pure domain object.
 */
public record TriageResponse(
        String priority,
        String category,
        String suggestedTeam,
        String rationale) {
}
