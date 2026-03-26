package dev.dearrudam.triage.adapter.out.db;

import dev.dearrudam.triage.domain.model.HistoricalIncident;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Persistence adapter for {@link HistoricalIncident}.
 *
 * <p>Implements vector similarity search using the pgvector {@code <=>} cosine
 * distance operator via a native SQL query.
 */
@ApplicationScoped
public class HistoricalIncidentRepository implements PanacheRepositoryBase<HistoricalIncidentEntity, UUID> {

    /**
     * Returns the top {@code limit} historical incidents ordered by cosine
     * similarity to the given embedding vector.
     *
     * <p>The cast {@code CAST(:embedding AS vector)} is required because JDBC
     * passes the parameter as a plain string; pgvector needs an explicit cast
     * to resolve the {@code <=>} operator.
     *
     * @param embedding the query embedding vector
     * @param limit     maximum number of results to return
     * @return domain objects ordered from most similar to least similar
     */
    @SuppressWarnings("unchecked")
    public List<HistoricalIncident> findSimilar(List<Float> embedding, int limit) {
        String vectorLiteral = toVectorLiteral(embedding);

        List<HistoricalIncidentEntity> entities = getEntityManager()
                .createNativeQuery(
                        "SELECT * FROM historical_incidents " +
                                "ORDER BY embedding <=> CAST(:embedding AS vector) " +
                                "LIMIT :limit",
                        HistoricalIncidentEntity.class)
                .setParameter("embedding", vectorLiteral)
                .setParameter("limit", limit)
                .getResultList();

        return entities.stream()
                .map(HistoricalIncidentEntity::toDomain)
                .collect(Collectors.toList());
    }

    /**
     * Persists a new historical incident.
     */
    @Transactional
    public void save(HistoricalIncident incident) {
        persist(HistoricalIncidentEntity.fromDomain(incident));
    }

    // --- helpers ---

    private static String toVectorLiteral(List<Float> embedding) {
        return embedding.stream()
                .map(Object::toString)
                .collect(Collectors.joining(",", "[", "]"));
    }
}
