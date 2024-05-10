package ai.azure.openai.rag.workshop.backend.configuration;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.vertexai.VertexAiGeminiChatModel;
import jakarta.enterprise.inject.Produces;

public class ChatLanguageModelProducer {
  @Produces
  public ChatLanguageModel chatLanguageModel() {
    ChatLanguageModel model = VertexAiGeminiChatModel.builder()
      .project(System.getenv("PROJECT_ID"))
      .location(System.getenv("LOCATION"))
      .modelName("gemini-1.0-pro")
      .temperature(0.1f)
      .build();
    return model;
  }
}

