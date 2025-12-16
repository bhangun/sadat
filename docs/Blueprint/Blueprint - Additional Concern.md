

Since llm are proabilistic & undeterministic, and when using tools there could be an error response. Since these are low code platform. How to make an error as new input in node or option for human in the loop to determine.

Below is the clean, production-ready architecture pattern to handle:

LLM probabilistic/undeterministic behavior

Tool execution failures

Errors becoming structured inputs

Human-in-the-loop (HITL) options

Low-code workflow compatibility

Standalone runtime compatibility

This is the same approach used in LangGraph, Microsoft Autogen, Google Vertex AI Agents, AWS Step Functions, and enterprise workflow engines—adapted to the visual low-code builder.

✅ 1\. Introduce the “Error Channel” as a First-Class Node Input

Every node in the workflow MUST expose two output ports:

✔ Success output

→ normal data flows to next node

✔ Error output

→ structured error emitted as new input to another node (automatic or human-supervised)

This becomes a universal design rule for all nodes.

Mermaid Pattern

flowchart TD

    Node \--\>|success| NextNode

    Node \--\>|error| ErrorHandler

\---

✅ 2\. Standardize the Error Payload Schema

All components (LLM, RAG, Tools, MCP, A2A, etc.) must return a uniform error object.

ErrorPayload

{

  "type": "ToolError | LLMError | NetworkError | ValidationError | UnknownError",

  "message": "string",

  "details": {},

  "retryable": true,

  "originNode": "string",

  "timestamp": "ISO-8601"

}

This allows the workflow engine to route errors as events.

\---

✅ 3\. Add Built-in “Error Handler Node”

A dedicated core node type:

ErrorHandlerNode

Inputs: ErrorPayload

Outputs:

auto-fix → next node

fallback-path → human node

retry → original node

abort → end node

Has internal policy rules (CEL or JSON logic)

Example decisions:

if error.retryable && retryCount \< 3:

    retry()

else if error.type \== "ValidationError":

    fixUsingLLM()

else:

    escalateToHuman()

\---

✅ 4\. Add “Human Approval Node” (HITL Node)

When an error cannot be auto-handled, route to the Human Decision Node.

HumanDecisionNode

Shows the error

Shows the input/output context

Allows human to choose:

✔ Retry

✔ Provide corrected input

✔ Choose an alternative path

✔ Skip node

✔ Halt workflow

\---

✅ 5\. Add “Self-Healing Node” (Auto-Recovery LLM)

This is optional but extremely powerful.

Used when LLM or RAG fails due to:

missing context

wrong prompt format

malformed tool output

inconsistent structure

SelfHealingNode

Uses the LLM to fix itself:

"Given the following error and input, generate a corrected request that will fix the issue"

Output goes back to the original node.

This can resolve 60–80% of workflow errors automatically.

\---

✅ 6\. Universal Node Structure (Future-Proof)

Every node in the platform must follow this triplet:

inputs → execute → outputs (success, error)

Node Contract

{

  "nodeType": "...",

  "inputs": {...},

  "outputs": {

    "success": {...},

    "error": "ErrorPayload"

  }

}

This makes modules usable in both:

Full platform mode

Standalone runtime

\---

✅ 7\. Modify the Visual Builder to Support Error Lines

In the UI:

Nodes automatically display “Success” and “Error” connectors.

Users can visually route error connections.

Example:

ToolNode → (error) → HumanReviewNode → (success) → Continue

ToolNode → (success) → NextStep

\---

✅ 8\. Error Re-Entry Becomes New Node Input

the question:

\> How to make an error as new input in node or option for human in the loop to determine?

This is solved by re-injecting the ErrorPayload into the graph as a new event.

Example full flow:

flowchart TD

    ToolCall \--\>|success| A\[Next Step\]

    ToolCall \--\>|error| E\[Error Handler\]

    E \--\>|auto-fix| ToolCall

    E \--\>|human-review| H\[Human Decision\]

    H \--\>|corrected| ToolCall

    H \--\>|skip| A

    E \--\>|abort| End

\---

✅ 9\. State Machine Implementation (Robust & Deterministic)

LLM is probabilistic, but the workflow state machine is deterministic.

the runtime (Quarkus \+ internal machine) does:

