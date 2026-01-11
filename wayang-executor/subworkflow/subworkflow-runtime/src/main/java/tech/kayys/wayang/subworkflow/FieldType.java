package tech.kayys.silat.ui.schema;

enum FieldType {
    TEXT,
    TEXTAREA,
    NUMBER,
    SELECT,
    MULTISELECT,
    CHECKBOX,
    RADIO,
    DATE,
    TIME,
    DURATION,
    JSON,
    CODE,              // Code editor with syntax highlighting
    KEY_VALUE,         // Key-value pair editor
    MAPPING,           // Input/output mapping editor
    FILE_UPLOAD,
    COLOR_PICKER,
    SLIDER,
    EXPRESSION,        // Expression builder
    WORKFLOW_SELECTOR, // Select another workflow (for sub-workflows)
    CREDENTIAL_SELECTOR // Select stored credentials
}