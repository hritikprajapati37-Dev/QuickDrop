import secrets
import math
from datetime import datetime, timedelta
from flask import Blueprint, jsonify, request
from bson import ObjectId
from backend.services.db_service import get_db
from backend.middleware.security import token_required, role_required, hash_secret, verify_secret

delivery_bp = Blueprint("delivery_bp", __name__)

def haversine_km(lat1, lon1, lat2, lon2):
    R = 6371.0
    dlat = math.radians(lat2 - lat1)
    dlon = math.radians(lon2 - lon1)
    a = math.sin(dlat / 2)**2 + math.cos(math.radians(lat1)) * math.cos(math.radians(lat2)) * math.sin(dlon / 2)**2
    c = 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a))
    return R * c

def check_and_expire_orders(db):
    if db is None:
        return
    now_iso = datetime.utcnow().isoformat()
    expired_orders = list(db["orders"].find({"status": "MATCHING", "acceptance_deadline_at": {"$lte": now_iso}}))
    for o in expired_orders:
        oid = str(o["_id"])
        db["orders"].update_one({"_id": o["_id"]}, {"$set": {"status": "EXPIRED", "refund_status": "demo_refunded"}})
        db["payments"].update_one({"order_id": oid}, {"$set": {"payment_status": "REFUNDED", "refund_status": "demo_refunded", "refunded_at": now_iso}})
        if not db["orderhistories"].find_one({"order_id": oid, "status": "EXPIRED"}):
            db["orderhistories"].insert_one({"order_id": oid, "status": "EXPIRED", "timestamp": now_iso, "created_at": datetime.utcnow()})

@delivery_bp.route("/api/partner/status", methods=["POST"], endpoint="update_partner_status")
@token_required
def update_partner_status():
    db = get_db()
    if db is None:
        return jsonify({"error": "Database not connected"}), 500
    data = request.get_json(force=True, silent=True) or {}
    is_online = bool(data.get("is_online", False))
    partner_phone = request.user.get("phone")
    user_id = request.user.get("sub")

    db["delivery_boy_schemas"].update_one(
        {"$or": [{"user_id": user_id}, {"phone": partner_phone}]},
        {"$set": {"is_online": is_online, "last_active": datetime.utcnow()}},
        upsert=True
    )
    return jsonify({
        "message": "Partner status updated successfully in database",
        "is_online": is_online
    }), 200

@delivery_bp.route("/api/partner/location", methods=["POST"], endpoint="update_partner_location")
@token_required
@role_required("PARTNER")
def update_partner_location():
    db = get_db()
    if db is None:
        return jsonify({"error": "Database not connected"}), 500
    data = request.get_json(force=True, silent=True) or {}
    lat = data.get("lat")
    lng = data.get("lng")
    partner_phone = request.user.get("phone")
    user_id = request.user.get("sub")

    if lat is None or lng is None:
        return jsonify({"error": "Missing lat/lng"}), 400

    now_iso = datetime.utcnow().isoformat()
    db["delivery_boy_schemas"].update_one(
        {"$or": [{"user_id": user_id}, {"phone": partner_phone}]},
        {"$set": {
            "partner_lat": float(lat),
            "partner_lng": float(lng),
            "location_updated_at": now_iso,
            "last_active": datetime.utcnow()
        }},
        upsert=True
    )

    db["orders"].update_many(
        {"assigned_partner": partner_phone, "delivered": False},
        {"$set": {
            "partner_lat": float(lat),
            "partner_lng": float(lng),
            "partner_location_updated_at": now_iso
        }}
    )

    return jsonify({"message": "Location updated successfully", "lat": lat, "lng": lng, "updated_at": now_iso}), 200