1\. Run a node

2\. Inspect the output type

3\. Route based on:

success

error

human-needed

This eliminates unpredictability.

\---

🔥 10\. Optional: “Error-As-Context” Node

Advanced enterprise feature:

When LLM fails multiple times, feed the error as a new context input to next nodes.

Example behavior:

Try RAG → RAG fails → Next node receives ErrorPayload as context → Node decides new strategy.

Works great for adaptive agents.

\---

🚀 Final Summary

To support deterministic workflows in an LLM-driven system:

You MUST add:

1\. Error Output Port for all nodes

2\. ErrorPayload Schema

3\. ErrorHandlerNode

4\. HumanDecisionNode (HITL)

5\. Self-HealingNode (optional but powerful)

6\. Visual Builder Error Lines

7\. Deterministic State Machine (core runtime)

8\. Error-As-Context Node (enterprise mode)

This makes the platform:

deterministic

robust

enterprise-ready

safe

future-proof

suitable for standalone runtime

compatible with unpredictable LLM & tool calls

I already have:

Add robust error-as-input semantics to every node,

Add HITL (Human-in-the-loop) decisioning, auto-repair (self-heal), and error-as-context behaviors,

Update the low-code schema (JSON/YAML) and MCP tool contract for errors,

Add visual/UI rules for the editor,

Add code-generation rules so standalone runtimes include only necessary modules for error handling, and

Provide operational/CEL examples, retry policies, provenance logs and test scenarios.

I kept this formal and implementation-oriented. Save or copy what you need. If you want any file converted to a downloadable file or added into the repo structure, tell me which piece.

\---

1 — Design summary (what changed)

1\. Every node now exposes success and error outputs by default.

2\. Introduced a canonical ErrorPayload schema used by all nodes, tools, and adapters.

3\. New node types: ErrorHandlerNode, HumanDecisionNode (HITL), SelfHealingNode, and ErrorAsContext Node.

4\. Visual editor automatically shows the error port and offers templates to connect it.

5\. Orchestrator and NodeExecutor updated to treat errors as first-class events: they can re-inject errors as inputs, schedule retries, or pause for human input.

6\. Code generator: when generating standalone runtimes, include the minimal error-handling modules required by used nodes (HITL UI optional — only included if user selected human-review nodes).

7\. MCP/ToolGateway contract extended with an error envelope for tool responses.

8\. Added standard CEL policy examples for routing, retry, and escalation.

9\. Added provenance & audit entries for all error events and human decisions.

2 — Core artifacts (copy/paste ready)

2.1 ErrorPayload JSON schema

Save as schemas/ErrorPayload.schema.json

{

  "$schema": "http://json-schema.org/draft-07/schema\#",

  "title": "ErrorPayload",

  "type": "object",

  "required": \["type","message","timestamp","originNode","retryable"\],

  "properties": {

    "type": { "type": "string", "enum": \["ToolError","LLMError","NetworkError","ValidationError","Timeout","UnknownError"\] },

    "message": { "type": "string" },

    "details": { "type": "object" },

    "retryable": { "type": "boolean" },

    "originNode": { "type": "string" },

    "originRunId": { "type": "string" },

    "attempt": { "type": "integer", "minimum": 0 },

    "maxAttempts": { "type": "integer", "minimum": 0 },

    "timestamp": { "type": "string", "format": "date-time" },

    "suggestedAction": { "type": "string", "enum": \["retry","fallback","escalate","human\_review","abort","auto\_fix"\] },

    "provenanceRef": { "type": "string" }

  }

}

\---

2.2 ExecutionPlan & NodeDescriptor extension

Add error port in node outputs; NodeDescriptor supports errorHandling metadata.

Example NodeDescriptor extension (YAML):

id: com.example/tool/call

version: 1.0.0

inputs:

  \- name: payload

    schema: /schemas/ToolCallRequest.schema.json

outputs:

  success:

    schema: /schemas/ToolCallResponse.schema.json

  error:

    schema: /schemas/ErrorPayload.schema.json

metadata:

  errorHandling:

    retryPolicy:

      maxAttempts: 3

      backoff: exponential

      initialDelayMs: 500

    fallbackNodeId: fallback-123

    humanReviewThreshold: CRITICAL

