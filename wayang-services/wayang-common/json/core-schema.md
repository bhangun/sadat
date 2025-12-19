{
    "$id": "https://kayys.tech/schema/v1/node-base.schema.json",
    "$schema": "http://json-schema.org/draft-07/schema#",
    "title": "Node Base Schema",
    "type": "object",
    "required": [
        "id",
        "type"
    ],
    "properties": {
        "id": {
            "type": "string"
        },
        "type": {
            "type": "string"
        },
        "author": {
            "$ref": "https://kayys.tech/schema/v1/author.schema.json"
        },
        "description": {
            "$ref": "https://kayys.tech/schema/v1/description.schema.json"
        },
        "properties": {
            "$ref": "https://kayys.tech/schema/v1/properties.schema.json"
        },
        "inputs": {
            "$ref": "https://kayys.tech/schema/v1/port-descriptor.schema.json"
        },
        "outputs": {
            "$ref": "https://kayys.tech/schema/v1/output.schema.json"
        },
        "rules": {
            "$ref": "https://kayys.tech/schema/v1/rules.schema.json"
        },
        "validation": {
            "$ref": "https://kayys.tech/schema/v1/validation.schema.json"
        },
        "errorHandling": {
            "$ref": "https://kayys.tech/schema/v1/errorHandling.schema.json"
        },
        "observability": {
            "$ref": "https://kayys.tech/schema/v1/observability.schema.json"
        },
        "audit": {
            "$ref": "https://kayys.tech/schema/v1/audit.schema.json"
        },
        "sla": {
            "$ref": "https://kayys.tech/schema/v1/sla.schema.json"
        },
        "lifecycle": {
            "$ref": "https://kayys.tech/schema/v1/lifecycle.schema.json"
        },
        "execution": {
            "$ref": "https://kayys.tech/schema/v1/execution.schema.json"
        },
        "triggers": {
            "$ref": "https://kayys.tech/schema/v1/triggers.schema.json"
        },
        "policy": {
            "$ref": "https://kayys.tech/schema/v1/policy.schema.json"
        },
        "telemetry": {
            "$ref": "https://kayys.tech/schema/v1/telemetry.schema.json"
        },
        "provenance": {
            "$ref": "https://kayys.tech/schema/v1/provenance.schema.json"
        },
        "resourceProfile": {
            "$ref": "https://kayys.tech/schema/v1/resourceProfile.schema.json"
        }
    }
}


{
    "$id": "https://kayys.tech/schema/v1/logic.schema.json",
    "$schema": "http://json-schema.org/draft-07/schema#",
    "title": "Workflow Logic Layer",
    "type": "object",
    "required": [
        "nodes",
        "connections"
    ],
    "properties": {
        "nodes": {
            "type": "array",
            "items": {
                "#ref": "https://kayys.tech/schema/v1/node-base.schema.json"
            }
        },
        "connections": {
            "type": "array",
            "items": {
                "#ref": "https://kayys.tech/schema/v1/connection.schema.json"
            }
        },
        "port": {
            "#ref": "https://kayys.tech/schema/v1/port-descriptor.schema.json"
        },
        "rules": {
            "#ref": "https://kayys.tech/schema/v1/rules.schema.json"
        }
    }
}


{
    "$id": "https://kayys.tech/schema/v1/schema.json",
    "$schema": "http://json-schema.org/draft-07/schema#",
    "title": "Kayys Workflow Root",
    "type": "object",
    "description": "Workflow Schema",
    "author": {
        "$ref": "author.schema.json"
    },
    "required": [
        "id",
        "version",
        "logic"
    ],
    "properties": {
        "id": {
            "type": "string"
        },
        "version": {
            "type": "string"
        },
        "createdAt": {
            "type": "string",
            "format": "date-time"
        },
        "createdBy": {
            "type": "string"
        },
        "tenantId": {
            "type": "string"
        },
        "logic": {
            "$ref": "logic.schema.json"
        },
        "ui": {
            "$ref": "ui.schema.json"
        },
        "runtime": {
            "$ref": "runtime.schema.json"
        },
        "observability": {
            "$ref": "observability.schema.json"
        },
        "audit": {
            "$ref": "audit.schema.json"
        },
        "dataContracts": {
            "type": "array",
            "items": {
                "$ref": "data-contracts.schema.json"
            }
        },
        "policy": {
            "$ref": "policy.schema.json"
        },
        "telemetry": {
            "$ref": "telemetry.schema.json"
        },
        "provenance": {
            "$ref": "provenance.schema.json"
        },
        "simulation": {
            "$ref": "simulation.schema.json"
        },
        "comments": {
            "type": "array",
            "items": {
                "type": "string"
            }
        }
    }
}

