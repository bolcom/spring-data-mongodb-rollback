package com.bol.store;

import com.bol.engine.ActionStore;
import com.bol.engine.RollbackableAction;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;

import java.util.List;

import static com.bol.engine.RollbackableAction.MONGO_FOR_OBJECT;
import static com.bol.engine.RollbackableAction.MONGO_TTLMS;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

public class MongoActionStore implements ActionStore<String> {

    final String collectionName;
    final MongoTemplate mongoTemplate;

    public MongoActionStore(MongoTemplate mongoTemplate, String collectionName) {
        this.collectionName = collectionName;
        this.mongoTemplate = mongoTemplate;

        mongoTemplate.indexOps(collectionName).ensureIndex(new Index(MONGO_FOR_OBJECT, Sort.Direction.ASC));
        // TODO: index on ttlMs too?
    }

    @Override
    public void put(RollbackableAction<String> action) {
        mongoTemplate.save(action, collectionName);
    }

    @Override
    public Iterable<RollbackableAction<String>> get(String id) {
        List objects = mongoTemplate.find(query(where(MONGO_FOR_OBJECT).is(id)), RollbackableAction.class, collectionName);
        return objects;
    }

    @Override
    public void clear(String id) {
        mongoTemplate.remove(query(where(MONGO_FOR_OBJECT).is(id)));
    }

    @Override
    public void cleanup(long now) {
        mongoTemplate.remove(query(where(MONGO_TTLMS).lt(now)));
    }
}
