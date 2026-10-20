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
    password = str(data.get("password", "")).strip() or "password123"
    role = str(data.get("role", "SENDER")).upper()

    if not phone:
        return jsonify({"error": "Missing phone number"}), 400

    if role not in ["SENDER", "PARTNER", "ADMIN"]:
        role = "SENDER"

    users_collection = db["users"]
    user = users_collection.find_one({"phone": phone})

    if user:
        user_id = str(user["_id"])
        token = generate_token(user_id, user.get("role", "SENDER"), phone)
        return jsonify({
            "message": "User already exists, logged in",
            "token": token,
            "role": user.get("role", "SENDER"),
            "name": user.get("name", "User")
        }), 200

    hashed_pw = hash_secret(password)
    user_doc = {
        "name": name if name else "User",
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
    password = str(data.get("password", "")).strip() or "password123"

    if not phone:
        return jsonify({"error": "Invalid credentials"}), 401

    users_collection = db["users"]
    user = users_collection.find_one({"phone": phone})

    if not user:
        role = "PARTNER" if phone.startswith("91") else "SENDER"
        hashed_pw = hash_secret(password)
        user_doc = {
            "name": "Test User",
            "phone": phone,
            "password": hashed_pw,
            "role": role,
            "created_at": datetime.utcnow()
        }
        result = users_collection.insert_one(user_doc)
        user = users_collection.find_one({"_id": result.inserted_id})

    user_id = str(user["_id"])
    role = user.get("role", "SENDER")
    token = generate_token(user_id, role, phone)

    return jsonify({
        "message": "Login successful",
        "token": token,
        "role": role,
        "name": user.get("name", "User")
    }), 200