{
    "$id": "https://kayys.tech/schema/v1/concerns/validation.schema.json",
    "$schema": "https://json-schema.org/draft/2020-12/schema",
    "title": "ValidationRule",
    "type": "object",
    "properties": {
        "id": {
            "type": "string"
        },
        "description": {
            "type": "string"
        },
        "level": {
            "type": "string",
            "enum": [
                "error",
                "warning",
                "info"
            ],
            "default": "error"
        },
        "jsonSchema": {
            "type": [
                "object",
                "string"
            ]
        },
        "cel": {
            "type": "string"
        },
        "customValidator": {
            "type": "object",
            "properties": {
                "functionRef": {
                    "type": "string"
                },
                "timeoutMs": {
                    "type": "integer",
                    "default": 3000
                }
            }
        },
        "onFailure": {
            "type": "object",
            "properties": {
                "action": {
                    "type": "string",
                    "enum": [
                        "reject",
                        "sanitize",
                        "coerce",
                        "log",
                        "warn"
                    ],
                    "default": "reject"
                },
                "message": {
                    "type": "string"
                },
                "fallback": {}
            }
        }
    }
}
{
    "$id": "https://kayys.tech/schema/v1/author.schema.json",
    "$schema": "http://json-schema.org/draft-07/schema#",
    "title": "Author Schema",
    "type": "object",
    "properties": {
        "name": {
            "type": "string"
        },
        "email": {
            "type": "string",
            "format": "email"
        },
        "organization": {
            "type": "string"
        }
    }
}
{
    "$id": "https://kayys.tech/schema/v1/description.schema.json",
    "$schema": "http://json-schema.org/draft-07/schema#",
    "type": "string",
    "maxLength": 2000
}

{
    "$id": "https://kayys.tech/schema/v1/workflow.audit.schema.json",
    "$schema": "http://json-schema.org/draft-07/schema#",
    "title": "Audit Layer - Append Only Log",
    "type": "object",
    "required": [
        "workflowId",
        "entries"
    ],
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
            "required": [
                "id",
                "event",
                "timestamp",
                "actor",
                "integrity"
            ],
            "properties": {
                "id": {
                    "type": "string"
                },
                "event": {
                    "type": "string",
                    "description": "Event type: NODE_ADDED, EXECUTED, FAILED, etc."
                },
                "timestamp": {
                    "type": "string"
                },
                "actor": {
                    "$ref": "#/$defs/auditActor"
                },
                "target": {
                    "$ref": "#/$defs/auditTarget"
                },
                "changes": {
                    "type": "object",
                    "properties": {
                        "before": {
                            "type": "object"
                        },
                        "after": {
                            "type": "object"
                        }
                    }
                },
                "context": {
                    "type": "object",
                    "properties": {
                        "ip": {
                            "type": "string"
                        },
                        "userAgent": {
                            "type": "string"
                        },
                        "sessionId": {
                            "type": "string"
                        },
                        "traceId": {
                            "type": "string"
                        }
                    }
                },
                "integrity": {
                    "type": "object",
                    "required": [
                        "hash",
                        "prevHash"
                    ],
                    "properties": {
                        "hash": {
                            "type": "string"
                        },
                        "prevHash": {
                            "type": "string"
                        },
                        "signature": {
                            "type": "string"
                        }
                    }
                }
            }
        },
        "auditActor": {
            "type": "object",
            "properties": {
                "type": {
                    "type": "string",
                    "enum": [
                        "user",
                        "system",
                        "external"
                    ]
                },
                "id": {
                    "type": "string"
                },
                "name": {
                    "type": "string"
                },
                "roles": {
                    "type": "array",
                    "items": {
                        "type": "string"
                    }
                }
            }
        },
        "auditTarget": {
            "type": "object",
            "properties": {
                "type": {
                    "type": "string",
                    "enum": [
                        "workflow",
                        "node",
                        "connection",
                        "plugin",
                        "runtime"
                    ]
                },
                "id": {
                    "type": "string"
                }
            }
        }
    }
}

{
    "$id": "https://kayys.tech/schema/v1/auth.schema.json",
    "$schema": "http://json-schema.org/draft-07/schema#",
    "title": "Authentication Schema",
    "type": "object",
    "properties": {
        "scheme": {
            "type": "string",
            "enum": [
                "none",
                "basic",
                "bearer",
                "oauth2",
                "apiKey",
                "aws_sigv4"
            ]
        },
        "credentialsRef": {
            "type": "string"
        },
        "scopes": {
            "type": "array",
            "items": {
                "type": "string"
            }
        }
    }
}

{
    "$id": "https://kayys.tech/schema/v1/connection.schema.json",
    "$schema": "http://json-schema.org/draft-07/schema#",
    "type": "array",
    "items": {
        "type": "object",
        "required": [
            "from",
            "to",
            "fromPort",
            "toPort"
        ],
        "properties": {
            "id": {
                "type": "string"
            },
            "from": {
                "type": "string"
            },
            "to": {
                "type": "string"
            },
            "fromPort": {
                "type": "string"
            },
            "toPort": {
                "type": "string"
            },
            "condition": {
                "type": "string",
                "description": "CEL expression"
            },
            "metadata": {
                "type": "object",
                "additionalProperties": true
            }
        }
    }
}

{
    "$id": "https://kayys.tech/schema/v1/connector.schema.json",
    "$schema": "http://json-schema.org/draft-07/schema#",
    "title": "Connector Schema",
    "type": "object",
    "required": [
        "id",
        "name",
        "type",
        "endpoint"
    ],
    "properties": {
        "id": {
            "$ref": "identifier.schema.json"
        },
        "name": {
            "type": "string"
        },
        "type": {
            "type": "string",
            "enum": [
                "http",
                "graphql",
                "mq",
                "database",
                "custom",
                "cloud"
            ]
        },
        "description": {
            "$ref": "description.schema.json"
        },
        "endpoint": {
            "type": "string",
            "description": "URI or connection string"
        },
        "auth": {
            "$ref": "http://kayys.tech/schema/v1/auth.schema.json"
        },
        "inputs": {
            "type": "array",
            "items": {
                "$ref": "http://kayys.tech/schema/v1/port-descriptor.schema.json"
            }
        },
        "outputs": {
            "$ref": "http://kayys.tech/schema/v1/outputs.schema.json"
        },
        "rateLimit": {
            "type": "object"
        },
        "resourceProfile": {
            "$ref": "https://kayys.tech/schema/v1/resource-profile.schema.json"
        },
        "policy": {
            "$ref": "https://kayys.tech/schema/v1/policy.schema.json"
        },
        "ui": {
            "type": "object"
        }
    }
}

