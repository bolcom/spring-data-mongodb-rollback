package com.bol.engine;

import java.util.function.Consumer;

public interface ActionStore<OBJECTID, ACTION extends RollbackableAction<OBJECTID>> {
    void put(ACTION action);
    void remove(ACTION action);
    Iterable<ACTION> get(OBJECTID forObject);
    void clear(OBJECTID forObject);

    void cleanup(long now);
    void retry(long now, Consumer<ACTION> retryAction);
}
