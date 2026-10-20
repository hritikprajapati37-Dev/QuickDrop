import unittest
import json
import secrets
from datetime import datetime, timedelta
from dotenv import load_dotenv
from backend.app import app
from backend.services.db_service import get_db

load_dotenv()

app.config['TESTING'] = True
app.config['RATELIMIT_ENABLED'] = False
db = get_db()

class QuickDropOTPTestCase(unittest.TestCase):
    
    @classmethod
    def setUpClass(cls):
        cls.client = app.test_client()
        
    def setUp(self):
        if db is not None:
            db["users"].delete_many({})
            db["orders"].delete_many({})
            
            sender_phone = "98" + "".join([str(secrets.randbelow(10)) for _ in range(8)])
            partner_phone = "91" + "".join([str(secrets.randbelow(10)) for _ in range(8)])
            
            res = self.client.post("/api/auth/signup", json={
                "phone": sender_phone,
                "name": "Test Sender",
                "password": "password123",
                "role": "SENDER"
            })
            self.sender_token = res.get_json().get("token")
            
            res = self.client.post("/api/auth/signup", json={
                "phone": partner_phone,
                "name": "Test Partner",
                "password": "password123",
                "role": "PARTNER"
            })
            self.partner_token = res.get_json().get("token")

    def test_01_order_creation_and_pickup_otp(self):
        res = self.client.post("/api/orders", 
            headers={"Authorization": f"Bearer {self.sender_token}"},
            json={
                "receiver_phone": "9988776655",
                "pickup_address": "123 Main St",
                "delivery_address": "456 Market St",
                "item_description": "Documents"
            }
        )
        self.assertEqual(res.status_code, 201)
        data = res.get_json()
        self.assertIn("order_id", data)
        self.assertIn("pickup_otp", data)
        self.assertIn("tracking_token", data)

    def test_02_pickup_verification_success(self):
        res = self.client.post("/api/orders", 
            headers={"Authorization": f"Bearer {self.sender_token}"},
            json={
                "receiver_phone": "9988776655",
                "pickup_address": "123 Main St",
                "delivery_address": "456 Market St",
                "item_description": "Documents"
            }
        )
        data = res.get_json()
        order_id = data["order_id"]
        pickup_otp = data["pickup_otp"]

        verify_res = self.client.post("/api/delivery/verify-pickup",
            headers={"Authorization": f"Bearer {self.partner_token}"},
            json={
                "order_id": order_id,
                "otp": pickup_otp
            }
        )
        self.assertEqual(verify_res.status_code, 200)
        verify_data = verify_res.get_json()
        self.assertEqual(verify_data["status"], "PICKED_UP")

    def test_03_invalid_pickup_otp_rejected(self):
        res = self.client.post("/api/orders", 
            headers={"Authorization": f"Bearer {self.sender_token}"},
            json={
                "receiver_phone": "9988776655",
                "pickup_address": "123 Main St",
                "delivery_address": "456 Market St",
                "item_description": "Documents"
            }
        )
        data = res.get_json()
        order_id = data["order_id"]

        verify_res = self.client.post("/api/delivery/verify-pickup",
            headers={"Authorization": f"Bearer {self.partner_token}"},
            json={
                "order_id": order_id,
                "otp": "000000"
            }
        )
        self.assertEqual(verify_res.status_code, 400)

    def test_04_delivery_cannot_complete_before_pickup(self):
        res = self.client.post("/api/orders", 
            headers={"Authorization": f"Bearer {self.sender_token}"},
            json={
                "receiver_phone": "9988776655",
                "pickup_address": "123 Main St",
                "delivery_address": "456 Market St",
                "item_description": "Documents"
            }
        )
        data = res.get_json()
        order_id = data["order_id"]

        delivery_res = self.client.post("/api/delivery/verify-delivery",
            headers={"Authorization": f"Bearer {self.partner_token}"},
            json={
                "order_id": order_id,
                "otp": "654321"
            }
        )
        self.assertEqual(delivery_res.status_code, 400)

    def test_05_receiver_tracking_and_delivery_otp(self):
        res = self.client.post("/api/orders", 
            headers={"Authorization": f"Bearer {self.sender_token}"},
            json={
                "receiver_phone": "9988776655",
                "pickup_address": "123 Main St",
                "delivery_address": "456 Market St",
                "item_description": "Documents"
            }
        )
        data = res.get_json()
        order_id = data["order_id"]
        pickup_otp = data["pickup_otp"]
        tracking_token = data["tracking_token"]

        track_res = self.client.get(f"/api/track/{tracking_token}")
        self.assertEqual(track_res.status_code, 200)
        self.assertIsNone(track_res.get_json().get("delivery_otp"))

        self.client.post("/api/delivery/verify-pickup",
            headers={"Authorization": f"Bearer {self.partner_token}"},
            json={"order_id": order_id, "otp": pickup_otp}
        )

        track_res_after = self.client.get(f"/api/track/{tracking_token}")
        self.assertEqual(track_res_after.status_code, 200)
        self.assertIsNotNone(track_res_after.get_json().get("delivery_otp"))

    def test_06_expired_otp_rejected(self):
        """Test expired OTP is rejected"""
        res = self.client.post("/api/orders", 
            headers={"Authorization": f"Bearer {self.sender_token}"},
            json={
                "receiver_phone": "9988776655",
                "pickup_address": "123 Main St",
                "delivery_address": "456 Market St",
                "item_description": "Documents"
            }
        )
        data = res.get_json()
        order_id = data["order_id"]
        pickup_otp = data["pickup_otp"]

        for doc in db["orders"].documents:
            if str(doc.get("_id")) == order_id:
                doc["pickup_otp_expiry"] = datetime.utcnow() - timedelta(minutes=5)

        verify_res = self.client.post("/api/delivery/verify-pickup",
            headers={"Authorization": f"Bearer {self.partner_token}"},
            json={"order_id": order_id, "otp": pickup_otp}
        )
        self.assertEqual(verify_res.status_code, 400)

    def test_07_duplicate_pickup_verification_rejected(self):
        """Test verifying pickup twice is rejected"""
        res = self.client.post("/api/orders", 
            headers={"Authorization": f"Bearer {self.sender_token}"},
            json={
                "receiver_phone": "9988776655",
                "pickup_address": "123 Main St",
                "delivery_address": "456 Market St",
                "item_description": "Documents"
            }
        )
        data = res.get_json()
        order_id = data["order_id"]
        pickup_otp = data["pickup_otp"]

        self.client.post("/api/delivery/verify-pickup",
            headers={"Authorization": f"Bearer {self.partner_token}"},
            json={"order_id": order_id, "otp": pickup_otp}
        )

        verify_res_2 = self.client.post("/api/delivery/verify-pickup",
            headers={"Authorization": f"Bearer {self.partner_token}"},
            json={"order_id": order_id, "otp": pickup_otp}
        )
        self.assertEqual(verify_res_2.status_code, 400)

if __name__ == "__main__":
    unittest.main()
