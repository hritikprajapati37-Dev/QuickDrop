from flask import Blueprint, jsonify, request
from bson import ObjectId
from backend.services.db_service import get_db
from backend.middleware.security import token_required, role_required

admin_bp = Blueprint("admin_bp", __name__)

@admin_bp.route("/api/admin/orders", methods=["GET"], endpoint="admin_get_orders")
@token_required
@role_required("ADMIN")
def admin_get_orders():
    db = get_db()
    if db is None:
        return jsonify({"error": "Database not connected"}), 500
    orders = list(db["orders"].find({}, {"_id": 1, "sender_phone": 1, "receiver_phone": 1, "status": 1, "pickup_verified": 1, "delivered": 1, "created_at": 1}))
    for o in orders:
        o["order_id"] = str(o["_id"])
        del o["_id"]
    return jsonify({"orders": orders}), 200

@admin_bp.route("/api/admin/users", methods=["GET"], endpoint="admin_get_users")
@token_required
@role_required("ADMIN")
def admin_get_users():
    db = get_db()
    if db is None:
        return jsonify({"error": "Database not connected"}), 500
    users = list(db["users"].find({}, {"_id": 1, "name": 1, "phone": 1, "role": 1, "created_at": 1}))
    for u in users:
        u["user_id"] = str(u["_id"])
        del u["_id"]
    return jsonify({"users": users}), 200

@admin_bp.route("/api/admin/assign-partner", methods=["POST"], endpoint="admin_assign_partner")
@token_required
@role_required("ADMIN")
def admin_assign_partner():
    db = get_db()
    if db is None:
        return jsonify({"error": "Database not connected"}), 500
    data = request.get_json() or {}
    order_id = data.get("order_id")
    partner_phone = data.get("partner_phone")

    if not order_id or not partner_phone:
        return jsonify({"error": "Missing order_id or partner_phone"}), 400

    try:
        obj_id = ObjectId(order_id)
    except Exception:
        return jsonify({"error": "Invalid order ID"}), 400

    result = db["orders"].update_one(
        {"_id": obj_id},
        {"$set": {"assigned_partner": partner_phone, "status": "ASSIGNED"}}
    )

    if result.modified_count == 0:
        return jsonify({"error": "Order not found or update failed"}), 404

    return jsonify({"message": "Delivery partner assigned successfully"}), 200