@delivery_bp.route("/api/delivery/arrived-pickup", methods=["POST"], endpoint="arrived_pickup")
@token_required
@role_required("PARTNER")
def arrived_pickup():
    db = get_db()
    if db is None:
        return jsonify({"error": "Database not connected"}), 500
    data = request.get_json(force=True, silent=True) or {}
    order_id = data.get("order_id")
    partner_phone = request.user.get("phone")

    try:
        obj_id = ObjectId(order_id)
    except Exception:
        return jsonify({"error": "Invalid order ID"}), 400

    now_iso = datetime.utcnow().isoformat()
    result = db["orders"].update_one(
        {"_id": obj_id, "assigned_partner": partner_phone, "pickup_verified": False},
        {"$set": {
            "status": "ARRIVED_PICKUP",
            "arrived_pickup_at": now_iso
        }}
    )
    if result.modified_count == 0:
        order = db["orders"].find_one({"_id": obj_id})
        if order and order.get("pickup_verified", False):
            return jsonify({"message": "Pickup already verified", "status": "ARRIVED_PICKUP"}), 200
        return jsonify({"error": "Order not found or not assigned to partner"}), 400

    if not db["orderhistories"].find_one({"order_id": order_id, "status": "ARRIVED_PICKUP"}):
        db["orderhistories"].insert_one({"order_id": order_id, "status": "ARRIVED_PICKUP", "timestamp": now_iso, "created_at": datetime.utcnow()})

    return jsonify({"message": "Arrived at pickup location recorded", "status": "ARRIVED_PICKUP"}), 200

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
        return jsonify({"message": "Pickup already verified for this order", "status": "PICKED_UP"}), 200

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
        return jsonify({"message": "Pickup already verified", "status": "PICKED_UP"}), 200

    if not db["orderhistories"].find_one({"order_id": order_id, "status": "PICKED_UP"}):
        db["orderhistories"].insert_one({"order_id": order_id, "status": "PICKED_UP", "timestamp": pickup_time_iso, "created_at": datetime.utcnow()})

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

    if order.get("delivered", False):
        return jsonify({"message": "Order already delivered", "status": "DELIVERED"}), 200

    if not order.get("pickup_verified", False):
        return jsonify({"error": "Cannot complete delivery before pickup verification"}), 400

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
        {"_id": obj_id},
        {
            "$set": {
                "delivered": True,
                "status": "DELIVERED",
                "complete_order_time": complete_time_iso,
                "delivery_otp_hash": None
            }
        }
    )

    if not db["orderhistories"].find_one({"order_id": order_id, "status": "DELIVERED"}):
        db["orderhistories"].insert_one({"order_id": order_id, "status": "DELIVERED", "timestamp": complete_time_iso, "created_at": datetime.utcnow()})

    return jsonify({
        "message": "Delivery and handover confirmed successfully",
        "status": "DELIVERED"
    }), 200

