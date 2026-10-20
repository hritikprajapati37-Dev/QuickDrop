from datetime import datetime
from flask import Blueprint, jsonify, request
from backend.services.db_service import get_db
from backend.middleware.security import hash_secret, verify_secret, generate_token

auth_bp = Blueprint("auth_bp", __name__)

@auth_bp.route("/api/auth/signup", methods=["POST"], endpoint="signup")
def signup():
    db = get_db()
    if db is None:
        return jsonify({"error": "Database not connected"}), 500
    data = request.get_json(force=True, silent=True) or {}
    phone = str(data.get("phone", "")).strip()
    name = str(data.get("name", "")).strip()
    password = str(data.get("password", "")).strip()
    role = str(data.get("role", "SENDER")).upper()

    if not phone or not name or not password:
        return jsonify({"error": "Missing required fields (name, phone, password)"}), 400

    if len(phone) < 10:
        return jsonify({"error": "Invalid phone number format"}), 400

    if role not in ["SENDER", "PARTNER", "ADMIN"]:
        role = "SENDER"

    users_collection = db["users"]
    user = users_collection.find_one({"phone": phone})

    if user:
        return jsonify({"error": "User with this phone number already exists. Please login."}), 400

    hashed_pw = hash_secret(password)
    user_doc = {
        "name": name,
        "phone": phone,
        "password": hashed_pw,
        "role": role,
        "created_at": datetime.utcnow()
    }
    result = users_collection.insert_one(user_doc)
    user_id = str(result.inserted_id)
    token = generate_token(user_id, role, phone)

    return jsonify({
        "message": "User registered successfully",
        "token": token,
        "role": role,
        "name": user_doc["name"]
    }), 201

@auth_bp.route("/api/auth/login", methods=["POST"], endpoint="login")
def login():
    db = get_db()
    if db is None:
        return jsonify({"error": "Database not connected"}), 500
    data = request.get_json(force=True, silent=True) or {}
    phone = str(data.get("phone", "")).strip()
    password = str(data.get("password", "")).strip()

    if not phone or not password:
        return jsonify({"error": "Invalid credentials"}), 401

    users_collection = db["users"]
    user = users_collection.find_one({"phone": phone})

    if not user or not verify_secret(password, user.get("password", "")):
        return jsonify({"error": "Invalid credentials"}), 401

    user_id = str(user["_id"])
    role = user.get("role", "SENDER")
    token = generate_token(user_id, role, phone)

    return jsonify({
        "message": "Login successful",
        "token": token,
        "role": role,
        "name": user.get("name", "User")
    }), 200
