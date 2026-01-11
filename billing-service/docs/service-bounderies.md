# Service Boundary Analysis - CAS vs Billing Service

## 🎯 Executive Summary

This document clarifies the **service boundaries** between:
- **Consumer Access Service (CAS)** - Access governance and entitlements
- **Billing Service** - Financial transactions and invoicing
- **Marketplace Service** - Could be separate or module within CAS

---

## 📊 Responsibility Matrix

| Feature | CAS | Billing Service | Marketplace | Notes |
|---------|-----|----------------|-------------|-------|
| **Product Catalog** | ✅ Master | 📖 Read-only | 📖 Read-only | CAS owns product definitions |
| **Product Pricing** | ❌ | ✅ Owner | ❌ | Billing owns all pricing |
| **Plans (Tiers)** | ✅ Owner | 📖 Read-only | ❌ | CAS defines quotas/features |
| **Plan Pricing** | ❌ | ✅ Owner | ❌ | Billing owns subscription prices |
| **Subscription State** | ✅ Owner | 🔄 Updates | ❌ | CAS owns state, Billing updates it |
| **Subscription Creation** | ✅ Creates | 🔄 Notified | ❌ | User subscribes via CAS, Billing handles payment |
| **Usage Recording** | ✅ Owner | 📖 Reads | ❌ | CAS is source of truth for usage |
| **Usage Aggregation** | ✅ Summarizes | 📖 Reads | ❌ | CAS aggregates, Billing reads for invoicing |
| **Quota Management** | ✅ Owner | ❌ | ❌ | CAS enforces quotas |
| **Quota Exceeded** | ✅ Detects | 🔔 Notified | ❌ | CAS detects, may notify Billing |
| **Invoice Generation** | ❌ | ✅ Owner | ❌ | Billing only |
| **Payment Processing** | ❌ | ✅ Owner | ❌ | Billing only |
| **Payment Status** | 🔔 Receives | ✅ Owner | ❌ | Billing updates, CAS reacts |
| **Marketplace Items** | ✅ Owner | 📖 Reads | 🎯 Module | CAS owns catalog |
| **Marketplace Pricing** | ❌ | ✅ Owner | 🎯 Module | Billing owns pricing |
| **Marketplace Purchase** | ✅ Entitlement | ✅ Payment | 🎯 Orchestrates | Both involved |
| **Marketplace Revenue** | ❌ | ✅ Owner | 📊 Reports | Billing handles money |
| **Orders (Purchases)** | ❌ | ✅ Owner | 🎯 Initiates | Billing owns order lifecycle |
| **Entitlements** | ✅ Owner | ❌ | 🔔 Grants | CAS grants after Billing confirms |

**Legend:**
- ✅ Owner/Primary - Service owns this data/logic
- 🔄 Updates - Service can modify state owned by another
- 📖 Read-only - Service only reads this data
- 🔔 Receives/Notified - Service receives events
- 🎯 Module/Orchestrates - Special role
- ❌ Not Responsible

---

## 🏗️ Service Architecture Decision

### ✅ RECOMMENDED: 3-Service Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    CAS (Access)                              │
│  - Consumers, API Keys, Products                            │
│  - Plans (quotas/features)                                  │
│  - Subscriptions (state only)                               │
│  - Usage (recording & aggregation)                          │
│  - Entitlements (marketplace + plan)                        │
│  - Access decisions (introspection)                         │
└─────────────────────────────────────────────────────────────┘
                            ↕ Events
┌─────────────────────────────────────────────────────────────┐
│                 Billing Service (Money)                      │
│  - Pricing (products, plans, marketplace)                   │
│  - Orders (purchases)                                        │
│  - Payments (processing)                                     │
│  - Invoices (generation)                                     │
│  - Subscriptions (payment tracking)                         │
└─────────────────────────────────────────────────────────────┘
                            ↕ Events
