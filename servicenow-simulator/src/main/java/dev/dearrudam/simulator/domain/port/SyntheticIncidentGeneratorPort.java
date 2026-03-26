package dev.dearrudam.simulator.domain.port;

import dev.dearrudam.simulator.domain.model.SimulatedIncident;

import java.util.List;

/**
 * Port for generating synthetic incidents using an LLM.
 * The domain layer depends only on this interface — no LangChain4j imports
 * here.
 */
public interface SyntheticIncidentGeneratorPort {

    /**
     * Generates a list of synthetic incidents for the given domain.
     *
     * @param count  the number of incidents to generate
     * @param domain the IT domain to generate incidents for (e.g., "database",
     *               "network")
     * @return a list of generated incidents
     */
    List<SimulatedIncident> generate(int count, String domain);
}
