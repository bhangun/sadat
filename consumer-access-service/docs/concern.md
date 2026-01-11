
> **Yes, enrich Consumer-Access Service with *consumer metadata*,
> but do NOT turn it into a full Identity or User Profile system.**

Let’s break this down properly and safely.

---

# 1️⃣ First: Clarify WHAT Consumer-Access Service IS

**Consumer-Access Service (CAS)** is a **commercial + access authority**, not an identity provider.

Its responsibility:

```text
WHO can call the API?
UNDER WHAT PLAN?
WITH WHAT LIMITS?
BILLED HOW?
```

So enrichment is allowed **only if it serves those questions**.

---

# 2️⃣ What Data You CAN Store in Consumer-Access Service

### ✅ Allowed (Recommended)

These are **business-identity attributes**:

| Data                      | Why allowed                          |
| ------------------------- | ------------------------------------ |
| Company / individual name | Invoicing                            |
| Email (business contact)  | Billing & support                    |
| Phone number              | Payment / legal                      |
| Country                   | Tax & compliance                     |
| Address                   | Invoices                             |
| Tax ID / NPWP             | Legal                                |
| Business type             | Pricing rules                        |
| KTP (Indonesia)           | **If required for KYC / compliance** |
| Social ID reference       | Fraud prevention                     |
| Verification status       | KYC                                  |

Example classification:

```text
"consumer metadata"
```

Not “user profile”.

---

# 3️⃣ What You SHOULD NOT Store There

### ❌ Do NOT store

| Data                   | Reason              |
| ---------------------- | ------------------- |
| Passwords              | Identity system job |
| Login sessions         | Keycloak            |
| OAuth tokens           | Keycloak            |
| Social login accounts  | Keycloak            |
| Personal activity logs | Privacy risk        |
| Chat history           | Not relevant        |
| Behavior tracking      | Scope creep         |

---

# 4️⃣ KTP (Indonesia) — Special Handling 🚨

KTP is **PII + regulated**.

### If you store KTP:

You MUST:

1. **Encrypt at rest**
2. **Mask on read**
3. **Never expose to Iket**
4. **Never forward to downstream services**
5. **Separate access roles**

Recommended storage model:

```text
consumer_sensitive_data
├── consumer_id
├── ktp_number (encrypted)
├── ktp_hash (for lookup)
├── verification_status
├── verified_at
└── provider (manual / 3rd party)
```

KTP should only be accessible by:

* Billing admin
* Compliance admin

---

# 5️⃣ Correct Data Model (Practical)

### Consumer-Access Service tables

```text
consumers
├── id
├── type (individual | company)
├── name
├── email
├── phone
├── country
├── address
├── status
└── created_at
```

```text
consumer_sensitive_data
├── consumer_id
├── ktp_encrypted
├── tax_id_encrypted
├── verification_level
├── verified_at
```

```text
api_keys
├── id
├── consumer_id
├── hash
├── plan
├── status
├── last_used_at
```

---

# 6️⃣ How This Interacts with Keycloak (Cleanly)

### Keycloak

* Authenticates **admins**
* Authenticates **internal services**

### Consumer-Access

* Stores **consumer metadata**
* Does **NOT authenticate consumers**
* Links Keycloak user → consumer_id

Example:

```text
keycloak_user_id → consumer_id
```

---

# 7️⃣ Iket NEVER sees sensitive consumer data

Iket only gets:

```http
X-Consumer-Id
X-Plan
X-Quota
X-Usage-Policy
```

❌ Never:

* Phone
* Address
* KTP
* Tax ID

This is **critical for zero-trust gateway design**.

---

# 8️⃣ Regulatory Reality (Indonesia & Global)

Storing KTP means:

* PDP Law (UU PDP Indonesia)
* GDPR (if EU customers)
* Audit logs
* Right to delete
* Consent tracking

If you’re **not ready**, alternative:

### ✅ Safer option

Store:

```text
ktp_verification_reference
```

And delegate real KYC to:

* Midtrans
* Xendit
* Stripe Identity
* Onfido

---

# 9️⃣ Recommended Final Architecture

```text
Keycloak
└── Admin identity

Consumer-Access Service
├── Consumers
├── API keys
├── Plans
├── Billing linkage
├── KYC metadata
└── Compliance flags

Iket Gateway
└── Enforces access only
```

---

# 🔐 Golden Rule (Don’t Break This)

> **Consumer-Access may know WHO the consumer is.
> It must NEVER become HOW they authenticate.**

That line keeps Wayang clean, scalable, and compliant.

---


