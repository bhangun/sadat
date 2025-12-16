
---

## 2. BUILD COMPARISON EXAMPLES

### Example 1: Simple RAG-Only Agent

```
WORKFLOW DEFINITION:
- 1 START node
- 1 RAG node (query documents)
- 1 END node

FULL PLATFORM BUILD:
├── All runtime components: 150MB+
├── All node types: 30MB
├── Full orchestrator: 20MB
├── All dependencies: 50MB+
└── TOTAL: ~250MB+

STANDALONE BUILD (Generated):
├── wayang-runtime-minimal: 2MB
├── wayang-runtime-rag: 3MB
├── wayang-node-rag: 0.5MB
├── jvector (vector lib): 1MB
└── TOTAL: ~6.5MB (97% reduction!)
```

### Example 2: LLM + Tool Agent

```
WORKFLOW DEFINITION:
- 1 START node
- 1 AGENT node (LLM reasoning)
- 1 TOOL node (API call)
- 1 DECISION node (conditional)
- 1 END node

FULL PLATFORM BUILD:
└── TOTAL: ~250MB+

STANDALONE BUILD (Generated):
├── wayang-runtime-minimal: 2MB
├── wayang-runtime-llm: 5MB
├── wayang-runtime-tools: 2MB
├── wayang-node-agent: 0.5MB
├── wayang-node-tool: 0.5MB
├── wayang-node-decision: 0.3MB
├── okhttp: 1MB
├── langchain4j-core: 3MB
└── TOTAL: ~14.3MB (94% reduction!)
```

### Example 3: Complex Multi-Node Agent

```
WORKFLOW DEFINITION:
- 1 START node
- 2 AGENT nodes
- 2 RAG nodes
- 1 TOOL node
- 1 MEMORY node
- 1 GUARDRAILS node
- 1 EVALUATOR node
- 1 END node

FULL PLATFORM BUILD:
└── TOTAL: ~250MB+

STANDALONE BUILD (Generated):
├── wayang-runtime-minimal: 2MB
├── wayang-runtime-llm: 5MB
├── wayang-runtime-rag: 3MB
├── wayang-runtime-tools: 2MB
├── wayang-runtime-memory: 1MB
├── wayang-runtime-guardrails: 2MB
├── All used node implementations: 3MB
├── External dependencies: 8MB
└── TOTAL: ~26MB (90% reduction!)
```