\---

3 — New node definitions (zoom-in)

3.1 ErrorHandlerNode

Purpose: central rule-driven node for handling errors emitted by any node.

Signature

Input: ErrorPayload

Output:

retry (routes to original node or a retried instance)

auto\_fix (routes to SelfHealingNode or the original node with a fixed input)

human\_review (routes to HumanDecisionNode)

abort (end or fail path)

Config (example)

nodeType: ErrorHandler

id: eh-01

config:

  rules:

    \- name: retry-if-transient

      when: "error.retryable \== true && error.attempt \< error.maxAttempts"

      action: "retry"

    \- name: auto-fix-validation

      when: "error.type \== 'ValidationError'"

      action: "auto\_fix"

    \- name: escalate-to-human

      when: "error.type \== 'LLMError' && error.attempt \>= 1"

      action: "human\_review"

    \- name: abort-default

      when: "true"

      action: "abort"

Mermaid snippet:

flowchart TD

  NodeA \--\>|error| ErrorHandler

  ErrorHandler \--\>|retry| NodeA

  ErrorHandler \--\>|auto\_fix| SelfHealing

  SelfHealing \--\>|fixed| NodeA

  ErrorHandler \--\>|human\_review| HumanDecision

  HumanDecision \--\>|corrected| NodeA

  HumanDecision \--\>|abort| End

\---

3.2 HumanDecisionNode (HITL)

Purpose: present error \+ context to human operator, capture decision and optional corrected input.

Fields to show in UI

Error summary (type, message, timestamp)

Node inputs/outputs snapshot (redacted)

Execution trace (last N steps)

Suggested actions (Retry / Provide corrected input / Skip / Abort)

A text box for free-form instruction or corrected JSON

TTL and escalation (remind after N minutes)

API

POST /human-tasks/{taskId}/complete with payload:

{

  "action": "retry|correct|skip|abort",

  "correctedInput": { /\* optional node input \*/ },

  "notes": "string",

  "operatorId": "user:123"

}

Behavior

On retry the Orchestrator re-schedules node run using same/resumed state.

On correct the corrected input replaces node input and node runs.

On skip the error path continues to next node as configured.

On abort the plan marks the run FAILED or triggers compensations.

\---

3.3 SelfHealingNode

Purpose: attempt to auto-correct or reformat inputs using an LLM or deterministic repair function.

Example prompt (LLM)

You are a data-wrangler assistant. Given this node input and the error returned, produce a corrected input that respects the node's input schema. Only output valid JSON for the node input named "payload".

Config

nodeType: SelfHealing

id: sh-01

config:

  modelHints: {preferred: \["local-vllm"\], maxTokens: 256}

  maxAttempts: 2

  validationSchema: /schemas/ToolCallRequest.schema.json

Behavior

Runs LLM with context \+ schema and returns corrected input; validated against schema.

If correction passes, it returns auto\_fix output with fixedInput used to re-run original node.

If it fails, route to HumanDecisionNode.

\---

3.4 ErrorAsContext Node

Purpose: allow downstream nodes to receive error payload as part of their normal input so they can change strategy.

Use cases

If RAG retrieval fails, next node may call a fallback knowledge source using the error as hint.

Downstream nodes can implement conditional logic: if ctx.error then use fallback.

Contract (NodeDescriptor)

Input: context object with optional error field (ErrorPayload or null)

Example:

inputs:

  \- name: context

    schema:

      type: object

      properties:

        error: { "$ref": "/schemas/ErrorPayload.schema.json" }

        data: { "type": "object" }

\---

4 — Editor (UI) changes & UX flow

4.1 Visual changes

1\. Every node shows two connectors by default:

success (green) — normal next

error (red) — error route

2\. When user drags an edge from error, Editor offers:

Connect to ErrorHandlerNode (create new)

Connect to HumanDecisionNode (create new)

Connect to SelfHealingNode (create new)

Connect to any other node (e.g., fallback path)

3\. Node property panel includes errorHandling tab with:

Retry policy (maxAttempts, backoff)

Fallback node id

Auto-fix enabled

Human review threshold

4\. Human Task UI (stack)

Task list / inbox for operators

For each task: context, action buttons, quick-correctors (LLM suggested corrections), attachments, SLA timer