{
    "$id": "https://kayys.tech/schema/v1/port-descriptor.schema.json",
    "$schema": "http://json-schema.org/draft-07/schema#",
    "title": "Port Descriptor",
    "type": "object",
    "required": [
        "name",
        "data"
    ],
    "properties": {
        "name": {
            "type": "string",
            "pattern": "^[a-zA-Z_][a-zA-Z0-9_]*$"
        },
        "displayName": {
            "type": "string"
        },
        "description": {
            "type": "string"
        },
        "data": {
            "type": "object",
            "required": [
                "type"
            ],
            "properties": {
                "type": {
                    "type": "string",
                    "enum": [
                        "json",
                        "string",
                        "markdown",
                        "number",
                        "boolean",
                        "object",
                        "array",
                        "binary",
                        "file_ref",
                        "image",
                        "audio",
                        "video",
                        "embedding",
                        "vector",
                        "llm_completion",
                        "tool_call",
                        "event",
                        "memory_ref",
                        "rag_ref"
                    ]
                },
                "format": {
                    "type": "string",
                    "enum": [
                        "text",
                        "html",
                        "yaml",
                        "base64",
                        "uri",
                        "sse",
                        "stream",
                        "jwt",
                        "sql",
                        "graphql",
                        "protobuf"
                    ]
                },
                "schema": {
                    "description": "JSON Schema or external schema reference",
                    "oneOf": [
                        {
                            "type": "object"
                        },
                        {
                            "type": "string",
                            "format": "uri"
                        }
                    ]
                },
                "multiplicity": {
                    "type": "string",
                    "enum": [
                        "single",
                        "list",
                        "map",
                        "stream"
                    ],
                    "default": "single"
                },
                "source": {
                    "type": "string",
                    "enum": [
                        "input",
                        "context",
                        "rag",
                        "memory",
                        "environment",
                        "secret"
                    ],
                    "default": "input"
                },
                "required": {
                    "type": "boolean",
                    "default": true
                },
                "defaultValue": {},
                "sensitive": {
                    "type": "boolean",
                    "default": false
                }
            }
        }
    }
}

{
    "$id": "https://kayys.tech/schema/v1/property.schema.json",
    "$schema": "http://json-schema.org/draft-07/schema#",
    "title": "Property description",
    "type": "object",
    "required": [
        "name",
        "type"
    ],
    "properties": {
        "name": {
            "type": "string",
            "pattern": "^[a-zA-Z_][a-zA-Z0-9_]*$"
        },
        "displayName": {
            "type": "string"
        },
        "description": {
            "type": "string"
        },
        "type": {
            "type": "string",
            "enum": [
                "string",
                "number",
                "integer",
                "boolean",
                "object",
                "array",
                "secret",
                "reference"
            ]
        },
        "default": {},
        "required": {
            "type": "boolean",
            "default": false
        },
        "validation": {
            "type": "object",
            "properties": {
                "min": {
                    "type": "number"
                },
                "max": {
                    "type": "number"
                },
                "pattern": {
                    "type": "string"
                },
                "enum": {
                    "type": "array"
                },
                "celExpression": {
                    "type": "string"
                }
            },
            "additionalProperties": false
        },
        "sensitive": {
            "type": "boolean",
            "default": false
        }
    }
}

{
    "$id": "https://kayys.tech/schema/v1/provenance.schema.json",
    "$schema": "http://json-schema.org/draft-07/schema#",
    "title": "Provenance Schema",
    "type": "object",
    "properties": {
        "enabled": {
            "type": "boolean",
            "default": true
        },
        "collect": {
            "type": "array",
            "items": {
                "type": "string",
                "enum": [
                    "inputs",
                    "outputs",
                    "model_tokens",
                    "latency",
                    "stderr",
                    "stdout"
                ]
            }
        },
        "retentionDays": {
            "type": "integer",
            "minimum": 0,
            "default": 30
        }
    }
}

{
    "$id": "https://kayys.tech/schema/v1/resource-profile.schema.json",
    "$schema": "http://json-schema.org/draft-07/schema#",
    "title": "Resource Profile Schema",
    "type": "object",
    "properties": {
        "cpu": {
            "type": "string",
            "pattern": "^\\d+m?$",
            "default": "100m"
        },
        "memory": {
            "type": "string",
            "pattern": "^\\d+[KMG]i?$",
            "default": "128Mi"
        },
        "gpu": {
            "type": "integer",
            "minimum": 0,
            "default": 0
        },
        "ephemeralStorage": {
            "type": "string",
            "pattern": "^\\d+[KMG]i?$"
        },
        "timeoutMs": {
            "type": "integer",
            "minimum": 100,
            "default": 30000
        }
    }
}

