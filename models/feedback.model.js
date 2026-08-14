const mongoose = require("mongoose");

const feedbackSchema = new mongoose.Schema(
    {
        order_id: {
            type: mongoose.Schema.Types.ObjectId,
            ref: "Order",
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

        rating: {
            type: Number,
            required: true,
            min: 1,
            max: 5
        },

        review: {
            type: String,
            trim: true
        },

        created_at: {
            type: Date,
            default: Date.now
        }
    }
);

const Feedback = mongoose.model("Feedback", feedbackSchema);

module.exports = Feedback;