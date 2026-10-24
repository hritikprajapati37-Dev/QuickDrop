from datetime import datetime
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
    orders = list(db["orders"].find({}))
    for o in orders:
        o["order_id"] = str(o["_id"])
        del o["_id"]
        for k, v in list(o.items()):
            if isinstance(v, datetime):
                o[k] = v.isoformat()
    return jsonify({"orders": orders}), 200

@admin_bp.route("/api/admin/users", methods=["GET"], endpoint="admin_get_users")
@token_required
@role_required("ADMIN")
def admin_get_users():
    db = get_db()
    if db is None:
        return jsonify({"error": "Database not connected"}), 500
    users = list(db["users"].find({}, {"password_hash": 0}))
    for u in users:
        u["user_id"] = str(u["_id"])
        del u["_id"]
        for k, v in list(u.items()):
            if isinstance(v, datetime):
                u[k] = v.isoformat()
    return jsonify({"users": users}), 200

@admin_bp.route("/api/admin/feedbacks", methods=["GET"], endpoint="admin_get_feedbacks")
@token_required
@role_required("ADMIN")
def admin_get_feedbacks():
    db = get_db()
    if db is None:
        return jsonify({"error": "Database not connected"}), 500
    feedbacks = list(db["feedbacks"].find({}))
    for f in feedbacks:
        f["feedback_id"] = str(f["_id"])
        del f["_id"]
        for k, v in list(f.items()):
            if isinstance(v, datetime):
                f[k] = v.isoformat()
    return jsonify({"feedbacks": feedbacks}), 200

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

    now_iso = datetime.utcnow().isoformat()
    result = db["orders"].update_one(
        {"_id": obj_id},
        {"$set": {"assigned_partner": partner_phone, "status": "ASSIGNED", "assigned_at": now_iso}}
    )

    if result.modified_count == 0:
        return jsonify({"error": "Order not found or update failed"}), 404

    if not db["orderhistories"].find_one({"order_id": order_id, "status": "ASSIGNED"}):
        db["orderhistories"].insert_one({"order_id": order_id, "status": "ASSIGNED", "timestamp": now_iso, "created_at": datetime.utcnow()})

    return jsonify({"message": "Delivery partner assigned successfully"}), 200