{
    "$id": "https://kayys.tech/schema/v1/rules.schema.json",
    "$schema": "http://json-schema.org/draft-07/schema#",
    "title": "Rule Schema",
    "type": "object",
    "properties": {
        "allowedConnections": {
            "type": "array",
            "description": "Whitelist of allowed node-type or port-type connections",
            "items": {
                "type": "object",
                "required": [
                    "from",
                    "to"
                ],
                "properties": {
                    "from": {
                        "type": "object",
                        "properties": {
                            "nodeType": {
                                "type": "string"
                            },
                            "portName": {
                                "type": "string"
                            },
                            "portType": {
                                "type": "string"
                            }
                        }
                    },
                    "to": {
                        "type": "object",
                        "properties": {
                            "nodeType": {
                                "type": "string"
                            },
                            "portName": {
                                "type": "string"
                            },
                            "portType": {
                                "type": "string"
                            }
                        }
                    },
                    "maxConnections": {
                        "type": "integer",
                        "minimum": 1,
                        "description": "Optional. Maximum number of edges allowed from 'from' → 'to'"
                    },
                    "direction": {
                        "type": "string",
                        "enum": [
                            "forward",
                            "reverse",
                            "bidirectional"
                        ],
                        "default": "forward"
                    }
                }
            }
        },
        "disallowedConnections": {
            "type": "array",
            "description": "Blacklist of forbidden node-type or port-type connections",
            "items": {
                "type": "object",
                "properties": {
                    "from": {
                        "type": "object",
                        "properties": {
                            "nodeType": {
                                "type": "string"
                            },
                            "portType": {
                                "type": "string"
                            },
                            "portName": {
                                "type": "string"
                            }
                        }
                    },
                    "to": {
                        "type": "object",
                        "properties": {
                            "nodeType": {
                                "type": "string"
                            },
                            "portType": {
                                "type": "string"
                            },
                            "portName": {
                                "type": "string"
                            }
                        }
                    },
                    "reason": {
                        "type": "string"
                    }
                }
            }
        },
        "topologyRules": {
            "type": "object",
            "properties": {
                "allowCycles": {
                    "type": "boolean",
                    "default": false
                },
                "maxDepth": {
                    "type": "integer",
                    "minimum": 1
                },
                "maxFanIn": {
                    "type": "integer",
                    "minimum": 1
                },
                "maxFanOut": {
                    "type": "integer",
                    "minimum": 1
                },
                "entryNodeTypes": {
                    "type": "array",
                    "items": {
                        "type": "string"
                    }
                },
                "exitNodeTypes": {
                    "type": "array",
                    "items": {
                        "type": "string"
                    }
                }
            }
        },
        "customRules": {
            "type": "array",
            "description": "CEL expressions evaluated on each attempted connection",
            "items": {
                "type": "object",
                "required": [
                    "expression",
                    "errorMessage"
                ],
                "properties": {
                    "expression": {
                        "type": "string",
                        "description": "CEL expression with context: fromNode, toNode, fromPort, toPort, workflow"
                    },
                    "severity": {
                        "type": "string",
                        "enum": [
                            "error",
                            "warning"
                        ],
                        "default": "error"
                    },
                    "errorMessage": {
                        "type": "string"
                    }
                }
            }
        }
    }
}

{
    "$id": "https://kayys.tech/schema/v1/workflow.runtime.schema.json",
    "$schema": "https://json-schema.org/draft/2020-12/schema",
    "title": "Runtime Layer",
    "type": "object",
    "properties": {
        "executedNodes": {
            "type": "array",
            "items": {
                "$ref": "#/$defs/executionEntry"
            }
        },
        "logs": {
            "type": "array",
            "items": {
                "type": "string"
            }
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
                "ref": {
                    "type": "string"
                },
                "timestamp": {
                    "type": "integer"
                },
                "latencyMs": {
                    "type": "number"
                },
                "tokensUsed": {
                    "type": "integer"
                },
                "costUsd": {
                    "type": "number"
                },
                "status": {
                    "type": "string"
                },
                "auditRef": {
                    "type": "string",
                    "description": "Points to audit entry for this execution"
                },
                "outputPreview": {
                    "type": "string"
                }
            }
        }
    }
}

{
    "$id": "https://kayys.tech/schema/v1/semver.schema.json",
    "$schema": "http://json-schema.org/draft-07/schema#",
    "title": "SemVer Schema",
    "type": "string",
    "pattern": "^\\d+\\.\\d+\\.\\d+(-[a-zA-Z0-9.-]+)?$"
}

{
    "$id": "https://kayys.tech/schema/v1/simulation.schema.json",
    "$schema": "http://json-schema.org/draft-07/schema#",
    "title": "Simulation Schema",
    "type": "object",
    "properties": {
        "enabled": {
            "type": "boolean",
            "default": false
        },
        "mockResponses": {
            "type": "object"
        },
        "seed": {
            "type": "string"
        }
    }
}

{
    "$id": "https://kayys.tech/schema/v1/concerns/sla.schema.json",
    "$schema": "https://json-schema.org/draft/2020-12/schema",
    "title": "SLAConfig",
    "type": "object",
    "properties": {
        "slo": {
            "type": "object",
            "properties": {
                "p95LatencyMs": {
                    "type": "integer"
                },
                "availabilityPct": {
                    "type": "number",
                    "minimum": 0,
                    "maximum": 100
                }
            }
        },
        "rateLimit": {
            "type": "object",
            "properties": {
                "requestsPerSecond": {
                    "type": "number"
                },
                "burst": {
                    "type": "integer"
                }
            }
        },
        "quotas": {
            "type": "object",
            "properties": {
                "monthlyTokens": {
                    "type": "integer"
                },
                "monthlyInvocations": {
                    "type": "integer"
                }
            }
        },
        "alerts": {
            "type": "array",
            "items": {
                "type": "object",
                "properties": {
                    "id": {
                        "type": "string"
                    },
                    "expr": {
                        "type": "string"
                    },
                    "for": {
                        "type": "string"
                    },
                    "severity": {
                        "type": "string",
                        "enum": [
                            "warning",
                            "critical"
                        ]
                    },
                    "notify": {
                        "type": "array",
                        "items": {
                            "type": "string"
                        }
                    }
                }
            }
        }
    }
}

