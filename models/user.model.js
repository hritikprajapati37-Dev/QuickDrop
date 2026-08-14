const mongoose = require("mongoose");

const userSchema = new mongoose.Schema(
    {
        full_name: {
            type: String,
            required: true,
            trim: true
        },

        email: {
            type: String,
            required: true,
            unique: true,
            trim: true
        },

        mobile_number: {
            type: String,
            required: true,
            maxlength:10,
            minlength:10
            
        },

        password: {
            type: String,
            required: true
        },

        role: {
            type: String,
            enum: ["consignor", "delivery_boy"],
            required: true
        },

        profile_image: {
            type: Buffer,
            required:true
        },

        address: {
            type: String,
            required: true
        }
    },

    {
        timestamps: {
            createdAt: "created_at",
            updatedAt: "updated_at"
        }
    }
);

const User = mongoose.model("User", userSchema);
module.exports = User; 

