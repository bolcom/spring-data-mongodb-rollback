package com.bol;

import com.bol.engine.ActionStore;
import com.bol.engine.RollbackableAction;
import com.bol.store.MongoActionStore;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {SpringConfiguration.class})
public class SystemTest {

    @Autowired MongoTemplate mongoTemplate;

    @Before
    public void cleanDb() {
        mongoTemplate.dropCollection("transaction");
    }

    @Test
    public void basicOperation() {
        MongoActionStore actionStore = new MongoActionStore(mongoTemplate, "transaction");
        ClaimCoupon action = new ClaimCoupon("order1", "coupon1", true);
        action.ttlMs = 345;
        actionStore.put(action);

        Iterable<RollbackableAction<String>> order1 = actionStore.get("order1");

        System.err.println(order1);

    }
}