{
    "$id": "https://kayys.tech/schema/v1/start.schema.json",
    "$schema": "http://json-schema.org/draft-07/schema#",
    "title": "Start Schema",
    "type": "object",
    "properties": {
        "enabled": {
            "type": "boolean",
            "default": false
        }
    }
}

{
    "$id": "https://kayys.tech/schema/v1/telemetry.json",
    "$schema": "http://json-schema.org/draft-07/schema#",
    "title": "Telemetry Schema",
    "type": "object",
    "properties": {
        "enabled": {
            "type": "boolean",
            "default": true
        },
        "events": {
            "type": "array",
            "items": {
                "type": "string",
                "enum": [
                    "latency",
                    "memory",
                    "token_usage",
                    "errors",
                    "throughput",
                    "custom"
                ]
            }
        },
        "sampleRate": {
            "type": "number",
            "minimum": 0,
            "maximum": 1,
            "default": 0.05
        },
        "traceContextHeader": {
            "type": "string",
            "default": "x-trace-id"
        },
        "metricsPrefix": {
            "type": "string"
        }
    }
}

{
    "$id": "https://kayys.tech/schema/v1/trigger.schema.json",
    "$schema": "http://json-schema.org/draft-07/schema#",
    "title": "Trigger Schema",
    "type": "array",
    "items": {
        "type": "object",
        "properties": {
            "type": {
                "type": "string",
                "enum": [
                    "cron",
                    "webhook",
                    "mq",
                    "manual",
                    "schedule"
                ]
            },
            "expression": {
                "type": "string"
            },
            "path": {
                "type": "string"
            }
        }
    }
}

{
    "$id": "https://kayys.tech/schema/v1/ui.schema.json",
    "$schema": "http://json-schema.org/draft-07/schema#",
    "title": "UI Schema",
    "type": "object",
    "properties": {
        "canvas": {
            "type": "object",
            "properties": {
                "zoom": {
                    "type": "number"
                },
                "offset": {
                    "type": "object",
                    "properties": {
                        "x": {
                            "type": "number"
                        },
                        "y": {
                            "type": "number"
                        }
                    }
                },
                "background": {
                    "type": "string"
                },
                "snapToGrid": {
                    "type": "boolean"
                }
            }
        },
        "nodes": {
            "type": "array",
            "items": {
                "$ref": "#/$defs/uiNode"
            }
        },
        "connections": {
            "type": "array",
            "items": {
                "$ref": "#/$defs/uiConnection"
            }
        }
    },
    "$defs": {
        "uiNode": {
            "type": "object",
            "required": [
                "ref",
                "position"
            ],
            "properties": {
                "ref": {
                    "type": "string"
                },
                "position": {
                    "type": "object",
                    "properties": {
                        "x": {
                            "type": "number"
                        },
                        "y": {
                            "type": "number"
                        }
                    }
                },
                "size": {
                    "type": "object",
                    "properties": {
                        "width": {
                            "type": "number"
                        },
                        "height": {
                            "type": "number"
                        }
                    }
                },
                "icon": {
                    "type": "string"
                },
                "color": {
                    "type": "string"
                },
                "shape": {
                    "type": "string"
                },
                "collapsed": {
                    "type": "boolean"
                },
                "zIndex": {
                    "type": "integer"
                },
                "themeVariant": {
                    "type": "string"
                }
            }
        },
        "uiConnection": {
            "type": "object",
            "properties": {
                "ref": {
                    "type": "string"
                },
                "color": {
                    "type": "string"
                },
                "pathStyle": {
                    "type": "string"
                }
            }
        }
    }
}

{
    "$id": "https://kayys.tech/schema/v1/data-contract.schema.json",
    "$schema": "http://json-schema.org/draft-07/schema#",
    "title": "Data Contract Schema",
    "type": "object",
    "required": [
        "id",
        "type"
    ],
    "properties": {
        "id": {
            "type": "string"
        },
        "displayName": {
            "type": "string"
        },
        "description": {
            "type": "string"
        },
        "type": {
            "type": "string"
        },
        "jsonSchema": {
            "oneOf": [
                {
                    "type": "object"
                },
                {
                    "type": "string",
                    "format": "uri"
                }
            ]
        },
        "constraints": {
            "type": "object",
            "additionalProperties": true
        },
        "transformations": {
            "type": "array",
            "items": {
                "type": "object"
            }
        },
        "version": {
            "type": "string"
        }
    }
}

{
    "$id": "https://kayys.tech/schema/v1/dependancy.schema.json",
    "$schema": "http://json-schema.org/draft-07/schema#",
    "title": "Dependancy Schema",
    "type": "object",
    "required": [
        "id",
        "version"
    ],
    "properties": {
        "id": {
            "type": "string"
        },
        "version": {
            "type": "string",
            "pattern": "^[\\^~]?\\d+\\.\\d+\\.\\d+(-[a-zA-Z0-9.-]+)?$"
        },
        "optional": {
            "type": "boolean",
            "default": false
        },
        "scope": {
            "type": "string",
            "enum": [
                "runtime",
                "compile",
                "test"
            ],
            "default": "runtime"
        }
    }
}

