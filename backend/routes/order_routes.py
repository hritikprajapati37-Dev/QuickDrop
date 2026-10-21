import secrets
import math
from datetime import datetime, timedelta
from flask import Blueprint, jsonify, request, send_from_directory
from bson import ObjectId
from backend.services.db_service import get_db
from backend.middleware.security import token_required, role_required, hash_secret

order_bp = Blueprint("order_bp", __name__)

@order_bp.route("/api/orders", methods=["POST"], endpoint="create_order")
@token_required
@role_required("SENDER")
def create_order():
    db = get_db()
    if db is None:
        return jsonify({"error": "Database not connected"}), 500
    data = request.get_json(force=True, silent=True) or {}
    sender_phone = request.user.get("phone")
    receiver_phone = str(data.get("receiver_phone", "")).strip()
    pickup_address = str(data.get("pickup_address", "")).strip()
    delivery_address = str(data.get("delivery_address", "")).strip()
    item_description = str(data.get("item_description", "")).strip()

    try:
        pickup_lat = float(data.get("pickup_lat", 12.9716))
        pickup_lng = float(data.get("pickup_lng", 77.5946))
        delivery_lat = float(data.get("delivery_lat", 12.9780))
        delivery_lng = float(data.get("delivery_lng", 77.6000))
    except (TypeError, ValueError):
        pickup_lat, pickup_lng, delivery_lat, delivery_lng = 12.9716, 77.5946, 12.9780, 77.6000

    if not receiver_phone or not pickup_address or not delivery_address:
        return jsonify({"error": "Missing required order fields"}), 400

    def haversine(lat1, lon1, lat2, lon2):
        R = 6371.0
        dlat = math.radians(lat2 - lat1)
        dlon = math.radians(lon2 - lon1)
        a = math.sin(dlat / 2)**2 + math.cos(math.radians(lat1)) * math.cos(math.radians(lat2)) * math.sin(dlon / 2)**2
        c = 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a))
        return R * c

    straight_km = haversine(pickup_lat, pickup_lng, delivery_lat, delivery_lng)
    distance_km = round(max(0.5, straight_km * 1.2), 2)

    if distance_km > 5.0:
        return jsonify({"error": f"Delivery distance ({distance_km} km) exceeds maximum allowed 5 km radius limit."}), 400

    delivery_charge = round(distance_km * 5.0, 2)
    platform_fee = 2.0
    total_amount = round(delivery_charge + platform_fee, 2)
    partner_earning = delivery_charge

    pickup_otp_plain = str(secrets.randbelow(900000) + 100000)
    pickup_otp_hash = hash_secret(pickup_otp_plain)
    pickup_otp_expiry = datetime.utcnow() + timedelta(minutes=10)

    tracking_token = secrets.token_urlsafe(32)

    order_doc = {
        "sender_phone": sender_phone,
        "receiver_phone": receiver_phone,
        "pickup_address": pickup_address,
        "delivery_address": delivery_address,
        "item_description": item_description,
        "pickup_lat": pickup_lat,
        "pickup_lng": pickup_lng,
        "delivery_lat": delivery_lat,
        "delivery_lng": delivery_lng,
        "distance_km": distance_km,
        "delivery_charge": delivery_charge,
        "platform_fee": platform_fee,
        "total_amount": total_amount,
        "partner_earning": partner_earning,
        "status": "CREATED",
        "payment_status": "PENDING",
        "pickup_verified": False,
        "delivered": False,
        "assigned_partner": data.get("assigned_partner", None),
        "tracking_token": tracking_token,
        "pickup_otp_hash": pickup_otp_hash,
        "pickup_otp_expiry": pickup_otp_expiry,
        "pickup_otp_attempts": 0,
        "delivery_otp_hash": None,
        "delivery_otp_expiry": None,
        "delivery_otp_attempts": 0,
        "created_at": datetime.utcnow()
    }

    orders_collection = db["orders"]
    result = orders_collection.insert_one(order_doc)
    order_id = str(result.inserted_id)

    return jsonify({
        "message": "Order created successfully",
        "order_id": order_id,
        "tracking_token": tracking_token,
        "pickup_otp": pickup_otp_plain,
        "distance_km": distance_km,
        "delivery_charge": delivery_charge,
        "platform_fee": platform_fee,
        "total_amount": total_amount
    }), 201

@order_bp.route("/api/orders/<order_id>/pay", methods=["POST"], endpoint="pay_order")
@token_required
@role_required("SENDER")
def pay_order(order_id):
    db = get_db()
    if db is None:
        return jsonify({"error": "Database not connected"}), 500
    try:
        obj_id = ObjectId(order_id)
    except Exception:
        return jsonify({"error": "Invalid order ID"}), 400

    result = db["orders"].update_one(
        {"_id": obj_id},
        {"$set": {"payment_status": "SUCCESS", "status": "CONFIRMED"}}
    )
    if result.modified_count == 0:
        return jsonify({"error": "Order not found"}), 404
    return jsonify({"message": "Payment recorded successfully", "payment_status": "SUCCESS"}), 200

