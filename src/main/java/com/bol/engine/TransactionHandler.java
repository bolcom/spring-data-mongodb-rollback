package com.bol.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Optimistic distributed engine for mongodb
 * <p>
 * Problem:
 * 1. users trying to claim same coupon ('double claim')
 * 2. flow save failed, need to roll back external resources
 * 3. 'pull the plug', we should be able to roll back (overlaps with 2.)
 * <p>
 * Solution:
 * 1. try insert unique ID in 'lock' collection
 * 1.1. if exists, use optimistic @Retry (this should not happen often)
 * 1.2. if not exist, claim (=insert lock)
 * 2. run flow
 * 3. if successful, for each resource, try to run action; if not, rollback
 * 3.2 if failure while running action, rollback executed ones and delete the rest
 * <p>
 * <p>
 * 4. schedule cleanup job that remove stale locks
 */
public class TransactionHandler<OBJECTID> {
    private static final Logger LOG = LoggerFactory.getLogger(TransactionHandler.class);
    private static final long DEFAULT_TTLMS = 300_000;

    final ActionStore<OBJECTID> preCommitStore;
    final ActionStore<OBJECTID> failedStore;

    public TransactionHandler(ActionStore<OBJECTID> preCommitStore, ActionStore<OBJECTID> failedStore) {
        this.preCommitStore = preCommitStore;
        this.failedStore = failedStore;
    }

    // FIXME: add a registerAndRunAsync() method for direct run
    public void register(RollbackableAction<OBJECTID> action) {
        preCommitStore.put(action);
    }

    public boolean runActions(OBJECTID forObject) {
        int executed = 0;
        boolean done = false;

        Iterable<RollbackableAction<OBJECTID>> actions = preCommitStore.get(forObject);

        try {
            for (RollbackableAction<OBJECTID> action : actions) {
                action.action();
                executed++;
            }
            done = true;
        } finally {
            if (!done) {
                // TODO: strategies to react to error;
                // - instant rollback
                // - keep for retry flow
                // - schedule for later rollback
                // - ...
                rollback(forObject, actions, executed);
            }
        }

        return done;
    }

    public void commit(OBJECTID forObject) {
        preCommitStore.clear(forObject);
    }

    public void rollback(OBJECTID forObject) {
        Iterable<RollbackableAction<OBJECTID>> actions = preCommitStore.get(forObject);
        rollback(forObject, actions, Integer.MAX_VALUE);
    }

    void rollback(OBJECTID forObject, Iterable<RollbackableAction<OBJECTID>> actions, int executed) {
        try {
            for (RollbackableAction<OBJECTID> action : actions) {
                try {
                    action.rollback();
                } catch (Exception e) {
                    action.ttlMs = System.currentTimeMillis() + DEFAULT_TTLMS;
                    failedStore.put(action);
                }

                if (--executed == 0) break;
            }
        } finally {
            preCommitStore.clear(forObject);
        }
    }

    public void cleanup(long now) {
        preCommitStore.cleanup(now);
        failedStore.cleanup(now);
    }
}

