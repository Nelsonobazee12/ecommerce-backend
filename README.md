# E-Commerce Backend API

A full production-grade e-commerce REST API built with **Kotlin** and **Spring Boot**.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| Framework | Spring Boot 3.5 |
| Database | PostgreSQL 16 |
| Cache | Redis 7 |
| Authentication | JWT + Refresh Tokens |
| Payments | Stripe |
| Image Upload | Cloudinary |
| Email | JavaMailSender + MailDev |
| Docs | Swagger / OpenAPI 3 |
| Containerization | Docker + Docker Compose |

---

## Features

- JWT authentication with access + refresh token rotation
- Role-based access control (ADMIN, SELLER, CUSTOMER)
- Email verification on registration
- Password reset via email
- Product catalog with pagination, search, and filtering
- Category management
- Redis-backed shopping cart with TTL
- Order management with status lifecycle
- Stripe payment integration
- Cloudinary image upload
- Review and rating system (purchase-verified)
- Admin dashboard with revenue metrics and top products
- Rate limiting on auth endpoints (5 requests/min per IP)
- Real-time push notifications via Server-Sent Events (SSE)
- Full Docker Compose setup

---

## Project Structure
src/main/kotlin/com/ecommerce/backend/
├── config/          # Security, Redis, Stripe, Cloudinary, OpenAPI configs
├── controller/      # REST controllers
├── service/         # Business logic interfaces
│   └── impl/        # Service implementations
├── repository/      # JPA repositories
├── model/
│   ├── entity/      # JPA entities
│   ├── enums/       # Enums (Role, OrderStatus, PaymentStatus)
│   ├── dto/
│   │   ├── request/ # Request DTOs
│   │   └── response/# Response DTOs
│   └── redis/       # Redis models
├── security/        # JWT filter, UserDetailsService, Rate Limiting
└── exception/       # Global exception handler

---

## Getting Started

### Prerequisites
- Docker Desktop
- IntelliJ IDEA
- Java 21

### 1. Clone the Repository
```bash
git clone https://github.com/Nelsonobazee12/ecommerce-backend.git
cd ecommerce-backend
```

### 2. Configure Environment

Open `src/main/resources/application.yaml` and update:

```yaml
stripe:
  secret-key: sk_test_YOUR_STRIPE_SECRET_KEY
  publishable-key: pk_test_YOUR_STRIPE_PUBLISHABLE_KEY
  webhook-secret: whsec_YOUR_WEBHOOK_SECRET

cloudinary:
  cloud-name: YOUR_CLOUD_NAME
  api-key: YOUR_API_KEY
  api-secret: YOUR_API_SECRET

application:
  jwt:
    secret: YOUR_JWT_SECRET
    expiration: 86400000
```

### 3. Start Infrastructure
```bash
docker-compose up -d postgres redis
```

### 4. Run the Application
```bash
./gradlew bootRun
```

Or open in IntelliJ and click the **Run** button on `BackendApplication.kt`.

### 5. Access Swagger UI
http://localhost:8080/swagger-ui.html

### 6. Access MailDev (local email testing)
http://localhost:1080

---

## API Endpoints

### Auth
| Method | Endpoint | Description | Auth |
|---|---|---|---|
| POST | `/api/auth/register` | Register new user | Public |
| POST | `/api/auth/login` | Login | Public |
| POST | `/api/auth/refresh` | Refresh access token | Public |
| POST | `/api/auth/logout` | Logout | Public |
| GET | `/api/auth/verify-email` | Verify email | Public |
| POST | `/api/auth/forgot-password` | Send reset email | Public |
| POST | `/api/auth/reset-password` | Reset password | Public |

### User
| Method | Endpoint | Description | Auth |
|---|---|---|---|
| GET | `/api/users/me` | Get my profile | User |
| PUT | `/api/users/me` | Update profile | User |
| PUT | `/api/users/me/change-password` | Change password | User |

