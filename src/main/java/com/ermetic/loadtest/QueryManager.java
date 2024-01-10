package com.ermetic.loadtest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.management.Query;

import org.bson.Document;

import com.mongodb.client.AggregateIterable;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Aggregates;

public class QueryManager {
    private MongoErmeticClient client;

    public QueryManager(MongoErmeticClient client) {
        this.client = client;
    }

    public List<Document> getQueries(String ctx) {
        MongoCollection<Document> logs = client.getCollection("mongoDBAnalysis", "logs");
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
                    if (command.containsKey("find")) {
                        String namespace = attr.getString("ns");
                        String targetDatabase = namespace.split("\\.")[0];

                        command.remove("$readPreference");
                        command.remove("lsid");
                        command.remove("$db");
                        command.remove("$clusterTime");
                        result.add(new Document()
                                .append("db", targetDatabase)
                                .append("command", command));
                        /*
                         * MongoDatabase queryDatabase = client.getDatabase(targetDatabse);
                         * Document result = queryDatabase.runCommand(command);
                         */
                    }
                    if (attr.getString("type").equals("update")) {
                        String namespace = attr.getString("ns");
                        String targetDatabase = namespace.split("\\.")[0];
                        String targetCollection = namespace.split("\\.")[1];
                        result.add(new Document()
                            .append("db", targetDatabase)
                            .append("command", new Document()
                                .append("update", targetCollection)
                                .append("updates", Arrays.asList(command))
                                )
                        );
                    }
                }
            }
        }

        return result;
    }

    public List<String> getContexts(int sample) {
        MongoCollection<Document> contexts = client.getCollection("mongoDBAnalysis", "contexts");

        AggregateIterable<Document> aggregate = contexts.aggregate(Arrays.asList(
            Aggregates.sample(sample)
        ));
        ArrayList<String> result = new ArrayList<String>();
        aggregate.forEach((d) -> {
            result.add(d.getString("_id"));
        });
        return result;
    }
}
