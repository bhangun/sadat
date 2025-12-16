Below is a **formal Mermaid use case diagram** that captures the **core use cases of your unified low-code agentic workflow platform**, including **Camel-based integration, orchestration agents, HTIL, RAG, MCP/A2A, and ACP**.

This diagram is **capability-oriented**, not implementation-oriented, and is suitable for **architecture documents and governance reviews**.

---

## Unified Agentic Workflow Platform – Use Case Diagram

```mermaid
flowchart TB
    %% =========================
    %% ACTORS
    %% =========================
    User[Business User]
    Dev[Workflow Designer]
    Ops[Platform Operator]
    Human[Human Reviewer]
    ExtAgent[External Agent]
    ExtSys[External System]

    %% =========================
    %% SYSTEM BOUNDARY
    %% =========================
    subgraph PLATFORM["Low-Code Agentic Workflow Platform"]
        UC1["Design Workflow"]
        UC2["Register Plugin / Connector"]
        UC3["Configure Agent"]
        UC4["Define Orchestration Strategy"]
        UC5["Execute Workflow"]
        UC6["Run Orchestration Agent"]
        UC7["Execute Integration (Camel)"]
        UC8["Invoke AI Agent"]
        UC9["Retrieve RAG Context"]
        UC10["Human-in-the-Loop Review"]
        UC11["Policy & Guardrail Enforcement"]
        UC12["Observe & Audit Execution"]
        UC13["Agent-to-Agent Communication (MCP / A2A)"]
        UC14["Agent Commerce & Billing (ACP)"]
        UC15["Replay / Simulate WorkflowRun"]
    end

    %% =========================
    %% USER RELATIONSHIPS
    %% =========================
    Dev --> UC1
    Dev --> UC2
    Dev --> UC3
    Dev --> UC4

    User --> UC5

    Ops --> UC11
    Ops --> UC12
    Ops --> UC15

    Human --> UC10

    ExtSys --> UC7
    ExtAgent --> UC13

    %% =========================
    %% INCLUDE / EXTEND
    %% =========================
    UC5 --> UC6
    UC6 --> UC8
    UC6 --> UC7
    UC6 --> UC9
    UC6 --> UC11

    UC8 --> UC13
    UC8 --> UC14

    UC11 --> UC10
    UC12 --> UC15
```

---

## How to Read This Diagram

### 1. **Primary Actors**

* **Workflow Designer**: Defines structure, agents, and orchestration logic
* **Business User**: Triggers workflows and consumes outcomes
* **Platform Operator**: Enforces policy, observability, and governance
* **Human Reviewer**: Participates only when HTIL is required
* **External Agent/System**: Integrated via MCP, A2A, or Camel

---

### 2. **Core Platform Capabilities**

| Capability              | Meaning                                |
| ----------------------- | -------------------------------------- |
| Design Workflow         | Graph, nodes, edges, triggers          |
| Configure Agent         | Model, memory, tools, policies         |
| Define Orchestration    | Sequential, parallel, dynamic planning |
| Execute Workflow        | Engine-owned runtime execution         |
| Run Orchestration Agent | Decision-making, not execution         |
| Execute Integration     | Camel routes, connectors, side effects |
| Retrieve RAG Context    | Scoped, auditable context retrieval    |
| Human-in-the-Loop       | Review, approve, correct, resume       |
| Policy Enforcement      | Centralized guardrails                 |
| Observability           | Telemetry, provenance, audit           |
| MCP / A2A               | Cross-agent interoperability           |
| ACP                     | Pricing, quota, settlement             |
| Replay / Simulation     | Deterministic re-execution             |

---

## Key Architectural Signals Encoded

### ✔ Engine Sovereignty

Execution (`Execute Workflow`, `Execute Integration`) is never delegated to agents.

### ✔ Agents as Cognitive Units

Agents participate only in:

* Planning
* Decision-making
* Evaluation
* Coordination

### ✔ HTIL Is an Extension, Not an Exception

Human involvement is triggered by:

* Policy
* Confidence
* Failure
* Escalation rules

### ✔ Commerce Is Explicit

Agent usage can be:

* Metered
* Quota-bounded
* Settled via ACP

---

## Alignment With Your Schema

| Diagram Use Case    | Schema Element                        |
| ------------------- | ------------------------------------- |
| Design Workflow     | `WorkflowDefinition`                  |
| Execute Workflow    | `WorkflowRun` (runtime)               |
| Orchestration Agent | `AgentDefinition + OrchestrationSpec` |
| Camel Integration   | `NodeDefinition + ExecutionContext`   |
| RAG                 | `ContextBinding`                      |
| HTIL                | `WaitFor.type = human`                |
| MCP / A2A           | `OrchestrationTarget.a2a`             |
| ACP                 | `CommercePolicy`                      |

---

## Status

This use case diagram is:

* **Complete**
* **Non-overlapping**
* **Enterprise-grade**
* **Consistent with your schema**


