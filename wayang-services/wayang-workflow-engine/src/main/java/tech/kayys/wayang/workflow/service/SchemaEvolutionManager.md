package tech.kayys.wayang.workflow.service;

import java.util.ArrayList;
import java.util.List;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.schema.node.NodeDefinition;
import tech.kayys.wayang.schema.workflow.WorkflowDefinition;

/**
 * Schema Evolution Manager
 */
@ApplicationScoped
public class SchemaEvolutionManager {

        /**
         * Validate schema compatibility
         */
        public Uni<CompatibilityResult> checkCompatibility(
                        WorkflowDefinition oldVersion,
                        WorkflowDefinition newVersion) {
                List<BreakingChange> breakingChanges = detectBreakingChanges(
                                oldVersion,
                                newVersion);

                if (!breakingChanges.isEmpty()) {
                        return Uni.createFrom().item(
                                        CompatibilityResult.incompatible(breakingChanges));
                }

                return Uni.createFrom().item(CompatibilityResult.compatible());
        }

        /**
         * Automatic migration
         */
        public Uni<Void> migrateWorkflows(
                        String workflowId,
                        String fromVersion,
                        String toVersion) {
                return workflowRunRepository.findActiveByWorkflow(workflowId)
                                .onItem().transformToMulti(Multi.createFrom()::iterable)
                                .onItem().transformToUniAndConcatenate(run -> migrateRun(run, toVersion))
                                .toUni().replaceWithVoid();
        }

        private List<BreakingChange> detectBreakingChanges(
                        WorkflowDefinition old,
                        WorkflowDefinition newDef) {
                List<BreakingChange> changes = new ArrayList<>();

                // Check removed nodes
                old.nodes().stream()
                                .filter(node -> !newDef.hasNode(node.id()))
                                .forEach(node -> changes.add(
                                                BreakingChange.nodeRemoved(node.id())));

                // Check changed node signatures
                old.nodes().stream()
                                .filter(node -> newDef.hasNode(node.id()))
                                .forEach(node -> {
                                        NodeDefinition newNode = newDef.getNode(node.id());
                                        if (!areCompatible(node.inputs(), newNode.inputs())) {
                                                changes.add(
                                                                BreakingChange.inputsChanged(node.id()));
                                        }
                                });

                return changes;
        }
}