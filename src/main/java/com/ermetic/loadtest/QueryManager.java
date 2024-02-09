package com.ermetic.loadtest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.management.Query;
import javax.print.attribute.standard.DocumentName;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.CursorType;
import com.mongodb.ReadPreference;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Aggregates;

public class QueryManager {
    private MongoErmeticClient client;
    private static Logger logger = LoggerFactory.getLogger(QueryManager.class);;

    public QueryManager(MongoErmeticClient client) {
        this.client = client;
    }

    private double calculateQueryTargetingRatio(Document doc) {
        Document attr = doc.get("attr", Document.class);
        int docsExamined = attr.getInteger("docsExamined");
        int nreturned = attr.getInteger("nreturned");

        return (double)docsExamined / (double)nreturned;
    }

    public List<Document> getQueries(String ctx, ErmeticLoadtestOptions options) {
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
                        String targetCollection = namespace.split("\\.")[1];
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
                    if (attr.getString("type").equals("command") && command.containsKey("findAndModify") && options.includeFindAndModify) {
                        command.remove("$readPreference");
                        command.remove("lsid");
                        command.remove("$db");
                        command.remove("$clusterTime");
                        command.remove("txnNumber");
                        String namespace = attr.getString("ns");
                        String targetDatabase = namespace.split("\\.")[0];
                        result.add(new Document()
                            .append("db", targetDatabase)
                            .append("command", command)
                        );
                    }
                    if (command.containsKey("aggregate") && options.includeAggregate) {
                        if (isBelowTargetRatio(maxQueryTargetingRatio, document)) {
                            String namespace = attr.getString("ns");
                            String targetDatabase = namespace.split("\\.")[0];

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

    public List<String> getContexts(int sample) {
        MongoCollection<Document> contexts = client.getCollection("mongoDBAnalysis", "contexts").withReadPreference(ReadPreference.secondaryPreferred());

        AggregateIterable<Document> aggregate = contexts.aggregate(Arrays.asList(
//            Aggregates.sample(sample)
            Aggregates.limit(sample)
        ));
        ArrayList<String> result = new ArrayList<String>();
        aggregate.forEach((d) -> {
            result.add(d.getString("_id"));
        });
        return result;
    }
}
