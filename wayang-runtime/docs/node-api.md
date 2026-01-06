# Node Component System - REST API Reference

## Base URL
```
http://localhost:8080/api/v1/nodes
```

---

## 📚 Node Definitions

### `GET /api/v1/nodes`
Get all node definitions

**Response:**
```json
[
  {
    "type": "memory.pgvector",
    "label": "PostgreSQL Vector Store",
    "category": "Memory",
    "subCategory": "Vector Database",
    "description": "Store and retrieve embeddings using pgvector",
    "icon": "database",
    "color": "#336791",
    "isAtomic": true,
    "isComposite": false,
    "configFields": [...],
    "ports": [...],
    "uiDescriptor": {...}
  }
]
```

---

### `GET /api/v1/nodes/{type}`
Get specific node definition

**Parameters:**
- `type` (path): Node type identifier (e.g., `memory.pgvector`)

**Example:**
```bash
curl http://localhost:8080/api/v1/nodes/agent.conversational
```

**Response:**
```json
{
  "type": "agent.conversational",
  "label": "Conversational Agent",
  "category": "AI Agents",
  "isComposite": true,
  "components": [
    {
      "id": "llm",
      "nodeType": "llm.openai",
      "config": {"model": "gpt-4"}
    },
    {
      "id": "memory",
      "nodeType": "memory.redis",
      "config": {}
    }
  ],
  "configFields": [
    {
      "name": "systemPrompt",
      "label": "System Prompt",
      "type": "TEXT",
      "required": true
    }
  ],
  "ports": [
    {
      "id": "message",
      "direction": "INPUT",
      "label": "User Message",
      "dataType": "string"
    },
    {
      "id": "response",
      "direction": "OUTPUT",
      "label": "Agent Response",
      "dataType": "string"
    }
  ]
}
```

---

### `GET /api/v1/nodes/category/{category}`
Get nodes by category

**Parameters:**
- `category` (path): Category name (e.g., `AI Agents`, `Memory`, `EIP`)

**Example:**
```bash
curl http://localhost:8080/api/v1/nodes/category/AI%20Agents
```

**Response:**
```json
[
  {
    "type": "agent.conversational",
    "label": "Conversational Agent",
    "category": "AI Agents"
  },
  {
    "type": "agent.orchestrator",
    "label": "Agent Orchestrator",
    "category": "AI Agents"
  }
]
```

---

### `GET /api/v1/nodes/categories`
Get all categories

**Response:**
```json
[
  "AI Agents",
  "Control Flow",
  "EIP",
  "Integration",
  "LLM",
  "Memory",
  "Tools"
]
```

---

### `GET /api/v1/nodes/search`
Search node definitions

**Query Parameters:**
- `q` (string): Search query

**Example:**
```bash
curl "http://localhost:8080/api/v1/nodes/search?q=database"
```

**Response:**
```json
[
  {
    "type": "memory.pgvector",
    "label": "PostgreSQL Vector Store",
    "description": "Store and retrieve embeddings using pgvector"
  },
  {
    "type": "tool.database",
    "label": "Database Query",
    "description": "Execute SQL queries"
  }
]
```

---

## 🎨 Node Palette

### `GET /api/v1/nodes/palette`
Get organized node palette for UI

**Response:**
```json
{
  "groups": [
    {
      "name": "AI Agents",
      "icon": "robot",
      "color": "#6366F1",
      "collapsed": false,
      "subGroups": [
        {
          "name": "Composite",
          "nodes": [
            {
              "type": "agent.conversational",
              "label": "Conversational Agent",
              "icon": "robot",
              "color": "#6366F1",
              "ui": {
                "width": 300,
                "height": 180,
                "badge": "AI"
              }
            }
          ]
        }
      ]
    },
    {
      "name": "Memory",
      "icon": "database",
      "color": "#10B981",
      "subGroups": [
        {
          "name": "Vector Database",
          "nodes": [...]
        },
        {
          "name": "Cache",
          "nodes": [...]
        }
      ]
    }
  ]
}
```

---

## 🔌 Plugin Management

### `POST /api/v1/nodes/plugins`
Upload plugin

**Content-Type:** `multipart/form-data`

**Body:**
- `file`: Plugin file (JSON or JAR)

**Example:**
```bash
curl -X POST http://localhost:8080/api/v1/nodes/plugins \
  -F "file=@custom-nodes.json" \
  -H "Authorization: Bearer $TOKEN"
```

**Response:**
```json
{
  "success": true,
  "message": "Plugin uploaded successfully",
  "nodesCount": 3
}
```

---

### `GET /api/v1/nodes/plugins`
List installed plugins

**Response:**
```json
[
  {
    "id": "com.example.custom-nodes",
    "name": "Custom Business Nodes",
    "version": "1.0.0",
    "author": "ACME Corp",
    "description": "Custom nodes for ACME business workflows",
    "nodesCount": 3
  }
]
```

---

## ✅ Validation

### `POST /api/v1/nodes/{type}/validate`
Validate node configuration

**Path Parameters:**
- `type`: Node type

**Request Body:**
```json
{
  "config": {
    "connectionString": "postgresql://localhost:5432/db",
    "tableName": "embeddings",
    "dimensions": 1536
  }
}
```

**Response (Valid):**
```json
{
  "isValid": true,
  "errors": [],
  "warnings": []
}
```

**Response (Invalid):**
```json
{
  "isValid": false,
  "errors": [
    "Missing required field: Connection String",
    "Invalid type for field: Dimensions"
  ],
  "warnings": [
    "Using default value for TTL"
  ]
}
```

