package com.ermetic.loadtest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.LogManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ErmeticLoadTestMain {
    private static Logger logger = LoggerFactory.getLogger(ErmeticLoadTestMain.class);;

    public static void main(String[] args) {

        ErmeticLoadtestOptions ermeticOps;
        LogManager.getLogManager().reset();
        MongoErmeticClient mongoErmeticClient = null;

        /*
        String[] args2 = new String[] {
            "-u",
            "mongodb+srv://juancrossley:JpnNqSFKnD7sokWS@ermetic.mpxmc.mongodb.net/?retryWrites=true",
            "-t",
            "10",
            "-d",
            "5",
            "-c",
            "20"
        };
        */

        logger.info("Load Test Ermetic");
        
        try {
            ermeticOps = new ErmeticLoadtestOptions(args);
            // Quit after displaying help message
            if (ermeticOps.helpOnly) {
                return;
            }

            if (ermeticOps.connectionDetails != null){
                mongoErmeticClient = new MongoErmeticClient(ermeticOps.connectionDetails);
            }
            List<String> contexts;
            if (ermeticOps.contextId != null) {
                contexts = Arrays.asList(ermeticOps.contextId);
            } else {
                QueryManager queryManager = new QueryManager(mongoErmeticClient);
                contexts = queryManager.getContexts(ermeticOps.contexts);
            }

            //List<Thread> threads = new ArrayList<Thread>();
            if (mongoErmeticClient != null) {
                ThreadPoolExecutor executor =  (ThreadPoolExecutor) Executors.newFixedThreadPool(ermeticOps.numThreads);
                for (String ctx : contexts) {
                    executor.submit(new ExecuteRandomQueries(mongoErmeticClient, ermeticOps.duration, ctx));
                }

                logger.info("Executing shutdown waiting for " + ermeticOps.duration + " minutes to complete");
                executor.shutdown();
                executor.awaitTermination(ermeticOps.duration, TimeUnit.MINUTES);
            } 
            logger.info("Completed");
        }
        catch(Exception e){
            logger.error("Error", e);
        }
    }
}
