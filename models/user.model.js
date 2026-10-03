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
            required: false,
            unique: true,
            trim: true
        },

        mobile_number: {
            type: String,
            required: true,
            maxlength:10,
            minlength:10
            
        },

        role: {
            type: String,
            enum: ["consignor", "delivery_boy","admin"],
            required: true
        },

        profile_image: {
            type: Buffer,
            required:true
        },

        address: {
            type: String,
            required: true
        },

        bank_account_number: {
               type: String,
               required: true,
               trim: true
          }

    },

    {
        timestamps: {
            createdAt: "created_at",
            updatedAt: false
        }
    }
);

const User = mongoose.model("User", userSchema);
module.exports = User; 