{
    "$id": "https://kayys.tech/schema/v1/end.schema.json",
    "$schema": "http://json-schema.org/draft-07/schema#",
    "title": "End Schema",
    "type": "object",
    "properties": {
        "enabled": {
            "type": "boolean",
            "default": false
        }
    }
}

{
    "$id": "https://kayys.tech/schema/v1/endpoint.schema.json",
    "$schema": "http://json-schema.org/draft-07/schema#",
    "title": "Endpoint Schema",
    "type": "string",
    "description": "URI or connection string"
}

{
    "$id": "https://kayys.tech/schema/v1/environment.schema.json",
    "$schema": "http://json-schema.org/draft-07/schema#",
    "title": "Environment Schema",
    "type": "string",
    "enum": [
        "dev",
        "staging",
        "prod"
    ],
    "default": "dev"
}

{
    "$id": "https://kayys.tech/schema/v1/concerns/errorHandlingConfig.schema.json",
    "$schema": "https://json-schema.org/draft/2020-12/schema",
    "title": "ErrorHandlingConfig",
    "type": "object",
    "properties": {
        "retryPolicy": {
            "type": "object",
            "properties": {
                "maxAttempts": {
                    "type": "integer",
                    "minimum": 0,
                    "default": 3
                },
                "initialDelayMs": {
                    "type": "integer",
                    "default": 200
                },
                "maxDelayMs": {
                    "type": "integer",
                    "default": 30000
                },
                "backoff": {
                    "type": "string",
                    "enum": [
                        "fixed",
                        "exponential",
                        "linear"
                    ],
                    "default": "exponential"
                },
                "jitter": {
                    "type": "boolean",
                    "default": true
                },
                "retryOn": {
                    "type": "array",
                    "items": {
                        "type": "string"
                    }
                }
            }
        },
        "fallback": {
            "type": "object",
            "properties": {
                "type": {
                    "type": "string",
                    "enum": [
                        "node",
                        "static",
                        "none"
                    ],
                    "default": "none"
                },
                "nodeId": {
                    "type": "string"
                },
                "staticResponse": {
                    "type": [
                        "object",
                        "string"
                    ]
                }
            }
        },
        "circuitBreaker": {
            "type": "object",
            "properties": {
                "enabled": {
                    "type": "boolean",
                    "default": false
                },
                "failureThreshold": {
                    "type": "integer",
                    "default": 5
                },
                "successThreshold": {
                    "type": "integer",
                    "default": 2
                },
                "timeoutMs": {
                    "type": "integer",
                    "default": 60000
                },
                "windowMs": {
                    "type": "integer",
                    "default": 600000
                }
            }
        },
        "escalation": {
            "type": "object",
            "properties": {
                "onSeverityAtLeast": {
                    "type": "string",
                    "enum": [
                        "WARN",
                        "ERROR",
                        "CRITICAL"
                    ],
                    "default": "ERROR"
                },
                "notify": {
                    "type": "array",
                    "items": {
                        "type": "string"
                    }
                }
            }
        },
        "humanReview": {
            "type": "object",
            "properties": {
                "enabled": {
                    "type": "boolean",
                    "default": false
                },
                "thresholdSeverity": {
                    "type": "string",
                    "enum": [
                        "WARN",
                        "ERROR",
                        "CRITICAL"
                    ],
                    "default": "CRITICAL"
                },
                "reviewQueue": {
                    "type": "string"
                }
            }
        }
    }
}

{
    "$id": "https://kayys.tech/schema/v1/concerns/error.schema.json",
    "$schema": "https://json-schema.org/draft/2020-12/schema",
    "title": "Error Payload",
    "type": "object",
    "required": [
        "code",
        "message",
        "timestamp"
    ],
    "properties": {
        "code": {
            "type": "string"
        },
        "type": {
            "type": "string",
            "enum": [
                "ToolError",
                "LLMError",
                "NetworkError",
                "ValidationError",
                "Timeout",
                "PluginLoadError",
                "SecurityError",
                "CircuitOpen",
                "Unknown"
            ]
        },
        "message": {
            "type": "string"
        },
        "details": {
            "type": "object",
            "additionalProperties": true
        },
        "retryable": {
            "type": "boolean"
        },
        "severity": {
            "type": "string",
            "enum": [
                "DEBUG",
                "INFO",
                "WARN",
                "ERROR",
                "CRITICAL"
            ],
            "default": "ERROR"
        },
        "origin": {
            "type": "object",
            "properties": {
                "nodeId": {
                    "type": "string"
                },
                "nodeType": {
                    "type": "string"
                },
                "runId": {
                    "type": "string",
                    "format": "uuid"
                }
            }
        },
        "attempt": {
            "type": "integer",
            "minimum": 0
        },
        "maxAttempts": {
            "type": "integer",
            "minimum": 0
        },
        "timestamp": {
            "type": "string",
            "format": "date-time"
        },
        "provenanceRef": {
            "type": "string",
            "format": "uuid"
        },
        "suggestedAction": {
            "type": "string",
            "enum": [
                "retry",
                "fallback",
                "escalate",
                "human_review",
                "abort",
                "ignore"
            ]
        }
    }
}

