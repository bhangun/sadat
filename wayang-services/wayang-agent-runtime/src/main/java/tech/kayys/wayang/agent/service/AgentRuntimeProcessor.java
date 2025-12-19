package tech.kayys.wayang.agent.service;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.vertx.http.deployment.HttpRootPathBuildItem;
import io.quarkus.vertx.http.deployment.RouteBuildItem;

import io.quarkus.ai.agent.runtime.engine.WorkflowRuntimeEngine;
import io.quarkus.ai.agent.runtime.executor.*;
import io.quarkus.ai.agent.runtime.executor.impl.*;
import io.quarkus.ai.agent.runtime.service.*;
import io.quarkus.ai.agent.runtime.service.provider.*;
import io.quarkus.ai.agent.runtime.resource.*;
import io.quarkus.ai.agent.runtime.repository.*;
import io.quarkus.ai.agent.runtime.context.*;
import io.quarkus.ai.agent.runtime.model.*;

/**
 * Quarkus Build Processor for AI Agent Runtime Extension
 */
public class AgentRuntimeProcessor {

    private static final String FEATURE = "ai-agent-runtime";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    /**
     * Register core beans for dependency injection
     */
    @BuildStep
    void registerBeans(BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        additionalBeans.produce(AdditionalBeanBuildItem.builder()
                .addBeanClasses(
                        // Engine
                        WorkflowRuntimeEngine.class,

                        // Executors
                        NodeExecutorRegistry.class,
                        StartNodeExecutor.class,
                        EndNodeExecutor.class,
                        LLMNodeExecutor.class,
                        ToolNodeExecutor.class,
                        ConditionNodeExecutor.class,
                        TransformNodeExecutor.class,
                        LoopNodeExecutor.class,

                        // Services
                        LLMService.class,
                        LLMProviderRegistry.class,
                        OpenAIProvider.class,
                        ToolService.class,
                        TransformService.class,

                        // Context
                        ExecutionContextManager.class,

                        // Repository
                        AgentRepository.class,

                        // Resources
                        AgentRuntimeResource.class,
                        HealthResource.class)
                .setUnremovable()
                .build());
    }

    /**
     * Register model classes for reflection (needed for JSON serialization)
     */
    @BuildStep
    void registerForReflection(BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {
        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(
                // Agent models
                AgentDefinition.class,
                AgentDefinition.Metadata.class,
                AgentDefinition.Personality.class,
                AgentDefinition.Personality.ExampleConversation.class,
                AgentDefinition.Capabilities.class,
                AgentDefinition.Capabilities.FileHandling.class,
                AgentDefinition.Safety.class,
                AgentDefinition.Analytics.class,
                AgentDefinition.Analytics.LoggingConfig.class,
                AgentDefinition.Deployment.class,
                AgentDefinition.Deployment.ScalingConfig.class,
                AgentDefinition.Deployment.Endpoint.class,
                AgentDefinition.Integration.class,
                AgentDefinition.Permissions.class,
                AgentDefinition.Permissions.Role.class,
                AgentDefinition.Permissions.UserPermission.class,

                // Workflow models
                Workflow.class,
                Workflow.Node.class,
                Workflow.Node.Position.class,
                Workflow.Node.NodeConfig.class,
                Workflow.Node.NodeConfig.PromptTemplate.class,
                Workflow.Node.NodeConfig.Condition.class,
                Workflow.Node.NodeConfig.LoopConfig.class,
                Workflow.Node.NodeConfig.ParallelConfig.class,
                Workflow.Node.NodeConfig.TransformConfig.class,
                Workflow.Node.NodeConfig.ValidationConfig.class,
                Workflow.Node.NodeConfig.HumanInputConfig.class,
                Workflow.Node.NodeInput.class,
                Workflow.Node.NodeOutput.class,
                Workflow.Node.ErrorHandling.class,
                Workflow.Edge.class,
                Workflow.Edge.EdgeCondition.class,
                Workflow.Trigger.class,

                // LLM and Tool models
                LLMConfig.class,
                LLMConfig.Parameters.class,
                LLMConfig.RetryConfig.class,
                Tool.class,
                Tool.ToolConfig.class,
                Tool.ToolConfig.Authentication.class,
                Tool.ToolConfig.RateLimit.class,
                Tool.ToolParameter.class,
                Variable.class,
                Variable.Validation.class,
                MemoryConfig.class,
                MemoryConfig.Retention.class,

                // Execution models
                WorkflowRuntimeEngine.ExecutionResult.class,
                WorkflowRuntimeEngine.NodeExecutionResult.class,
                WorkflowRuntimeEngine.ExecutionTrace.class,

                // Request/Response
                AgentRuntimeResource.ExecutionRequest.class,
                AgentRuntimeResource.ExecutionResponse.class)
                .methods()
                .fields()
                .build());
    }

    /**
     * Register enums for reflection
     */
    @BuildStep
    void registerEnums(BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {
        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(
                AgentDefinition.AgentType.class,
                AgentDefinition.AgentStatus.class,
                AgentDefinition.Personality.Tone.class,
                Workflow.Node.NodeType.class,
                Workflow.Edge.EdgeType.class,
                Workflow.Trigger.TriggerType.class,
                LLMConfig.Provider.class,
                Tool.ToolType.class,
                Variable.VariableType.class,
                Variable.Scope.class,
                MemoryConfig.MemoryType.class,
                MemoryConfig.StorageBackend.class)
                .methods()
                .fields()
                .build());
    }
}
