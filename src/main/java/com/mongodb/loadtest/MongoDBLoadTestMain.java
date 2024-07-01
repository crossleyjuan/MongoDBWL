package com.mongodb.loadtest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.LogManager;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MongoDBLoadTestMain {
    private static Logger logger = LoggerFactory.getLogger(MongoDBLoadTestMain.class);;

    public static void main(String[] args) {

        MongoDBLoadtestOptions MongoDBOps;
        LogManager.getLogManager().reset();
        MongoMongoDBClient mongoMongoDBClient = null;

        /*
        String[] args2 = new String[] {
            "-u",
            "mongodb+srv://juancrossley:JpnNqSFKnD7sokWS@MongoDB.mpxmc.mongodb.net/?retryWrites=true",
            "-t",
            "10",
            "-d",
            "5",
            "-c",
            "20"
        };
        */

        logger.info("Load Test MongoDB");
        
        try {
            MongoDBOps = new MongoDBLoadtestOptions(args);
            // Quit after displaying help message
            if (MongoDBOps.helpOnly) {
                return;
            }

            if (MongoDBOps.connectionDetails != null){
                mongoMongoDBClient = new MongoMongoDBClient(MongoDBOps.connectionDetails);
            }
            ThreadPoolExecutor executor =  (ThreadPoolExecutor) Executors.newFixedThreadPool(MongoDBOps.numThreads);

            logger.info("Starting test");
            //List<Thread> threads = new ArrayList<Thread>();
            QueryManager queryManager = new QueryManager(mongoMongoDBClient, MongoDBOps);
            if (MongoDBOps.contextFile != null) {
                Path contextFilePath = Paths.get(MongoDBOps.contextFile);
                Stack<String> tempContexts = new Stack<String>();
                int contextsRead = 0;
                while (contextsRead < MongoDBOps.contexts) {
                    if (tempContexts.size() == 0) {
                        if (Files.exists(contextFilePath, LinkOption.NOFOLLOW_LINKS)) {
                            tempContexts.addAll(Files.readAllLines(contextFilePath));
                            Collections.shuffle(tempContexts);
                        } else {
                            throw new FileNotFoundException("File " + MongoDBOps.contextFile + " not found");
                        }
                    }
                    String ctxId = tempContexts.pop();
                    if (queryManager.addContext(executor, ctxId)) {
                        contextsRead++;
                    }
                    
                }
            } else {
                queryManager.addContexts(executor, MongoDBOps.contexts);
            }

            logger.info("Executing shutdown waiting for " + MongoDBOps.duration + " minutes to complete");
            executor.shutdown();
            executor.awaitTermination(MongoDBOps.duration, TimeUnit.MINUTES);

            logger.info("Completed");
        }
        catch(Exception e){
            logger.error("Error", e);
        }
    }
}
