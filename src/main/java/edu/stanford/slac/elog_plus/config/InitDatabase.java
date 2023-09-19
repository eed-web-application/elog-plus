package edu.stanford.slac.elog_plus.config;


import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;
import edu.stanford.slac.elog_plus.model.EntityListener;
import io.mongock.driver.mongodb.springdata.v4.config.MongoDBConfiguration;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.config.MongoDbFactoryParser;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.List;
import java.util.Objects;

@Log4j2
@Configuration
@EnableMongoAuditing
@EnableTransactionManagement
@EnableMongoRepositories(basePackages = "edu.stanford.slac.elog_plus")
public class InitDatabase {
    @Value("${spring.data.mongodb.uri}")
    private String mongoAdminUri;
    private final AppProperties appProperties;

    public InitDatabase(AppProperties appProperties) {
        log.debug("InitDatabase activated");
        this.appProperties = appProperties;
    }

    @Bean
    @Primary
    MongoTransactionManager transactionManager(MongoDatabaseFactory dbFactory) {
        return new MongoTransactionManager(dbFactory);
    }

    @Bean
    public MongoClient mongoAdmin() {
        ConnectionString adminConnectionString = new ConnectionString(appProperties.getDbAdminUri());
        MongoClientSettings mongoClientSettings = MongoClientSettings.builder()
                .applyConnectionString(adminConnectionString)
                .applicationName("elog-plus-admin")
                .build();
        return MongoClients.create(mongoClientSettings);
    }

    @Bean
    public MongoDatabaseFactory mongoDbFactory() {
        ConnectionString connectionString = new ConnectionString(mongoAdminUri);

        // ensure database and user
        createApplicationUser(mongoAdmin(), connectionString);

        MongoClientSettings mongoClientSettings = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                .build();

        return new SimpleMongoClientDatabaseFactory(
                MongoClients.create(mongoClientSettings),
                Objects.requireNonNull(connectionString.getDatabase())
        );
    }

    private void createApplicationUser(MongoClient mongoClient, ConnectionString connectionString) {
        log.info("Start user creation");
        // Connect to the admin database
        MongoDatabase applicationDb = mongoClient.getDatabase(Objects.requireNonNull(connectionString.getDatabase()));

        // Retrieve the list of users
        @SuppressWarnings("unchecked")
        List<Document> users = applicationDb.runCommand(new Document("usersInfo", 1)).get("users", List.class);
        // Check if the desired user exists
        for (Document user : users) {
            if (Objects.equals(connectionString.getUsername(), user.getString("user"))) {
                return;
            }
        }
        // Create user command
        Document createUserCommand = new Document("createUser", Objects.requireNonNull(connectionString.getCredential()).getUserName())
                .append("pwd", new String(Objects.requireNonNull(connectionString.getCredential().getPassword())))
                .append("roles", List.of(
                                new Document("role", "readWrite").append("db", connectionString.getDatabase())
                        )
                );

        // Execute the createUser command
        Document result = applicationDb.runCommand(createUserCommand);
        log.info("User creation result: {}", result);
    }
}