import re
from datetime import datetime
from flask import Blueprint, jsonify, request
from backend.services.db_service import get_db
from backend.middleware.security import hash_secret, verify_secret, generate_token, token_required

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
    email = str(data.get("email", "")).strip()
    role = str(data.get("role", "SENDER")).upper()

    if not phone or not name or not password:
        return jsonify({"error": "Missing required fields (name, phone, password)"}), 400

    if len(phone) < 10:
        return jsonify({"error": "Invalid phone number format"}), 400

    if email:
        if not re.match(r"[^@]+@[^@]+\.[^@]+", email):
            return jsonify({"error": "Invalid email format"}), 400

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
        "email": email,
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
        "name": user_doc["name"],
        "email": user_doc["email"]
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
        "name": user.get("name", "User"),
        "email": user.get("email", "")
    }), 200

@auth_bp.route("/api/user/profile", methods=["GET"], endpoint="get_user_profile")
@token_required
def get_user_profile():
    db = get_db()
    if db is None:
        return jsonify({"error": "Database not connected"}), 500
    phone = request.user.get("phone")
    user = db["users"].find_one({"phone": phone})
    if not user:
        return jsonify({"error": "User not found"}), 404
    return jsonify({
        "name": user.get("name", "User"),
        "phone": user.get("phone", ""),
        "email": user.get("email", ""),
        "role": user.get("role", "SENDER")
    }), 200

@auth_bp.route("/api/user/profile", methods=["PUT", "POST"], endpoint="update_user_profile")
@token_required
def update_user_profile():
    db = get_db()
    if db is None:
        return jsonify({"error": "Database not connected"}), 500
    phone = request.user.get("phone")
    user = db["users"].find_one({"phone": phone})
    if not user:
        return jsonify({"error": "User not found"}), 404

    data = request.get_json(force=True, silent=True) or {}
    name = str(data.get("name", "")).strip()
    email = str(data.get("email", "")).strip()

    update_fields = {}
    if name:
        update_fields["name"] = name
    
    if email:
        if not re.match(r"[^@]+@[^@]+\.[^@]+", email):
            return jsonify({"error": "Invalid email format"}), 400
        update_fields["email"] = email
    else:
        update_fields["email"] = ""

    db["users"].update_one({"phone": phone}, {"$set": update_fields})

    updated_user = db["users"].find_one({"phone": phone})
    return jsonify({
        "message": "Profile updated successfully",
        "name": updated_user.get("name", "User"),
        "phone": updated_user.get("phone", ""),
        "email": updated_user.get("email", ""),
        "role": updated_user.get("role", "SENDER")
    }), 200
