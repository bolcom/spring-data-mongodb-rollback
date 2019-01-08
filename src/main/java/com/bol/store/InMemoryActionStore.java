package com.bol.store;

import com.bol.engine.ActionStore;
import com.bol.engine.RollbackableAction;

import java.util.*;

public class InMemoryActionStore<OBJECTID> implements ActionStore<OBJECTID> {

    protected HashMap<OBJECTID, List<RollbackableAction<OBJECTID>>> store = new HashMap<>();

    @Override
    public void put(RollbackableAction<OBJECTID> action) {
        store.compute(action.forObject, (k, v) -> {
            if (v == null) v = new ArrayList<>();
            v.add(action);
            return v;
        });
    }

    @Override
    public Iterable<RollbackableAction<OBJECTID>> get(OBJECTID id) {
        return store.getOrDefault(id, Collections.emptyList());
    }

    @Override
    public void clear(OBJECTID id) {
        store.remove(id);
    }

    @Override
    public void cleanup(long now) {
        store.entrySet().removeIf(entry -> {
            entry.getValue().removeIf(action -> action.ttlMs < now);
            return entry.getValue().isEmpty();
        });
    }
}
