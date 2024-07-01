package com.mongodb.loadtest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.ReadPreference;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Aggregates;

public class QueryManager {
    private MongoMongoDBClient client;
    private MongoDBLoadtestOptions options;
    private static Logger logger = LoggerFactory.getLogger(QueryManager.class);;

    public QueryManager(MongoMongoDBClient client, MongoDBLoadtestOptions options) {
        this.client = client;
        this.options = options;
    }

    private double calculateQueryTargetingRatio(Document doc) {
        Document attr = doc.get("attr", Document.class);
        int docsExamined = attr.getInteger("docsExamined");
        int nreturned = attr.getInteger("nreturned");

        return (double)docsExamined / (double)nreturned;
    }

    private boolean isSystemDatabase(String db) {
        return db.equals("config") || db.equals("local");
    }

    public List<Document> getQueries(String ctx) {
        int maxQueryTargetingRatio = options.queryTargettingMaxRatio;
        String logsCollection = "logs";
        String mergedDatabase = null;

        if (options.dbMerge != null) {
            logsCollection = "transformedLog";
            mergedDatabase = options.dbMerge;
        }
        MongoCollection<Document> logs = client.getCollection("mongoDBAnalysis", logsCollection).withReadPreference(ReadPreference.secondaryPreferred());
        FindIterable<Document> findIterable = logs.find(new Document()
                .append("ctx", ctx))
                .sort(new Document()
                        .append("t", 1));

        /*
         * FindIterable<Document> findIterable = logs.find(new Document()
         * .append("ctx", "conn26571042"))
         * .limit(1);
         */
        List<Document> result = new ArrayList<Document>();
        for (Document document : findIterable) {
            if (document.containsKey("attr")) {
                Document attr = document.get("attr", Document.class);
                if (attr.containsKey("command")) {
                    Document command = attr.get("command", Document.class);
                    String namespace = attr.getString("ns");
                    String originalDatabase = namespace.split("\\.")[0];
                    String targetDatabase = originalDatabase;
                    String targetCollection = namespace.split("\\.")[1];
                    if (isSystemDatabase(targetDatabase)) {
                        continue;
                    }
                    if (mergedDatabase != null) {
                        targetDatabase = mergedDatabase;
                        attr.append("ns", mergedDatabase + "." + targetCollection);
                    }
                    Document queryDocument = null;
                    // TODO: update?
                    if (command.containsKey("find") && options.includeFind) {

                        if (isBelowTargetRatio(maxQueryTargetingRatio, document)) {
                            command.remove("$readPreference");
                            command.remove("lsid");
                            command.remove("$db");
                            command.remove("$clusterTime");
                            //fixHint(command);
                            if (!options.hints) {
                                command.remove("hint");
                            }
                            queryDocument = new Document()
                                    .append("db", targetDatabase)
                                    .append("originalDB", originalDatabase)
                                    .append("command", command);
                            result.add(queryDocument);
                            /*
                            * MongoDatabase queryDatabase = client.getDatabase(targetDatabse);
                            * Document result = queryDatabase.runCommand(command);
                            */
                        }
                    }
                    if (attr.getString("type").equals("update") && options.includeUpdates) {
                        if (command.containsKey("u")) {
                            if (command.containsKey("filter")) {
                                command.remove("filter");
                            }
                            queryDocument = new Document()
                                .append("db", targetDatabase)
                                .append("originalDB", originalDatabase)
                                .append("command", new Document()
                                    .append("update", targetCollection)
                                    .append("updates", Arrays.asList(command))
                                    );
                            result.add(queryDocument);
                        }
                    }
                    if (command.containsKey("findAndModify") && options.includeFindAndModify) {
                        command.remove("$readPreference");
                        command.remove("lsid");
                        command.remove("$db");
                        command.remove("$clusterTime");
                        command.remove("txnNumber");
                        if (!options.hints) {
                            command.remove("hint");
                        }
                        queryDocument = new Document()
                            .append("db", targetDatabase)
                            .append("originalDB", originalDatabase)
                            .append("command", command);
                        result.add(queryDocument);
                    }
                    if (command.containsKey("aggregate") && options.includeAggregate) {
                        if (isBelowTargetRatio(maxQueryTargetingRatio, document)) {
                            command.remove("$readPreference");
                            command.remove("lsid");
                            command.remove("$db");
                            command.remove("$clusterTime");
                            if (!options.hints) {
                                command.remove("hint");
                            }
                            queryDocument = new Document()
                                .append("db", targetDatabase)
                                .append("originalDB", originalDatabase)
                                .append("command", command);
                            result.add(queryDocument);
                        }
                    }

                    if (queryDocument != null) {
                        queryDocument.append("ctx", ctx);
                        queryDocument.append("_id", document.get("_id", ObjectId.class));
                    }
                }
            }
        }

        return result;
    }

