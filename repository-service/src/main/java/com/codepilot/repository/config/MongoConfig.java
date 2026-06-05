package com.codepilot.repository.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

/**
 * Enables MongoDB auditing so that @CreatedDate and @LastModifiedDate
 * fields are automatically populated when documents are saved.
 */
@Configuration
@EnableMongoAuditing
public class MongoConfig {
}