import secrets
import math
from datetime import datetime, timedelta
from flask import Blueprint, jsonify, request, send_from_directory
from bson import ObjectId
from backend.services.db_service import get_db
from backend.middleware.security import token_required, role_required, hash_secret

order_bp = Blueprint("order_bp", __name__)

def check_and_expire_orders(db):
    if db is None:
        return
    now_iso = datetime.utcnow().isoformat()
    expired_orders = list(db["orders"].find({"status": "MATCHING", "acceptance_deadline_at": {"$lte": now_iso}}))
    for o in expired_orders:
        oid = str(o["_id"])
        db["orders"].update_one({"_id": o["_id"]}, {"$set": {"status": "EXPIRED", "refund_status": "refunded"}})
        db["payments"].update_one({"order_id": oid}, {"$set": {"payment_status": "REFUNDED", "refunded_at": now_iso}})
        db["orderhistories"].insert_one({"order_id": oid, "status": "EXPIRED", "timestamp": now_iso})

@order_bp.route("/api/categories", methods=["GET"], endpoint="get_categories")
def get_categories():
    db = get_db()
    if db is None:
        return jsonify({"error": "Database not connected"}), 500
    cats = list(db["categories"].find({}, {"_id": 0}))
    if not cats:
        cats = [
            {"category_id": "1", "name": "Documents"},
            {"category_id": "2", "name": "Electronics"},
            {"category_id": "3", "name": "Food"},
            {"category_id": "4", "name": "Fragile"},
            {"category_id": "5", "name": "Clothing"},
            {"category_id": "6", "name": "General"}
        ]
    return jsonify({"categories": cats}), 200

