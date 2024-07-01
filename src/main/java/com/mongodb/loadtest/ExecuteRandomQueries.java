package com.mongodb.loadtest;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.ReadPreference;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;

public class ExecuteRandomQueries implements Runnable {

    private static Logger logger = LoggerFactory.getLogger(ExecuteRandomQueries.class);;
    String threadName;
    int duration;
    MongoMongoDBClient mongoMongoDBClient;
    private ContextData context;
    private int maxQueryTargetRatio;
    private boolean skipExhaust;
    private MongoDBLoadtestOptions options;

    public ExecuteRandomQueries(MongoMongoDBClient mongoMongoDBClient, ContextData context, MongoDBLoadtestOptions options) {

        this.duration = options.duration;
        this.mongoMongoDBClient = mongoMongoDBClient;
        this.context = context;
        this.maxQueryTargetRatio = options.queryTargettingMaxRatio;
        this.skipExhaust = options.skipExhaust;
        this.options = options;
    }

    public void run() {
        // logger.info("Thread " + context + " starting");
        try {
            List<Document> commands = context.getQueries();

            long end = System.currentTimeMillis() + duration * 1000L;
            int iteration = 0;
            int emptyIterations = 0;
            List<Document> queryAnalysisDoc = new ArrayList<Document>();

            Iterator<Document> itCommands = commands.iterator();
            ClientSession session = mongoMongoDBClient.createSession();
            while (System.currentTimeMillis() < end) {
                if (!itCommands.hasNext()) {
                    break;
                }
                Document nextCommand = itCommands.next();
                String databaseName = nextCommand.getString("db");
                MongoDatabase database = mongoMongoDBClient.getDatabase(databaseName);
                Document command = nextCommand.get("command", Document.class);
                Document analysisResult = new Document();
                analysisResult.append("db", databaseName);
                analysisResult.append("i", iteration);
                if (command.containsKey("find") || command.containsKey("aggregate")) {
                    //analysisResult.append("type", "find");
                    ReadPreference readPreference = ReadPreference.primary();
                    if (command.containsKey("$readPreference")) {
                        readPreference = ReadPreference.valueOf(command.get("$readPreference", Document.class).getString("mode"));
                        command.remove("$readPreference");
                    }
                    Document result = database.runCommand(session, command, readPreference);
                    analysisResult.append("query", command);
                    int queryResult = result.get("cursor", Document.class).get("firstBatch", List.class).size();
                    if (queryResult == 0) {
                        if (false && command.containsKey("filter") && !command.get("filter", Document.class).containsKey("_id")) {
                            logger.info("empty results: " + nextCommand.toJson());
                        }
                        emptyIterations++;
                    }
                    while (true && !skipExhaust) {
                        Document cursor = result.get("cursor", Document.class);

                        long cursorId = cursor.getLong("id");
                        if (cursorId != 0) {
                            String namespace = cursor.getString("ns");
                            String collectionName = namespace.split("\\.")[1];
                            try {
                                result = database.runCommand(session, new Document()
                                        .append("getMore", cursorId)
                                        .append("collection", collectionName), readPreference);
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
                if (command.containsKey("findAndModify")) {
                    analysisResult.append("type", "findAndModify");
                    try {
                        Document result = database.runCommand(session, command);
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                    }
                } else if (command.containsKey("update")) {
                    analysisResult.append("type", "update");
                    try {
                        Document result = database.runCommand(session, command);
                        Integer matched = result.getInteger("n");
                        Integer modified = result.getInteger("nModified");

                        if (false && matched == 0) {
                            logger.info("empty results: " + nextCommand.toJson());
                        }
                        analysisResult.append("matched", matched);
                        analysisResult.append("modified", modified);
                    } catch (Exception e) {
                        logger.error("ctx: " + nextCommand.toJson() + ". Exception: " + e.getMessage(), e);
                    }
                }

                //queryAnalysisDoc.add(analysisResult);
                iteration++;
            }
            session.close();
            if (iteration > 0) {
                logger.info("Context completed " + context.getCtxId() + " iterations: " + iteration);
            }
            /*
            if (queryAnalysisDoc.size() > 0) {
                mongoMongoDBClient.getMongoClient().getDatabase("mongoDBAnalysis").getCollection("queryResult")
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
