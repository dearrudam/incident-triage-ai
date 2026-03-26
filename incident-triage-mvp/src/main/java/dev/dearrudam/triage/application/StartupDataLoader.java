package dev.dearrudam.triage.application;

import dev.dearrudam.triage.adapter.out.db.HistoricalIncidentRepository;
import dev.dearrudam.triage.domain.model.HistoricalIncident;
import dev.dearrudam.triage.domain.port.EmbeddingGeneratorPort;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.UUID;

/**
 * Seeds a minimal set of historical incidents at startup if the table is empty.
 * This gives the vector search something to work against from the first
 * request.
 */
@ApplicationScoped
public class StartupDataLoader {

    private static final Logger LOG = Logger.getLogger(StartupDataLoader.class);

    @Inject
    EmbeddingGeneratorPort embeddingGeneratorPort;

    @Inject
    HistoricalIncidentRepository repository;

    void onStartup(@Observes StartupEvent event) {
        if (repository.count() > 0) {
            LOG.info("Historical incidents already seeded — skipping.");
            return;
        }
        LOG.info("Seeding historical incidents...");
        SEED_DATA.forEach(this::seed);
        LOG.infof("Seeded %d historical incidents.", SEED_DATA.size());
    }

    private void seed(SeedRecord s) {
        String text = s.title() + " " + s.description();
        List<Float> embedding = embeddingGeneratorPort.generate(text);
        HistoricalIncident incident = new HistoricalIncident(
                UUID.randomUUID(), s.title(), s.description(),
                s.priority(), s.category(), s.resolvedBy(), embedding);
        repository.save(incident);
    }

    private record SeedRecord(String title, String description,
            String priority, String category, String resolvedBy) {
    }

    private static final List<SeedRecord> SEED_DATA = List.of(
            new SeedRecord(
                    "Database connection pool exhausted",
                    "The application is unable to acquire new database connections. All connections in the pool are in use. Response times have degraded significantly.",
                    "HIGH", "Database", "DBA Team"),
            new SeedRecord(
                    "Network switch unresponsive in data center rack 3",
                    "Multiple servers in rack 3 have lost network connectivity. The top-of-rack switch is not responding to pings. Approximately 12 hosts affected.",
                    "CRITICAL", "Network", "Network Ops"),
            new SeedRecord(
                    "Memory leak in order processing service",
                    "The order-processor microservice memory usage grows continuously and causes OOM restarts every 6 hours. Heap dumps indicate retained HttpClient instances.",
                    "MEDIUM", "Application", "App Support"),
            new SeedRecord(
                    "SSL certificate expiring in 3 days for api.example.com",
                    "The TLS certificate for the public API endpoint will expire in 72 hours. Clients will receive certificate warnings if not renewed.",
                    "HIGH", "Security", "Platform Team"),
            new SeedRecord(
                    "Disk space critical on primary PostgreSQL host",
                    "The primary database server /var/lib/postgresql partition is at 94% capacity. Write operations will fail if disk reaches 100%.",
                    "CRITICAL", "Infrastructure", "DBA Team"),
            new SeedRecord(
                    "Payment service returning 500 errors for Visa transactions",
                    "Since 14:30 UTC, all Visa card payment attempts return HTTP 500. Other card types are unaffected. The payment gateway logs show a serialization error.",
                    "CRITICAL", "Application", "Payments Team"),
            new SeedRecord(
                    "Slow query degrading reporting dashboard performance",
                    "The monthly sales report dashboard takes over 2 minutes to load. EXPLAIN ANALYZE shows a sequential scan on the orders table (8M rows) with no index.",
                    "MEDIUM", "Database", "DBA Team"));
}
