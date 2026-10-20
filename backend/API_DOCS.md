# QuickDrop API Documentation for Future Web Frontends

This document outlines the API endpoints designed to power the upcoming **Receiver Website** and **Admin Website**.

---

## 1. Receiver Website API
The Receiver website is unauthenticated and does not require user registration. Receivers access tracking and delivery confirmation using secure, high-entropy, order-specific tracking tokens.

### Track Order & Retrieve Delivery OTP
- **URL**: `/api/track/<tracking_token>`
- **Method**: `GET`
- **Auth Required**: None (Authorized via tracking token)
- **Response (Success - 200 OK)**:
  ```json
  {
    "order_id": "651a2b3c4d5e6f7a8b9c0d1e",
    "status": "PICKED_UP",
    "pickup_verified": true,
    "delivered": false,
    "item_description": "Office Documents",
    "delivery_address": "789 Park Avenue, City",
    "delivery_otp": "654321"
  }
  ```
- **Notes**: 
  - `delivery_otp` is only exposed when `pickup_verified` is `true` and `delivered` is `false`.
  - Pickup OTP is **never** exposed on the receiver website.

---

## 2. Admin Website API
The Admin website requires administrative authentication (`role: "ADMIN"`).

### Get All System Orders
- **URL**: `/api/admin/orders`
- **Method**: `GET`
- **Auth Required**: Bearer Token (`ADMIN` role)
- **Response (Success - 200 OK)**:
  ```json
  {
    "orders": [
      {
        "order_id": "651a2b3c4d5e6f7a8b9c0d1e",
        "sender_phone": "9876543210",
        "receiver_phone": "9988776655",
        "status": "CREATED",
        "pickup_verified": false,
        "delivered": false,
        "created_at": "2023-10-24T12:00:00"
      }
    ]
  }
  ```

### Get All System Users
- **URL**: `/api/admin/users`
- **Method**: `GET`
- **Auth Required**: Bearer Token (`ADMIN` role)
- **Response (Success - 200 OK)**:
  ```json
  {
    "users": [
      {
        "user_id": "651a2b3c4d5e6f7a8b9c0d1e",
        "name": "John Doe",
        "phone": "9876543210",
        "role": "SENDER",
        "created_at": "2023-10-24T12:00:00"
      }
    ]
  }
  ```

### Assign Delivery Partner
- **URL**: `/api/admin/assign-partner`
- **Method**: `POST`
- **Auth Required**: Bearer Token (`ADMIN` role)
- **Request Body**:
  ```json
  {
    "order_id": "651a2b3c4d5e6f7a8b9c0d1e",
    "partner_phone": "9123456789"
  }
  ```
- **Response (Success - 200 OK)**:
  ```json
  {
    "message": "Delivery partner assigned successfully"
  }
  ```
