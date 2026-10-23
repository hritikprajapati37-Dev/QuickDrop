import os
import sys
from pymongo import MongoClient
import certifi
from dotenv import load_dotenv

load_dotenv()

MONGO_URI = os.getenv("MONGO_URI")

db = None
mongo_client = None
_mock_db_singleton = None

class MockCursor(list):
    def sort(self, *args, **kwargs):
        return self

class MockCollection:
    def __init__(self):
        self.documents = []

    def _matches_query(self, doc, query):
        if not query:
            return True
        for k, v in query.items():
            if k == "$or":
                sub_match = any(self._matches_query(doc, cond) for cond in v)
                if not sub_match:
                    return False
            else:
                doc_val = doc.get(k)
                if isinstance(v, dict):
                    if "$ne" in v:
                        if str(doc_val) == str(v["$ne"]):
                            return False
                    if "$lte" in v:
                        if doc_val is None or str(doc_val) > str(v["$lte"]):
                            return False
                    if "$gte" in v:
                        if doc_val is None or str(doc_val) < str(v["$gte"]):
                            return False
                    if "$in" in v:
                        if doc_val not in v["$in"] and str(doc_val) not in [str(x) for x in v["$in"]]:
                            return False
                else:
                    if doc_val != v and str(doc_val) != str(v):
                        return False
        return True

    def find_one(self, query):
        for doc in self.documents:
            if self._matches_query(doc, query):
                return doc
        return None

    def find(self, query=None, projection=None):
        results = []
        query = query or {}
        for doc in self.documents:
            if self._matches_query(doc, query):
                results.append(doc)
        return MockCursor(results)

    def insert_one(self, doc):
        if "_id" not in doc:
            from bson import ObjectId
            doc["_id"] = ObjectId()
        else:
            from bson import ObjectId
            if not isinstance(doc["_id"], ObjectId):
                try:
                    doc["_id"] = ObjectId(doc["_id"])
                except Exception:
                    pass
        self.documents.append(doc)
        class Res:
            def __init__(self, inserted_id):
                self.inserted_id = inserted_id
        return Res(doc["_id"])

    def update_one(self, query, update, upsert=False):
        doc = self.find_one(query)
        modified = 0
        if doc:
            if "$set" in update:
                for k, v in update["$set"].items():
                    doc[k] = v
            if "$inc" in update:
                for k, v in update["$inc"].items():
                    doc[k] = doc.get(k, 0) + v
            modified = 1
        elif upsert:
            new_doc = {}
            for k, v in query.items():
                if not isinstance(v, dict):
                    new_doc[k] = v
            if "$set" in update:
                for k, v in update["$set"].items():
                    new_doc[k] = v
            self.documents.append(new_doc)
            modified = 1
        class Res:
            def __init__(self, modified_count):
                self.modified_count = modified_count
        return Res(modified)

    def update_many(self, query, update):
        matched = 0
        for doc in self.documents:
            if self._matches_query(doc, query):
                if "$set" in update:
                    for k, v in update["$set"].items():
                        doc[k] = v
                matched += 1
        class Res:
            def __init__(self, modified_count):
                self.modified_count = modified_count
        return Res(matched)

    def delete_many(self, query):
        if not query:
            count = len(self.documents)
            self.documents.clear()
            return count
        initial_len = len(self.documents)
        self.documents = [doc for doc in self.documents if not self._matches_query(doc, query)]
        return initial_len - len(self.documents)

class MockDatabase(dict):
    def __init__(self):
        super().__init__()
        self.collections = {}

    def __getitem__(self, name):
        if name not in self.collections:
            self.collections[name] = MockCollection()
        return self.collections[name]

def init_db(testing=False):
    global db, mongo_client, _mock_db_singleton
    if db is not None and not testing:
        return db

    is_running_tests = 'unittest' in sys.modules or testing or os.environ.get("FLASK_ENV") == "testing"

    if is_running_tests:
        if _mock_db_singleton is None:
            _mock_db_singleton = MockDatabase()
        db = _mock_db_singleton
        return db

    try:
        uri = MONGO_URI.strip() if MONGO_URI else ""
        if not uri:
            raise ValueError("MONGO_URI environment variable is not set.")

        mongo_client = MongoClient(uri, tlsCAFile=certifi.where(), serverSelectionTimeoutMS=5000)
        mongo_client.admin.command('ping')
        db = mongo_client["quick_dropDB"]
        print("Successfully connected to MongoDB Atlas database: quick_dropDB")
        return db
    except Exception as e:
        print(f"CRITICAL: Failed to connect to MongoDB Atlas production database. Error: {type(e).__name__}")
        raise e

def get_db():
    global db, _mock_db_singleton
    if db is not None:
        return db
    return init_db()
