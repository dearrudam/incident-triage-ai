package dev.dearrudam.triage.adapter.out.ai;

import dev.dearrudam.triage.domain.port.EmbeddingGeneratorPort;
import dev.langchain4j.model.embedding.EmbeddingModel;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

/**
 * Outbound AI adapter that implements {@link EmbeddingGeneratorPort} using the
 * LangChain4j {@link EmbeddingModel} bean provided by the Quarkus Ollama
 * extension.
 *
 * <p>The actual model is configured via {@code application.properties}:
 * {@code quarkus.langchain4j.ollama.embedding-model.model-id}.
 */
@ApplicationScoped
public class LangChain4jEmbeddingGeneratorAdapter implements EmbeddingGeneratorPort {

    @Inject
    EmbeddingModel embeddingModel;

    /**
     * {@inheritDoc}
     *
     * <p>Delegates to the configured Ollama embedding model and returns the
     * resulting vector as a {@code List<Float>}.
     */
    @Override
    public List<Float> generate(String text) {
        return embeddingModel.embed(text).content().vectorAsList();
    }
}
