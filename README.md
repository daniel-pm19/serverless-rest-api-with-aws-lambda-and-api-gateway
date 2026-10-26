# Serverless REST API with AWS Lambda and API Gateway

A small **Order Management API** built with Java 17 and Maven, designed to be deployed on AWS Lambda and exposed through Amazon API Gateway.

The application logic is written in plain Java first (no HTTP, no AWS classes, no Spring Boot, no web server), so it can be tested locally with JUnit before any infrastructure is involved.

---

## 1. API Design (Contract)

### Order representation

```json
{
  "id": "ORD-001",
  "customerId": "CUST-42",
  "product": "MacBook Air",
  "quantity": 1
}
```

### Error representation

Every error returned by the API uses the same JSON shape:

```json
{
  "error": "ORDER_NOT_FOUND",
  "message": "Order ORD-999 does not exist"
}
```

| Field     | Description                                                                 |
|-----------|-----------------------------------------------------------------------------|
| `error`   | Stable, machine-readable error code (UPPER_SNAKE_CASE). Clients branch on it. |
| `message` | Human-readable explanation of what went wrong, useful for debugging.         |

Error codes used by the API:

| Error code          | HTTP status | When                                                        |
|---------------------|-------------|-------------------------------------------------------------|
| `INVALID_REQUEST`   | 400         | Body is missing, malformed JSON, or a field is invalid       |
| `MISSING_PARAMETER` | 400         | A required path/query parameter is missing or blank          |
| `ORDER_NOT_FOUND`   | 404         | No order exists with the given id                            |
| `INTERNAL_ERROR`    | 500         | Any unexpected failure (details are logged, not exposed)     |

---

### 1.1 Create an order

| Item                  | Value |
|-----------------------|-------|
| **Method**            | `POST` |
| **URL**               | `/orders` |
| **Path parameters**   | — |
| **Query parameters**  | — |
| **Success status**    | `201 Created` |
| **Response headers**  | `Location: /orders/{id}` |

**Request body**

```json
{
  "customerId": "CUST-42",
  "product": "MacBook Air",
  "quantity": 1
}
```

The client does **not** send the `id`; the server generates it.

Validation rules:
- `customerId` is required and not blank.
- `product` is required and not blank.
- `quantity` is required and must be greater than 0.

**Successful response body** (`201 Created`)

```json
{
  "id": "ORD-001",
  "customerId": "CUST-42",
  "product": "MacBook Air",
  "quantity": 1
}
```

**Error responses**

| Status | `error`           | Example `message`                         |
|--------|-------------------|-------------------------------------------|
| 400    | `INVALID_REQUEST` | `customerId is required`                  |
| 400    | `INVALID_REQUEST` | `quantity must be greater than 0`         |
| 400    | `INVALID_REQUEST` | `Request body is not valid JSON`          |
| 500    | `INTERNAL_ERROR`  | `Unexpected error while creating order`   |

---

### 1.2 Retrieve an order

| Item                  | Value |
|-----------------------|-------|
| **Method**            | `GET` |
| **URL**               | `/orders/{id}` |
| **Path parameters**   | `id` (string, required) — e.g. `ORD-001` |
| **Query parameters**  | — |
| **Request body**      | — |
| **Success status**    | `200 OK` |

**Successful response body** (`200 OK`)

```json
{
  "id": "ORD-001",
  "customerId": "CUST-42",
  "product": "MacBook Air",
  "quantity": 1
}
```

**Error responses**

| Status | `error`             | Example `message`                    |
|--------|---------------------|--------------------------------------|
| 400    | `MISSING_PARAMETER` | `Order id cannot be null or blank`   |
| 404    | `ORDER_NOT_FOUND`   | `Order ORD-999 does not exist`       |
| 500    | `INTERNAL_ERROR`    | `Unexpected error while retrieving order` |

---

### 1.3 Find orders belonging to a customer

| Item                  | Value |
|-----------------------|-------|
| **Method**            | `GET` |
| **URL**               | `/orders?customerId={customerId}` |
| **Path parameters**   | — |
| **Query parameters**  | `customerId` (string, required) — e.g. `CUST-42` |
| **Request body**      | — |
| **Success status**    | `200 OK` |

**Successful response body** (`200 OK`)

```json
[
  {
    "id": "ORD-001",
    "customerId": "CUST-42",
    "product": "MacBook Air",
    "quantity": 1
  },
  {
    "id": "ORD-002",
    "customerId": "CUST-42",
    "product": "Magic Mouse",
    "quantity": 2
  }
]
```

If the customer has no orders, the response is still `200 OK` with an **empty array** `[]`. An empty search result is a valid answer, not an error, so `404` is not used here.

**Error responses**

| Status | `error`             | Example `message`                        |
|--------|---------------------|------------------------------------------|
| 400    | `MISSING_PARAMETER` | `customerId query parameter is required` |
| 500    | `INTERNAL_ERROR`    | `Unexpected error while searching orders` |

---

### 1.4 Status codes summary

| Status                      | Used? | Where |
|-----------------------------|-------|-------|
| `200 OK`                    | Yes   | `GET /orders/{id}`, `GET /orders?customerId=...` |
| `201 Created`               | Yes   | `POST /orders` |
| `204 No Content`            | Not yet | Reserved for a future `DELETE /orders/{id}` (success with no body) |
| `400 Bad Request`           | Yes   | Invalid body, invalid fields, missing parameters |
| `404 Not Found`             | Yes   | `GET /orders/{id}` when the order does not exist |
| `500 Internal Server Error` | Yes   | Any unexpected failure |

