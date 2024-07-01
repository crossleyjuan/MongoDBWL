This script allows to reproduce a workload to perform load tests.

# Notes
===============

diagnosticDataCollectionVerboseTCMalloc

# Preparation steps
===============

1. Enable the recording of all the queries in the logs using:

```javascript
db.setProfilingLevel(0, { slowms: 0 })
```

This will allow to consider all the queries as "slow" and therefore they will be recorded in the logs, let this parameter for a couple of hours and then switch it back to the original value:

```javascript
db.setProfilingLevel(0, { slowms: 100 })
```

Now collect the logs for the different servers.

2. Import the log into the database, the name of the database and collection are currently hard-coded so please keep the name:

```bash
mongoimport -d mongoDBAnalysis -c logs --uri="$MONGODB_URI" atlas-1254uk-shard-00-02.xlruw.mongodb.net_2023-12-12T19_30_00_2023-12-12T22_30_00_mongodb.log
```

connect to mongo and execute the following aggregation pipeline to make available the contexts that would be used:

```javascript
db.logs.aggregate([
  {
    $group:
      {
        _id: "$ctx",
        minDate: { "$min": "$t" },
        maxDate: { "$max": "$t" }
      },
  },
  {
    $merge:
      {
        into: "contexts",
        on: "_id",
      },
  }
]);
```

Create an index in the logs collection that would avoid COLLSCANS in the support collection:

```javascript
db.getSiblingDB("mongoDBAnalysis").logs.createIndex({ "msg": 1, "ctx": 1, "t": 1 });
db.getSiblingDB("mongoDBAnalysis").logs.createIndex({ "ctx": 1, "t": 1 });
```

3. Create a run.sh file like this:
```bash
java -jar loadTest.jar -u "mongodb+srv://user:password@customer.mpxmc.mongodb.net/?retryWrites=true" -t 20 -d 60 -c 100000
```

Note: you can use the run.sh.sample as template

4. Create some VMs in AWS using t2.xlarge (I recommend at least 10) so they can have enough CPUs to run multiple threads, update the prepare.sh with the ips of the servers:

```bash
servers=( "34.242.75.105" "34.245.92.139" "34.250.46.169" "52.213.157.161" "34.253.224.149" "52.19.49.69" "34.243.176.198" "34.254.249.56" "34.249.129.197" "34.245.57.86" )

for server in ${servers[@]}; do
  echo "$server"
  rsync -avh loadTest.jar ec2-user@$server:.
  rsync -avh run.sh ec2-user@$server:.
done
for server in ${servers[@]}; do
  ssh ec2-user@$server "sudo yum -y install java"
done
```

The above script will upload the jar file to the servers and also a run.sh script that would be used to execute the tests.

5. execute the prepare.sh to upload the files and setup the java environment in the test workers.

```bash
./prepare.sh
```

6. Once the servers are ready to execute you can execute the workload like this:

```bash
servers=( "34.242.75.105" "34.245.92.139" "34.250.46.169" "52.213.157.161" "34.253.224.149" "52.19.49.69" "34.243.176.198" "34.254.249.56" "34.249.129.197" "34.245.57.86" )

for server in ${servers[@]}; do
  (echo "output from $server"; ssh ec2-user@$server "chmod +x run.sh;./run.sh"; echo End $server) &
done
wait
echo All subshells finished
```

The sample file executetests.sh can be used for this.

