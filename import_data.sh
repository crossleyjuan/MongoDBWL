uri="mongodb+srv://juancrossley:JpnNqSFKnD7sokWS@ermetic.mpxmc.mongodb.net/?retryWrites=true"
/opt/mongodb-database-tools-macos-x86_64-100.6.0/bin/mongoimport --uri "$uri" -d mongoDBAnalysis -c logs $1 

mongosh "$uri" update_log.js
