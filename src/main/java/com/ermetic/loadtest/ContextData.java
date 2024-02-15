package com.ermetic.loadtest;

import java.util.List;

import org.bson.Document;

public class ContextData {

    private String ctxId;
    private List<Document> queries;

    public String getCtxId() {
        return ctxId;
    }

    public List<Document> getQueries() {
        return queries;
    }

    public ContextData(String ctxId, List<Document> queries) {
        this.ctxId = ctxId;
        this.queries = queries;
    }

}
