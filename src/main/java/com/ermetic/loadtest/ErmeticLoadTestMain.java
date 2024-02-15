package com.ermetic.loadtest;

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
            ThreadPoolExecutor executor =  (ThreadPoolExecutor) Executors.newFixedThreadPool(ermeticOps.numThreads);

            List<ContextData> contexts = new ArrayList<ContextData>();

            logger.info("Starting test");
            //List<Thread> threads = new ArrayList<Thread>();
            QueryManager queryManager = new QueryManager(mongoErmeticClient, ermeticOps);
            if (ermeticOps.contextFile != null) {
                Path contextFilePath = Paths.get(ermeticOps.contextFile);
                Stack<String> tempContexts = new Stack<String>();
                while (contexts.size() < ermeticOps.contexts) {
                    if (tempContexts.size() == 0) {
                        if (Files.exists(contextFilePath, LinkOption.NOFOLLOW_LINKS)) {
                            tempContexts.addAll(Files.readAllLines(contextFilePath));
                            Collections.shuffle(tempContexts);
                        } else {
                            throw new FileNotFoundException("File " + ermeticOps.contextFile + " not found");
                        }
                    }
                    String ctxId = tempContexts.pop();
                    queryManager.addContext(executor, ctxId);
                }
            } else {
                queryManager.addContexts(executor, ermeticOps.contexts);
            }

            logger.info("Executing shutdown waiting for " + ermeticOps.duration + " minutes to complete");
            executor.shutdown();
            executor.awaitTermination(ermeticOps.duration, TimeUnit.MINUTES);

            logger.info("Completed");
        }
        catch(Exception e){
            logger.error("Error", e);
        }
    }
}
