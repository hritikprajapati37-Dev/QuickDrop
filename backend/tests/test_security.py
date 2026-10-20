import unittest
import json
import secrets
from dotenv import load_dotenv
from backend.app import app
from backend.services.db_service import init_db

load_dotenv()

app.config['TESTING'] = True
app.config['RATELIMIT_ENABLED'] = False
db = init_db(testing=True)

class QuickDropSecurityTestCase(unittest.TestCase):
    
    @classmethod
    def setUpClass(cls):
        cls.client = app.test_client()
        
    def setUp(self):
        if db is not None:
            db["users"].delete_many({})
            db["orders"].delete_many({})
            
            # Create Sender A
            res = self.client.post("/api/auth/signup", json={
                "phone": "9811111111",
                "name": "Sender A",
                "password": "password123",
                "role": "SENDER"
            })
            self.sender_a_token = res.get_json().get("token")

            # Create Sender B
            res = self.client.post("/api/auth/signup", json={
                "phone": "9822222222",
                "name": "Sender B",
                "password": "password123",
                "role": "SENDER"
            })
            self.sender_b_token = res.get_json().get("token")
            
            # Create Partner
            res = self.client.post("/api/auth/signup", json={
                "phone": "9133333333",
                "name": "Delivery Partner",
                "password": "password123",
                "role": "PARTNER"
            })
            self.partner_token = res.get_json().get("token")

            # Create Admin
            res = self.client.post("/api/auth/signup", json={
                "phone": "9044444444",
                "name": "Admin User",
                "password": "password123",
                "role": "ADMIN"
            })
            self.admin_token = res.get_json().get("token")

    def test_unauthenticated_request_rejected(self):
        """Test protected endpoints reject requests without token (401)"""
        res = self.client.post("/api/orders", json={
            "receiver_phone": "9999999999",
            "pickup_address": "A",
            "delivery_address": "B"
        })
        self.assertEqual(res.status_code, 401)

    def test_unauthorized_role_rejected(self):
        """Test role-based authorization rejects incorrect roles (403)"""
        res = self.client.post("/api/orders",
            headers={"Authorization": f"Bearer {self.partner_token}"},
            json={
                "receiver_phone": "9999999999",
                "pickup_address": "A",
                "delivery_address": "B"
            }
        )
        self.assertEqual(res.status_code, 403)

    def test_sender_ownership_isolation_idor(self):
        """Test Sender B cannot access Sender A's order OTPs (IDOR protection)"""
        res = self.client.post("/api/orders",
            headers={"Authorization": f"Bearer {self.sender_a_token}"},
            json={
                "receiver_phone": "9988776655",
                "pickup_address": "123 Main St",
                "delivery_address": "456 Market St"
            }
        )
        data = res.get_json()
        order_id = data["order_id"]

        fetch_res = self.client.get(f"/api/orders/{order_id}/otps",
            headers={"Authorization": f"Bearer {self.sender_b_token}"}
        )
        self.assertEqual(fetch_res.status_code, 404)

    def test_otp_brute_force_locking(self):
        """Test OTP verification locks out after 3 failed attempts (429)"""
        res = self.client.post("/api/orders",
            headers={"Authorization": f"Bearer {self.sender_a_token}"},
            json={
                "receiver_phone": "9988776655",
                "pickup_address": "123 Main St",
                "delivery_address": "456 Market St"
            }
        )
        data = res.get_json()
        order_id = data["order_id"]

        for _ in range(3):
            verify_res = self.client.post("/api/delivery/verify-pickup",
                headers={"Authorization": f"Bearer {self.partner_token}"},
                json={"order_id": order_id, "otp": "999999"}
            )
            self.assertEqual(verify_res.status_code, 400)

        verify_res_4 = self.client.post("/api/delivery/verify-pickup",
            headers={"Authorization": f"Bearer {self.partner_token}"},
            json={"order_id": order_id, "otp": "999999"}
        )
        self.assertEqual(verify_res_4.status_code, 429)

    def test_admin_access_allowed(self):
        """Test admin can access admin oversight endpoints"""
        res = self.client.get("/api/admin/orders",
            headers={"Authorization": f"Bearer {self.admin_token}"}
        )
        self.assertEqual(res.status_code, 200)

    def test_admin_access_denied_for_sender(self):
        """Test non-admin cannot access admin endpoints (403)"""
        res = self.client.get("/api/admin/orders",
            headers={"Authorization": f"Bearer {self.sender_a_token}"}
        )
        self.assertEqual(res.status_code, 403)

if __name__ == "__main__":
    unittest.main()