4.2 Default templates (for quick wiring)

Error → Retry Template (connects to ErrorHandler with built-in retry rule)

Error → Human Review Template (auto-creates HumanDecisionNode)

Error → Self-Heal → Retry (chain: ErrorHandler \-\> SelfHealing \-\> Retry)

\---

5 — Runtime semantics & Orchestrator changes

5.1 Deterministic State Machine

Node states extended: PENDING, RUNNING, SUCCEEDED, FAILED, ERROR\_HANDLED, AWAITING\_HITL, RETRYING, SKIPPED.

On node invocation:

If result is success → proceed.

If result is an error → create ErrorPayload, persist in ExecutionStore, emit node.error event, enqueue ErrorHandler processing.

The Orchestrator handles error routing by evaluating the ErrorHandler rules (CEL) deterministically.

5.2 Retry semantics

Retry keys must be idempotent: implement idempotency tokens, or use checkpoint/resume tokens provided by NodeExecutor.

Retries increment attempt in ErrorPayload; Orchestrator enforces maxAttempts.

Backoff policy: fixed or exponential with jitter.

5.3 Human decisions

When human\_review selected:

Orchestrator persists a HumanTask object and pauses run at that node.

UI operator resolves it; Orchestrator resumes with chosen action.

Task TTL with escalation — if TTL expires, auto-escalation policy applies (e.g., route to Senior Ops or auto-abort).

5.4 Provenance & Audit

Emit events:

node.error.created (with provenanceRef)

error.handler.invoked

human.task.created

human.task.completed

node.retried

node.auto\_fix.attempt

Each event includes runId, nodeId, operatorId (if HITL), and cryptographic hash for integrity if enabled.

6 — MCP / Tool Gateway: error envelope & contract

Extend MCP tool response object to include error object (ErrorPayload). Example tool response structure:

{

  "requestId": "uuid",

  "status": "ok|error",

  "result": { /\* tool-specific \*/ },

  "error": { /\* ErrorPayload or null \*/ },

  "provider": "mcp://tool/xyz",

  "timings": { "start":"..", "end":".." }

}

If error present:

NodeExecutor treats it as node-level error output.

ToolGateway can optionally provide suggestedAction (retryable, fallback, human\_review).

Tool contracts must document what error types may be produced and any structured fields in details.

\---

7 — CEL policy examples (error routing)

Assume error bound to variable err in CEL. Examples:

Retry rule

err.retryable \== true && err.attempt \< err.maxAttempts

Escalate to human

err.type \== "LLMError" && err.attempt \>= 1

Auto-fix (validation error)

err.type \== "ValidationError" && has\_schema(err.details.schema)

Fallback if tool-specific

err.type \== "ToolError" && err.details.code in \["TOOL\_DEPRECATED","UNSUPPORTED\_OPERATION"\]

\---

8 — Code generation rules & minimal module inclusion (for standalone)

8.1 High-level algorithm

Given a workflow definition workflow.json:

1\. Parse all nodes and collect requiredModules per node type.

2\. Add coreKernelModules (NodeExecutorCore, ConfigLoader, GuardrailsLite, Logging).

3\. For each requiredModule, map it to a dependency artifact (Maven/Gradle, or for native builds include specific libraries).

4\. Prune duplicates and transitive heavy modules (eg. do not include Orchestrator).

5\. Generate pom.xml (or build.gradle) with only those dependencies.

6\. Generate Main runtime that:

Loads workflow, registers nodes

Initializes minimal RAG/Memory/Model adapters if used

Exposes a small admin API if HITL nodes included (or push to remote HITL service)

7\. Package as fat-jar / Docker with minimal base image.

8.2 Mapping examples (module→artifact)

NodeExecutorCore → com.acme:node-exec-core:1.0.0

GuardrailsLite → com.acme:guardrails-lite:1.0.0

ToolGatewayClient (lite) → com.acme:mcp-client-lite:1.0.0

LocalRAG → com.acme:rag-lite:1.0.0 (uses embedded SQLite or pgvector-lite)

LLM Adapter (Ollama local) → com.acme:llm-ollama-adapter:1.0.0

SelfHealing (LLM) → include LLM adapter and selfheal-core service

