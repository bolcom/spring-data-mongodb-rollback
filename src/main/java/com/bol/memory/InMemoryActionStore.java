package com.bol.memory;

import com.bol.engine.ActionStore;
import com.bol.engine.RollbackableAction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class InMemoryActionStore<OBJECTID, ACTION extends RollbackableAction<OBJECTID>> implements ActionStore<OBJECTID, ACTION> {

    protected ConcurrentHashMap<OBJECTID, List<ACTION>> store = new ConcurrentHashMap<>();

    @Override
    public void put(ACTION action) {
        store.compute(action.forObject, (k, v) -> {
            if (v == null) v = new ArrayList<>();
            v.add(action);
            return v;
        });
    }

    @Override
    public void remove(ACTION action) {
        List<ACTION> forId = store.get(action.forObject);
        if (forId == null) return;
        forId.remove(action);
    }

    @Override
    public Iterable<ACTION> get(OBJECTID forObject) {
        return store.getOrDefault(forObject, Collections.emptyList());
    }

    @Override
    public void clear(OBJECTID forObject) {
        store.remove(forObject);
    }

    @Override
    public void cleanup(long now) {
        store.entrySet().removeIf(entry -> {
            entry.getValue().removeIf(action -> action.ttlMs < now);
            return entry.getValue().isEmpty();
        });

    }

    @Override
    public void retry(long now, Consumer<ACTION> retryAction) {
        store.entrySet().removeIf(entry -> {
            entry.getValue().removeIf(action -> {
                if (action.ttlMs < now) return true;
                retryAction.accept(action);
                return false;
            });
            return entry.getValue().isEmpty();
        });
    }
}