> **Note on storage:** orders are kept in an in-memory collection. AWS Lambda execution environments are ephemeral and can be recycled or run in parallel at any time, so this data is **not** persistent and may differ between invocations. A real deployment would need a database (e.g. DynamoDB).

---

## 2. Design Questions

**What are the resources in your API?**
The main resource is **Order**. `/orders` represents the *collection* of orders and `/orders/{id}` represents a *single* order inside that collection. Customers are not exposed as a resource in this version; `customerId` is just an attribute of an order used to filter the collection.

**Why did you choose `/orders/{id}` to retrieve a particular order?**
Because in REST each individual resource should have its own unique URL. The `id` uniquely identifies one order, so it belongs in the path: `/orders/ORD-001` *is* that order. It follows the standard collection/item hierarchy (`/orders` → `/orders/{id}`), is easy to read and cache, and naturally supports `404 Not Found` when the identified resource does not exist.

**Why is `customerId` represented as a query parameter in the search operation?**
Because it does not identify a resource; it **filters** the `/orders` collection. Query parameters are the conventional place for filters, searches, sorting and pagination. The result is still a list of orders (possibly empty), and more filters can be added later (`?customerId=CUST-42&product=...`) without changing the resource path. Putting it in the path (`/orders/CUST-42`) would be confused with an order id.

**What should `POST /orders` return when an order is successfully created?**
`201 Created`, with the full created order in the body (including the server-generated `id`) and a `Location: /orders/{id}` header pointing to the new resource. This lets the client know the id immediately without making another request.

**What should the API return when an order does not exist?**
`404 Not Found` with a consistent error body, for example:

```json
{
  "error": "ORDER_NOT_FOUND",
  "message": "Order ORD-999 does not exist"
}
```

This applies to `GET /orders/{id}`. For the customer search, a customer with no orders returns `200 OK` with `[]`.

**What information should an error response contain?**
- A **stable, machine-readable error code** (`error`) so clients can react programmatically.
- A **human-readable message** (`message`) explaining the problem, ideally mentioning the offending value or field.
- The correct **HTTP status code** on the response itself.

It must **not** expose internal details such as stack traces, class names or infrastructure information. Optionally it could include a timestamp, the request path, or a request/correlation id to help with tracing in CloudWatch logs.

---

## 3. Part I — Plain Java Application

### Project structure

```
serverless-rest-api-with-aws-lambda-and-api-gateway/
├── pom.xml
└── src/
    ├── main/java/co/edu/escuelaing/
    │   ├── Order.java          # Domain entity: id, customerId, product, quantity
    │   ├── OrderRequest.java   # Data needed to create an order (no id)
    │   ├── OrderService.java   # Business logic: create, get by id, find by customer
    │   ├── Exceptions.java     # Domain exception with a type (ORDER_NOT_FOUND, INVALID_*...)
    │   └── Main.java
    └── test/java/co/edu/escuelaing/
        └── OrderServiceTest.java
```

### Application operations

| Operation                                        | Maps to API operation              |
|--------------------------------------------------|------------------------------------|
| `Order createOrder(OrderRequest request)`        | `POST /orders`                     |
| `Order getOrder(String id)`                      | `GET /orders/{id}`                 |
| `List<Order> getOrdersByCustomer(String customerId)` | `GET /orders?customerId={customerId}` |

`OrderService` stores orders in an in-memory `List<Order>`. It has no dependency on HTTP, API Gateway, Lambda, Spring Boot or any web server. Errors are reported with the domain exception `Exceptions`, whose `InnerExceptions` type (e.g. `ORDER_NOT_FOUND`, `INVALID_QUANTITY`) tells what went wrong; translating them into HTTP status codes (400, 404, 500) is the job of a later adapter layer (the Lambda handler / API Gateway), not of the service.

### Build and test

```bash
mvn test      # run JUnit tests
mvn package   # build target/serverless-rest-api-with-aws-lambda-and-api-gateway-1.0-SNAPSHOT.jar
```

Tests cover:
- creation of a valid order;
- retrieval of an existing order;
- behavior when an order does not exist;
- searching orders by customer;
- invalid input (null/blank ids, invalid create requests).

Result: `Tests run: 14, Failures: 0, Errors: 0, Skipped: 0`.

---

## 4. Checkpoint 1

**Can the application functionality be tested without AWS?**
Yes. `OrderService` is plain Java, so JUnit tests instantiate it directly and call its methods. No AWS account, credentials, Lambda runtime or network is needed; `mvn test` runs everything locally.

**Does your application require a web server?**
No. There is no Tomcat, Jetty, Netty or any HTTP listener. In production, API Gateway receives the HTTP request and Lambda invokes our code directly as a function.

**Does it require Spring Boot?**
No. It uses only the Java standard library (plus JUnit for tests). Avoiding Spring Boot also keeps the JAR small and reduces Lambda cold-start time.

**What does the generated JAR contain?**
The compiled `.class` files of our own code (`Order`, `OrderRequest`, `OrderService`, `Exceptions`, `Exceptions$InnerExceptions`, `Main`) under `co/edu/escuelaing/`, plus `META-INF/` with the `MANIFEST.MF` and Maven metadata (`pom.xml`, `pom.properties`). It does **not** contain test classes or third-party dependencies (it is a "thin" JAR). Once AWS libraries are added for the Lambda handler, a "fat"/shaded JAR will be needed so Lambda has all dependencies.

**Which parts of your application currently know about HTTP?**
None. No class references HTTP requests, responses, status codes, headers or API Gateway events. HTTP concepts exist only in this README's API contract. The mapping from domain results/exceptions to HTTP will be added later in a separate handler layer.
