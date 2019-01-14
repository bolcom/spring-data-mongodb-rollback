package com.bol.mongo;

import com.bol.engine.ActionStore;
import com.bol.engine.RollbackableAction;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.util.CloseableIterator;

import java.util.List;
import java.util.function.Consumer;

import static com.bol.mongo.MongoRollbackableAction.MONGO_FOR_OBJECT;
import static com.bol.mongo.MongoRollbackableAction.MONGO_TTLMS;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

public class MongoActionStore implements ActionStore<String, MongoRollbackableAction<String>> {

    final String collectionName;
    final MongoTemplate mongoTemplate;

    public MongoActionStore(MongoTemplate mongoTemplate, String collectionName) {
        this.collectionName = collectionName;
        this.mongoTemplate = mongoTemplate;

        mongoTemplate.indexOps(collectionName).ensureIndex(new Index(MONGO_FOR_OBJECT, Sort.Direction.ASC));
        // TODO: index on ttlMs too?
    }

    @Override
    public void put(MongoRollbackableAction<String> action) {
        mongoTemplate.save(action, collectionName);
    }

    @Override
    public void remove(MongoRollbackableAction<String> action) {
        mongoTemplate.remove(action.id);
    }

    @Override
    public Iterable<MongoRollbackableAction<String>> get(String forObject) {
        List objects = mongoTemplate.find(query(where(MONGO_FOR_OBJECT).is(forObject)), RollbackableAction.class, collectionName);
        return objects;
    }

    @Override
    public void clear(String forObject) {
        mongoTemplate.remove(query(where(MONGO_FOR_OBJECT).is(forObject)));
    }

    @Override
    public void cleanup(long now) {
        mongoTemplate.remove(query(where(MONGO_TTLMS).lt(now)));
    }

    @Override
    public void retry(long now, Consumer<MongoRollbackableAction<String>> retryAction) {
        try (CloseableIterator<MongoRollbackableAction> stream = mongoTemplate.stream(new Query(), MongoRollbackableAction.class, collectionName)) {
            while (stream.hasNext()) {
                MongoRollbackableAction<String> next = stream.next();
                if (next.ttlMs >= now) retryAction.accept(next);
            }
        }
    }
}
