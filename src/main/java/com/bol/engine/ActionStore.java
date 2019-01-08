package com.bol.engine;

public interface ActionStore<OBJECTID> {
    void put(RollbackableAction<OBJECTID> action);
    Iterable<RollbackableAction<OBJECTID>> get(OBJECTID id);
    void clear(OBJECTID id);

    void cleanup(long now);
}
