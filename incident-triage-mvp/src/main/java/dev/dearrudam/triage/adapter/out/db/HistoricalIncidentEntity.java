package dev.dearrudam.triage.adapter.out.db;

import dev.dearrudam.triage.domain.model.HistoricalIncident;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Array;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.UUID;

/**
 * Hibernate entity mapping to the {@code historical_incidents} table.
 *
 * <p>The {@code embedding} column uses the pgvector {@code vector(768)} type.
 * The dimension (768) must match the configured embedding model.
 * <strong>WARNING:</strong> changing the embedding model requires dropping and
 * recreating this table because vector dimensions are fixed in the schema.
 */
@Entity
@Table(name = "historical_incidents")
public class HistoricalIncidentEntity extends PanacheEntityBase {

    @Id
    @Column(nullable = false, updatable = false)
    public UUID id;

    @Column(nullable = false)
    public String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    public String description;

    @Column(nullable = false)
    public String priority;

    @Column(nullable = false)
    public String category;

    @Column(name = "resolved_by", nullable = false)
    public String resolvedBy;

    /**
     * Stored as a pgvector {@code vector(768)} column.
     * Converted to/from {@code List<Float>} via {@link FloatListVectorConverter}.
     */
    @Column(nullable = false, columnDefinition = "vector(768)")
    @JdbcTypeCode(SqlTypes.VECTOR)
    @Array(length = 768)
    public List<Float> embedding;

    // --- Mapping helpers ---

    public static HistoricalIncidentEntity fromDomain(HistoricalIncident incident) {
        HistoricalIncidentEntity entity = new HistoricalIncidentEntity();
        entity.id = incident.id();
        entity.title = incident.title();
        entity.description = incident.description();
        entity.priority = incident.priority();
        entity.category = incident.category();
        entity.resolvedBy = incident.resolvedBy();
        entity.embedding = incident.embedding();
        return entity;
    }

    public HistoricalIncident toDomain() {
        return new HistoricalIncident(id, title, description, priority, category, resolvedBy, embedding);
    }
}