┌─────────────────────────────────────────────────────────────┐
│              Marketplace Module (in CAS)                     │
│  - Item catalog                                              │
│  - Ownership                                                 │
│  - Entitlements (grants after payment)                      │
│  - Discovery/Search                                          │
└─────────────────────────────────────────────────────────────┘
```

---

## 🔄 Integration Flows

### Flow 1: User Subscribes to Plan

```
1. User → CAS: "Subscribe to Pro Plan"
2. CAS: Create subscription (status: PENDING_PAYMENT)
3. CAS → Billing: Event {subscription.created, planId, consumerId}
4. Billing: Calculate price, create order
5. Billing → Payment Gateway: Process payment
6. Payment Gateway → Billing: Payment success
7. Billing → CAS: Event {payment.succeeded, subscriptionId}
8. CAS: Update subscription (status: ACTIVE)
9. CAS → Iket: Cache invalidation
```

### Flow 2: User Purchases Marketplace Item

```
1. User → Marketplace (CAS): "Purchase Premium Node X"
2. Marketplace: Check if already entitled → No
3. Marketplace → Billing: Event {marketplace.purchase.initiated}
4. Billing: Create order, calculate price
5. Billing → Payment Gateway: Process payment
6. Payment Gateway → Billing: Payment success
7. Billing → Marketplace: Event {payment.succeeded, itemId, consumerId}
8. Marketplace: Grant entitlement
9. Marketplace → User: Purchase confirmed
```

### Flow 3: Usage Recording & Billing

```
1. Iket → CAS: Record usage (1000 API calls)
2. CAS: Validate, store in usage_records
3. CAS: Check quota (8000/10000 used)
4. CAS: OK, return success
5. [End of Month]
6. CAS: Aggregate usage by consumer/product
7. CAS → Billing: Event {usage.aggregated, consumerId, totals}
8. Billing: Calculate invoice
9. Billing → Consumer: Invoice email
10. Billing → Payment Gateway: Charge payment method
11. Payment Gateway → Billing: Payment success
12. Billing: Mark invoice paid
```

### Flow 4: Payment Failed (Subscription)

```
1. Payment Gateway → Billing: Payment failed
2. Billing: Update order (status: FAILED)
3. Billing → CAS: Event {payment.failed, subscriptionId, reason}
4. CAS: Update subscription (status: SUSPENDED)
5. CAS: Emit event {access.suspended, consumerId}
6. Iket Cache: Invalidate consumer's API keys
7. Next API call: CAS returns "access denied"
```

---

## 📋 Detailed Responsibility Breakdown

### CAS Responsibilities

#### 1. Product Catalog ✅
- **What**: Product definitions (code, name, type, unit)
- **Why**: CAS needs to validate usage against products
- **Not**: Pricing (Billing owns)

```java
// CAS owns
Product {
    code: "wayang-core-api",
    name: "Wayang Core API",
    type: API,
    unit: "request",
    billable: true
}

// Billing owns
ProductPricing {
    productCode: "wayang-core-api",
    basePrice: 0.001,
    currency: "USD",
    pricingModel: "PER_UNIT"
}
```

#### 2. Plans ✅
- **What**: Quotas, rate limits, features
- **Why**: CAS enforces access rules
- **Not**: Subscription pricing (Billing owns)

```java
// CAS owns
Plan {
    code: "pro",
    quotas: {
        "wayang-core-api": { limit: 100000, period: "MONTH" }
    },
    features: ["advanced-analytics", "priority-support"]
}

// Billing owns
PlanPricing {
    planCode: "pro",
    monthlyPrice: 99.00,
    currency: "USD",
    trialDays: 14
}
```

#### 3. Subscription State ✅
- **What**: Current state (ACTIVE, SUSPENDED, etc.)
- **Why**: CAS needs to know if consumer has access
- **Not**: Payment tracking (Billing owns)

```java
// CAS owns
Subscription {
    consumerId: uuid,
    planId: uuid,
    status: ACTIVE,  // ← CAS owns this
    startDate: ...,
    endDate: ...
}

// Billing owns
SubscriptionPayment {
    subscriptionId: uuid,
    lastPaymentDate: ...,
    nextPaymentDate: ...,
    paymentMethod: ...,
    invoiceHistory: [...]
}
```

#### 4. Usage Recording ✅
- **What**: Raw usage events (append-only)
- **Why**: CAS is closest to the source (Iket)
- **How**: Aggregate and share with Billing

```java
// CAS owns
UsageRecord {
    consumerId: uuid,
    productCode: "wayang-core-api",
    quantity: 1,
    recordedAt: timestamp,
    metadata: {...}
}

