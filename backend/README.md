# QuickDrop Backend (Render Deployment)

Dedicated Python Flask backend for QuickDrop connected to MongoDB Atlas.

## Render Deployment Configuration

### 1. Build Command
```bash
pip install -r requirements.txt
```

### 2. Start Command (Gunicorn)
Render uses the `$PORT` environment variable to bind the web server. Configure the start command as:
```bash
gunicorn backend.app:app
```

### 3. Required Environment Variables
Configure the following environment variables in your Render service settings:
- `MONGO_URI`: `mongodb+srv://<username>:<password>@<cluster>.mongodb.net/quick_dropDB?retryWrites=true&w=majority`
- `JWT_SECRET_KEY`: `your_secure_random_jwt_secret_key_here`

---

## Local Setup & Testing

1. Navigate to the `backend/` directory (or run from project root).
2. Install dependencies:
   ```bash
   pip install -r requirements.txt
   ```
3. Copy `.env.example` to `.env` and fill in your credentials:
   ```bash
   cp .env.example .env
   ```
4. Run the application locally:
   ```bash
   python app.py
   ```
5. Run tests:
   ```bash
   python -m unittest discover -s tests -p "test_*.py"
   ```
