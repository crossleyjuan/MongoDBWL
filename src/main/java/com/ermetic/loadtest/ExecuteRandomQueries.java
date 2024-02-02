package com.ermetic.loadtest;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;

public class ExecuteRandomQueries implements Runnable {

    private static Logger logger = LoggerFactory.getLogger(ExecuteRandomQueries.class);;
    String threadName;
    int duration;
    MongoErmeticClient mongoErmeticClient;
    private String context;
    private int maxQueryTargetRatio;

    public ExecuteRandomQueries(MongoErmeticClient mongoErmeticClient, int duration, String context, int maxQueryTargetRatio) {

        this.duration = duration;
        this.mongoErmeticClient = mongoErmeticClient;
        this.context = context;
        this.maxQueryTargetRatio = maxQueryTargetRatio;

    }

    public void run() {
        // logger.info("Thread " + context + " starting");
        try {
            QueryManager manager = new QueryManager(mongoErmeticClient);
            List<Document> commands = manager.getQueries(context, maxQueryTargetRatio);

            long end = System.currentTimeMillis() + duration * 1000L;
            int iteration = 0;
            int emptyIterations = 0;
            List<Document> queryAnalysisDoc = new ArrayList<Document>();

            Iterator<Document> itCommands = commands.iterator();
            while (System.currentTimeMillis() < end) {
                if (!itCommands.hasNext()) {
                    break;
                }
                Document nextCommand = itCommands.next();
                String databaseName = nextCommand.getString("db");
                MongoDatabase database = mongoErmeticClient.getDatabase(databaseName);
                ClientSession session = mongoErmeticClient.createSession();
                Document command = nextCommand.get("command", Document.class);
                Document analysisResult = new Document();
                analysisResult.append("db", databaseName);
                analysisResult.append("i", iteration);
                if (command.containsKey("find") || command.containsKey("aggregate")) {
                    //analysisResult.append("type", "find");
                    Document result = database.runCommand(session, command);

                    analysisResult.append("query", command);
                    int queryResult = result.get("cursor", Document.class).get("firstBatch", List.class).size();
                    if (queryResult == 0) {
                        emptyIterations++;
                    }
                    Document originalResult = result;
                    while (true) {
                        Document cursor = result.get("cursor", Document.class);

                        long cursorId = cursor.getLong("id");
                        if (cursorId != 0) {
                            String namespace = cursor.getString("ns");
                            String collectionName = namespace.split("\\.")[1];
                            try {
                                result = database.runCommand(session, new Document()
                                        .append("getMore", cursorId)
                                        .append("collection", collectionName));
                                queryResult += result.get("cursor", Document.class).get("nextBatch", List.class).size();
                                iteration++;
                            } catch (Exception e ) {
                                logger.error(e.getMessage(), e);
                                break; // Breaks the loop and continues with the next
                            }
                        } else {
                            analysisResult.append("cursorExhausted", true);
                            break;
                        }
                    }
                    analysisResult.append("docs", queryResult);
                }
                if (command.containsKey("update")) {
                    analysisResult.append("type", "update");
                    try {
                        Document result = database.runCommand(session, command);
                        int matched = result.getInteger("n");
                        int modified = result.getInteger("nModified");

                        analysisResult.append("matched", matched);
                        analysisResult.append("modified", modified);
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                    }
                }

                //queryAnalysisDoc.add(analysisResult);
                iteration++;
            }
            logger.info("Context completed " + context + " iterations: " + iteration);
            /*
            if (queryAnalysisDoc.size() > 0) {
                mongoErmeticClient.getMongoClient().getDatabase("mongoDBAnalysis").getCollection("queryResult")
                        .insertMany(queryAnalysisDoc);
            } else {
                logger.info("Empty context " + context);
            }
            */

            // logger.info("Thread " + context + " completed: " + iteration + " queries, " +
            // emptyIterations + " of them are empty.");

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

}
