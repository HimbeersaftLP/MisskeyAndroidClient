const express = require('express');
const app = express();
const Concat = require('concat-stream');
const fs = require('fs');
const webPushDecipher = require('./webPushDecipher.js');

const AUTH_SECRET = fs.readFileSync('./key/auth_secret.txt');
const PUBLICK_KEY = fs.readFileSync('./key/public_key.txt', 'utf8');
const PRIVATE_KEY = fs.readFileSync('./key/private_key.txt', 'utf8');

console.log('start server');


const rawBodyMiddlware = (req, _, next) => {
    req.pipe(new Concat(function(data) {
        req.rawBody = data;
        next();
    }))
}

const decodeBodyMiddleware = (req, res, next) => {
    let rawBody = req.rawBody;
    if (!rawBody) { 
        return res.status(200).send('Invalid Body.').end(); 
    }
    const converted = rawBody.toString('base64');
    const key = webPushDecipher.buildReciverKey(PUBLICK_KEY, PRIVATE_KEY, AUTH_SECRET);
    let decrypted = webPushDecipher.decrypt(converted, key, false);
    req.rawJson = decrypted;
    next();
}

app.post('/webpushcallback', rawBodyMiddlware, decodeBodyMiddleware ,(req, res)=>{
    console.log();
    let deviceToken = req.query.deviceToken
    let accountId = req.query.accountId;
    let rawJson = req.rawJson;
    console.log(JSON.parse(rawJson));
    if(!(deviceToken && accountId)) {
        return cres.status(422).end();
    }
    console.log(`deviceToken:${deviceToken}, accountId:${accountId}`);
    res.json({status: 'ok'});
});
app.listen(3000);