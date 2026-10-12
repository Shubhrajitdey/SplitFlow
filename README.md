# SplitFlow AI - Conversational WhatsApp Financial Bookkeeper (Backend)

An enterprise-grade, clean architecture backend built with **Java 21** and **Spring Boot 3.3.4** for conversational bill splitting and bookkeeping inside WhatsApp.

---

## 🏛 System Design & Architecture (Hexagonal / Clean Architecture)

The system strictly follows **Hexagonal Architecture (Ports and Adapters)** to eliminate operator and vendor dependency:

```
                  ┌──────────────────────────────────────────────┐
                  │            INBOUND CONTROLLERS               │
                  │   • WhatsApp Webhook (Meta Graph API)        │
                  │   • REST Test Controller                     │
                  └──────────────────────┬───────────────────────┘
                                         │
                                         ▼
┌────────────────────────────────────────────────────────────────────────┐
│                        CORE DOMAIN & APPLICATION                       │
│                                                                        │
│   ┌─────────────────────┐   ┌──────────────────────────────────────┐   │
│   │   Domain Entities   │   │            Domain Services           │   │
│   │ • Split (State Mach)│   │ • FifoWaterfallSettlementEngine      │   │
│   │ • PairwiseLedger    │   │ • FunkyNameGenerator                 │   │
│   │ • SplitStatus       │   │ • DisputeCircuitBreaker              │   │
│   └─────────────────────┘   └──────────────────────────────────────┘   │
│                                                                        │
│                       PORTS (Decoupled Interfaces)                     │
│     MessagingGatewayPort | NlpParserPort | SplitRepositoryPort         │
└──────────────────────┬─────────────────┬───────────────────────┬───────┘
                       │                 │                       │
                       ▼                 ▼                       ▼
     ┌───────────────────┐ ┌───────────────────┐ ┌───────────────────────┐
     │ WhatsApp / Mock   │ │   Gemini / Groq   │ │ Spring Data JPA /     │
     │ Gateway Adapters  │ │   / Rule-Based    │ │ PostgreSQL / H2       │
     └───────────────────┘ └───────────────────┘ └───────────────────────┘
```

---

## 🛡 Business Guardrails & Acceptance Criteria Met

| ID | Requirement | Implementation Details |
| :--- | :--- | :--- |
| **AC-1** | **Concurrency Shield** | Rapid clicks on *"I've Paid"* are protected via atomic `idempotency_locks` and PostgreSQL/H2 `PESSIMISTIC_WRITE` locks (`SELECT ... FOR UPDATE`). Only the 1st request transitions to `CLAIMED_PAID`. |
| **AC-2** | **Dispute Anti-Loop** | Single-turn circuit breaker. Once the creditor resolves or rejects the dispute, `disputeLocked` is permanently set to `true`, and the *"❓ Dispute"* button is permanently omitted from subsequent cards. |
| **AC-3** | **FIFO Waterfall** | Lump-sum underpayment (e.g. ₹300 against ₹200 and ₹500 debts) clears the oldest split first and marks the second split as `PARTIALLY_PAID` (₹400 remaining). |
| **AC-4** | **Overpayment Credit** | Paying ₹700 for a ₹600 debt clears all splits to ₹0 and deposits ₹100 as a `Credit Advance` in `pairwise_ledgers`. Future expenses automatically offset from this credit advance! |
| **AC-5** | **Input Guardrails** | Prohibited inputs (receipt photos, documents, random chit-chat) trigger polite immediate refusal with an actionable menu of supported commands. |
| **AC-6** | **Funky Identity** | Bills logged without a user title automatically receive creative tags (e.g., `🍕 Cheesy-Blast`, `🌮 Taco-Chaos`). Technical UUIDs remain hidden in button payloads. |

---

## 🚀 Running Locally

### 1. Zero-Config Run (In-Memory H2 DB & Console Mock)
You can run the app immediately without any external services or API keys:
```bash
./mvnw spring-boot:run
```
* The app runs on port `8080`.
* H2 Database Console is available at `http://localhost:8080/h2-console`.
* Outgoing WhatsApp interactive cards and notifications are printed cleanly to the console.

### 2. Testing via REST Simulation API
Simulate incoming WhatsApp messages without needing a Meta account:

```bash
# 1. Log an expense with auto funky name:
curl -X POST http://localhost:8080/api/test/simulate-message \
  -H "Content-Type: application/json" \
  -d '{"from": "+919876543210", "text": "Paid 1200 with +919123456789"}'

# 2. View generated split and get its ID:
curl http://localhost:8080/api/test/splits

# 3. Simulate debtor clicking "I've Paid":
curl -X POST http://localhost:8080/api/test/simulate-button \
  -H "Content-Type: application/json" \
  -d '{"from": "+919123456789", "buttonId": "CLAIM_PAID:<SPLIT_UUID>"}'

# 4. Simulate creditor confirming receipt (Double Handshake):
curl -X POST http://localhost:8080/api/test/simulate-button \
  -H "Content-Type: application/json" \
  -d '{"from": "+919876543210", "buttonId": "CONFIRM_PAID:<SPLIT_UUID>"}'
```

---

## 📦 Production Deployment (Free Tiers)
1. **Database**: Create a free PostgreSQL instance on [Supabase](https://supabase.com) or [Neon](https://neon.tech).
2. **Hosting**: Deploy to [Render.com](https://render.com) or [Fly.io](https://fly.io) with profile `prod`:
   ```bash
   SPRING_PROFILES_ACTIVE=prod
   DATABASE_URL=jdbc:postgresql://<host>:5432/<dbname>
   DATABASE_USERNAME=<user>
   DATABASE_PASSWORD=<password>
   ```
3. **WhatsApp Webhook**: Register the webhook URL `https://your-domain/webhook/whatsapp` in Meta Developer Portal.
