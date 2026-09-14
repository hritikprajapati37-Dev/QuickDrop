const mongoose = require("mongoose");

const paymentSchema = new mongoose.Schema(
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

        delivery_boy_id: {
            type: mongoose.Schema.Types.ObjectId,
            ref: "Delivery_Boy_Schema",
            required: true
        },

        total_amount: {
            type: Number,
            required: true,
            min: 0
        },

        payment_status: {
            type: String,
            enum: ["Pending", "Paid", "Failed", "Refunded"],
            default: "Pending",
            required: true
        },

        transaction_id: {
            type: String,
            required: true,
            unique: true,
            trim: true
        },
    },



    {

          timestamps: {
               createdAt: "created_at",
               updatedAt: "updated_at"
          }
     });


const Payment = mongoose.model("Payment", paymentSchema);

module.exports = Payment;
