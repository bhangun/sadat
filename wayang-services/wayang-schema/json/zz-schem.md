
Below is the **fully production-ready, modular multi-layer workflow schema**, now including the **AUDIT LAYER** integrated cleanly with:

* Logic
* UI
* Runtime
* Observability / Metrics
* Audit (append-only, tamper-proof)

Everything stays **modular**, **backward-compatible**, and **future-proof**.

I will present only the **final updated schemas**, not explanations unless you ask.

---

# ✅ **ROOT WORKFLOW SCHEMA **

`workflow.schema.json`

```json
{
  "$id": "https://kayys.tech/schema/v1/workflow.schema.json",
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "title": "Kayys Workflow Root",
  "type": "object",
  "required": ["id", "version", "logic"],
  "properties": {
    "id": { "type": "string" },
    "version": { "type": "string" },

    "logic": {
      "$ref": "workflow.logic.schema.json"
    },

    "ui": {
      "$ref": "workflow.ui.schema.json"
    },

    "runtime": {
      "$ref": "workflow.runtime.schema.json"
    },

    "observability": {
      "$ref": "workflow.observability.schema.json"
    },

    "audit": {
      "$ref": "workflow.audit.schema.json"
    }
  }
}
```

---

# 🟦 **LOGIC LAYER (UNCHANGED)**

`workflow.logic.schema.json`
*(same as previous message; no changes needed)*

---

# 🟧 **UI LAYER (UNCHANGED)**

`workflow.ui.schema.json`
*(same as previous message)*

---

# 🟩 **RUNTIME LAYER (ADD auditRef FOR LINKING)**

`workflow.runtime.schema.json`

```json
{
  "$id": "https://kayys.tech/schema/v1/workflow.runtime.schema.json",
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "title": "Runtime Layer",
  "type": "object",

  "properties": {
    "executedNodes": {
      "type": "array",
      "items": { "$ref": "#/$defs/executionEntry" }
    },
    "logs": {
      "type": "array",
      "items": { "type": "string" }
    },
    "metrics": {
      "type": "object",
      "additionalProperties": true
    }
  },

  "$defs": {
    "executionEntry": {
      "type": "object",
      "properties": {
        "ref": { "type": "string" },
        "timestamp": { "type": "integer" },
        "latencyMs": { "type": "number" },
        "tokensUsed": { "type": "integer" },
        "costUsd": { "type": "number" },
        "status": { "type": "string" },

        "auditRef": {
          "type": "string",
          "description": "Points to audit entry for this execution"
        },

        "outputPreview": { "type": "string" }
      }
    }
  }
}
```

---

# 🟥 **NEW: OBSERVABILITY LAYER**

This layer stores:

* traces
* alerts
* SLA breaches
* structured logs
* high-cardinality metrics

`workflow.observability.schema.json`

```json
{
  "$id": "https://kayys.tech/schema/v1/workflow.observability.schema.json",
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "title": "Observability Layer",
  "type": "object",

  "properties": {
    "traces": {
      "type": "array",
      "items": { "$ref": "#/$defs/trace" }
    },

    "alerts": {
      "type": "array",
      "items": { "$ref": "#/$defs/alert" }
    },

    "metrics": {
      "type": "object",
      "properties": {
        "throughput": { "type": "number" },
        "errorRate": { "type": "number" },
        "avgLatencyMs": { "type": "number" },
        "tokenUsage": { "type": "integer" }
      }
    }
  },

  "$defs": {
    "trace": {
      "type": "object",
      "properties": {
        "traceId": { "type": "string" },
        "spanId": { "type": "string" },
        "parentSpanId": { "type": "string" },
        "ref": { "type": "string" },
        "timestamp": { "type": "string" },
        "durationMs": { "type": "number" },
        "attributes": {
          "type": "object",
          "additionalProperties": true
        }
      }
    },

    "alert": {
      "type": "object",
      "properties": {
        "id": { "type": "string" },
        "level": { "type": "string" },
        "message": { "type": "string" },
        "timestamp": { "type": "string" },
        "ref": { "type": "string" },
        "resolved": { "type": "boolean" }
      }
    }
  }
}
```

---

# 🟪 **NEW: AUDIT LAYER (FULL PRODUCTION EDITION)**

This is a **deep** and **enterprise-ready** implementation.

`workflow.audit.schema.json`

```json
{
  "$id": "https://kayys.tech/schema/v1/workflow.audit.schema.json",
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "title": "Audit Layer - Append Only Log",
  "type": "object",

  "required": ["workflowId", "entries"],

  "properties": {
    "workflowId": {
      "type": "string",
      "description": "Workflow this audit belongs to"
    },

    "entries": {
      "type": "array",
      "items": {
        "$ref": "#/$defs/auditEntry"
      }
    }
  },

  "$defs": {
    "auditEntry": {
      "type": "object",
      "required": ["id", "event", "timestamp", "actor", "integrity"],

      "properties": {
        "id": { "type": "string" },

        "event": {
          "type": "string",
          "description": "Event type: NODE_ADDED, EXECUTED, FAILED, etc."
        },

        "timestamp": { "type": "string" },

        "actor": {
          "$ref": "#/$defs/auditActor"
        },

        "target": {
          "$ref": "#/$defs/auditTarget"
        },

        "changes": {
          "type": "object",
          "properties": {
            "before": { "type": "object" },
            "after": { "type": "object" }
          }
        },

        "context": {
          "type": "object",
          "properties": {
            "ip": { "type": "string" },
            "userAgent": { "type": "string" },
            "sessionId": { "type": "string" },
            "traceId": { "type": "string" }
          }
        },

        "integrity": {
          "type": "object",
          "required": ["hash", "prevHash"],
          "properties": {
            "hash": { "type": "string" },
            "prevHash": { "type": "string" }
          }
        }
      }
    },

    "auditActor": {
      "type": "object",
      "properties": {
        "type": {
          "type": "string",
          "enum": ["user", "system", "external"]
        },
        "id": { "type": "string" },
        "name": { "type": "string" },
        "roles": {
          "type": "array",
          "items": { "type": "string" }
        }
      }
    },

    "auditTarget": {
      "type": "object",
      "properties": {
        "type": {
          "type": "string",
          "enum": ["workflow", "node", "connection", "plugin", "runtime"]
        },
        "id": { "type": "string" }
      }
    }
  }
}
```

---

# 🧱 **STRUCTURE VISUAL SUMMARY**

```
workflow/
 ├── workflow.schema.json
 ├── workflow.logic.schema.json
 ├── workflow.ui.schema.json
 ├── workflow.runtime.schema.json
 ├── workflow.observability.schema.json
 ├── workflow.audit.schema.json
 └── nodes/
     ├── node.base.schema.json
     ├── agent.llm.schema.json
     ├── memory.vector.schema.json
     ├── start.websocket.schema.json
     └── ...
```

This is now **enterprise-grade**, similar to:

* Temporal.io
* LangGraph Enterprise
* Kubernetes CRD Operators
* SOC2/ISO compliant workflow platforms

---

# 👉 Next step?

Choose one:

1. **Generate complete TypeScript typings from all schemas**
2. **Generate example workflow JSON using the new audit + observability**
3. **Generate backend validator (Dart/Java/TS)**
4. **Generate Flutter schema → dynamic form generator mapping**
5. **Add Role-Based Access Control + Permissions**

Which one?
