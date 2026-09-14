const mongoose = require("mongoose");

const consignorSchema = new mongoose.Schema(
    {
        user_id: {
            type: mongoose.Schema.Types.ObjectId,
            ref: "User",
            required: true
        },

        status: {
            type: String,
            enum: ["Active", "Inactive"],
            default: "Inactive"
        }
    }
    
);

const consignorModel = mongoose.model("Consignor", consignorSchema);

module.exports = consignorModel;
