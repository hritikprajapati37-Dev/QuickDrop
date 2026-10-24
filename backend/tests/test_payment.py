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

class QuickDropPaymentTestCase(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        cls.client = app.test_client()

    def setUp(self):
        if db is not None:
            db["users"].delete_many({})
            db["orders"].delete_many({})
            db["payments"].delete_many({})

            res = self.client.post("/api/auth/signup", json={
                "phone": "9876543210",
                "name": "Payment Test Sender",
                "password": "password123",
                "role": "SENDER"
            })
            self.sender_token = res.get_json().get("token")

    def test_01_create_order_returns_pricing(self):
        res = self.client.post("/api/orders",
            headers={"Authorization": f"Bearer {self.sender_token}"},
            json={
                "receiver_phone": "9988776655",
                "pickup_address": "123 Main St",
                "delivery_address": "456 Market St",
                "item_description": "Documents",
                "item_weight": "1.0 kg"
            }
        )
        self.assertEqual(res.status_code, 201)
        data = res.get_json()
        self.assertIn("total_amount", data)
        self.assertIn("delivery_charge", data)
        self.assertIn("platform_fee", data)

    def test_02_pay_order_updates_status_to_matching_and_5min_deadline(self):
        res = self.client.post("/api/orders",
            headers={"Authorization": f"Bearer {self.sender_token}"},
            json={
                "receiver_phone": "9988776655",
                "pickup_address": "123 Main St",
                "delivery_address": "456 Market St",
                "item_description": "Documents"
            }
        )
        order_id = res.get_json()["order_id"]

        pay_res = self.client.post(f"/api/orders/{order_id}/pay",
            headers={"Authorization": f"Bearer {self.sender_token}"},
            json={
                "payment_method": "demo_gateway"
            }
        )
        self.assertEqual(pay_res.status_code, 200)
        pay_data = pay_res.get_json()
        self.assertEqual(pay_data["payment_status"], "SUCCESS")
        self.assertEqual(pay_data["status"], "MATCHING")
        self.assertIn("acceptance_deadline_at", pay_data)

    def test_03_expired_matching_order_auto_expires_and_demo_refunds(self):
        res = self.client.post("/api/orders",
            headers={"Authorization": f"Bearer {self.sender_token}"},
            json={
                "receiver_phone": "9988776655",
                "pickup_address": "123 Main St",
                "delivery_address": "456 Market St"
            }
        )
        order_id = res.get_json()["order_id"]

        self.client.post(f"/api/orders/{order_id}/pay",
            headers={"Authorization": f"Bearer {self.sender_token}"},
            json={"payment_method": "demo_gateway"}
        )

        # Fast-forward deadline in mock DB to simulate 5-min expiry
        for doc in db["orders"].documents:
            if str(doc.get("_id")) == order_id:
                doc["acceptance_deadline_at"] = (datetime.utcnow() - timedelta(minutes=1)).isoformat()

        # Trigger check by querying details
        get_res = self.client.get(f"/api/orders/{order_id}",
            headers={"Authorization": f"Bearer {self.sender_token}"}
        )
        self.assertEqual(get_res.status_code, 200)
        order_data = get_res.get_json()
        self.assertEqual(order_data["status"], "EXPIRED")
        self.assertEqual(order_data["refund_status"], "demo_refunded")

if __name__ == "__main__":
    unittest.main()
