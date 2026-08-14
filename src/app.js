
const express = require("express");
const app = express();

app.set("view engine", "ejs");

app.use(express.json());
app.use(express.urlencoded({ extended: true }));

const userModel = require("../models/user.model");
const consinerModel = require("../models/consiner.model");
const delivery_boy_model = require("../models/deliveryBoy.model");

const multer = require("multer");

const storage = multer.memoryStorage();
const upload = multer({ storage: storage });


app.get("/", (req, res) => {
    res.send("Hello");
});


app.get("/signUp", (req, res) => {
    res.render("signUp");
});


app.post("/signUp",upload.fields([{ name: "profile_image", maxCount: 1 },{ name: "aadhar_photo", maxCount: 1 },{ name: "license_photo", maxCount: 1 }]), async (req, res) => {
            // User fields
            const {full_name,email,mobile_number,password,role,address} = req.body;


            let existUser = await userModel.findOne({email:email}); 
            if(existUser){
                return res.send("Something went wrong....");
            }

            // 1. Create User
            const user = await userModel.create({
                full_name,
                email,
                mobile_number,
                password,
                role,
                profile_image: req.files.profile_image[0].buffer,
                address
            });






            if (role === "consiner") {

                const consiner = await consinerModel.create({
                    user_id: user._id
                });

                return res.send({message: "Consiner created successfully",user});
            }


            
            else if (role === "delivery_boy") {

                const {aadhar_number,license_id,vehicle_type,vehicle_number,bank_account_number} = req.body;


                const deliveryBoy = await delivery_boy_model.create({
                    user_id: user._id,
                    aadhar_number,
                    aadhar_photo: req.files.aadhar_photo[0].buffer,
                    license_id,
                    license_photo: req.files.license_photo[0].buffer,
                    vehicle_type,
                    vehicle_number,
                    is_available: false,
                    current_status: "Free",
                    current_location: {
                        latitude: null,
                        longitude: null
                    },
                    bank_account_number
                });


                return res.send({
                    message: "Delivery Boy created successfully",user});
            }


            // Invalid role
            return res.status(400).send({
                message: "Invalid role"
            });

        } 
    
);

app.get("/signIn",(req,res)=>{
    res.render("signIn");
});



app.get("/order/create", (req, res) => {
    res.render("consignee_or_order_create");
});




module.exports = app;
