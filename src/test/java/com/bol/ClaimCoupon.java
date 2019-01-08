package com.bol;

import com.bol.engine.RollbackableAction;

public class ClaimCoupon extends RollbackableAction<String> {

    final String couponId;
    final boolean requested_availability;

    public ClaimCoupon(String forObject, String couponId, boolean requested_availability) {
        super(forObject);
        this.couponId = couponId;
        this.requested_availability = requested_availability;
    }

    @Override
    public void action() {

    }

    @Override
    public void rollback() {

    }
}