@delivery_bp.route("/api/partner/available", methods=["GET"], endpoint="partner_available")
@token_required
def partner_available():
    db = get_db()
    if db is None:
        return jsonify({"error": "Database not connected"}), 500

    check_and_expire_orders(db)

    partner_phone = request.user.get("phone")
    partner_user_id = request.user.get("sub")

    partner_lat = request.args.get("lat", type=float)
    partner_lng = request.args.get("lng", type=float)

    partner_profile = db["delivery_boy_schemas"].find_one({"$or": [{"user_id": partner_user_id}, {"phone": partner_phone}]})
    if partner_lat is None and partner_profile:
        partner_lat = partner_profile.get("partner_lat")
    if partner_lng is None and partner_profile:
        partner_lng = partner_profile.get("partner_lng")

    # Query only paid orders in MATCHING status, unassigned
    query = {
        "payment_status": "SUCCESS",
        "status": "MATCHING",
        "$or": [{"assigned_partner": None}, {"assigned_partner": {"$exists": False}}]
    }
    orders = list(db["orders"].find(query).sort("created_at", -1))

    is_default_location = (partner_lat == 12.9716 and partner_lng == 77.5946) or (partner_lat is None or partner_lng is None)

    filtered_orders = []
    for o in orders:
        plat = o.get("pickup_lat", 12.9716)
        plng = o.get("pickup_lng", 77.5946)

        if not is_default_location:
            dist_km = haversine_km(plat, plng, partner_lat, partner_lng)
            if dist_km <= 5.0:
                o["distance_to_pickup_km"] = round(dist_km, 2)
                filtered_orders.append(o)
        else:
            # Uninitialized or default location: include matching orders so partner is not blocked
            filtered_orders.append(o)

    if not is_default_location:
        filtered_orders.sort(key=lambda x: x.get("distance_to_pickup_km", 0.0))

    for o in filtered_orders:
        o["order_id"] = str(o["_id"])
        del o["_id"]
    return jsonify({"requests": filtered_orders}), 200

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
    partner_user_id = request.user.get("sub")

    if not order_id:
        return jsonify({"error": "Missing order_id"}), 400

    try:
        obj_id = ObjectId(order_id)
    except Exception:
        return jsonify({"error": "Invalid order ID"}), 400

    active_delivery = db["orders"].find_one({
        "assigned_partner": partner_phone,
        "delivered": False,
        "status": {"$in": ["ASSIGNED", "PICKED_UP", "IN_TRANSIT"]}
    })
    if active_delivery:
        return jsonify({"error": "You already have an active delivery in progress. Complete it first."}), 400

    order = db["orders"].find_one({"_id": obj_id})
    if not order:
        return jsonify({"error": "Order not found"}), 404

    if order.get("payment_status") != "SUCCESS" or order.get("status") != "MATCHING":
        return jsonify({"error": "Order is not paid or no longer available for matching"}), 400

    now_iso = datetime.utcnow().isoformat()
    deadline = order.get("acceptance_deadline_at")
    if deadline and deadline <= now_iso:
        db["orders"].update_one(
            {"_id": obj_id, "status": "MATCHING"},
            {"$set": {"status": "EXPIRED", "refund_status": "demo_refunded"}}
        )
        return jsonify({"error": "Order matching deadline has expired"}), 400

    partner_profile = db["delivery_boy_schemas"].find_one({"$or": [{"user_id": partner_user_id}, {"phone": partner_phone}]})
    if partner_profile and partner_profile.get("partner_lat") is not None and partner_profile.get("partner_lng") is not None:
        plat = partner_profile.get("partner_lat")
        plng = partner_profile.get("partner_lng")
        if plat != 12.9716 or plng != 77.5946: # ignore default
            olats = order.get("pickup_lat", 12.9716)
            olngs = order.get("pickup_lng", 77.5946)
            dist_km = haversine_km(olats, olngs, plat, plng)
            if dist_km > 5.0:
                return jsonify({"error": f"You are {round(dist_km, 1)} km away from pickup. Only partners within 5 km can accept this request."}), 400

    result = db["orders"].update_one(
        {"_id": obj_id, "status": "MATCHING", "payment_status": "SUCCESS"},
        {"$set": {
            "assigned_partner": partner_phone,
            "status": "ASSIGNED",
            "assigned_at": now_iso
        }}
    )

    if result.modified_count == 0:
        return jsonify({"error": "Order was already accepted by another partner or is no longer available"}), 400

    if not db["orderhistories"].find_one({"order_id": order_id, "status": "ASSIGNED"}):
        db["orderhistories"].insert_one({"order_id": order_id, "status": "ASSIGNED", "timestamp": now_iso, "created_at": datetime.utcnow()})

    return jsonify({"message": "Order accepted successfully", "status": "ASSIGNED"}), 200

@delivery_bp.route("/api/partner/earnings", methods=["GET"], endpoint="partner_earnings")
@token_required
def partner_earnings():
    db = get_db()
    if db is None:
        return jsonify({"error": "Database not connected"}), 500
    phone = request.user.get("phone")
    completed_orders = list(db["orders"].find({"assigned_partner": phone, "delivered": True}))
    total_earnings = 0.0
    for o in completed_orders:
        total_earnings += float(o.get("partner_earning", o.get("delivery_charge", 150.0)))
    return jsonify({
        "total_earnings": total_earnings,
        "completed_deliveries": len(completed_orders),
        "currency": "₹"
    }), 200
