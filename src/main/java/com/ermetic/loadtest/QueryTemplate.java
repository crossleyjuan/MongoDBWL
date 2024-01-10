package com.ermetic.loadtest;

import org.bson.Document;

public class QueryTemplate
{

    private String database;
    private String collection;
    private Document query;

    public QueryTemplate(String database, String collection, Document query){
        this.database = database;
        this.collection = collection;
        this.query = query;
    }

    public String getCollection(){
        return collection;
    }

    public String getDatabase(){
        return database;
    }

    public Document getQuery(){
        return query;
    }

}
