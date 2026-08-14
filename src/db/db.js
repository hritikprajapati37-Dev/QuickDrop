const mongoose = require("mongoose");


async function db () {

    try {
        await mongoose.connect("mongodb://localhost:27017/mydb");
        console.log("MongoDB Connected");
    } catch (error) {
        console.log("MongoDB Connection Error:", error);
    }
}

module.exports = db;
