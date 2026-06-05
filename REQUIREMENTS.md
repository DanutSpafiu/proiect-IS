# Online Store – Project Requirements

## Overview

Design and implement a Spring Boot REST API for managing an online store. The system supports three types of users: **Administrator**, **Sellers**, and **Buyers**.

---

## Tech Stack

- **Framework:** Spring Boot
- **Security:** Spring Security (session-based or JWT)
- **Persistence:** Spring Data JPA + Hibernate
- **Database:** H2 (development) or MySQL/PostgreSQL (production)
- **Validation:** Spring Boot Validation (`@Valid`, `@NotNull`, `@Email`, etc.)
- **Boilerplate reduction:** Lombok

---

## User Roles

### 1. Administrator
- Single hardcoded account: `email: admin@email.com`, `password: admin`
- Can view all registered sellers
- Can approve seller accounts
- Can deactivate a seller account at any time
  - Deactivated sellers remain in the database but **cannot log in**

### 2. Seller
- Identified by **email** and **password**
- Must request account approval from the administrator before being able to operate
- Once approved, can:
  - List products for sale
  - Cancel a listing (remove a product from sale)
  - Approve or reject offers on negotiable-price products

### 3. Buyer
- Identified by **email** and **password**
- Self-registration — **no approval required**
- Can:
  - View all available products
  - Purchase a fixed-price product
  - Submit an offer on a negotiable-price product
  - Purchase a negotiable-price product **only after the seller approves the offer**

---

## Product Model

| Field         | Type    | Visibility | Notes                                              |
|---------------|---------|------------|----------------------------------------------------|
| `id`          | Long    | Internal   | Never exposed in API responses                     |
| `name`        | String  | Public     |                                                    |
| `price`       | Double  | Public     | Listed/asking price                                |
| `seller`      | Seller  | Public     | Reference to the seller                            |
| `description` | String  | Public     |                                                    |
| `saleType`    | Enum    | Public     | `FIXED_PRICE` or `NEGOTIABLE`                      |
| `minimumPrice`| Double  | Internal   | Only present for `NEGOTIABLE` products; never exposed |

---

## Sale Types

### Fixed Price
- Buyers can purchase directly at the listed price
- No offers allowed

### Negotiable Price
- Buyers can submit offers
- An offer contains:
  - Product ID
  - Buyer details (email)
  - Proposed price
- **Automatic rejection:** If the proposed price is **below the minimum price**, the offer is rejected immediately and is **not stored** in the system
- If the proposed price meets or exceeds the minimum price, the offer is saved and awaits seller review
- The seller can **approve** or **reject** saved offers
- A buyer can complete the purchase only after their offer is approved

---

## Offer Model

| Field           | Type   | Notes                        |
|-----------------|--------|------------------------------|
| `id`            | Long   | Internal                     |
| `productId`     | Long   | Reference to the product     |
| `buyerEmail`    | String | Identifies the buyer         |
| `proposedPrice` | Double | Must be ≥ product minimumPrice to be saved |
| `status`        | Enum   | `PENDING`, `APPROVED`, `REJECTED` |

---

## Purchase Flow

1. Buyer purchases a product (fixed price or approved negotiated price)
2. A **sale history record** is created containing:
   - Product name and description
   - Final price paid
   - Buyer reference
   - Seller reference
   - Timestamp
3. The product is **deleted** from the active listings
4. All associated offers (if any) are **deleted**

---

## Sale History Record

| Field       | Type      | Notes                         |
|-------------|-----------|-------------------------------|
| `id`        | Long      | Internal                      |
| `productName` | String  | Snapshot at time of sale      |
| `finalPrice`| Double    | Price actually paid           |
| `buyer`     | Buyer     | Reference to buyer            |
| `seller`    | Seller    | Reference to seller           |
| `soldAt`    | Timestamp | Date and time of sale         |

---

## Authentication & Authorization

| Endpoint Category              | Allowed Roles         |
|--------------------------------|-----------------------|
| Register as buyer              | Public                |
| Request seller account         | Public                |
| View product list              | Public / Buyer        |
| Purchase product               | Buyer                 |
| Submit offer                   | Buyer                 |
| List / cancel own products     | Seller (approved)     |
| Approve / reject offers        | Seller (approved)     |
| View & approve seller accounts | Admin                 |
| Deactivate seller account      | Admin                 |

---

## Business Rules Summary

1. A seller whose account is **deactivated** cannot log in, even if their record exists in the database
2. A seller account requires **admin approval** before the seller can list products
3. Offers below the product's minimum price are **silently rejected** — not stored, not shown
4. When a product is sold:
   - A sale history record is inserted
   - The product is removed
   - All associated offers are removed
5. The `minimumPrice` field is **never returned** in any API response
6. The product `id` is **never returned** in any API response

---

## Suggested Package Structure

```
com.example.store
├── config/          # Security config, DataInitializer (admin account)
├── controller/      # REST controllers per role
├── dto/             # Request and response DTOs
├── model/           # JPA entities (User, Product, Offer, SaleHistory)
├── repository/      # Spring Data JPA repositories
├── service/         # Business logic
└── exception/       # Custom exceptions and global error handler
```

---

## Notes for Implementation

- Use a **discriminator column** or a **role enum** on the `User` entity to distinguish Admin, Seller, and Buyer
- The admin account should be **seeded via a `DataInitializer` / `CommandLineRunner`** on startup if not already present
- Sensitive fields (`minimumPrice`, `id`) must be excluded from all response DTOs — do **not** use entity classes directly as response bodies
- Deactivated seller flag: add an `active` boolean on the Seller entity; Spring Security should reject login if `active == false`
