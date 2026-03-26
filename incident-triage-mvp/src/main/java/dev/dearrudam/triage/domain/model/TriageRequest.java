package dev.dearrudam.triage.domain.model;

/**
 * Incoming triage request from a client.
 * No framework dependencies — pure domain object.
 */
public record TriageRequest(String title, String description) {
}