// CAS provides to Billing
UsageAggregate {
    consumerId: uuid,
    period: "2025-01",
    usage: [
        {productCode: "wayang-core-api", total: 85000}
    ]
}
```

#### 5. Marketplace Catalog ✅
- **What**: Items, ownership, entitlements
- **Why**: CAS checks entitlements for access
- **Not**: Purchase transactions (Billing owns)

```java
// CAS owns
MarketplaceItem {
    code: "premium-node-xyz",
    ownerId: uuid,
    published: true
}

MarketplaceEntitlement {
    consumerId: uuid,
    itemId: uuid,
    grantedAt: timestamp
}

// Billing owns
MarketplaceOrder {
    orderId: uuid,
    consumerId: uuid,
    itemId: uuid,
    price: 49.99,
    paymentStatus: COMPLETED
}
```

### Billing Service Responsibilities

#### 1. All Pricing ✅
- Product pricing (per-unit, tiered)
- Plan pricing (monthly, annual)
- Marketplace item pricing
- Discounts and promotions
- Currency conversion

#### 2. Orders ✅
- Order lifecycle (created → paid → fulfilled)
- Order items and totals
- Tax calculation
- Discount application

#### 3. Payments ✅
- Payment method management
- Payment processing
- Payment gateway integration
- Refunds and chargebacks

#### 4. Invoices ✅
- Invoice generation
- Invoice line items
- Invoice delivery (email, PDF)
- Invoice history

#### 5. Financial Reporting ✅
- Revenue reports
- MRR/ARR calculations
- Churn analysis
- Financial reconciliation

---

## 🎯 Why This Split?

### CAS Focus: Access & Entitlements
- Fast access decisions (< 10ms)
- Quota enforcement in real-time
- Clear source of truth for usage
- No financial logic complexity

### Billing Focus: Money & Transactions
- Complex pricing logic
- Payment gateway integration
- Invoice generation
- Financial compliance (SOX, PCI-DSS)
- Accounting system integration

### Benefits of Separation
1. **Clear Boundaries** - No confusion about ownership
2. **Independent Scaling** - Access checks vs payment processing
3. **Team Autonomy** - Different skill sets
4. **Failure Isolation** - Billing down ≠ access checks down
5. **Security** - PCI compliance isolated to Billing
6. **Testability** - Easier to test each domain

---

## 🔔 Event-Driven Integration

### Events FROM CAS → Billing

| Event | Payload | Purpose |
|-------|---------|---------|
| `subscription.created` | subscriptionId, consumerId, planId | Billing creates payment schedule |
| `usage.aggregated` | consumerId, period, totals | Billing generates invoice |
| `consumer.updated` | consumerId, changes | Billing updates customer record |

### Events FROM Billing → CAS

| Event | Payload | Purpose |
|-------|---------|---------|
| `payment.succeeded` | subscriptionId, orderId | CAS activates subscription |
| `payment.failed` | subscriptionId, reason | CAS suspends access |
| `order.completed` | orderId, itemId, consumerId | CAS grants entitlement |
| `subscription.canceled` | subscriptionId | CAS revokes access |
| `refund.issued` | orderId, itemId | CAS may revoke entitlement |

---

## 🏁 Recommendation: Implement in CAS

For **Phase 2**, implement in CAS:

1. ✅ **Plan Service** - Full CRUD + quota management
2. ✅ **Subscription Service** - State management (not payment)
3. ✅ **Usage Service** - Recording + aggregation
4. ✅ **Marketplace Module** - Catalog + entitlements

Defer to Billing Service (separate project):
- ❌ Pricing management
- ❌ Order processing
- ❌ Payment processing
- ❌ Invoice generation

---

## 📝 Implementation Notes

### In CAS
- Keep financial data to minimum (references only)
- Store subscription state, not payment history
- Grant entitlements, don't process payments
- Provide usage data, don't calculate charges

### In Billing Service (Future)
- Import product/plan catalog from CAS (read-only)
- Listen to usage events from CAS
- Update subscription status in CAS via events
- Handle all money-related operations

---

This architecture ensures **clean separation of concerns** while maintaining **strong cohesion** within each service.