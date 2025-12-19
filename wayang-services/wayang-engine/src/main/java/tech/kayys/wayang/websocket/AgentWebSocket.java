package tech.kayys.wayang.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import tech.kayys.wayang.agent.repository.AgentDefinitionRepository;

import tech.kayys.wayang.service.SchemaProcessor;
import tech.kayys.wayang.workflow.model.ExecutionContext;
import tech.kayys.wayang.workflow.model.WebSocketMessage;
import tech.kayys.wayang.workflow.service.ExecutionContextManager;
import tech.kayys.wayang.workflow.service.WorkflowRuntimeEngine;
import org.jboss.logging.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket endpoint for real-time agent/workflow management
 */
@ServerEndpoint("/ws/agents/{sessionId}")
@ApplicationScoped
public class AgentWebSocket {

    private static final Logger LOG = Logger.getLogger(AgentWebSocket.class);

    @Inject
    ObjectMapper objectMapper;

    @Inject
    SchemaProcessor schemaProcessor;

    @Inject
    AgentDefinitionRepository agentRepository;

    @Inject
    WorkflowRuntimeEngine workflowEngine;

    @Inject
    ExecutionContextManager contextManager;

    // Session management
    private static final Map<String, Set<Session>> sessions = new ConcurrentHashMap<>();
    private static final Map<String, AgentSession> agentSessions = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session, @PathParam("sessionId") String sessionId) {
        LOG.infof("WebSocket connected: %s, sessionId: %s", session.getId(), sessionId);

        sessions.computeIfAbsent(sessionId, k -> ConcurrentHashMap.newKeySet()).add(session);

        // Send welcome message
        sendMessage(session, new WebSocketMessage(
                "connected",
                Map.of(
                        "sessionId", sessionId,
                        "message", "Connected to AI Agent Runtime")));
    }

    @OnClose
    public void onClose(Session session, @PathParam("sessionId") String sessionId) {
        LOG.infof("WebSocket disconnected: %s", session.getId());

        Set<Session> sessionSet = sessions.get(sessionId);
        if (sessionSet != null) {
            sessionSet.remove(session);
            if (sessionSet.isEmpty()) {
                sessions.remove(sessionId);
            }
        }
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        LOG.errorf(throwable, "WebSocket error: %s", session.getId());
    }

    @OnMessage
    public void onMessage(String message, Session session, @PathParam("sessionId") String sessionId) {
        try {
            WebSocketMessage wsMessage = objectMapper.readValue(message, WebSocketMessage.class);

            LOG.debugf("Received message: type=%s, sessionId=%s", wsMessage.getType(), sessionId);

            handleMessage(wsMessage, session, sessionId);

        } catch (Exception e) {
            LOG.errorf(e, "Failed to process message");
            sendError(session, "Failed to process message: " + e.getMessage());
        }
    }

    private void handleMessage(WebSocketMessage message, Session session, String sessionId) {
        switch (message.getType()) {
            case "createAgent":
                handleCreateAgent(message, session, sessionId);
                break;
            case "updateAgent":
                handleUpdateAgent(message, session, sessionId);
                break;
            case "deleteAgent":
                handleDeleteAgent(message, session, sessionId);
                break;
            case "createWorkflow":
                handleCreateWorkflow(message, session, sessionId);
                break;
            case "updateWorkflow":
                handleUpdateWorkflow(message, session, sessionId);
                break;
            case "addNode":
                handleAddNode(message, session, sessionId);
                break;
            case "updateNode":
                handleUpdateNode(message, session, sessionId);
                break;
            case "deleteNode":
                handleDeleteNode(message, session, sessionId);
                break;
            case "addEdge":
                handleAddEdge(message, session, sessionId);
                break;
            case "deleteEdge":
                handleDeleteEdge(message, session, sessionId);
                break;
            case "validateAgent":
                handleValidateAgent(message, session, sessionId);
                break;
            case "executeWorkflow":
                handleExecuteWorkflow(message, session, sessionId);
                break;
            case "subscribeExecution":
                handleSubscribeExecution(message, session, sessionId);
                break;
            case "getAgent":
                handleGetAgent(message, session, sessionId);
                break;
            default:
                sendError(session, "Unknown message type: " + message.getType());
        }
    }

    /**
     * Create agent
     */
    private void handleCreateAgent(WebSocketMessage message, Session session, String sessionId) {
        try {
            String agentJson = objectMapper.writeValueAsString(message.getData());

            schemaProcessor.parseAgentDefinition(agentJson)
                    .chain(agent -> agentRepository.saveAgent(agent))
                    .subscribe().with(
                            agent -> {
                                // Store in session
                                agentSessions.put(sessionId, new AgentSession(agent.getId().getValue(), agent));

                                // Broadcast to all connected clients
                                broadcastToSession(sessionId, new WebSocketMessage(
                                        "agentCreated",
                                        Map.of("agent", agent)));
                            },
                            error -> sendError(session, "Failed to create agent: " + error.getMessage()));

        } catch (Exception e) {
            sendError(session, "Invalid agent data: " + e.getMessage());
        }
    }

    /**
     * Update agent
     */
    private void handleUpdateAgent(WebSocketMessage message, Session session, String sessionId) {
        try {

            Map<String, Object> data = (Map<String, Object>) message.getData();
            String agentId = (String) data.get("agentId");

            AgentSession agentSession = agentSessions.get(sessionId);
            if (agentSession == null || !agentSession.getAgentId().equals(agentId)) {
                sendError(session, "Agent not found in session");
                return;
            }

            // Apply updates
            AgentDefinition agent = agentSession.getAgent();
            applyAgentUpdates(agent, data);

            // Save to database
            agentRepository.updateAgent(agent)
                    .subscribe().with(
                            updated -> {
                                agentSession.setAgent(updated);

                                // Broadcast update
                                broadcastToSession(sessionId, new WebSocketMessage(
                                        "agentUpdated",
                                        Map.of("agent", updated)));
                            },
                            error -> sendError(session, "Failed to update agent: " + error.getMessage()));

        } catch (Exception e) {
            sendError(session, "Failed to update agent: " + e.getMessage());
        }
    }

    private void handleDeleteAgent(WebSocketMessage message, Session session, String sessionId) {
        sendError(session, "Delete agent not implemented");
    }

    /**
     * Create workflow
     */
    private void handleCreateWorkflow(WebSocketMessage message, Session session, String sessionId) {
        try {
            AgentSession agentSession = agentSessions.get(sessionId);
            if (agentSession == null) {
                sendError(session, "No active agent session");
                return;
            }

            Map<String, Object> data = (Map<String, Object>) message.getData();

            WorkflowDefinition workflow = objectMapper.convertValue(data.get("workflow"), WorkflowDefinition.class);

            AgentDefinition agent = agentSession.getAgent();
            if (agent.getWorkflows() == null) {
                agent.setWorkflows(new ArrayList<>());
            }
            agent.getWorkflows().add(workflow);

            agentRepository.updateAgent(agent)
                    .subscribe().with(
                            updated -> {
                                agentSession.setAgent(updated);
                                broadcastToSession(sessionId, new WebSocketMessage(
                                        "workflowCreated",
                                        Map.of("workflow", workflow)));
                            },
                            error -> sendError(session, "Failed to create workflow: " + error.getMessage()));

        } catch (Exception e) {
            sendError(session, "Failed to create workflow: " + e.getMessage());
        }
    }

    /**
     * Update workflow
     */
    private void handleUpdateWorkflow(WebSocketMessage message, Session session, String sessionId) {
        try {
            AgentSession agentSession = agentSessions.get(sessionId);
            if (agentSession == null) {
                sendError(session, "No active agent session");
                return;
            }

            Map<String, Object> data = (Map<String, Object>) message.getData();
            String workflowId = (String) data.get("workflowId");

            AgentDefinition agent = agentSession.getAgent();
            WorkflowDefinition workflow = agent.getWorkflows().stream()
                    .filter(w -> w.getId().equals(workflowId))
                    .findFirst()
                    .orElse(null);

            if (workflow == null) {
                sendError(session, "Workflow not found: " + workflowId);
                return;
            }

            // Apply updates
            applyWorkflowUpdates(workflow, data);

            // Validate
            schemaProcessor.validateAgentDefinition(
                    objectMapper.writeValueAsString(agent)).subscribe().with(
                            validation -> {
                                if (!validation.isValid()) {
                                    sendError(session, "Validation failed: " +
                                            String.join(", ", validation.getErrors()));
                                    return;
                                }

                                // Save
                                agentRepository.updateAgent(agent)
                                        .subscribe().with(
                                                updated -> {
                                                    agentSession.setAgent(updated);
                                                    broadcastToSession(sessionId, new WebSocketMessage(
                                                            "workflowUpdated",
                                                            Map.of("workflow", workflow)));
                                                },
                                                error -> sendError(session, "Failed to update: " + error.getMessage()));
                            },
                            error -> sendError(session, "Validation error: " + error.getMessage()));

        } catch (Exception e) {
            sendError(session, "Failed to update workflow: " + e.getMessage());
        }
    }

    /**
     * Add node to workflow
     */
    private void handleAddNode(WebSocketMessage message, Session session, String sessionId) {
        try {
            AgentSession agentSession = agentSessions.get(sessionId);
            if (agentSession == null) {
                sendError(session, "No active agent session");
                return;
            }

            Map<String, Object> data = (Map<String, Object>) message.getData();
            String workflowId = (String) data.get("workflowId");

            AgentDefinition agent = agentSession.getAgent();
            WorkflowDefinition workflow = findWorkflow(agent, workflowId);

            if (workflow == null) {
                sendError(session, "Workflow not found");
                return;
            }

            NodeDefinition node = objectMapper.convertValue(data.get("node"), NodeDefinition.class);

            if (workflow.getNodes() == null) {
                workflow.setNodes(new ArrayList<>());
            }
            workflow.getNodes().add(node);

            // Save and broadcast
            saveAndBroadcast(agentSession, sessionId, "nodeAdded", Map.of("node", node));

        } catch (Exception e) {
            sendError(session, "Failed to add node: " + e.getMessage());
        }
    }

    /**
     * Update node
     */
    private void handleUpdateNode(WebSocketMessage message, Session session, String sessionId) {
        try {
            AgentSession agentSession = agentSessions.get(sessionId);

            Map<String, Object> data = (Map<String, Object>) message.getData();

            String workflowId = (String) data.get("workflowId");
            String nodeId = (String) data.get("nodeId");

            WorkflowDefinition workflow = findWorkflow(agentSession.getAgent(), workflowId);
            if (workflow == null) {
                sendError(session, "Workflow not found");
                return;
            }

            NodeDefinition node = workflow.getNodes().stream()
                    .filter(n -> n.getId().equals(nodeId))
                    .findFirst()
                    .orElse(null);

            if (node == null) {
                sendError(session, "Node not found");
                return;
            }

            // Apply updates
            applyNodeUpdates(node, data);

            // Save and broadcast
            saveAndBroadcast(agentSession, sessionId, "nodeUpdated", Map.of("node", node));

        } catch (Exception e) {
            sendError(session, "Failed to update node: " + e.getMessage());
        }
    }

    /**
     * Delete node
     */
    private void handleDeleteNode(WebSocketMessage message, Session session, String sessionId) {
        try {
            AgentSession agentSession = agentSessions.get(sessionId);

            Map<String, Object> data = (Map<String, Object>) message.getData();

            String workflowId = (String) data.get("workflowId");
            String nodeId = (String) data.get("nodeId");

            WorkflowDefinition workflow = findWorkflow(agentSession.getAgent(), workflowId);
            if (workflow == null) {
                sendError(session, "Workflow not found");
                return;
            }

            workflow.getNodes().removeIf(n -> n.getId().equals(nodeId));
            workflow.getEdges().removeIf(e -> e.getFrom().equals(nodeId) || e.getTo().equals(nodeId));

            saveAndBroadcast(agentSession, sessionId, "nodeDeleted", Map.of("nodeId", nodeId));

        } catch (Exception e) {
            sendError(session, "Failed to delete node: " + e.getMessage());
        }
    }

    /**
     * Add edge
     */
    private void handleAddEdge(WebSocketMessage message, Session session, String sessionId) {
        try {
            AgentSession agentSession = agentSessions.get(sessionId);

            Map<String, Object> data = (Map<String, Object>) message.getData();

            String workflowId = (String) data.get("workflowId");
            WorkflowDefinition workflow = findWorkflow(agentSession.getAgent(), workflowId);

            EdgeDefinition edge = objectMapper.convertValue(data.get("edge"), EdgeDefinition.class);

            if (workflow.getEdges() == null) {
                workflow.setEdges(new ArrayList<>());
            }
            workflow.getEdges().add(edge);

            saveAndBroadcast(agentSession, sessionId, "edgeAdded", Map.of("edge", edge));

        } catch (Exception e) {
            sendError(session, "Failed to add edge: " + e.getMessage());
        }
    }

    /**
     * Delete edge
     */
    private void handleDeleteEdge(WebSocketMessage message, Session session, String sessionId) {
        try {
            AgentSession agentSession = agentSessions.get(sessionId);

            Map<String, Object> data = (Map<String, Object>) message.getData();

            String workflowId = (String) data.get("workflowId");
            String edgeId = (String) data.get("edgeId");

            WorkflowDefinition workflow = findWorkflow(agentSession.getAgent(), workflowId);
            workflow.getEdges().removeIf(e -> e.getId().equals(edgeId));

            saveAndBroadcast(agentSession, sessionId, "edgeDeleted", Map.of("edgeId", edgeId));

        } catch (Exception e) {
            sendError(session, "Failed to delete edge: " + e.getMessage());
        }
    }

    /**
     * Validate agent
     */
    private void handleValidateAgent(WebSocketMessage message, Session session, String sessionId) {
        try {
            AgentSession agentSession = agentSessions.get(sessionId);
            if (agentSession == null) {
                sendError(session, "No active agent session");
                return;
            }

            String agentJson = objectMapper.writeValueAsString(agentSession.getAgent());

            schemaProcessor.validateAgentDefinition(agentJson)
                    .subscribe().with(
                            validation -> sendMessage(session, new WebSocketMessage(
                                    "validationResult",
                                    Map.of(
                                            "valid", validation.isValid(),
                                            "errors", validation.getErrors()))),
                            error -> sendError(session, "Validation failed: " + error.getMessage()));

        } catch (Exception e) {
            sendError(session, "Validation error: " + e.getMessage());
        }
    }

    /**
     * Execute workflow with real-time updates
     */
    @SuppressWarnings("unchecked")
    private void handleExecuteWorkflow(WebSocketMessage message, Session session, String sessionId) {
        try {
            AgentSession agentSession = agentSessions.get(sessionId);

            Map<String, Object> data = (Map<String, Object>) message.getData();

            String workflowId = (String) data.get("workflowId");

            Map<String, Object> input = (Map<String, Object>) data.get("input");

            WorkflowDefinition workflow = findWorkflow(agentSession.getAgent(), workflowId);
            if (workflow == null) {
                sendError(session, "Workflow not found");
                return;
            }

            ExecutionContext context = contextManager.createContext();
            String executionId = context.getExecutionId();

            // Store execution tracking
            agentSession.addExecution(executionId);

            // Send execution started
            sendMessage(session, new WebSocketMessage(
                    "executionStarted",
                    Map.of("executionId", executionId)));

            // Execute with progress tracking
            workflowEngine.executeWorkflow(workflow, input, context)
                    .subscribe().with(
                            result -> {
                                // Send final result
                                broadcastToSession(sessionId, new WebSocketMessage(
                                        "executionCompleted",
                                        Map.of(
                                                "executionId", executionId,
                                                "success", result.isSuccess(),
                                                "output", result.getOutput(),
                                                "trace", result.getTrace())));
                            },
                            error -> {
                                broadcastToSession(sessionId, new WebSocketMessage(
                                        "executionFailed",
                                        Map.of(
                                                "executionId", executionId,
                                                "error", error.getMessage())));
                            });

            // Start progress monitoring
            monitorExecution(executionId, sessionId, context);

        } catch (Exception e) {
            sendError(session, "Failed to execute workflow: " + e.getMessage());
        }
    }

    /**
     * Subscribe to execution updates
     */
    private void handleSubscribeExecution(WebSocketMessage message, Session session, String sessionId) {

        Map<String, Object> data = (Map<String, Object>) message.getData();
        String executionId = (String) data.get("executionId");

        ExecutionContext context = contextManager.getContext(executionId);
        if (context != null) {
            monitorExecution(executionId, sessionId, context);
        }
    }

    /**
     * Get agent details
     */
    private void handleGetAgent(WebSocketMessage message, Session session, String sessionId) {
        try {

            Map<String, Object> data = (Map<String, Object>) message.getData();
            String agentId = (String) data.get("agentId");

            agentRepository.findAgentById(agentId)
                    .subscribe().with(
                            agent -> {
                                if (agent != null) {
                                    agentSessions.put(sessionId, new AgentSession(agentId, agent));
                                    sendMessage(session, new WebSocketMessage(
                                            "agentLoaded",
                                            Map.of("agent", agent)));
                                } else {
                                    sendError(session, "Agent not found");
                                }
                            },
                            error -> sendError(session, "Failed to load agent: " + error.getMessage()));

        } catch (Exception e) {
            sendError(session, "Failed to get agent: " + e.getMessage());
        }
    }

    /**
     * Monitor execution progress and send updates
     */
    private void monitorExecution(String executionId, String sessionId, ExecutionContext context) {
        new Thread(() -> {
            try {
                List<WorkflowRuntimeEngine.ExecutionTrace> lastTrace = new ArrayList<>();

                while (contextManager.getContext(executionId) != null) {
                    List<WorkflowRuntimeEngine.ExecutionTrace> currentTrace = context.getExecutionTrace();

                    // Send new trace entries
                    if (currentTrace.size() > lastTrace.size()) {
                        List<WorkflowRuntimeEngine.ExecutionTrace> newEntries = currentTrace.subList(lastTrace.size(),
                                currentTrace.size());

                        broadcastToSession(sessionId, new WebSocketMessage(
                                "executionProgress",
                                Map.of(
                                        "executionId", executionId,
                                        "trace", newEntries,
                                        "duration", context.getExecutionDuration())));

                        lastTrace = new ArrayList<>(currentTrace);
                    }

                    Thread.sleep(500); // Update every 500ms
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    // Helper methods

    private void sendMessage(Session session, WebSocketMessage message) {
        try {
            session.getAsyncRemote().sendText(objectMapper.writeValueAsString(message));
        } catch (Exception e) {
            LOG.errorf(e, "Failed to send message to session: %s", session.getId());
        }
    }

    private void sendError(Session session, String error) {
        sendMessage(session, new WebSocketMessage("error", Map.of("error", error)));
    }

    private void broadcastToSession(String sessionId, WebSocketMessage message) {
        Set<Session> sessionSet = sessions.get(sessionId);
        if (sessionSet != null) {
            sessionSet.forEach(session -> sendMessage(session, message));
        }
    }

    private void saveAndBroadcast(AgentSession agentSession, String sessionId,
            String eventType, Map<String, Object> data) {
        agentRepository.updateAgent(agentSession.getAgent())
                .subscribe().with(
                        updated -> {
                            agentSession.setAgent(updated);
                            broadcastToSession(sessionId, new WebSocketMessage(eventType, data));
                        },
                        error -> LOG.errorf(error, "Failed to save agent"));
    }

    private WorkflowDefinition findWorkflow(AgentDefinition agent, String workflowId) {
        if (agent.getWorkflows() == null)
            return null;

        return agent.getWorkflows().stream()
                .filter(w -> w.getId().getValue().equals(workflowId))
                .findFirst()
                .orElse(null);
    }

    private void applyAgentUpdates(AgentDefinition agent, Map<String, Object> updates) {
        if (updates.containsKey("name")) {
            agent.setName((String) updates.get("name"));
        }
        if (updates.containsKey("description")) {
            agent.setDescription((String) updates.get("description"));
        }
        // Add more update handlers as needed
    }

    private void applyWorkflowUpdates(WorkflowDefinition workflow, Map<String, Object> updates) {
        if (updates.containsKey("name")) {
            workflow.setName((String) updates.get("name"));
        }
        if (updates.containsKey("description")) {
            workflow.setDescription((String) updates.get("description"));
        }
    }

    @SuppressWarnings("unchecked")
    private void applyNodeUpdates(NodeDefinition node, Map<String, Object> updates) {
        if (updates.containsKey("name")) {
            node.setDisplayName((String) updates.get("name"));
        }
        if (updates.containsKey("position")) {

            Map<String, Double> pos = (Map<String, Double>) updates.get("position");
            Position position = new Position();
            position.setX(pos.get("x"));
            position.setY(pos.get("y"));
            node.getUi().setPosition(position);
        }
    }

    // Session tracking
    private static class AgentSession {
        private final String agentId;
        private AgentDefinition agent;
        private final Set<String> executions = ConcurrentHashMap.newKeySet();

        public AgentSession(String agentId, AgentDefinition agent) {
            this.agentId = agentId;
            this.agent = agent;
        }

        public String getAgentId() {
            return agentId;
        }

        public AgentDefinition getAgent() {
            return agent;
        }

        public void setAgent(AgentDefinition agent) {
            this.agent = agent;
        }

        public void addExecution(String executionId) {
            executions.add(executionId);
        }

        public Set<String> getExecutions() {
            return executions;
        }
    }
}
