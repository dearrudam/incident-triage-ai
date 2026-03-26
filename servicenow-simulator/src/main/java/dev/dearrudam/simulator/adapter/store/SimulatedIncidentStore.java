package dev.dearrudam.simulator.adapter.store;

import dev.dearrudam.simulator.domain.model.SimulatedIncident;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * In-memory store for generated synthetic incidents.
 * Contents are replaced on each generate call and reset on application restart.
 */
@ApplicationScoped
public class SimulatedIncidentStore {

    private final AtomicReference<List<SimulatedIncident>> incidents = new AtomicReference<>(Collections.emptyList());

    public void replace(List<SimulatedIncident> newIncidents) {
        incidents.set(Collections.unmodifiableList(newIncidents));
    }

    public List<SimulatedIncident> getAll() {
        return incidents.get();
    }

    public int count() {
        return incidents.get().size();
    }
}
