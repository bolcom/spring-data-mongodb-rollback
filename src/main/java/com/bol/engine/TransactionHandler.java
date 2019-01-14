package com.bol.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.function.Consumer;

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
public class TransactionHandler<OBJECTID, ACTION extends RollbackableAction<OBJECTID>> {
    private static final Logger LOG = LoggerFactory.getLogger(TransactionHandler.class);

    final ActionStore<OBJECTID, ACTION> preCommitStore;
    final ActionStore<OBJECTID, ACTION> failedRollbackStore;

    Consumer<RollbackableAction<OBJECTID>> rollbackLogger = action -> LOG.warn("Rollback failed for " + action);
    long rollbackTtlMs = 300_000;

    public TransactionHandler(ActionStore<OBJECTID, ACTION> preCommitStore, ActionStore<OBJECTID, ACTION> failedRollbackStore) {
        this.preCommitStore = preCommitStore;
        this.failedRollbackStore = failedRollbackStore;
    }

    public TransactionHandler<OBJECTID, ACTION> withRollbackLogger(Consumer<RollbackableAction<OBJECTID>> rollbackLogger) {
        this.rollbackLogger = rollbackLogger;
        return this;
    }

    public TransactionHandler<OBJECTID, ACTION> withRollbackTtlMs(long rollbackTtlMs) {
        this.rollbackTtlMs = rollbackTtlMs;
        return this;
    }

    // FIXME: add a registerAndRunAsync() method for direct run
    public void register(ACTION action) {
        preCommitStore.put(action);
    }

    public boolean runActions(OBJECTID forObject) {
        int executed = 0;
        boolean done = false;

        Iterable<ACTION> actions = preCommitStore.get(forObject);

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
        Iterable<ACTION> actions = preCommitStore.get(forObject);
        rollback(forObject, actions, Integer.MAX_VALUE);
    }

    private void rollback(OBJECTID forObject, Iterable<ACTION> actions, int executed) {
        try {
            for (ACTION action : actions) {
                try {
                    action.rollback();
                } catch (Exception e) {
                    action.ttlMs = System.currentTimeMillis() + rollbackTtlMs;
                    failedRollbackStore.put(action);
                    rollbackLogger.accept(action);
                }

                if (--executed == 0) break;
            }
        } finally {
            preCommitStore.clear(forObject);
        }
    }

    // FIXME: make delay+rate configurable
    @Scheduled(initialDelay = 15000, fixedDelay = 15000)
    public void retry() {
        failedRollbackStore.retry(System.currentTimeMillis(), failedAction -> {
            try {
                failedAction.rollback();
                failedRollbackStore.remove(failedAction);
            } catch (Exception e) {
                rollbackLogger.accept(failedAction);
            }
        });
    }

    // FIXME: call cleanup
    public void cleanup(long now) {
        preCommitStore.cleanup(now);
        failedRollbackStore.cleanup(now);
    }
}