---

## 📊 Complete API Examples

### Example 1: Build Node Palette UI

```javascript
// Frontend code to fetch and render palette
async function loadNodePalette() {
  const response = await fetch('/api/v1/nodes/palette');
  const palette = await response.json();
  
  // Render in React/Vue/etc
  return (
    <NodePalette>
      {palette.groups.map(group => (
        <PaletteGroup key={group.name} {...group}>
          {group.subGroups.map(sub => (
            <SubGroup key={sub.name} {...sub}>
              {sub.nodes.map(node => (
                <DraggableNode key={node.type} {...node} />
              ))}
            </SubGroup>
          ))}
        </PaletteGroup>
      ))}
    </NodePalette>
  );
}
```

---

### Example 2: Dynamic Configuration Form

```javascript
// Fetch node definition and render config form
async function renderConfigForm(nodeType) {
  const response = await fetch(`/api/v1/nodes/${nodeType}`);
  const definition = await response.json();
  
  return (
    <Form>
      {definition.configFields.map(field => {
        switch (field.type) {
          case 'TEXT':
            return (
              <TextField
                key={field.name}
                label={field.label}
                required={field.required}
                defaultValue={field.defaultValue}
                placeholder={field.placeholder}
              />
            );
          
          case 'SELECT':
            return (
              <Select
                key={field.name}
                label={field.label}
                options={field.options}
                defaultValue={field.defaultValue}
              />
            );
          
          case 'SLIDER':
            return (
              <Slider
                key={field.name}
                label={field.label}
                min={field.validation.min}
                max={field.validation.max}
                step={field.validation.step}
                defaultValue={field.defaultValue}
              />
            );
          
          case 'CODE':
            return (
              <CodeEditor
                key={field.name}
                label={field.label}
                language={field.uiProps.language}
              />
            );
        }
      })}
    </Form>
  );
}
```

---

### Example 3: Validate Before Save

```javascript
// Validate node configuration before saving
async function saveNodeConfig(nodeType, config) {
  // Validate first
  const validateResponse = await fetch(
    `/api/v1/nodes/${nodeType}/validate`,
    {
      method: 'POST',
      headers: {'Content-Type': 'application/json'},
      body: JSON.stringify({config})
    }
  );
  
  const validation = await validateResponse.json();
  
  if (!validation.isValid) {
    // Show errors
    showErrors(validation.errors);
    return;
  }
  
  // Show warnings but allow save
  if (validation.warnings.length > 0) {
    showWarnings(validation.warnings);
  }
  
  // Save to canvas
  await saveToCanvas(nodeType, config);
}
```

---

### Example 4: Search and Add Node

```javascript
// Search nodes and add to canvas
async function searchAndAddNode(query, position) {
  const response = await fetch(
    `/api/v1/nodes/search?q=${encodeURIComponent(query)}`
  );
  const nodes = await response.json();
  
  if (nodes.length === 0) {
    showMessage('No nodes found');
    return;
  }
  
  // Show selection dialog
  const selectedNode = await showNodeSelector(nodes);
  
  // Fetch full definition
  const defResponse = await fetch(`/api/v1/nodes/${selectedNode.type}`);
  const definition = await defResponse.json();
  
  // Add to canvas
  addNodeToCanvas({
    id: generateId(),
    type: definition.type,
    label: definition.label,
    position: position,
    config: getDefaultConfig(definition),
    ui: definition.uiDescriptor
  });
}
```

---

### Example 5: Upload Custom Plugin

```javascript
// Upload plugin file
async function uploadPlugin(file) {
  const formData = new FormData();
  formData.append('file', file);
  
  const response = await fetch('/api/v1/nodes/plugins', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${getAuthToken()}`
    },
    body: formData
  });
  
  const result = await response.json();
  
  if (result.success) {
    showSuccess(`Plugin uploaded: ${result.nodesCount} nodes added`);
    // Reload palette
    await reloadNodePalette();
  } else {
    showError(result.message);
  }
}
```

---

## 🔐 Authentication

All API endpoints require authentication (except `GET /nodes` endpoints which may be public).

**Headers:**
```
Authorization: Bearer <jwt-token>
```

or

```
X-API-Key: <api-key>
```

---

## 📈 Rate Limits

- **Anonymous**: 100 requests/hour
- **Authenticated**: 10,000 requests/hour
- **Plugin Upload**: 10 uploads/hour

---

## 🎯 Error Responses

### 400 Bad Request
```json
{
  "error": "Invalid request",
  "message": "Node type is required",
  "code": "INVALID_REQUEST"
}
```

### 404 Not Found
```json
{
  "error": "Not found",
  "message": "Node type 'invalid.node' not found",
  "code": "NODE_NOT_FOUND"
}
```

### 500 Internal Server Error
```json
{
  "error": "Internal server error",
  "message": "Failed to process request",
  "code": "INTERNAL_ERROR"
}
```

---

## 📦 OpenAPI Specification

Full OpenAPI 3.0 specification available at:
```
http://localhost:8080/openapi
```

Swagger UI available at:
```
http://localhost:8080/swagger-ui
```

---

This API enables building a complete **drag-and-drop visual workflow builder** with:
- ✅ Dynamic node palette
- ✅ Real-time validation
- ✅ Plugin system
- ✅ Type-safe configuration
- ✅ Professional UI rendering

Perfect for creating low-code platforms! 🚀