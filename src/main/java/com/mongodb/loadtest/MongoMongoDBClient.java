package com.mongodb.loadtest;

import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MongoMongoDBClient {

    private static final Map<String, Integer> databaseDistribution = new LinkedHashMap<String, Integer> (){
        {
            put("530b2522-99a6-41b6-8048-29f935df3d77",335);
            put("4ab52a4c-d641-42ca-b2cc-f87d8b42b752",286);
            put("54a04517-15eb-4bf2-b1e0-f5c32a92fde1",261);
            put("cef32587-a65d-42a8-b4a4-8360534febbd",226);
            put("0183e767-5bee-4283-ba41-cf767e164430",124);
            put("3df4b6ec-e0ae-48b9-a74c-794354bfcf09",123);
        } 
    };

    private static final Map<String, Integer> collectionDistribution = new LinkedHashMap<String, Integer> (){
        {
            put("Entities", 1234);
            put("AwsDirectFragmentedPermissionEdges",195);
            put("PermissionEdgeFragments", 176);
            put("AzureDirectFragmentedPermissionEdges", 152);
            put("EntityStates", 95);
            put("Risks", 81);
        }
    };

    private int databasesWeight=0;
    private int collectionsWeight=0;
    private MongoClient mongoClient;

    Logger logger;
    Random rd;

    public MongoMongoDBClient (String connectionString){

        logger = LoggerFactory.getLogger(MongoMongoDBClient.class);
        try {
            // For not authentication via connection string passing of user/pass only
            mongoClient = MongoClients.create(connectionString);

            for(int weight: databaseDistribution.values()){
               databasesWeight += weight;
            }

            for(int weight: collectionDistribution.values()){
                collectionsWeight += weight;
            }

            rd = new Random();

        } catch (Exception ex) {
            StringWriter errors = new StringWriter();
            ex.printStackTrace(new PrintWriter(errors));
            logger.error(errors.toString());
        }
        logger.info("Connected to MongoDB database");

    }

    MongoClient getMongoClient (){
        return mongoClient;
    }

     public String getRandomDatabase()
    {
        int randomWeight = rd.nextInt(databasesWeight+1);
        int currentWeight = 0;
        String returnValue = "";

        for (String databaseName: databaseDistribution.keySet())
        {
            currentWeight += databaseDistribution.get(databaseName);
            if (currentWeight >= randomWeight)
            {
                returnValue = databaseName;
                break;
            }
        }
        return returnValue;
    }

    public String getRandomCollection()
    {
        int randomWeight = rd.nextInt(collectionsWeight+1);
        int currentWeight = 0;
        String returnValue = "";

        for (String collectionName: collectionDistribution.keySet())
        {
            currentWeight += collectionDistribution.get(collectionName);
            if (currentWeight >= randomWeight)
            {
                returnValue = collectionName;
                break;
            }
        }
        return returnValue;
    }

    public MongoDatabase getDatabase(String databaseName) {
        return mongoClient.getDatabase(databaseName);
    }

    public MongoCollection<Document> getCollection(String databaseName, String collectionName)
    {
        return getDatabase(databaseName).getCollection(collectionName);
    }

    public ClientSession createSession() {
        return mongoClient.startSession();
    }
}