@order_bp.route("/api/orders/<order_id>/otps", methods=["GET"], endpoint="get_order_otps")
@token_required
@role_required("SENDER")
def get_order_otps(order_id):
    db = get_db()
    if db is None:
        return jsonify({"error": "Database not connected"}), 500
    sender_phone = request.user.get("phone")
    orders_collection = db["orders"]

    try:
        obj_id = ObjectId(order_id)
    except Exception:
        return jsonify({"error": "Invalid order ID"}), 400

    order = orders_collection.find_one({"_id": obj_id, "sender_phone": sender_phone})
    if not order:
        return jsonify({"error": "Order not found or unauthorized"}), 404

    return jsonify({
        "order_id": order_id,
        "status": order.get("status"),
        "pickup_verified": order.get("pickup_verified", False),
        "delivered": order.get("delivered", False),
        "delivery_otp_available": order.get("delivery_otp_hash") is not None
    }), 200

@order_bp.route("/api/orders/list", methods=["GET"], endpoint="orders_list")
@token_required
def orders_list():
    db = get_db()
    if db is None:
        return jsonify({"error": "Database not connected"}), 500
    phone = request.user.get("phone")
    role = request.user.get("role", "SENDER")
    if role == "SENDER":
        query = {"sender_phone": phone}
    elif role == "PARTNER":
        query = {"$or": [{"assigned_partner": phone}, {"assigned_partner": None}, {"assigned_partner": {"$exists": False}}]}
    else:
        query = {}
    orders = list(db["orders"].find(query).sort("created_at", -1))
    for o in orders:
        o["order_id"] = str(o["_id"])
        del o["_id"]
    return jsonify({"orders": orders}), 200

@order_bp.route("/api/orders/<order_id>", methods=["GET"], endpoint="get_order_details")
@token_required
def get_order_details(order_id):
    db = get_db()
    if db is None:
        return jsonify({"error": "Database not connected"}), 500
    try:
        obj_id = ObjectId(order_id)
    except Exception:
        return jsonify({"error": "Invalid order ID"}), 400

    order = db["orders"].find_one({"_id": obj_id})
    if not order:
        return jsonify({"error": "Order not found"}), 404

    order["order_id"] = str(order["_id"])
    del order["_id"]
    return jsonify(order), 200

@order_bp.route("/api/user/dashboard", methods=["GET"], endpoint="user_dashboard")
@token_required
def user_dashboard():
    db = get_db()
    if db is None:
        return jsonify({"error": "Database not connected"}), 500

    phone = request.user.get("phone")
    role = request.user.get("role", "SENDER")

    users_collection = db["users"]
    user_doc = users_collection.find_one({"phone": phone})
    name = user_doc.get("name", "User") if user_doc else request.user.get("name", "User")

    orders_collection = db["orders"]

    if role == "SENDER":
        query = {"sender_phone": phone}
    elif role == "PARTNER":
        query = {"$or": [{"assigned_partner": phone}, {"assigned_partner": None}, {"assigned_partner": {"$exists": False}}]}
    else:
        query = {}

    orders = list(orders_collection.find(query).sort("created_at", -1))

    active_count = 0
    completed_count = 0
    pending_count = 0
    active_delivery = None
    recent_orders = []

    for o in orders:
        o["order_id"] = str(o["_id"])
        del o["_id"]
        status = o.get("status", "CREATED")
        delivered = o.get("delivered", False)
        pickup_verified = o.get("pickup_verified", False)

        if delivered or status == "DELIVERED":
            completed_count += 1
        elif pickup_verified or status == "PICKED_UP" or status == "ASSIGNED":
            active_count += 1
            if role == "PARTNER" and o.get("assigned_partner") == phone and not active_delivery:
                active_delivery = o
            elif role == "SENDER" and not active_delivery:
                active_delivery = o
        else:
            pending_count += 1

        recent_orders.append(o)

    return jsonify({
        "name": name,
        "phone": phone,
        "role": role,
        "counts": {
            "active": active_count,
            "completed": completed_count,
            "pending": pending_count,
            "total": len(orders)
        },
        "active_delivery": active_delivery,
        "recent_orders": recent_orders
    }), 200

@order_bp.route("/api/track/<tracking_token>", methods=["GET"], endpoint="track_order")
def track_order(tracking_token):
    db = get_db()
    if not tracking_token:
        return jsonify({"error": "Missing tracking token"}), 400

    orders_collection = db["orders"] if db is not None else None
    if orders_collection is None:
        return jsonify({"error": "Database not available"}), 500

    order = orders_collection.find_one({"tracking_token": tracking_token})
    if not order:
        return jsonify({"error": "Invalid or expired tracking token"}), 404

    show_delivery_otp = order.get("pickup_verified", False) and not order.get("delivered", False)

    return jsonify({
        "order_id": str(order["_id"]),
        "status": order.get("status", "CREATED"),
        "pickup_verified": order.get("pickup_verified", False),
        "delivered": order.get("delivered", False),
        "item_description": order.get("item_description"),
        "delivery_address": order.get("delivery_address"),
        "delivery_otp": "654321" if show_delivery_otp else None
    }), 200

@order_bp.route("/track/<tracking_token>", endpoint="serve_tracking_page")
def serve_tracking_page(tracking_token):
    return send_from_directory('..', 'tracking.html')
