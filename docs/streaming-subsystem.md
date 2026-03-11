# Streaming Subsystem

## Stream Lifecycle & Message Handling

```plantuml
@startuml streaming-subsystem
title Streaming Subsystem — Stream Lifecycle & Message Handling

' ── Stream lifecycle ──────────────────────────────────────────────
state "Stream Lifecycle" as lifecycle {
    [*] --> Validating : createStream(name, parent)

    Validating --> Error_InvalidFormat   : invalid name format
    Validating --> Error_AlreadyExists   : group already exists
    Validating --> Error_ParentNotFound  : parent not in DB (child streams)
    Validating --> Persisting            : valid

    Persisting --> Active : saved to DB\n+ NATS dispatcher initialised

    Active --> Active       : messages received
    Active --> Deleted      : deleteStream(name)

    Deleted --> [*]

    Error_InvalidFormat  --> [*]
    Error_AlreadyExists  --> [*]
    Error_ParentNotFound --> [*]
}

' ── Message handling (inside Active state) ────────────────────────
state Active {
    [*] --> ReceivingMessage

    ReceivingMessage --> ParsingMessage   : NATS message arrives

    ParsingMessage --> LookingUpProducer  : JSON → TimeSeriesMessageDTO
    ParsingMessage --> DeadLetterQueue    : parse error

    LookingUpProducer --> PersistingRecord : producer found
    LookingUpProducer --> DeadLetterQueue  : producer null / not found

    PersistingRecord --> Acknowledged  : record saved to timeseries table
    DeadLetterQueue  --> Acknowledged  : forwarded to _DLQ.<subject>

    Acknowledged --> ReceivingMessage : ready for next message
}

@enduml
```

---

## State Descriptions

### Stream Lifecycle

| State | Description |
|---|---|
| **Validating** | Entry point. Checks name format, uniqueness, and parent existence (for child streams). |
| **Persisting** | Stream record is written to the database and the NATS dispatcher is initialised. |
| **Active** | Stream is live and accepting messages. |
| **Deleted** | Stream has been removed via `deleteStream(name)`. Terminal state. |
| **Error_InvalidFormat** | Name did not pass format validation. Terminal state. |
| **Error_AlreadyExists** | A group with this name already exists. Terminal state. |
| **Error_ParentNotFound** | The specified parent group was not found in the database. Terminal state. |

### Message Handling (inside Active)

| State | Description |
|---|---|
| **ReceivingMessage** | Idle, waiting for the next NATS message. |
| **ParsingMessage** | Deserialises the raw NATS payload into a `TimeSeriesMessageDTO`. |
| **LookingUpProducer** | Resolves the producer associated with the incoming message. |
| **PersistingRecord** | Writes the validated time-series record to the database. |
| **DeadLetterQueue** | Handles failed messages (parse error or unknown producer) by forwarding to `_DLQ.<subject>`. |
| **Acknowledged** | Message processing is complete. Returns to `ReceivingMessage`. |
