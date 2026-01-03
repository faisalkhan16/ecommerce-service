# Postman Setup (Ecommerce Service)

This folder contains a ready-to-use **Postman Collection** and **Postman Environment** for testing the Ecommerce Service APIs (Auth, Users, Products, Orders).

---

## Prerequisites

1. **Run the backend API**
   - Start the application locally or via Docker Compose.
   - Ensure the API is reachable.

2. **Have Postman installed**
   - Postman Desktop App is recommended.

---

## API Documentation (Swagger)

Once the application is running, API documentation will be available at:

- `http://<host>/swagger-ui/index.html`

Example (local):
- `http://localhost:8081/swagger-ui/index.html`

> Tip: If you set `base-url` to `http://localhost:8081`, your Swagger URL is simply:
> `{{base-url}}/swagger-ui/index.html`

---

## Postman Files Included

### Collection
- Path: `postman/collections/ecommerce-api.postman_collection.json`
- Name: `ecommerce-api`

### Environment
- Path: `postman/environments/ecommerce-app.postman_environment.json`
- Name: `ecommerce-app`

The environment contains:
- `base-url` → Base API URL (example: `http://localhost:8081`)
- `auth-token` → JWT token used for Bearer authentication in secured requests

---

## Import the Postman Collection

1. Open **Postman**
2. Go to **Import**
3. Select:
   - `postman/collections/ecommerce-api.postman_collection.json`
4. Click **Import**

You should now see a collection named **ecommerce-api** with folders for:
- Auth-Service
- User-Service
- Product-Service
- Order-Service

---

## Import the Postman Environment

1. Open **Postman**
2. Go to **Import**
3. Select:
   - `postman/environments/ecommerce-app.postman_environment.json`
4. Click **Import**
5. In the top-right environment dropdown, select:
   - **ecommerce-app**

---

## Configure `base-url`

All requests in the collection use `{{base-url}}` so you can switch environments easily.

1. In Postman, open:
   - **Environments → ecommerce-app**
2. Set:
   - `base-url` = `http://localhost:8081` (for local run)
3. Save the environment.

### Examples

- Local run:
   - `base-url = http://localhost:8081`
- Docker (if published on a different port):
   - `base-url = http://localhost:<PORT>`
- Remote server:
   - `base-url = http://<SERVER_HOST>:<PORT>` or `https://<DOMAIN>`

---

## Authentication Flow (How Bearer Token Works)

Most requests in this collection use **Bearer Token** authentication and reference:

- `{{auth-token}}`

That token is automatically set by the **Auth-Service → Login** request.

### Steps

1. In the collection, open:
   - **Auth-Service → Login**
2. Click **Send**
3. After a successful login, Postman automatically saves the token into the environment variable:
   - `auth-token`

You can confirm this by:
- Opening **Environment → ecommerce-app**
- Checking that `auth-token` is now populated

After that, you can call secured endpoints such as:
- Create User
- Create Product
- Place Order
- etc.

---

## Common Issues

### 1) Requests return 401 Unauthorized
- Run **Auth-Service → Login** first.
- Ensure the environment **ecommerce-app** is selected.
- Confirm `auth-token` is set.

### 2) Could not send request / Connection refused
- Ensure the backend is running.
- Confirm `base-url` is correct.
- Try opening Swagger UI in your browser:
   - `{{base-url}}/swagger-ui/index.html`

---

## Notes

- The Login request includes a Postman test script that extracts the token from the response and stores it automatically into `auth-token`.
- Secured endpoints in the collection are configured to send `Authorization: Bearer {{auth-token}}`.

---