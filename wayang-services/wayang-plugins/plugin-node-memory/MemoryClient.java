@ApplicationScoped
public class MemoryClient {

    @Inject
    @RestClient
    MemoryService memoryService;

    @Inject
    ModelRouterClient modelRouter;

    /**
     * Write memory entry
     */
    public Uni<String> write(Memory memory) {
        return Uni.createFrom().item(() -> {
            // Generate embedding for semantic memory
            if(memory.getType() == MemoryType.SEMANTIC) {
            var content = extractTextContent(memory.getContent());
            return modelRouter.embed(EmbedRequest.builder()
                .texts(List.of(content))
                .build());
        }
        return null;
    })
        .flatMap(embedResponse -> {
    var request = new MemoryWriteRequest();
    request.setType(memory.getType().name().toLowerCase());
    request.setContent(memory.getContent());
    request.setTtl(memory.getTtl().toString());
    request.setRunId(memory.getRunId());
    request.setTenantId(memory.getTenantId());
    request.setMetadata(memory.getMetadata());

    if(embedResponse != null) {
    request.setEmbedding(embedResponse.getEmbeddings().get(0));
}

return memoryService.writeMemory(request);
        })
        .map(response -> response.getMemoryId());
    }

    /**
     * Query memories
     */
    public Uni < List < Memory >> query(MemoryQuery query) {
    return modelRouter.embed(EmbedRequest.builder()
        .texts(List.of(query.getQuery()))
        .build())
        .flatMap(embedResponse -> {
            var request = new MemoryQueryRequest();
            request.setEmbedding(embedResponse.getEmbeddings().get(0));
            request.setTopK(query.getTopK());
            request.setTenantId(query.getTenantId());

            if(query.getTypes() != null) {
        request.setTypes(query.getTypes().stream()
            .map(t -> t.name().toLowerCase())
            .collect(Collectors.toList()));
    }

    return memoryService.queryMemory(request);
})
        .map(response -> response.getMemories().stream()
    .map(this:: toMemory)
    .collect(Collectors.toList()));
    }

    /**
     * Consolidate memories (periodic cleanup)
     */
    public Uni < Void > consolidate(String tenantId) {
    return memoryService.consolidateMemories(tenantId)
        .replaceWithVoid();
}
    
    private String extractTextContent(Object content) {
    if (content instanceof String) {
        return (String) content;
    }
    if (content instanceof Map) {
        // Extract text from structured content
        var map = (Map < String, Object >) content;
        return map.getOrDefault("text", content.toString()).toString();
    }
    return content.toString();
}
    
    private Memory toMemory(MemoryDto dto) {
    return Memory.builder()
        .id(dto.getId())
        .type(MemoryType.valueOf(dto.getType().toUpperCase()))
        .content(dto.getContent())
        .runId(dto.getRunId())
        .tenantId(dto.getTenantId())
        .metadata(dto.getMetadata())
        .createdAt(dto.getCreatedAt())
        .build();
}
}