package com.bol.mongo;

import com.bol.engine.RollbackableAction;
import org.springframework.data.annotation.Id;

public abstract class MongoRollbackableAction<OBJECTID> extends RollbackableAction<OBJECTID> {
    public static final String MONGO_FOR_OBJECT = "forObject";
    public static final String MONGO_TTLMS = "ttlMs";

    @Id
    public String id;

    public MongoRollbackableAction(OBJECTID forObject) {
        super(forObject);
    }

    @Override
    public String toString() {
        return getClass().getCanonicalName() + "{" +
                "id='" + id + '\'' +
                ", ttlMs=" + ttlMs +
                ", forObject=" + forObject +
                '}';
    }
}
