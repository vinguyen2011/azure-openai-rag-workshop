package ai.azure.openai.rag.workshop.backend.rest;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.content.injector.DefaultContentInjector;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.router.DefaultQueryRouter;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import dev.langchain4j.data.embedding.Embedding;

@Path("/chat")
public class ChatResource {
  private static final Logger log = LoggerFactory.getLogger(ChatResource.class);

  private static final String SYSTEM_MESSAGE_PROMPT = """
    Assistant helps the ING company customers with support questions regarding terms of service, privacy policy, and questions about investments.
    Be brief in your answers.
    If asking a clarifying question to the user would help, ask the question.
    For tabular information return it as an html table.
    Do not return markdown format.
    If the question is not in English, answer in the language used in the question.
    Each source has a name followed by colon and the actual information, always include the source name for each fact you use in the response.
    Use square brackets to reference the source, for example: [info1.txt].
    Don't combine sources, list each source separately, for example: [info1.txt][info2.pdf].
    Here is the question: {{userMessage}}

    Answer with the help of this information:
    {{contents}}
    """;

  private static final String SYSTEM_MESSAGE_PROMPT_STRICT = """
    Assistant helps the ING company customers with support questions regarding terms of service, privacy policy, and questions about investments.
    Be brief in your answers.
    Answer ONLY with the facts listed in the list of sources below.
    If there isn't enough information below, say you don't know.
    Do not generate answers that don't use the sources below.
    If asking a clarifying question to the user would help, ask the question.
    For tabular information return it as an html table.
    Do not return markdown format.
    If the question is not in English, answer in the language used in the question.
    Each source has a name followed by colon and the actual information, always include the source name for each fact you use in the response.
    Use square brackets to reference the source, for example: [info1.txt].
    Don't combine sources, list each source separately, for example: [info1.txt][info2.pdf].
    Here is the question: {{userMessage}}

    Answer using the following information:
    {{contents}}
    """;

  @Inject
  EmbeddingModel embeddingModel;

  @Inject
  EmbeddingStore<TextSegment> embeddingStore;

  @Inject
  ChatLanguageModel chatLanguageModel;

  interface LlmExpert {
    String ask(String question);
  }

  @POST
  @Consumes({"application/json"})
  @Produces({"application/json"})
  public ChatResponse chat(ChatRequest chatRequest) {
    log.info("Receiving message: {}", chatRequest.messages);
    // Embed the question (convert the user's question into vectors that represent the meaning)
    String question = chatRequest.messages.get(chatRequest.messages.size() - 1).content;

    log.info("### Embed the question (convert the question into vectors that represent the meaning) using embeddedQuestion model");
    Embedding embeddedQuestion = embeddingModel.embed(question).content();
    log.debug("# Vector length: {}", embeddedQuestion.vector().length);

    EmbeddingStoreContentRetriever retriever =
      new EmbeddingStoreContentRetriever(embeddingStore, embeddingModel);

    InstrumentService instrumentService = new InstrumentService();

    LlmExpert expert = AiServices.builder(LlmExpert.class)
      .chatLanguageModel(chatLanguageModel)
      .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
//      .contentRetriever(retriever)
      .retrievalAugmentor(DefaultRetrievalAugmentor.builder()
          .contentInjector(DefaultContentInjector.builder()
              .promptTemplate(PromptTemplate.from(SYSTEM_MESSAGE_PROMPT_STRICT))
              .build())
          .queryRouter(new DefaultQueryRouter(retriever))
          .build())
      .tools(instrumentService)
      .build();

    // Return the response
    return ChatResponse.fromMessage(expert.ask(question));
  }

  static class InstrumentService {
    @Tool("Get the instrument details for a name")
    Instrument getInstrument(@P("Instrument name to look up for") String name) {
      return new Instrument("NL1212121", name, true);
    }
  }

  record Instrument(String isin, String name, Boolean isSustainable) {}
}