HumanTask UI → optional web UI module com.acme:human-frontend (only include if human nodes used)

8.3 Example generated pom.xml snippet (pseudo)

\<dependencies\>

  \<dependency\>\<groupId\>com.acme\</groupId\>\<artifactId\>node-exec-core\</artifactId\>\<version\>1.0.0\</version\>\</dependency\>

  \<dependency\>\<groupId\>com.acme\</groupId\>\<artifactId\>guardrails-lite\</artifactId\>\<version\>1.0.0\</version\>\</dependency\>

  \<\!-- conditional \--\>

  \<dependency\>\<groupId\>com.acme\</groupId\>\<artifactId\>rag-lite\</artifactId\>\<version\>1.0.0\</version\>\</dependency\>

  \<dependency\>\<groupId\>com.acme\</groupId\>\<artifactId\>llm-ollama-adapter\</artifactId\>\<version\>1.0.0\</version\>\</dependency\>

\</dependencies\>

8.4 Main runtime pseudocode (simplified)

public class Main {

  public static void main(String\[\] args) {

    Config cfg \= ConfigLoader.load();

    NodeExecutorCore exec \= new NodeExecutorCore(cfg);

    Workflow wf \= WorkflowLoader.load("workflow.json");

    exec.registerNodes(wf.getNodes());

    exec.onEvent("node.error", event \-\> ErrorHandler.process(event));

    exec.start();

  }

}

\---

9 — Test cases & QA

9.1 Unit tests

1\. Tool returns error with retryable=true → ErrorHandler should schedule retry (attempt++), backoff applied.

2\. Tool returns ValidationError → SelfHealing invoked; if SelfHealing returns valid, node retried with corrected input.

3\. LLM returns malformed JSON → SelfHealing attempts fix; if fails twice, HumanDecision created.

4\. HumanDecision: operator selects 'skip' → workflow continues to fallback node.

5\. HITL TTL expires → escalation policy triggers (e.g., to senior operator or auto-abort).

9.2 Integration tests

Simulate Orchestrator crash during AWAITING\_HITL: after recovery the human task remains and resuming works.

End-to-end with RAG fallback: RAG fails, ErrorAsContext causes alternate retrieval to succeed.

\---

10 — Provenance & audit entries

Standardize events (persist in Provenance store):

{

  "eventType": "NODE\_ERROR",

  "runId": "uuid",

  "nodeId": "node-123",

  "error": { /\* ErrorPayload \*/ },

  "timestamp": "2025-11-28T10:00:00Z",

  "actor": "system|tool|operator:user:321",

  "traceId": "trace-uuid"

}

Human decisions:

{

  "eventType": "HUMAN\_TASK\_COMPLETED",

  "humanTaskId": "t-123",

  "operatorId": "user:321",

  "action": "correct|retry|abort|skip",

  "notes": "...",

  "timestamp": "..."

}

\---

11 — Edge cases & operational concerns

Idempotency: ensure retried nodes are idempotent or use compensation. Provide idempotencyKey in node metadata.

Sensitive data: redact PII before sending to HITL UI unless operator is authorized — use Guardrails policy.

HITL scaling: provide operator routing, SLAs, and worker pools for human tasks. Optionally export tasks to external task queues (e.g., Jira, ServiceNow).

Audit & compliance: sign human decisions and store hashes.

Offline standalone runtime: HITL nodes cannot rely on platform UI; either:

include an embedded minimal admin UI, or

configure humanReviewCallback URL to remote platform where operators review tasks.

\---

12 — Example integrated Mermaid (full error flow)

flowchart TD

  Start \--\> NodeA

  NodeA \--\>|success| NodeB

  NodeA \--\>|error| ErrorHandler

  ErrorHandler \--\>|retry| NodeA

  ErrorHandler \--\>|auto\_fix| SelfHealing

  SelfHealing \--\>|fixed| NodeA

  ErrorHandler \--\>|human\_review| HumanDecision

  HumanDecision \--\>|corrected| NodeA

  HumanDecision \--\>|skip| NodeB

  ErrorHandler \--\>|abort| End

  NodeB \--\> End

Below is a complete Audit Node and Built-in Auditing Layer design that integrates cleanly with the current architecture (error-as-input, HITL, self-healing, CEL rules, MCP tools, code generator, orchestrator state machine).

