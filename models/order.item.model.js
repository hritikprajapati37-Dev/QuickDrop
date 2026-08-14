const mongoose = require("mongoose");

const itemSchema = new mongoose.Schema(
    {
        order_id: {
            type: mongoose.Schema.Types.ObjectId,
            ref: "Order",
            required: true
        },

        category_id: {
            type: mongoose.Schema.Types.ObjectId,
            ref: "Category",
            required: true
        },

        sub_category_id: {
            type: mongoose.Schema.Types.ObjectId,
            ref: "SubCategory",
            required: true
        },

        quantity: {
            type: Number,
            required: true,
            min: 1
        },

        weight: {
            type: String,
            required: true,
            enum: ["0.5kg", "1kg", "2kg", "5kg", "10kg"]
        }
        
    },

    {
        timestamps: {
            createdAt: "created_at",
            updatedAt: "updated_at"
        }
    }
);

const Item = mongoose.model("Item", itemSchema);

module.exports = Item;