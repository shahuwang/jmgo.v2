package com.shahuwang.jmgo;

import org.bson.BsonBoolean;
import org.bson.BsonDocument;
import org.bson.BsonInt32;
import org.bson.BsonString;

/**
 * Created by rickey on 2017/3/25.
 */
public class QueryWrapper {
    private BsonDocument query;
    private BsonDocument orderBy;
    private BsonDocument hint; // Forces MongoDB to use a specific index
    private boolean explain; // Forces MongoDB to report on query execution plans.
    private boolean snapshot; // Guarantees that a query returns each document no more than once.
    private BsonDocument readPreference;
    private int maxScan;
    private int maxTimeMS;
    private String comment; // 会在mongo的日志里输出这个注释

    public QueryWrapper(BsonDocument query, BsonDocument orderBy, BsonDocument hint, BsonDocument readPreference,
                        boolean explain, boolean snapshot, int maxScan, int maxTimeMS, String comment){
        this.query = query;
        this.orderBy = orderBy;
        this.hint = hint;
        this.readPreference = readPreference;
        this.explain = explain;
        this.snapshot = snapshot;
        this.maxScan = maxScan;
        this.maxTimeMS = maxTimeMS;
        this.comment = comment;
    }

    public void setReadPreference(BsonDocument readPreference) {
        this.readPreference = readPreference;
    }

    public void setQuery(BsonDocument query) {
        this.query = query;
    }

    public BsonDocument getDocument(){
         BsonDocument doc = new BsonDocument("$query", this.query);
         if(this.orderBy != null){
             doc.append("$orderby", this.orderBy);
         }
         if(this.hint != null){
             doc.append("$hit", this.hint);
         }
         if(this.explain){
             doc.append("$explain", new BsonBoolean(this.explain));
         }
         if(this.snapshot){
             doc.append("$snapshot", new BsonBoolean(this.snapshot));
         }
         if(this.readPreference != null){
             doc.append("$readPreference", this.readPreference);
         }
         if(this.maxScan != 0){
             doc.append("$maxScan", new BsonInt32(this.maxScan));
         }
         if(this.maxTimeMS != 0){
             doc.append("$maxTimeMS", new BsonInt32(this.maxTimeMS));
         }
         if(this.comment != ""){
             doc.append("$comment", new BsonString(this.comment));
         }
         return doc;
    }

    public static class Builder{
        private BsonDocument query;
        private BsonDocument orderBy;
        private BsonDocument hint;
        private boolean explain;
        private boolean snapshot;
        private BsonDocument readPreference;
        private int maxScan;
        private int maxTimeMS;
        private String comment;

        public Builder query(BsonDocument query){
            this.query = query;
            return this;
        }
        public Builder orderBy(BsonDocument orderBy){
            this.orderBy = orderBy;
            return this;
        }
        public Builder hint(BsonDocument hint){
            this.hint = hint;
            return this;
        }
        public Builder explain(boolean explain){
            this.explain = explain;
            return this;
        }
        public Builder snapshot(boolean snapshot){
            this.snapshot = snapshot;
            return this;
        }
        public Builder readPreference(BsonDocument readPreference){
            this.readPreference = readPreference;
            return this;
        }
        public Builder maxScan(int maxScan){
            this.maxScan = maxScan;
            return this;
        }
        public Builder maxTimeMS(int maxTimeMS){
            this.maxTimeMS = maxTimeMS;
            return this;
        }
        public Builder comment(String comment){
            this.comment = comment;
            return this;
        }
        public QueryWrapper build(){
            return new QueryWrapper(this.query, this.orderBy, this.hint, this.readPreference,
                    this.explain, this.snapshot, this.maxScan, this.maxTimeMS, this.comment);
        }
    }
}
