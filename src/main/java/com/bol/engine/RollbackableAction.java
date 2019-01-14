package com.bol.engine;

public abstract class RollbackableAction<OBJECTID> {

    public long ttlMs;
    public final OBJECTID forObject;

    public RollbackableAction(OBJECTID forObject) {
        this.forObject = forObject;
    }

    abstract public void action();

    abstract public void rollback();

    @Override
    public String toString() {
        return getClass().getCanonicalName() + "{" +
                "ttlMs=" + ttlMs +
                ", forObject=" + forObject +
                '}';
    }
}