Everything is formal, production-ready, and consistent with the visual AI agent builder platform you are building.

\---

✅ 1 — Purpose of the Audit Layer

the system now has many dynamic and error-handling flows:

HITL decisions

Retry, fallback, auto-fix

Error routing

Tool calls and LLM calls

RAG retrieval

Node execution steps

A dedicated Audit Node and Built-in Audit Service give you:

1\. Tamper-proof execution logs

2\. Chain-of-custody tracking, especially for human decisions

3\. Compliance-friendly event trails (Syirkah / finance / regulated workflows)

4\. Cross-run correlation

5\. Optional export to external systems (PostgreSQL, Elasticsearch, Kafka, S3)

\---

✅ 2 — AuditPayload Schema

Save as schemas/AuditPayload.schema.json:

{

  "$schema": "http://json-schema.org/draft-07/schema\#",

  "title": "AuditPayload",

  "type": "object",

  "required": \["timestamp", "event", "level", "actor", "nodeId", "runId"\],

  "properties": {

    "timestamp": { "type": "string", "format": "date-time" },

    "runId": { "type": "string" },

    "nodeId": { "type": "string" },

    "actor": {

      "type": "object",

      "required": \["type"\],

      "properties": {

        "type": { "type": "string", "enum": \["system","human","agent"\] },

        "id": { "type": "string" },

        "role": { "type": "string" }

      }

    },

    "event": { "type": "string" },

    "level": { "type": "string", "enum": \["INFO","WARN","ERROR","CRITICAL"\] },

    "tags": { "type": "array", "items": { "type": "string" } },

    "metadata": { "type": "object" },

    "contextSnapshot": { "type": "object" },

    "hash": { "type": "string" }

  }

}

\---

✅ 3 — Audit Node (Built-in Node Type)

The Audit Node is a sink node for logging any event at any step.

3.1 Node signature

Inputs:

audit (AuditPayload)

Outputs:

success (always succeeds unless storage fails)

error (if auditing storage fails) — integrates naturally with the ErrorHandlerNode

3.2 NodeDescriptor (YAML)

id: builtin.audit

version: 1.0.0

inputs:

  \- name: audit

    schema: /schemas/AuditPayload.schema.json

outputs:

  success:

    schema: /schemas/Empty.schema.json

  error:

    schema: /schemas/ErrorPayload.schema.json

metadata:

  category: system

  capabilities: \[sink, compliance, tamper\_proof\]

config:

  sink:

    type: pg|es|s3|stdout|kafka

    connection: env://AUDIT\_SINK\_URL

    hashing: sha256

\---

✅ 4 — How Audit Node is used inside flows

Example: RAG retrieval failure → error handler → audit

flowchart TD

  RAG \--\>|error| EH

  EH \--\>|retry| Audit\["Audit Node"\]

  Audit \--\> RAG

Example: HITL: Human approved correction → audit

flowchart TD

  HumanDecision \--\>|operator\_action| Audit

  Audit \--\> NextNode

\---

✅ 5 — When the system auto-generates Audit Node calls

The system auto-injects audit events for:

Event	Example

Node started	event="NODE\_START"

Node completed	event="NODE\_SUCCESS"

Node error	event="NODE\_ERROR"

Retry scheduled	event="RETRY\_SCHEDULED"

Auto-fix attempt	event="AUTO\_FIX\_ATTEMPT"

Human task created	event="HITL\_CREATED"

Human task completed	event="HITL\_COMPLETED"

Escalation triggered	event="ESCALATED"

Workflow completed	event="WORKFLOW\_COMPLETED"

These are generated without the user needing to explicitly add Audit Nodes, unless they want custom auditing.

\---

✅ 6 — Audit Node automatic hashing (tamper-proof)

Each AuditPayload receives:

hash \= SHA256(timestamp \+ runId \+ nodeId \+ event \+ actor.id)

Optionally include:

Previous hash

Chain-of-custody index

This converts the audit log into a blockchain-style chain of events without requiring a blockchain.

Example:

{

  "event": "HITL\_COMPLETED",

  "hash": "9b8de1a57c1d..."

}

\---

✅ 7 — Built-in Audit Service (Quarkus)