    private void fixHint(Document command) {
        if (command.containsKey("find")) {
            if (command.containsKey("hint")) {
                Document hint = command.get("hint", Document.class);
                if (hint.containsKey("_id") && command.get("filter", Document.class).size() == 1
                   && command.get("filter", Document.class).containsKey("customerId")) {
                    command.append("hint", new Document().append("customerId", 1).append("_id", 1 ));
                }
            }
        }
    }

    private boolean isBelowTargetRatio(int maxQueryTargetingRatio, Document document) {
        try {
            return calculateQueryTargetingRatio(document) < maxQueryTargetingRatio;
        } catch (Exception e) {
            return true;
        }
    }

    public List<ContextData> getContexts(int sample) throws IOException {
        String contextCollection = "contexts";
        if (options.dbMerge != null) {
            contextCollection = "contextsmerged";
        }
        MongoCollection<Document> contexts = client.getCollection("mongoDBAnalysis", contextCollection).withReadPreference(ReadPreference.secondaryPreferred());

        logger.info("Preparing contexts: " + sample);
        ArrayList<ContextData> result = new ArrayList<ContextData>();
        while (result.size() < sample) {
            AggregateIterable<Document> aggregate = contexts.aggregate(Arrays.asList(
//                Aggregates.match(new Document().append("_id", Pattern.compile("^conn"))),
    //            Aggregates.sample(sample)
                Aggregates.limit(sample)
            ));
            
            for (Document d : aggregate) {
                List<Document> queries = getQueries(d.getString("_id"));
                if (queries.size() > 0) {
                    appendContextToFile(d.getString("_id"));
                    result.add(new ContextData(d.getString("_id"), queries));
                }
                if (result.size() >= sample) {
                    break;
                }
            }
            logger.info("Prepared " + Math.abs(result.size() * 100 / sample) + "%");
        }
        return result;
    }

    private void appendContextToFile(String ctxId) throws IOException {
        Files.writeString(Paths.get("lastContexts.txt"),  ctxId + "\n", StandardOpenOption.APPEND, StandardOpenOption.CREATE);
    }

    public void addContexts(ThreadPoolExecutor executor, int num) throws IOException {
        String contextCollection = "contexts";
        if (options.dbMerge != null) {
            contextCollection = "contextsmerged";
        }
        MongoCollection<Document> contexts = client.getCollection("mongoDBAnalysis", contextCollection).withReadPreference(ReadPreference.secondaryPreferred());

        int size = 0;
        while (size < num) {
            AggregateIterable<Document> aggregate = contexts.aggregate(Arrays.asList(
//                Aggregates.match(new Document().append("_id", Pattern.compile("^conn"))),
    //            Aggregates.sample(sample)
                Aggregates.limit(num)
            ));
            
            for (Document d : aggregate) {
                String ctxId = d.getString("_id");
                List<Document> queries = getQueries(ctxId);
                if (queries.size() > 0) {
                    appendContextToFile(d.getString("_id"));
                    executor.submit(new ExecuteRandomQueries(client, new ContextData(d.getString("_id"), queries), options));
                    size++;
                }
                if (size >= num) {
                    break;
                }
            }
        }
    }

    public boolean addContext(ThreadPoolExecutor executor, String ctxId) throws IOException {
        List<Document> queries = getQueries(ctxId);
        if (queries.size() > 0) {
            if (options.contextFile == null) {
                appendContextToFile(ctxId);
            }
            executor.submit(new ExecuteRandomQueries(client, new ContextData(ctxId, queries), options));
            return true;
        } else {
            return false;
        }
    }
}
