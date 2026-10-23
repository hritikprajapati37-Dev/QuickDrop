import os
from datetime import datetime, timedelta
from functools import wraps
from flask import jsonify, request
import bcrypt
import jwt

JWT_SECRET_KEY = os.getenv("JWT_SECRET_KEY", "quickdrop_fixed_jwt_secret_key_2026_prod")

def hash_secret(secret_str: str) -> str:
    salt = bcrypt.gensalt(rounds=12)
    return bcrypt.hashpw(str(secret_str).encode('utf-8'), salt).decode('utf-8')

def verify_secret(secret_str: str, hashed: str) -> bool:
    try:
        return bcrypt.checkpw(str(secret_str).encode('utf-8'), hashed.encode('utf-8'))
    except Exception:
        return False

def generate_token(user_id: str, role: str, phone: str) -> str:
    payload = {
        "sub": user_id,
        "role": role,
        "phone": phone,
        "iat": datetime.utcnow(),
        "exp": datetime.utcnow() + timedelta(days=30) # 30 days token expiry for smooth mobile app user experience
    }
    return jwt.encode(payload, JWT_SECRET_KEY, algorithm="HS256")

def token_required(role_or_func=None):
    if callable(role_or_func):
        return _verify_token(role_or_func, required_role=None)
    else:
        required_role = role_or_func
        def decorator(f):
            return _verify_token(f, required_role=required_role)
        return decorator

def _verify_token(f, required_role=None):
    @wraps(f)
    def decorated(*args, **kwargs):
        auth_header = request.headers.get("Authorization")
        if not auth_header or not auth_header.startswith("Bearer "):
            return jsonify({"error": "Missing or invalid authorization token"}), 401
        token = auth_header.split(" ")[1]
        try:
            payload = jwt.decode(token, JWT_SECRET_KEY, algorithms=["HS256"])
            request.user = payload
        except jwt.ExpiredSignatureError:
            return jsonify({"error": "Token has expired. Please log in again."}), 401
        except jwt.InvalidTokenError:
            return jsonify({"error": "Invalid authentication token. Please log in again."}), 401
        
        if required_role and payload.get("role") != required_role:
            return jsonify({"error": "Insufficient permissions"}), 403

        return f(*args, **kwargs)
    return decorated

def role_required(required_role):
    return token_required(required_role)
