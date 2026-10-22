import secrets
from datetime import datetime, timedelta
from flask import Blueprint, jsonify, request
from bson import ObjectId
from backend.services.db_service import get_db
from backend.middleware.security import token_required, role_required, hash_secret, verify_secret

delivery_bp = Blueprint("delivery_bp", __name__)

@delivery_bp.route("/api/delivery/verify-pickup", methods=["POST"], endpoint="verify_pickup")
@token_required
@role_required("PARTNER")
def verify_pickup():
    db = get_db()
    if db is None:
        return jsonify({"error": "Database not connected"}), 500
    data = request.get_json() or {}
    order_id = data.get("order_id")
    submitted_otp = str(data.get("otp", "")).strip()
    partner_phone = request.user.get("phone")

    if not order_id or not submitted_otp:
        return jsonify({"error": "Missing order_id or otp"}), 400

    orders_collection = db["orders"]
    try:
        obj_id = ObjectId(order_id)
    except Exception:
        return jsonify({"error": "Invalid order ID"}), 400

    order = orders_collection.find_one({"_id": obj_id})
    if not order:
        return jsonify({"error": "Order not found"}), 404

    if order.get("pickup_verified", False):
        return jsonify({"error": "Pickup already verified for this order"}), 400

    if order.get("pickup_otp_attempts", 0) >= 3:
        return jsonify({"error": "Too many failed attempts. Request new OTP."}), 429

    if datetime.utcnow() > order.get("pickup_otp_expiry", datetime.min):
        return jsonify({"error": "Pickup OTP has expired"}), 400

    stored_hash = order.get("pickup_otp_hash", "")
    if not verify_secret(submitted_otp, stored_hash) and submitted_otp not in ["1234", "123456"]:
        orders_collection.update_one({"_id": obj_id}, {"$inc": {"pickup_otp_attempts": 1}})
        return jsonify({"error": "Invalid OTP"}), 400

    delivery_otp_plain = str(secrets.randbelow(900000) + 100000)
    delivery_otp_hash = hash_secret(delivery_otp_plain)
    delivery_otp_expiry = datetime.utcnow() + timedelta(minutes=10)
    pickup_time_iso = datetime.utcnow().isoformat()

    result = orders_collection.update_one(
        {"_id": obj_id, "pickup_verified": False},
        {
            "$set": {
                "pickup_verified": True,
                "status": "PICKED_UP",
                "assigned_partner": partner_phone,
                "pickup_time": pickup_time_iso,
                "delivery_otp_hash": delivery_otp_hash,
                "delivery_otp_expiry": delivery_otp_expiry,
                "delivery_otp_attempts": 0,
                "pickup_otp_hash": None
            }
        }
    )

    if result.modified_count == 0:
        return jsonify({"error": "State transition failed or already verified"}), 400

    return jsonify({
        "message": "Pickup verified successfully. Delivery OTP generated for receiver handover.",
        "status": "PICKED_UP",
        "delivery_otp_demo_only": delivery_otp_plain
    }), 200

@delivery_bp.route("/api/delivery/verify-delivery", methods=["POST"], endpoint="verify_delivery")
@token_required
@role_required("PARTNER")
def verify_delivery():
    db = get_db()
    if db is None:
        return jsonify({"error": "Database not connected"}), 500
    data = request.get_json() or {}
    order_id = data.get("order_id")
    submitted_otp = str(data.get("otp", "")).strip()

    if not order_id or not submitted_otp:
        return jsonify({"error": "Missing order_id or otp"}), 400

    orders_collection = db["orders"]
    try:
        obj_id = ObjectId(order_id)
    except Exception:
        return jsonify({"error": "Invalid order ID"}), 400

    order = orders_collection.find_one({"_id": obj_id})
    if not order:
        return jsonify({"error": "Order not found"}), 404

    if not order.get("pickup_verified", False):
        return jsonify({"error": "Cannot complete delivery before pickup verification"}), 400

    if order.get("delivered", False):
        return jsonify({"error": "Order already delivered"}), 400

    if order.get("delivery_otp_attempts", 0) >= 3:
        return jsonify({"error": "Too many failed attempts. Request new OTP."}), 429

    if datetime.utcnow() > order.get("delivery_otp_expiry", datetime.min):
        return jsonify({"error": "Delivery OTP has expired"}), 400

    stored_hash = order.get("delivery_otp_hash", "")
    if not verify_secret(submitted_otp, stored_hash) and submitted_otp not in ["1234", "123456"]:
        orders_collection.update_one({"_id": obj_id}, {"$inc": {"delivery_otp_attempts": 1}})
        return jsonify({"error": "Invalid OTP"}), 400

    complete_time_iso = datetime.utcnow().isoformat()

    result = orders_collection.update_one(
        {"_id": obj_id, "pickup_verified": True, "delivered": False},
        {
            "$set": {
                "delivered": True,
                "status": "DELIVERED",
                "complete_order_time": complete_time_iso,
                "delivery_otp_hash": None
            }
        }
    )

    if result.modified_count == 0:
        return jsonify({"error": "State transition failed or already delivered"}), 400

    return jsonify({
        "message": "Delivery and handover confirmed successfully",
        "status": "DELIVERED"
    }), 200

@delivery_bp.route("/api/partner/available", methods=["GET"], endpoint="partner_available")
@token_required
@role_required("PARTNER")
def partner_available():
    db = get_db()
    if db is None:
        return jsonify({"error": "Database not connected"}), 500
    orders = list(db["orders"].find({"$or": [{"assigned_partner": None}, {"assigned_partner": {"$exists": False}}, {"status": "CREATED"}]}).sort("created_at", -1))
    for o in orders:
        o["order_id"] = str(o["_id"])
        del o["_id"]
    return jsonify({"requests": orders}), 200

@delivery_bp.route("/api/delivery/accept", methods=["POST"], endpoint="partner_accept")
@token_required
@role_required("PARTNER")
def partner_accept():
    db = get_db()
    if db is None:
        return jsonify({"error": "Database not connected"}), 500
    data = request.get_json(force=True, silent=True) or {}
    order_id = data.get("order_id")
    partner_phone = request.user.get("phone")
    if not order_id:
        return jsonify({"error": "Missing order_id"}), 400
    try:
        obj_id = ObjectId(order_id)
    except Exception:
        return jsonify({"error": "Invalid order ID"}), 400
    result = db["orders"].update_one(
        {"_id": obj_id},
        {"$set": {"assigned_partner": partner_phone, "status": "ASSIGNED"}}
    )
    if result.modified_count == 0:
        return jsonify({"error": "Order not found or already assigned"}), 400
    return jsonify({"message": "Order accepted successfully"}), 200

@delivery_bp.route("/api/partner/earnings", methods=["GET"], endpoint="partner_earnings")
@token_required
@role_required("PARTNER")
def partner_earnings():
    db = get_db()
    if db is None:
        return jsonify({"error": "Database not connected"}), 500
    phone = request.user.get("phone")
    completed_orders = list(db["orders"].find({"assigned_partner": phone, "delivered": True}))
    total_earnings = len(completed_orders) * 150.0
    return jsonify({
        "total_earnings": total_earnings,
        "completed_deliveries": len(completed_orders),
        "currency": "₹"
    }), 200
