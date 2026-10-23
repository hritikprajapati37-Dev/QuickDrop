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

    if len(phone) != 10:
        return jsonify({"error": "Phone number must be exactly 10 digits"}), 400

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
        "phone": phone,
        "password": hashed_pw,
        "role": role,
        "status": "active",
        "created_at": datetime.utcnow()
    }
    result = users_collection.insert_one(user_doc)
    user_id = str(result.inserted_id)

    # Store profile in role-specific collection
    if role == "SENDER":
        consignor_doc = {
            "user_id": user_id,
            "name": name,
            "phone": phone,
            "email": email,
            "created_at": datetime.utcnow()
        }
        db["consignors"].insert_one(consignor_doc)
    elif role == "PARTNER":
        partner_doc = {
            "user_id": user_id,
            "name": name,
            "phone": phone,
            "email": email,
            "is_online": True,
            "partner_lat": 12.9716,
            "partner_lng": 77.5946,
            "vehicle_type": "Motorcycle",
            "vehicle_number": "MH 01 AB 1234",
            "created_at": datetime.utcnow()
        }
        db["delivery_boy_schemas"].insert_one(partner_doc)

    token = generate_token(user_id, role, phone)

    return jsonify({
        "message": "User registered successfully",
        "token": token,
        "role": role,
        "name": name,
        "email": email
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

    if len(phone) != 10:
        return jsonify({"error": "Phone number must be exactly 10 digits"}), 400

    users_collection = db["users"]
    user = users_collection.find_one({"phone": phone})

    if not user or not verify_secret(password, user.get("password", "")):
        return jsonify({"error": "Invalid credentials"}), 401

    user_id = str(user["_id"])
    role = user.get("role", "SENDER")

    # Fetch name and email from profile collection
    name = "User"
    email = ""
    if role == "SENDER":
        profile = db["consignors"].find_one({"$or": [{"user_id": user_id}, {"phone": phone}]})
        if profile:
            name = profile.get("name", "User")
            email = profile.get("email", "")
    elif role == "PARTNER":
        profile = db["delivery_boy_schemas"].find_one({"$or": [{"user_id": user_id}, {"phone": phone}]})
        if profile:
            name = profile.get("name", "Partner")
            email = profile.get("email", "")

    token = generate_token(user_id, role, phone)

    return jsonify({
        "message": "Login successful",
        "token": token,
        "role": role,
        "name": name,
        "email": email
    }), 200

@auth_bp.route("/api/user/profile", methods=["GET"], endpoint="get_user_profile")
@token_required
def get_user_profile():
    db = get_db()
    if db is None:
        return jsonify({"error": "Database not connected"}), 500
    phone = request.user.get("phone")
    role = request.user.get("role", "SENDER")
    user = db["users"].find_one({"phone": phone})
    if not user:
        return jsonify({"error": "User not found"}), 404

    user_id = str(user["_id"])
    name = "User"
    email = ""
    is_online = True

    if role == "SENDER":
        profile = db["consignors"].find_one({"$or": [{"user_id": user_id}, {"phone": phone}]})
        if profile:
            name = profile.get("name", "User")
            email = profile.get("email", "")
    elif role == "PARTNER":
        profile = db["delivery_boy_schemas"].find_one({"$or": [{"user_id": user_id}, {"phone": phone}]})
        if profile:
            name = profile.get("name", "Partner")
            email = profile.get("email", "")
            is_online = profile.get("is_online", True)

    return jsonify({
        "name": name,
        "phone": phone,
        "email": email,
        "role": role,
        "is_online": is_online
    }), 200

@auth_bp.route("/api/user/profile", methods=["PUT", "POST"], endpoint="update_user_profile")
@token_required
def update_user_profile():
    db = get_db()
    if db is None:
        return jsonify({"error": "Database not connected"}), 500
    phone = request.user.get("phone")
    role = request.user.get("role", "SENDER")
    user = db["users"].find_one({"phone": phone})
    if not user:
        return jsonify({"error": "User not found"}), 404

    user_id = str(user["_id"])
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

    if role == "SENDER":
        db["consignors"].update_one(
            {"$or": [{"user_id": user_id}, {"phone": phone}]},
            {"$set": update_fields},
            upsert=True
        )
    elif role == "PARTNER":
        db["delivery_boy_schemas"].update_one(
            {"$or": [{"user_id": user_id}, {"phone": phone}]},
            {"$set": update_fields},
            upsert=True
        )

    return jsonify({
        "message": "Profile updated successfully",
        "name": name if name else "User",
        "phone": phone,
        "email": email,
        "role": role
    }), 200
