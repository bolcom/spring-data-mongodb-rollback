package com.bol;

import com.bol.mongo.MongoRollbackableAction;

public class ClaimCoupon extends MongoRollbackableAction<String> {

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