### Products
| Method | Endpoint | Description | Auth |
|---|---|---|---|
| GET | `/api/products` | List products (paginated) | Public |
| GET | `/api/products/{id}` | Get product | Public |
| POST | `/api/admin/products` | Create product | Admin |
| PUT | `/api/admin/products/{id}` | Update product | Admin |
| DELETE | `/api/admin/products/{id}` | Delete product | Admin |

### Categories
| Method | Endpoint | Description | Auth |
|---|---|---|---|
| GET | `/api/categories` | List categories | Public |
| POST | `/api/admin/categories` | Create category | Admin |

### Cart
| Method | Endpoint | Description | Auth |
|---|---|---|---|
| GET | `/api/cart` | Get cart | User |
| POST | `/api/cart/add` | Add item | User |
| PUT | `/api/cart/update` | Update quantity | User |
| DELETE | `/api/cart/remove/{productId}` | Remove item | User |
| DELETE | `/api/cart/clear` | Clear cart | User |

### Orders
| Method | Endpoint | Description | Auth |
|---|---|---|---|
| POST | `/api/orders` | Place order | User |
| GET | `/api/orders` | My orders | User |
| GET | `/api/orders/{id}` | Get order | User |
| DELETE | `/api/orders/{id}/cancel` | Cancel order | User |
| PUT | `/api/admin/orders/{id}/status` | Update status | Admin |

### Payments
| Method | Endpoint | Description | Auth |
|---|---|---|---|
| POST | `/api/payments/create-intent` | Create payment intent | User |
| POST | `/api/payments/confirm/{orderId}` | Confirm payment | User |
| GET | `/api/payments/order/{orderId}` | Get payment status | User |
| POST | `/api/payments/webhook` | Stripe webhook | Public |

### Reviews
| Method | Endpoint | Description | Auth |
|---|---|---|---|
| POST | `/api/reviews` | Submit review | User |
| GET | `/api/products/{id}/reviews` | Get product reviews | Public |
| GET | `/api/reviews/my` | My reviews | User |
| DELETE | `/api/reviews/{id}` | Delete review | User |

### Upload
| Method | Endpoint | Description | Auth |
|---|---|---|---|
| POST | `/api/upload/image` | Upload image | User |
| POST | `/api/upload/product-image/{id}` | Upload product image | Admin |
| DELETE | `/api/upload/image` | Delete image | Admin |

### Notifications
| Method | Endpoint | Description | Auth |
|---|---|---|---|
| GET | `/api/notifications/subscribe` | SSE stream | User |
| GET | `/api/notifications` | Get notifications | User |
| GET | `/api/notifications/unread-count` | Unread count | User |
| PUT | `/api/notifications/mark-all-read` | Mark all read | User |
| POST | `/api/notifications/send` | Send notification | Admin |

### Admin
| Method | Endpoint | Description | Auth |
|---|---|---|---|
| GET | `/api/admin/dashboard` | Dashboard stats | Admin |
| GET | `/api/admin/users` | All users | Admin |
| GET | `/api/admin/users/{id}` | Get user | Admin |
| PUT | `/api/admin/users/{id}/role` | Update role | Admin |
| PUT | `/api/admin/users/{id}/status` | Enable/disable user | Admin |
| GET | `/api/admin/orders` | All orders | Admin |

---

## Order Status Lifecycle
PENDING → CONFIRMED → SHIPPED → DELIVERED
PENDING → CANCELLED
CONFIRMED → CANCELLED

---

## Rate Limiting

Auth endpoints are rate limited to **5 requests per minute per IP**:
- `/api/auth/login`
- `/api/auth/register`
- `/api/auth/refresh`
- `/api/auth/verify-email`

---

## Running with Full Docker

```bash
# Build the JAR first
./gradlew bootJar -x test

# Start everything
docker-compose up --build -d

# View logs
docker-compose logs -f app
```

---

## Author

**Nelson** — [@Nelsonobazee12](https://github.com/Nelsonobazee12)

Built as part of a learning journey into advanced Spring Boot backend development.
