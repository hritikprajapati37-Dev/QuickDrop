import os
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

    def find_one(self, query):
        for doc in self.documents:
            match = True
            for k, v in query.items():
                if k == "$or":
                    sub_match = any(str(doc.get(sub_k)) == str(sub_v) for cond in v for sub_k, sub_v in cond.items() if sub_k != "$exists")
                    if not sub_match:
                        match = False
                else:
                    doc_val = doc.get(k)
                    if doc_val != v and str(doc_val) != str(v):
                        match = False
                        break
            if match:
                return doc
        return None

    def find(self, query=None, projection=None):
        results = []
        query = query or {}
        for doc in self.documents:
            match = True
            for k, v in query.items():
                if k == "$or":
                    sub_match = any(str(doc.get(sub_k)) == str(sub_v) for cond in v for sub_k, sub_v in cond.items() if sub_k != "$exists")
                    if not sub_match:
                        match = False
                else:
                    doc_val = doc.get(k)
                    if doc_val != v and str(doc_val) != str(v):
                        match = False
                        break
            if match:
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

    def delete_many(self, query):
        if not query:
            count = len(self.documents)
            self.documents.clear()
            return count
        initial_len = len(self.documents)
        self.documents = [doc for doc in self.documents if not all(str(doc.get(k)) == str(v) for k, v in query.items())]
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
    if db is not None:
        return db
    try:
        uri = MONGO_URI.strip() if MONGO_URI else ""
        if not uri:
            raise ValueError("Empty MONGO_URI")
        mongo_client = MongoClient(uri, tlsCAFile=certifi.where(), serverSelectionTimeoutMS=2000)
        mongo_client.admin.command('ping')
        db_name = "quick_drop_testDB" if testing else "quick_dropDB"
        db = mongo_client[db_name]
        print(f"Successfully connected to MongoDB Atlas database: {db_name}")
        return db
    except Exception as e:
        if _mock_db_singleton is None:
            _mock_db_singleton = MockDatabase()
        db = _mock_db_singleton
        return db

def get_db():
    global db, _mock_db_singleton
    if db is not None:
        return db
    return init_db()