This is a Quarkus service that receives audit events from workflow runtime and stores them.

7.1 REST endpoint

POST /audit/record

Body: AuditPayload

Response:

{"status":"ok"}

7.2 Storage options

Configured via application.yml:

audit:

  sink: pg

  pg:

    url: jdbc:postgresql://audit-db:5432/audit

    table: audit\_events

Other sinks supported:

PostgreSQL

Kafka

ElasticSearch

S3/GCS/MinIO (append-only log)

Local JSONL file (for standalone runtime)

\---

✅ 8 — Auto-generated audit logs in the standalone runtime

When you generate a minimal runtime (for embedded workflows), the generator includes AuditLite, not the full Audit Service.

AuditLite writes:

JSONL file

Or SQLite table

Or in-memory ring buffer

Minimal dependency (for offline execution).

\---

✅ 9 — Integration with ErrorPayload, SelfHeal, HITL

9.1 When an error happens

Audit event:

{

  "event":"NODE\_ERROR",

  "actor":{"type":"system"},

  "metadata":{"errorType":"ValidationError","attempt":1}

}

9.2 When self-healing runs

{

  "event":"AUTO\_FIX\_ATTEMPT",

  "level":"WARN",

  "metadata":{"model":"local-vllm","accepted":true}

}

9.3 When human corrects input

{

  "event":"HITL\_COMPLETED",

  "actor":{"type":"human","id":"operator123"},

  "metadata":{"action":"correct","notes":"Value format fixed."}

}

\---

✅ 10 — Adding Audit Node to the Visual Editor

10.1 Appearance

System/utility node

Icon: shield / ledger

Color: deep blue

Ports:

input: audit

outputs: success, error

10.2 Auto-connect suggestions in editor

When user drags an error connector: System suggests:

ErrorHandlerNode

SelfHealingNode

HumanDecisionNode

AuditNode (log incident only)

Fallback Node

When a HITL node emits a decision:

Automatically show an Audit Node inline (ghost, can be removed)

\---

✅ 11 — CEL rules for custom auditing

You can route events to custom audit channels:

If critical → audit with level CRITICAL

err.type in \["LLMError","ToolError"\] && err.retryable \== false

If human touched the flow → mandatory audit

event \== "HITL\_COMPLETED"

If auto-fix changed the schema → escalate

autoFix.delta \> 0

\---

audit/

 ├─ schemas/

 │   ├─ AuditPayload.schema.json

 │   ├─ ErrorPayload.schema.json (patched)

 ├─ nodes/

 │   ├─ AuditNode.yaml

 │   ├─ ErrorHandlerNode.yaml (patched)

 │   ├─ HumanDecisionNode.yaml (patched)

 │   └─ SelfHealingNode.yaml (patched)

 ├─ quarkus/

 │   ├─ AuditServiceResource.java

 │   ├─ AuditRepository.java

 │   ├─ application.yml

 ├─ editor/

 │   ├─ AuditNode.json

 │   └─ icons/

 ├─ runtime-lite/

 │   ├─ AuditLite.java

 │   └─ jsonl-writer.java

 └─ README.md



# Node Abstraction


