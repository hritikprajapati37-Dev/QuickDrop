const mongoose = require("mongoose");

const subCategorySchema = new mongoose.Schema(
    {
        
        category_id: {
            type: mongoose.Schema.Types.ObjectId,
            ref: "Category",
            required: true 
        },
        
        name: {
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

const SubCategory = mongoose.model("SubCategory", subCategorySchema);

module.exports = SubCategory;
