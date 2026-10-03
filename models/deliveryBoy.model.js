
const mongoose = require("mongoose");

const deliveryBoySchema = new mongoose.Schema(
     {
          user_id: {
               type: mongoose.Schema.Types.ObjectId,
               ref: "User",
               required: true
          },

          aadhar_number: {
               type: String,
               required: true,
               unique: true,
               trim: true
          },

          aadhar_photo: {
               type: Buffer,
               required: true
          },

          license_id: {
               type: String,
               required: true,
               unique: true,
               trim: true
          },

          license_photo: {
               type: Buffer,
               required: true
          },

          vehicle_type: {
               type: String,
               required: true,
               enum: ["Bike", "Scooter", "Car", "Auto"]
          },

          vehicle_number: {
               type: String,
               required: true,
               unique: true,
               trim: true
          },

          is_available: {
               type: Boolean,
               default: false
          },

          current_status: {
               type: String,
               enum: ["Busy", "Free"],
               default: "Free"
          },

          current_location: {
               latitude: {
                    type: Number,
                    default: null
               },
               longitude: {
                    type: Number,
                    default: null
               }
          },

          
          
     });

const deliveryBoy = mongoose.model("Delivery_Boy_Schema", deliveryBoySchema);
module.exports = deliveryBoy; 