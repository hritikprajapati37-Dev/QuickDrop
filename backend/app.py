import os
from datetime import datetime
from flask import Flask, jsonify, request
from flask_cors import CORS
from flask_limiter import Limiter
from flask_limiter.util import get_remote_address
from backend.services.db_service import init_db
from backend.routes.auth_routes import auth_bp
from backend.routes.order_routes import order_bp
from backend.routes.delivery_routes import delivery_bp
from backend.routes.admin_routes import admin_bp

app = Flask(__name__, static_folder='../', static_url_path='')

app.config['MAX_CONTENT_LENGTH'] = 1 * 1024 * 1024  # 1MB max request size

CORS(app, resources={r"/*": {"origins": "*"}}, supports_credentials=True)

limiter = Limiter(
    app=app,
    key_func=get_remote_address,
    default_limits=["200 per day", "50 per hour"],
    storage_uri="memory://"
)

db = init_db()

@app.before_request
def disable_limiter_in_testing():
    if app.config.get("TESTING"):
        limiter.enabled = False

@app.after_request
def set_security_headers(response):
    response.headers['X-Content-Type-Options'] = 'nosniff'
    response.headers['X-Frame-Options'] = 'DENY'
    response.headers['X-XSS-Protection'] = '1; mode=block'
    response.headers['Strict-Transport-Security'] = 'max-age=31536000; includeSubDomains'
    response.headers['Content-Security-Policy'] = "default-src 'self' 'unsafe-inline' cdn.tailwindcss.com fonts.googleapis.com fonts.gstatic.com"
    return response

@app.errorhandler(404)
def not_found(e):
    return jsonify({"error": "Resource not found"}), 404

@app.errorhandler(500)
def internal_error(e):
    return jsonify({"error": "An internal server error occurred"}), 500

@app.errorhandler(429)
def rate_limit_exceeded(e):
    return jsonify({"error": "Rate limit exceeded. Please try again later."}), 429

# Register blueprints
app.register_blueprint(auth_bp)
app.register_blueprint(order_bp)
app.register_blueprint(delivery_bp)
app.register_blueprint(admin_bp)

@app.route("/")
def index():
    return jsonify({
        "status": "running",
        "database": "quick_dropDB",
        "connected": db is not None
    })

@app.route("/health")
@app.route("/api/health")
def health_check():
    return jsonify({
        "status": "healthy",
        "service": "quickdrop-backend",
        "timestamp": datetime.utcnow().isoformat()
    }), 200

@app.route("/test-db")
def test_db():
    from backend.services.db_service import MONGO_URI, mongo_client
    if not MONGO_URI or not MONGO_URI.strip():
        return jsonify({
            "success": False,
            "message": "MONGO_URI is not configured in .env. Please set MONGO_URI."
        }), 500
    try:
        mongo_client.admin.command('ping')
        return jsonify({
            "success": True,
            "message": "Successfully connected to MongoDB Atlas (quick_dropDB)."
        }), 200
    except Exception as e:
        return jsonify({
            "success": False,
            "message": "Database connection failed"
        }), 500

if __name__ == "__main__":
    port = int(os.environ.get("PORT", 5000))
    app.run(debug=False, host="0.0.0.0", port=port)
