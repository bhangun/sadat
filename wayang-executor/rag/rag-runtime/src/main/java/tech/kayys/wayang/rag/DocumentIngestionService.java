package tech.kayys.silat.executor.rag.examples;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.silat.client.SilatClient;
import tech.kayys.silat.executor.rag.domain.*;
import tech.kayys.silat.executor.rag.langchain.*;

import java.nio.file.Path;
import java.util.*;

/**
 * Service for ingesting documents into the vector store using LangChain4j
 */
@ApplicationScoped
public class DocumentIngestionService {

    private static final Logger LOG = LoggerFactory.getLogger(DocumentIngestionService.class);

    @Inject
    LangChain4jModelFactory modelFactory;

    @Inject
    LangChain4jEmbeddingStoreFactory storeFactory;

    @Inject
    LangChain4jConfig config;

    /**
     * Ingest PDF documents into vector store
     */
    public Uni<IngestResult> ingestPdfDocuments(
            String tenantId,
            List<Path> pdfPaths,
            Map<String, String> metadata) {

        LOG.info("Ingesting {} PDF documents for tenant: {}", pdfPaths.size(), tenantId);

        return Uni.createFrom().item(() -> {
            long startTime = System.currentTimeMillis();

            // 1. Load documents
            ApachePdfBoxDocumentParser parser = new ApachePdfBoxDocumentParser();
            List<Document> documents = new ArrayList<>();

            for (Path path : pdfPaths) {
                Document doc = FileSystemDocumentLoader.loadDocument(path, parser);

                // Add metadata
                doc.metadata().put("tenantId", tenantId);
                doc.metadata().put("source", path.getFileName().toString());
                doc.metadata().put("collection", metadata.getOrDefault("collection", "default"));
                metadata.forEach((k, v) -> doc.metadata().put(k, v));

                documents.add(doc);
            }

            // 2. Split documents into chunks
            DocumentSplitter splitter = DocumentSplitters.recursive(
                512,  // maxChunkSize
                50    // maxOverlapSize
            );

            List<TextSegment> segments = new ArrayList<>();
            for (Document doc : documents) {
                segments.addAll(splitter.split(doc));
            }

            LOG.info("Split {} documents into {} segments", documents.size(), segments.size());

            // 3. Get embedding store and model
            EmbeddingStore<TextSegment> embeddingStore =
                storeFactory.getStore(tenantId, RetrievalConfig.defaults());

            EmbeddingModel embeddingModel =
                modelFactory.createEmbeddingModel(tenantId, "text-embedding-3-small");

            // 4. Ingest segments
            EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .build();

            ingestor.ingest(segments);

            long duration = System.currentTimeMillis() - startTime;

            LOG.info("Successfully ingested {} segments in {}ms", segments.size(), duration);

            return new IngestResult(
                true,
                documents.size(),
                segments.size(),
                duration,
                "Successfully ingested documents"
            );
        });
    }

    /**
     * Ingest text documents with custom chunking
     */
    public Uni<IngestResult> ingestTextDocuments(
            String tenantId,
            List<String> texts,
            Map<String, String> metadata,
            ChunkingConfig chunkingConfig) {

        LOG.info("Ingesting {} text documents for tenant: {}", texts.size(), tenantId);

        return Uni.createFrom().item(() -> {
            long startTime = System.currentTimeMillis();

            // 1. Create documents
            List<Document> documents = texts.stream()
                .map(text -> {
                    Document doc = Document.from(text);
                    doc.metadata().put("tenantId", tenantId);
                    metadata.forEach((k, v) -> doc.metadata().put(k, v));
                    return doc;
                })
                .toList();

            // 2. Create splitter based on strategy
            DocumentSplitter splitter = createSplitter(chunkingConfig);

            List<TextSegment> segments = new ArrayList<>();
            for (Document doc : documents) {
                segments.addAll(splitter.split(doc));
            }

            // 3. Ingest
            EmbeddingStore<TextSegment> embeddingStore =
                storeFactory.getStore(tenantId, RetrievalConfig.defaults());

            EmbeddingModel embeddingModel =
                modelFactory.createEmbeddingModel(tenantId, "text-embedding-3-small");

            EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .build();

            ingestor.ingest(segments);

            long duration = System.currentTimeMillis() - startTime;

            return new IngestResult(
                true,
                documents.size(),
                segments.size(),
                duration,
                "Successfully ingested text documents"
            );
        });
    }

    /**
     * Batch ingest from multiple sources
     */
    public Uni<IngestResult> batchIngest(
            String tenantId,
            List<DocumentSource> sources) {

        LOG.info("Batch ingesting {} sources for tenant: {}", sources.size(), tenantId);

        List<Uni<IngestResult>> unis = sources.stream()
            .map(source -> {
                return switch (source.type()) {
                    case PDF -> ingestPdfDocuments(
                        tenantId,
                        List.of(Path.of(source.path())),
                        source.metadata()
                    );
                    case TEXT -> ingestTextDocuments(
                        tenantId,
                        List.of(source.content()),
                        source.metadata(),
                        ChunkingConfig.defaults()
                    );
                    case URL -> ingestFromUrl(tenantId, source.path(), source.metadata());
                };
            })
            .toList();

        return Uni.combine().all().unis(unis).combinedWith(results -> {
            int totalDocs = 0;
            int totalSegments = 0;
            long totalDuration = 0;

            for (Object result : results) {
                IngestResult r = (IngestResult) result;
                totalDocs += r.documentsIngested();
                totalSegments += r.segmentsCreated();
                totalDuration += r.durationMs();
            }

            return new IngestResult(
                true,
                totalDocs,
                totalSegments,
                totalDuration,
                "Batch ingestion completed"
            );
        });
    }

    private Uni<IngestResult> ingestFromUrl(
            String tenantId,
            String url,
            Map<String, String> metadata) {

        // TODO: Implement URL scraping and ingestion
        return Uni.createFrom().item(new IngestResult(
            true, 0, 0, 0, "URL ingestion not implemented yet"
        ));
    }

    private DocumentSplitter createSplitter(ChunkingConfig config) {
        return switch (config.strategy()) {
            case RECURSIVE -> DocumentSplitters.recursive(
                config.chunkSize(),
                config.chunkOverlap()
            );
            case SENTENCE -> DocumentSplitters.recursive(
                config.chunkSize(),
                config.chunkOverlap(),
                new dev.langchain4j.model.chat.ChatLanguageModel[0]
            );
            default -> DocumentSplitters.recursive(
                config.chunkSize(),
                config.chunkOverlap()
            );
        };
    }
}