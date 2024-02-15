package com.ermetic.loadtest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.ReadPreference;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Aggregates;

public class QueryManager {
    private MongoErmeticClient client;
    private ErmeticLoadtestOptions options;
    private static Logger logger = LoggerFactory.getLogger(QueryManager.class);;

    public QueryManager(MongoErmeticClient client, ErmeticLoadtestOptions options) {
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
        MongoCollection<Document> logs = client.getCollection("mongoDBAnalysis", "logs").withReadPreference(ReadPreference.secondaryPreferred());
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
                    // TODO: update?
                    if (command.containsKey("find") && options.includeFind) {

                        if (isBelowTargetRatio(maxQueryTargetingRatio, document)) {
                            String namespace = attr.getString("ns");
                            String targetDatabase = namespace.split("\\.")[0];
                            if (isSystemDatabase(targetDatabase)) {
                                continue;
                            }
    
                            command.remove("$readPreference");
                            command.remove("lsid");
                            command.remove("$db");
                            command.remove("$clusterTime");
//                            command.remove("hint");
                            result.add(new Document()
                                    .append("db", targetDatabase)
                                    .append("command", command));
                            /*
                            * MongoDatabase queryDatabase = client.getDatabase(targetDatabse);
                            * Document result = queryDatabase.runCommand(command);
                            */
                        }
                    }
                    if (attr.getString("type").equals("update") && options.includeUpdates) {
                        String namespace = attr.getString("ns");
                        String targetDatabase = namespace.split("\\.")[0];
                        String targetCollection = namespace.substring(namespace.indexOf(".") + 1);
                        if (isSystemDatabase(targetDatabase)) {
                            continue;
                        }
                        if (command.containsKey("u")) {
                            result.add(new Document()
                                .append("db", targetDatabase)
                                .append("command", new Document()
                                    .append("update", targetCollection)
                                    .append("updates", Arrays.asList(command))
                                    )
                            );
                        }
                    }
                    if (command.containsKey("findAndModify") && options.includeFindAndModify) {
                        command.remove("$readPreference");
                        command.remove("lsid");
                        command.remove("$db");
                        command.remove("$clusterTime");
                        command.remove("txnNumber");
                        String namespace = attr.getString("ns");
                        String targetDatabase = namespace.split("\\.")[0];
                        if (isSystemDatabase(targetDatabase)) {
                            continue;
                        }
                        result.add(new Document()
                            .append("db", targetDatabase)
                            .append("command", command)
                        );
                    }
                    if (command.containsKey("aggregate") && options.includeAggregate) {
                        if (isBelowTargetRatio(maxQueryTargetingRatio, document)) {
                            String namespace = attr.getString("ns");
                            String targetDatabase = namespace.split("\\.")[0];
                            if (isSystemDatabase(targetDatabase)) {
                                continue;
                            }
    
                            command.remove("$readPreference");
                            command.remove("lsid");
                            command.remove("$db");
                            command.remove("$clusterTime");
                            //command.remove("hint");
                            result.add(new Document()
                                .append("db", targetDatabase)
                                .append("command", command));
                        }
                    }
                }
            }
        }

        return result;
    }

    private boolean isBelowTargetRatio(int maxQueryTargetingRatio, Document document) {
        try {
            return calculateQueryTargetingRatio(document) < maxQueryTargetingRatio;
        } catch (Exception e) {
            return true;
        }
    }

    public List<ContextData> getContexts(int sample) {
        MongoCollection<Document> contexts = client.getCollection("mongoDBAnalysis", "contexts").withReadPreference(ReadPreference.secondaryPreferred());

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

    public void addContexts(ThreadPoolExecutor executor, int num) {
        MongoCollection<Document> contexts = client.getCollection("mongoDBAnalysis", "contexts").withReadPreference(ReadPreference.secondaryPreferred());

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
                    executor.submit(new ExecuteRandomQueries(client, new ContextData(d.getString("_id"), queries), options));
                    size++;
                }
                if (size >= num) {
                    break;
                }
            }
        }
    }

    public void addContext(ThreadPoolExecutor executor, String ctxId) {
        MongoCollection<Document> contexts = client.getCollection("mongoDBAnalysis", "contexts").withReadPreference(ReadPreference.secondaryPreferred());

        List<Document> queries = getQueries(ctxId);
        if (queries.size() > 0) {
            executor.submit(new ExecuteRandomQueries(client, new ContextData(ctxId, queries), options));
        }
    }
}
