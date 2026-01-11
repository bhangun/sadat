package tech.kayys.silat.enhancements;

import io.smallrye.mutiny.Uni;
import java.util.Map;

/**
 * Enhancement: Workflow Documentation Generator
 */
interface WorkflowDocumentationService {

    /**
     * Generate documentation from workflow
     */
    Uni<Documentation> generateDocs(
        tech.kayys.silat.core.domain.WorkflowDefinitionId workflowId,
        DocumentationFormat format // MARKDOWN, HTML, PDF
    );

    /**
     * Generate API documentation
     */
    Uni<APIDocumentation> generateAPIDocs(
        tech.kayys.silat.core.domain.WorkflowDefinitionId workflowId,
        APIDocFormat format // OPENAPI, ASYNCAPI
    );

    /**
     * Generate visual diagrams
     */
    Uni<byte[]> generateDiagram(
        tech.kayys.silat.core.domain.WorkflowDefinitionId workflowId,
        DiagramType type // FLOWCHART, SEQUENCE, BPMN
    );
}