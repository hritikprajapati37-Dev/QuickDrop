const mongoose = require("mongoose");

const orderHistorySchema = new mongoose.Schema(
    {
        order_id: {
            type: mongoose.Schema.Types.ObjectId,
            ref: "Order",
            required: true
        },

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
            required: true
        },

        status: {
            type: String,
            required: true,
            trim: true
        },

        time: {
            type: Date,
            default: Date.now,
            required: true
        }
    }
);

const OrderHistory = mongoose.model("OrderHistory", orderHistorySchema);

module.exports = OrderHistory;