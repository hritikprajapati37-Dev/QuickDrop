const app = require("./src/app");
const db = require("./src/db/db");

db();


app.listen(3000,(req,res)=>{
    console.log("Done");
});

