package com.bol;

import com.bol.engine.TransactionHandler;
import com.bol.store.InMemoryActionStore;
import com.bol.store.MongoActionStore;
import com.mongodb.MongoClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoConfiguration;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.Collection;
import java.util.Collections;

@Configuration
public abstract class SpringConfiguration extends AbstractMongoConfiguration {

    @Value("${mongodb.port:27017}")
    int port;

    @Override
    protected String getDatabaseName() {
        return "test";
    }

    @Override
    protected Collection<String> getMappingBasePackages() {
        return Collections.singletonList(SpringConfiguration.class.getPackage().getName());
    }

    @Override
    public MongoClient mongoClient() {
        return new MongoClient("localhost", port);
    }

    @Bean
    public TransactionHandler<String> transactionHandler(MongoTemplate mongoTemplate) {
        return new TransactionHandler<>(new InMemoryActionStore<>(), new MongoActionStore(mongoTemplate, "transaction"));
    }
}