{
    "$id": "https://kayys.tech/schema/v1/execution.schema.json",
    "$schema": "http://json-schema.org/draft-07/schema#",
    "title": "Execution Schema",
    "type": "object",
    "properties": {
        "mode": {
            "type": "string",
            "enum": [
                "sync",
                "async",
                "stream"
            ],
            "default": "sync"
        },
        "retryPolicy": {
            "$ref": "#/definitions/ErrorHandlingConfig"
        },
        "timeoutMs": {
            "type": "integer"
        },
        "emitEvents": {
            "type": "array",
            "items": {
                "type": "string"
            }
        },
        "sideEffects": {
            "type": "array",
            "items": {
                "type": "string"
            }
        }
    }
}

{
    "$id": "https://kayys.tech/schema/v1/identifier.schema.json",
    "$schema": "http://json-schema.org/draft-07/schema#",
    "title": "Identifier Schema",
    "type": "string",
    "pattern": "^[a-z0-9_.-]+(/[a-z0-9_.-]+)?$",
    "description": "Unique plugin ID (namespace/name)"
}

{
    "$id": "https://kayys.tech/schema/v1/lifecycle.schema.json",
    "$schema": "http://json-schema.org/draft-07/schema#",
    "title": "Lifecycle Schema",
    "type": "object",
    "properties": {
        "initialize": {
            "type": "string"
        },
        "beforeExecute": {
            "type": "string"
        },
        "afterExecute": {
            "type": "string"
        },
        "onError": {
            "type": "string"
        },
        "stop": {
            "type": "string"
        }
    }
}

{
    "$id": "https://kayys.tech/schema/v1/workflow.observability.schema.json",
    "$schema": "https://json-schema.org/draft/2020-12/schema",
    "title": "Observability Layer",
    "type": "object",
    "properties": {
        "traces": {
            "type": "array",
            "items": {
                "$ref": "#/$defs/trace"
            }
        },
        "alerts": {
            "type": "array",
            "items": {
                "$ref": "#/$defs/alert"
            }
        },
        "metrics": {
            "type": "object",
            "properties": {
                "throughput": {
                    "type": "number"
                },
                "errorRate": {
                    "type": "number"
                },
                "avgLatencyMs": {
                    "type": "number"
                },
                "tokenUsage": {
                    "type": "integer"
                }
            }
        }
    },
    "$defs": {
        "trace": {
            "type": "object",
            "properties": {
                "traceId": {
                    "type": "string"
                },
                "spanId": {
                    "type": "string"
                },
                "parentSpanId": {
                    "type": "string"
                },
                "ref": {
                    "type": "string"
                },
                "timestamp": {
                    "type": "string"
                },
                "durationMs": {
                    "type": "number"
                },
                "attributes": {
                    "type": "object",
                    "additionalProperties": true
                }
            }
        },
        "alert": {
            "type": "object",
            "properties": {
                "id": {
                    "type": "string"
                },
                "level": {
                    "type": "string"
                },
                "message": {
                    "type": "string"
                },
                "timestamp": {
                    "type": "string"
                },
                "ref": {
                    "type": "string"
                },
                "resolved": {
                    "type": "boolean"
                }
            }
        }
    }
}

{
    "$id": "https://kayys.tech/schema/v1/output.schema.json",
    "$schema": "http://json-schema.org/draft-07/schema#",
    "title": "Output Schema",
    "type": "object",
    "properties": {
        "channels": {
            "type": "array",
            "description": "Multiple output branches",
            "items": {
                "type": "object",
                "required": [
                    "name",
                    "type"
                ],
                "properties": {
                    "name": {
                        "type": "string"
                    },
                    "displayName": {
                        "type": "string"
                    },
                    "description": {
                        "type": "string"
                    },
                    "type": {
                        "type": "string",
                        "enum": [
                            "success",
                            "error",
                            "conditional",
                            "agent_decision",
                            "stream",
                            "event"
                        ]
                    },
                    "condition": {
                        "type": "string",
                        "description": "CEL expression that determines routing"
                    },
                    "schema": {
                        "$ref": "output.schema.json"
                    }
                }
            }
        },
        "default": {
            "$ref": "output.schema.json"
        },
        "streaming": {
            "type": "boolean",
            "default": false
        }
    }
}

{
    "$id": "https://kayys.tech/schema/v1/plugin-implementation.schema.json",
    "$schema": "http://json-schema.org/draft-07/schema#",
    "title": "Plugin Implementation Schema",
    "type": "object",
    "required": [
        "type",
        "coordinate",
        "digest"
    ],
    "properties": {
        "type": {
            "type": "string",
            "enum": [
                "maven",
                "wasm",
                "container",
                "python",
                "jar",
                "node",
                "binary"
            ]
        },
        "coordinate": {
            "type": "string",
            "description": "Artifact coordinate (e.g., 'com.example:plugin:1.0.0')"
        },
        "digest": {
            "type": "string",
            "pattern": "^(sha256|sha512|blake3):[a-f0-9]{64,128}$"
        },
        "repository": {
            "type": "string",
            "format": "uri"
        },
        "entrypoint": {
            "type": "string"
        },
        "runtime": {
            "type": "object",
            "properties": {
                "javaVersion": {
                    "type": "string"
                },
                "wasmRuntime": {
                    "type": "string"
                },
                "pythonVersion": {
                    "type": "string"
                },
                "nodeVersion": {
                    "type": "string"
                }
            }
        }
    }
}