```java
public abstract class AbstractNode implements Node {

    protected NodeDescriptor descriptor;

    protected NodeConfig config;

    protected MetricsCollector metrics;


    @Override
    public Uni<Void> onLoad(NodeDescriptor descriptor, NodeConfig config) {
        this.descriptor = descriptor;
        this.config = config;
        this.metrics = MetricsCollector.forNode(descriptor.getId());
        doOnLoad(descriptor, config);
        return Uni.createFrom().voidItem();
    }


    @Override
    public final Uni<ExecutionResult> execute(NodeContext context) {

        Span span = startTrace(context);
        long startTime = System.nanoTime();

        return Uni.createFrom().deferred(() -> {
            Guardrails guardrails = context.getGuardrails();
            ProvenanceService provenance = context.getProvenance().getService();

            // 1. Pre-check
            Uni<GuardrailResult> preCheck = config.guardrailsConfig().enabled()
                    ? guardrails.preCheck(context, descriptor)
                    : Uni.createFrom().item(GuardrailResult.allow());

            return preCheck
                    .onItem().transformToUni(guardResult -> {
                        if (!guardResult.isAllowed()) {
                            return Uni.createFrom().item(
                                    ExecutionResult.blocked(guardResult.getReason()));
                        }

                        // 2. Validate inputs
                        return validateInputs(context)
                                .onItem().transformToUni(valid -> {
                                    if (!valid) {
                                        return Uni.createFrom().item(
                                                ExecutionResult.failed("Input validation failed"));
                                    }

                                    // 3. Execute the node logic
                                    return doExecute(context);
                                });
                    })

                    // 4. Post-check
                    .onItem().transformToUni(result -> {
                        if (!config.guardrailsConfig().enabled() || !result.isSuccess()) {
                            return Uni.createFrom().item(result);
                        }

                        return guardrails.postCheck(result, descriptor)
                                .map(g -> g.isAllowed()
                                        ? result
                                        : ExecutionResult.blocked(g.getReason()));
                    })

                    // 5. Metrics + provenance
                    .onItem().invoke(result -> {
                        long duration = System.nanoTime() - startTime;
                        metrics.recordExecution(duration, result.getStatus());
                        provenance.log(context.getNodeId(), context, result);
                    })

                    // 6. Error handling
                    .onFailure().recoverWithItem(th -> {
                        metrics.recordFailure(th);
                        return ExecutionResult.error(
                                ErrorPayload.from(th, descriptor.getId(), context));
                    })

                    // 7. Close span
                    .eventually(() -> endTrace(span));
        });
    }


    protected abstract Uni<ExecutionResult> doExecute(NodeContext context);

    protected void doOnLoad(NodeDescriptor descriptor, NodeConfig config) {
        // Default: no-op
    }

    private Uni<Boolean> validateInputs(NodeContext context) {
        return Uni.createFrom().item(() -> {
            for (var input : descriptor.getInputs()) {
                Object value = context.getInput(input.getName());

                if (input.isRequired() && value == null) {
                    return false;
                }

                if (value != null && input.getSchema() != null) {
                    if (!SchemaUtils.validate(value, input.getSchema())) {
                        return false;
                    }
                }
            }
            return true;
        });
    }

    private Span startTrace(NodeContext context) {
        return Tracer.spanBuilder("node.execute")
                .withTag("node.id", descriptor.getId())
                .withTag("node.type", descriptor.getType())
                .withTag("run.id", context.getRunId())
                .withTag("tenant.id", context.getTenantId());
    }


    private void endTrace(Span span) {
        if (span != null) {
            span.finish();
        }
    }


    @Override
    public Uni<Void> onUnload() {
        if (metrics != null) {
            metrics.close();
        }
        return Uni.createFrom().voidItem();
    }
}


/**
 * Base class for integration/connector/transformer nodes.
 * Integration nodes perform deterministic IO, transformation,
 * mapping, and communication with external systems.
 * They are intentionally lightweight:
 */
public abstract class IntegrationNode extends AbstractNode {
    @Override
    protected final Uni<ExecutionResult> doExecute(NodeContext context) {
        return executeIntegration(context);
    }

    protected abstract Uni<ExecutionResult> executeIntegration(NodeContext context);
}

/**
 * Base class for all AI/LLM-driven nodes.
 * Agent nodes perform reasoning, planning, tool invocation,
 * and may require strong safety/guardrail enforcement.
 */
public abstract class AgentNode extends AbstractNode {
    /**
     * Performs optional pre-execution AI-specific validations.
     * Override if the agent requires additional safety steps.
     */
    protected Uni<Void> preAgentSafety(NodeContext context) {
        return Uni.createFrom().voidItem();
    }

    /**
     * Performs optional post-execution sanity checks on the LLM output
     * (e.g., hallucination filters, JSON schema repair).
*/
    protected ExecutionResult postAgentValidation(ExecutionResult result) {
        return result;
    }

    @Override
    protected final Uni<ExecutionResult> doExecute(NodeContext context) {
        return preAgentSafety(context)
                .onItem().transformToUni(v -> executeAgent(context))
                .map(this::postAgentValidation);
    }

    /**
     * Agent-specific execution logic. Subclasses must implement.
     */
    protected abstract Uni<ExecutionResult> executeAgent(NodeContext context);
}
```