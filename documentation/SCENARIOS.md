## Scenarios 

###  General info:
We have multiple possible values for credit_dst_type = `[pan, panId, external_card_id, card_token, mobile, iban]`.

According to this field we can implement routing logic (use this logic to find actual corresponding service):

- pan, panId, external_card_id, card_token -> `p2p-service`
- mobile -> `simple-payments-service` (p2m)
- iban -> `simple-payments-service` (c2a)
- else -> `simple-payments-service` (tokenization)

### 1. POST /check (generic check endpoint)
```mermaid
sequenceDiagram
    participant ma as mobile-app
    participant pg as payment-gateway 
    participant cs as corresponding-service
    ma ->> pg: POST /check
    pg ->> cs: POST /check
    cs ->> pg: check response
    pg ->> ma: check response
```

### 2. POST /payments (generic payment endpoint)
```mermaid
sequenceDiagram
    participant ma as mobile-app
    participant pg as payment-gateway 
    participant cs as corresponding-service
    ma ->> pg: POST /payments
    pg ->> cs: POST /payments
    cs ->> pg: payments response
    pg ->> ma: payments response
```

### 3. PATCH /cancel-payment (generic cancel payment endpoint)
```mermaid
sequenceDiagram
    participant ma as mobile-app
    participant pg as payment-gateway
    participant ps as payments-service
    participant cs as corresponding-service
    ma ->> pg: PATCH /cancel-payment
    pg ->> ps: GET /payments/{payment_id}
    ps ->> pg: credit destination type response
    pg ->> cs: GET /payments/{payment_id}
    cs ->> pg: Status response
    pg ->> ma: Status response
```

### 4. GET /payments/{payment_id} (generic get payment status endpoint) 

Stage 1
```mermaid
sequenceDiagram
    participant ma as mobile-app
    participant pg as payment-gateway
    participant ps as payments-service
    participant cs as corresponding-service
    ma ->> pg: GET /payments/{payment_id}
    pg ->> ps: GET /payments/{payment_id}
    ps ->> pg: credit destination type response
    pg ->> cs: GET /payments/{payment_id}
    cs ->> pg: Status response
    pg ->> ma: Status response
```

Stage 2 - Avoid extra request using redis


### 5. GET /repeat/{operationId} (generic repeat payment endpoint)

Stage 1
```mermaid
sequenceDiagram
    participant ma as mobile-app
    participant pg as payment-gateway
    participant ps as payments-service
    participant cs as corresponding-service
    ma ->> pg: GET /repeat/{operation_id}
    pg ->> ps: GET /operations/{operation_id}/payment
    ps ->> pg: credit destination type response
    pg ->> cs: GET /repeat/{payment_id}
    cs ->> pg: Repetition response
    pg ->> ma: Repetition response
```

Stage 2 - Avoid extra request using redis