{
    "$id": "https://kayys.tech/schema/v1/plugin.schema.json",
    "$schema": "http://json-schema.org/draft-07/schema#",
    "title": "Plugin Schema",
    "type": "object",
    "required": [
        "id",
        "name",
        "version",
        "implementation",
        "inputs",
        "outputs",
        "type",
        "coordinate",
        "digest"
    ],
    "properties": {
        "id": {
            "type": "string",
            "pattern": "^[a-z0-9-]+/[a-z0-9-]+$",
            "description": "Unique plugin ID (namespace/name)"
        },
        "name": {
            "type": "string",
            "minLength": 3,
            "maxLength": 128
        },
        "version": {
            "$ref": "#/definitions/SemVer"
        },
        "description": {
            "type": "string",
            "maxLength": 2000
        },
        "author": {
            "type": "object",
            "properties": {
                "name": {
                    "type": "string"
                },
                "email": {
                    "type": "string",
                    "format": "email"
                },
                "organization": {
                    "type": "string"
                }
            }
        },
        "implementation": {
            "$ref": "#/definitions/PluginImplementation"
        },
        "inputs": {
            "type": "array",
            "items": {
                "$ref": "#/definitions/PortDescriptorV2"
            }
        },
        "outputs": {
            "$ref": "#/definitions/OutputsV2"
        },
        "properties": {
            "type": "array",
            "items": {
                "$ref": "#/definitions/PropertyDescriptor"
            }
        },
        "capabilities": {
            "type": "array",
            "items": {
                "type": "string",
                "enum": [
                    "network",
                    "filesystem",
                    "database",
                    "llm_access",
                    "gpu",
                    "tool_execution",
                    "rag_access",
                    "memory_access"
                ]
            },
            "uniqueItems": true
        },
        "requiredSecrets": {
            "type": "array",
            "items": {
                "type": "string"
            }
        },
        "sandboxLevel": {
            "type": "string",
            "enum": [
                "trusted",
                "semi-trusted",
                "untrusted"
            ],
            "default": "semi-trusted"
        },
        "resourceProfile": {
            "$ref": "#/definitions/ResourceProfile"
        },
        "errorHandling": {
            "$ref": "#/definitions/ErrorHandlingConfig"
        },
        "checksum": {
            "type": "string",
            "pattern": "^(sha256|sha512|blake3):[a-f0-9]{64,128}$"
        },
        "signature": {
            "type": "string"
        },
        "publishedBy": {
            "type": "string"
        },
        "createdAt": {
            "type": "string",
            "format": "date-time"
        },
        "status": {
            "type": "string",
            "enum": [
                "pending",
                "scanning",
                "approved",
                "rejected",
                "revoked",
                "deprecated"
            ],
            "default": "pending"
        },
        "tags": {
            "type": "array",
            "items": {
                "type": "string"
            }
        },
        "dependencies": {
            "type": "array",
            "items": {
                "$ref": "#/definitions/DependencyDescriptor"
            }
        },
        "compatibility": {
            "type": "object",
            "properties": {
                "engine": {
                    "type": "string"
                },
                "minRuntimeVersion": {
                    "type": "string"
                },
                "maxRuntimeVersion": {
                    "type": "string"
                },
                "breakingChanges": {
                    "type": "array",
                    "items": {
                        "type": "string"
                    }
                }
            }
        },
        "type": {
            "type": "string",
            "enum": [
                "maven",
                "wasm",
                "container",
                "python",
                "jar",
                "node",
                "binary"
            ]
        },
        "coordinate": {
            "type": "string",
            "description": "Artifact coordinate (e.g., 'com.example:plugin:1.0.0')"
        },
        "digest": {
            "type": "string",
            "pattern": "^(sha256|sha512|blake3):[a-f0-9]{64,128}$"
        },
        "repository": {
            "type": "string",
            "format": "uri"
        },
        "entrypoint": {
            "type": "string"
        },
        "runtime": {
            "type": "object",
            "properties": {
                "javaVersion": {
                    "type": "string"
                },
                "wasmRuntime": {
                    "type": "string"
                },
                "pythonVersion": {
                    "type": "string"
                },
                "nodeVersion": {
                    "type": "string"
                }
            }
        }
    }
}

{
    "$id": "https://kayys.tech/schema/v1/policy.schema.json",
    "$schema": "http://json-schema.org/draft-07/schema#",
    "title": "Policy Schema",
    "type": "object",
    "properties": {
        "accessPolicy": {
            "type": "object",
            "properties": {
                "rolesAllowed": {
                    "type": "array",
                    "items": {
                        "type": "string"
                    }
                },
                "principals": {
                    "type": "array",
                    "items": {
                        "type": "string"
                    }
                }
            }
        },
        "rateLimit": {
            "type": "object",
            "properties": {
                "requestsPerSecond": {
                    "type": "number"
                },
                "burst": {
                    "type": "integer"
                }
            }
        },
        "dataRetentionDays": {
            "type": "integer",
            "minimum": 0
        },
        "piiHandling": {
            "type": "string",
            "enum": [
                "redact",
                "hash",
                "tokenize",
                "none"
            ],
            "default": "redact"
        },
        "networkEgressPolicy": {
            "type": "object",
            "properties": {
                "allowedDomains": {
                    "type": "array",
                    "items": {
                        "type": "string",
                        "format": "uri"
                    }
                },
                "deniedDomains": {
                    "type": "array",
                    "items": {
                        "type": "string"
                    }
                }
            }
        }
    }
}