@order_bp.route("/api/orders", methods=["POST"], endpoint="create_order")
@token_required
@role_required("SENDER")
def create_order():
    db = get_db()
    if db is None:
        return jsonify({"error": "Database not connected"}), 500
    data = request.get_json(force=True, silent=True) or {}
    sender_phone = request.user.get("phone")
    sender_user_id = request.user.get("sub")
    receiver_phone = str(data.get("receiver_phone", "")).strip()
    pickup_address = str(data.get("pickup_address", "")).strip()
    delivery_address = str(data.get("delivery_address", "")).strip()
    item_description = str(data.get("item_description", "")).strip()
    item_weight = str(data.get("item_weight", "1.0 kg")).strip()

    try:
        pickup_lat = float(data.get("pickup_lat", 12.9716))
        pickup_lng = float(data.get("pickup_lng", 77.5946))
        delivery_lat = float(data.get("delivery_lat", 12.9780))
        delivery_lng = float(data.get("delivery_lng", 77.6000))
    except (TypeError, ValueError):
        pickup_lat, pickup_lng, delivery_lat, delivery_lng = 12.9716, 77.5946, 12.9780, 77.6000

    if not receiver_phone or not pickup_address or not delivery_address:
        return jsonify({"error": "Missing required order fields"}), 400

    if len(receiver_phone) != 10:
        return jsonify({"error": "Receiver phone number must be exactly 10 digits"}), 400

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
    now_iso = datetime.utcnow().isoformat()

    order_doc = {
        "sender_id": sender_user_id,
        "sender_phone": sender_phone,
        "receiver_phone": receiver_phone,
        "pickup_address": pickup_address,
        "delivery_address": delivery_address,
        "item_description": item_description,
        "item_weight": item_weight if item_weight else "1.0 kg",
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
        "order_time": now_iso,
        "pickup_time": None,
        "complete_order_time": None,
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

    result = db["orders"].insert_one(order_doc)
    order_id = str(result.inserted_id)

    # 1. Insert package item into `items` collection
    item_doc = {
        "order_id": order_id,
        "item_description": item_description,
        "item_weight": item_weight,
        "category": "General",
        "created_at": datetime.utcnow()
    }
    db["items"].insert_one(item_doc)

    # 2. Insert recipient details into `consignees` collection
    consignee_doc = {
        "order_id": order_id,
        "name": "Receiver",
        "receiver_phone": receiver_phone,
        "delivery_address": delivery_address,
        "created_at": datetime.utcnow()
    }
    db["consignees"].insert_one(consignee_doc)

    # 3. Insert payment record into `payments` collection
    payment_doc = {
        "order_id": order_id,
        "sender_id": sender_user_id,
        "sender_phone": sender_phone,
        "delivery_charge": delivery_charge,
        "platform_fee": platform_fee,
        "total_amount": total_amount,
        "payment_status": "PENDING",
        "refund_status": "none",
        "created_at": datetime.utcnow()
    }
    db["payments"].insert_one(payment_doc)

    # 4. Insert initial history in `orderhistories` collection
    history_doc = {
        "order_id": order_id,
        "status": "CREATED",
        "timestamp": now_iso,
        "created_at": datetime.utcnow()
    }
    db["orderhistories"].insert_one(history_doc)

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

    now = datetime.utcnow()
    now_iso = now.isoformat()
    deadline_iso = (now + timedelta(minutes=10)).isoformat()

    result = db["orders"].update_one(
        {"_id": obj_id},
        {"$set": {
            "payment_status": "SUCCESS",
            "status": "MATCHING",
            "paid_at": now_iso,
            "matching_started_at": now_iso,
            "acceptance_deadline_at": deadline_iso,
            "refund_status": "none"
        }}
    )
    if result.modified_count == 0:
        return jsonify({"error": "Order not found"}), 404

    # Update `payments` collection
    db["payments"].update_one(
        {"order_id": order_id},
        {"$set": {
            "payment_status": "SUCCESS",
            "paid_at": now_iso
        }},
        upsert=True
    )

    # Log in `orderhistories`
    db["orderhistories"].insert_one({
        "order_id": order_id,
        "status": "MATCHING",
        "timestamp": now_iso,
        "created_at": datetime.utcnow()
    })

    return jsonify({
        "message": "Payment recorded successfully and order entered matching",
        "payment_status": "SUCCESS",
        "status": "MATCHING",
        "acceptance_deadline_at": deadline_iso
    }), 200

@order_bp.route("/api/feedbacks", methods=["POST"], endpoint="submit_feedback")
@token_required
def submit_feedback():
    db = get_db()
    if db is None:
        return jsonify({"error": "Database not connected"}), 500
    data = request.get_json(force=True, silent=True) or {}
    order_id = str(data.get("order_id", "")).strip()
    rating = float(data.get("rating", 5.0))
    comment = str(data.get("comment", "")).strip()
    phone = request.user.get("phone")
    user_id = request.user.get("sub")

    feedback_doc = {
        "order_id": order_id,
        "user_id": user_id,
        "user_phone": phone,
        "rating": rating,
        "comment": comment,
        "created_at": datetime.utcnow()
    }
    db["feedbacks"].insert_one(feedback_doc)
    return jsonify({"message": "Feedback submitted successfully"}), 201

@order_bp.route("/api/orders/<order_id>/otps", methods=["GET"], endpoint="get_order_otps")
@token_required
@role_required("SENDER")
def get_order_otps(order_id):
    db = get_db()
    if db is None:
        return jsonify({"error": "Database not connected"}), 500
    sender_phone = request.user.get("phone")

    try:
        obj_id = ObjectId(order_id)
    except Exception:
        return jsonify({"error": "Invalid order ID"}), 400

    order = db["orders"].find_one({"_id": obj_id, "sender_phone": sender_phone})
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

    check_and_expire_orders(db)

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
        if isinstance(o.get("created_at"), datetime):
            o["created_at"] = o["created_at"].isoformat()
    return jsonify({"orders": orders}), 200

@order_bp.route("/api/orders/<order_id>", methods=["GET"], endpoint="get_order_details")
@token_required
def get_order_details(order_id):
    db = get_db()
    if db is None:
        return jsonify({"error": "Database not connected"}), 500

    check_and_expire_orders(db)

    try:
        obj_id = ObjectId(order_id)
    except Exception:
        return jsonify({"error": "Invalid order ID"}), 400

    order = db["orders"].find_one({"_id": obj_id})
    if not order:
        return jsonify({"error": "Order not found"}), 404

    order["order_id"] = str(order["_id"])
    del order["_id"]
    if isinstance(order.get("created_at"), datetime):
        order["created_at"] = order["created_at"].isoformat()
    return jsonify(order), 200

@order_bp.route("/api/user/dashboard", methods=["GET"], endpoint="user_dashboard")
@token_required
def user_dashboard():
    db = get_db()
    if db is None:
        return jsonify({"error": "Database not connected"}), 500

    check_and_expire_orders(db)

    phone = request.user.get("phone")
    role = request.user.get("role", "SENDER")

    user_doc = db["users"].find_one({"phone": phone})
    name = "User"
    if role == "SENDER":
        consignor = db["consignors"].find_one({"phone": phone})
        if consignor:
            name = consignor.get("name", "User")
    elif role == "PARTNER":
        partner = db["delivery_boy_schemas"].find_one({"phone": phone})
        if partner:
            name = partner.get("name", "Partner")

    if role == "SENDER":
        query = {"sender_phone": phone}
    elif role == "PARTNER":
        query = {"$or": [{"assigned_partner": phone}, {"assigned_partner": None}, {"assigned_partner": {"$exists": False}}]}
    else:
        query = {}

    orders = list(db["orders"].find(query).sort("created_at", -1))

    active_count = 0
    completed_count = 0
    pending_count = 0
    active_delivery = None
    recent_orders = []

    for o in orders:
        o["order_id"] = str(o["_id"])
        del o["_id"]
        if isinstance(o.get("created_at"), datetime):
            o["created_at"] = o["created_at"].isoformat()

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
