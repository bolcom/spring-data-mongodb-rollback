package com.bol.engine;

public abstract class RollbackableAction<OBJECTID> {
    public static final String MONGO_FOR_OBJECT = "forObject";
    public static final String MONGO_TTLMS = "ttlMs";

    public long ttlMs;
    public final OBJECTID forObject;

    public RollbackableAction(OBJECTID forObject) {
        this.forObject = forObject;
    }

    abstract public void action();
    abstract public void rollback();
}
