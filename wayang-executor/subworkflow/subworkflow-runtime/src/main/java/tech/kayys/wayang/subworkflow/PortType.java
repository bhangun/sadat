package tech.kayys.silat.ui.schema;

enum PortType {
    DATA,      // Data flow
    CONTROL,   // Control flow (success/failure transitions)
    EVENT      // Event-based connections
}