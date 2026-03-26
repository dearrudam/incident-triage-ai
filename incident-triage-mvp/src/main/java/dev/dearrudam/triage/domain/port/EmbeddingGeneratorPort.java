package dev.dearrudam.triage.domain.port;

import java.util.List;

/**
 * Port for generating vector embeddings from text.
 * The domain layer depends only on this interface — no LangChain4j imports
 * here.
 */
public interface EmbeddingGeneratorPort {

    /**
     * Generates a vector embedding for the given text.
     *
     * @param text the input text to embed
     * @return a list of floats representing the embedding vector
     */
    List<Float> generate(String text);
}
