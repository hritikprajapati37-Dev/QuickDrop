const mongoose = require("mongoose");

const consigneeSchema = new mongoose.Schema(
  {
    name: {
      type: String,
      required: true,
      trim: true
    },

    mobile_number: {
      type: String,
      required: true,
      maxlength: 10,
      minlength: 10,
      trim: true
    },

    address: {
      type: String,
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

const Consignee = mongoose.model("Consignee", consigneeSchema);

module.exports = Consignee;
