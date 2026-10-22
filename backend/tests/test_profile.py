import unittest
import json
from backend.app import app
from backend.services.db_service import get_db

class QuickDropProfileTestCase(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.client = app.test_client()

    def test_profile_update_and_email_storing(self):
        db = get_db()
        if db is None:
            self.skipTest("Database not connected")
        
        phone = "9988776655"
        db["users"].delete_many({"phone": phone})
        
        signup_res = self.client.post("/api/auth/signup", json={
            "name": "Test User",
            "phone": phone,
            "password": "Password123",
            "role": "SENDER"
        })
        data = signup_res.get_json()
        token = data.get("token")
        if not token:
            login_res = self.client.post("/api/auth/login", json={
                "phone": phone,
                "password": "Password123"
            })
            token = login_res.get_json().get("token")

        headers = {"Authorization": f"Bearer {token}"}

        # Test updating valid email
        update_res = self.client.put("/api/user/profile", json={
            "name": "Test User Updated",
            "email": "testuser@example.com"
        }, headers=headers)
        self.assertEqual(update_res.status_code, 200)
        self.assertEqual(update_res.get_json().get("email"), "testuser@example.com")

        # Test updating invalid email
        invalid_res = self.client.put("/api/user/profile", json={
            "name": "Test User Updated",
            "email": "invalid-email"
        }, headers=headers)
        self.assertEqual(invalid_res.status_code, 400)

        # Test clearing email (empty string)
        clear_res = self.client.put("/api/user/profile", json={
            "name": "Test User Updated",
            "email": ""
        }, headers=headers)
        self.assertEqual(clear_res.status_code, 200)
        self.assertEqual(clear_res.get_json().get("email"), "")

        # Cleanup
        db["users"].delete_many({"phone": phone})

if __name__ == "__main__":
    unittest.main()
