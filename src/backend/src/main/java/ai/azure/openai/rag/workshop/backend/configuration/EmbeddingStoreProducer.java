package ai.azure.openai.rag.workshop.backend.configuration;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.net.URI;
import java.net.URISyntaxException;

public class EmbeddingStoreProducer {

  @ConfigProperty(name = "SEARCH_INDEX", defaultValue = "kbindex")
  String searchIndexName;

  @ConfigProperty(name = "QDRANT_URL", defaultValue = "http://localhost:6334")
  String qdrantUrl;

  @Produces
  public EmbeddingStore<TextSegment> embeddingStore() throws URISyntaxException {
    String qdrantHostname = new URI(qdrantUrl).getHost();
    int qdrantPort = new URI(qdrantUrl).getPort();
    return QdrantEmbeddingStore.builder()
      .collectionName(searchIndexName)
      .host(qdrantHostname)
      .port(qdrantPort)
      .build();
  }
}

