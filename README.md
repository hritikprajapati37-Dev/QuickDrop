# QuickDrop

QuickDrop is a modern delivery and logistics platform comprising an Android client application and a Python Flask secure backend connected to MongoDB Atlas.

## Prerequisites
- Python 3.9+
- Android Studio / Android SDK (Min SDK 24, Target SDK 36)
- MongoDB Atlas cluster (`quick_dropDB`)

## Setup & Configuration

### 1. Backend Setup (Flask)
1. Navigate to the project root directory.
2. Install dependencies:
   ```bash
   pip install -r requirements.txt
   ```
3. Copy `.env.example` to `.env`:
   ```bash
   cp .env.example .env
   ```
4. Fill in your MongoDB Atlas `MONGO_URI` and a secure `JWT_SECRET_KEY` in `.env`.
5. Run the Flask server:
   ```bash
   python app.py
   ```

### 2. Android App Setup
1. Open the project in Android Studio.
2. Sync project with Gradle files.
3. Run the app on an emulator or physical device.

## Security
For detailed information on security controls, encryption, RBAC, and data protection, please refer to [SECURITY.md](file:///C:/Users/ItsBo/AndroidStudioProjects/Quick_Drop/SECURITY.md).
