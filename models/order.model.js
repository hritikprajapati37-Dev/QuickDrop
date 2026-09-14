const mongoose = require("mongoose");

const orderSchema = new mongoose.Schema(
    {
        consignor_id: {
            type: mongoose.Schema.Types.ObjectId,
            ref: "Consignor",
            required: true
        },

        consignee_id: {
            type: mongoose.Schema.Types.ObjectId,
            ref: "Consignee",
            required: true
        },

        delivery_boy_id: {
            type: mongoose.Schema.Types.ObjectId,
            ref: "Delivery_Boy_Schema",
            default: null
        },


        pickup_address: {
            type: String,
            required: true,
            trim: true
        },

        delivery_address: {
            type: String,
            required: true,
            trim: true
        },

        distance_km: {
            type: Number,
            required: true,
            min: 0,
            max:5
        },

        delivery_charge: {
            type: Number,
            required: true,
            min: 5
        },

        platform_fee: {
            type: Number,
            required: true,
            default: 5,
            min:5
        },

        total_amount: {
            type: Number,
            min:10,
            required: true
        },

        order_status: {
            type: String,
            enum: [
                "Pending",
                "Assigned",
                "Picked_Up",
                "Delivered",
                "Cancelled"
            ],
            default: "Pending",
            required: true
        }
    },

    {
        timestamps: {
            createdAt: "created_at"
            // updatedAt: "updated_at"
        }
    }
);

const Order = mongoose.model("Order", orderSchema);

module.exports = Order;

