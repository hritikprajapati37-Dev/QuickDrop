const mongoose = require("mongoose");

const categorySchema = new mongoose.Schema(
    {
        name: {
            type: String,
            enum: ["Document", "Food", "Cloth", "Others"],
            required: true,
            trim: true
        }
    },
    {
        timestamps: {
            createdAt: "created_at",
            updatedAt: "updated_at"
        }
    }
);

const Category = mongoose.model("Category", categorySchema);

module.exports = Category;
