package dev.dearrudam.triage.domain.model;

import java.util.List;
import java.util.UUID;

/**
 * Domain model for a historical incident stored in the database.
 * No framework dependencies — pure domain object.
 */
public record HistoricalIncident(
        UUID id,
        String title,
        String description,
        String priority,
        String category,
        String resolvedBy,
        List<Float> embedding) {
}
