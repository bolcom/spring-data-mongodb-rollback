package com.bol.store;

import com.bol.engine.ActionStore;
import com.bol.engine.RollbackableAction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class InMemoryActionStore<OBJECTID> implements ActionStore<OBJECTID, RollbackableAction<OBJECTID>> {

    protected ConcurrentHashMap<OBJECTID, List<RollbackableAction<OBJECTID>>> store = new ConcurrentHashMap<>();

    @Override
    public void put(RollbackableAction<OBJECTID> action) {
        store.compute(action.forObject, (k, v) -> {
            if (v == null) v = new ArrayList<>();
            v.add(action);
            return v;
        });
    }

    @Override
    public void remove(RollbackableAction<OBJECTID> action) {
        List<RollbackableAction<OBJECTID>> forId = store.get(action.forObject);
        if (forId == null) return;
        forId.remove(action);
    }

    @Override
    public Iterable<RollbackableAction<OBJECTID>> get(OBJECTID forObject) {
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
    public void retry(long now, Consumer<RollbackableAction<OBJECTID>> retryAction) {
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
