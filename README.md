# QuickDrop — Delivery & Matching Engine

QuickDrop is a modern delivery platform featuring an Android client application (Java/XML) and a Flask secure backend connected to MongoDB Atlas (`quick_dropDB`).

> **NOTICE**: Payments and refunds are simulated; no real money is collected or returned.

## Order Lifecycle & State Machine
The backend enforces a strict server-authoritative state machine:
`CREATED` → `MATCHING` → `ASSIGNED` → `PICKED_UP` → `IN_TRANSIT` → `DELIVERED`
Terminal/Exception States: `EXPIRED`, `CANCELLED`, `REFUNDED`

1. **Order Creation & Pricing**:
   - Road distance is calculated authoritatively on the backend using the Haversine formula with route multiplier.
   - **Delivery Charge**: Exact distance in km × ₹5 (no arbitrary km rounding).
   - **Platform Fee**: ₹2.
   - **Total Payable**: Delivery Charge + Platform Fee.
2. **Server-Side Simulated Payment**:
   - Order enters matching (`status: MATCHING`) only after server-side payment verification via `POST /api/orders/<id>/pay`.
3. **5-Minute Acceptance Deadline**:
   - Server initializes `paid_at`, `matching_started_at`, and `acceptance_deadline_at = matching_started_at + 5 mins`.
   - Persisted in MongoDB Atlas so matching deadlines survive server restarts or redeployments.
   - If unassigned after 5 minutes, the backend automatically transitions order status to `EXPIRED` and marks `refund_status: "demo_refunded"`.
4. **Nearest Available Partner Matching**:
   - `/api/partner/available` queries online partners (`is_online: true`), excludes partners with active deliveries, and prioritizes requests by proximity to pickup coordinates.
5. **One Active Delivery Per Partner Constraint**:
   - Enforced atomically in MongoDB Atlas during `POST /api/delivery/accept`. A partner with an active delivery (`ASSIGNED`, `PICKED_UP`, `IN_TRANSIT`) is blocked from accepting a second order.
   - Simultaneous acceptance attempts by multiple partners are handled atomically using MongoDB `update_one` query filters.

## Prerequisites
- Python 3.9+
- Android Studio / Android SDK (Min SDK 24, Target SDK 36)
- MongoDB Atlas cluster (`quick_dropDB`)

## Setup & Configuration

### 1. Backend Setup (Flask)
1. Install dependencies:
   ```bash
   pip install -r requirements.txt
   ```
2. Configure `.env`:
   ```bash
   MONGO_URI=mongodb+srv://<username>:<password>@cluster0.mongodb.net/quick_dropDB
   JWT_SECRET_KEY=quickdrop_fixed_jwt_secret_key_2026_prod
   ```
3. Run the Flask server:
   ```bash
   python app.py
   ```

### 2. Testing
Run the backend test suite:
```bash
python -m unittest discover -s backend/tests -p "test_*.py"
```

## Security & Architecture
For detailed information on security controls, encryption, RBAC, and data protection, please refer to [SECURITY.md](file:///C:/Users/ItsBo/AndroidStudioProjects/Quick_Drop/SECURITY.md).
