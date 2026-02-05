package com.ftn.drumigo.service;

import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.hibernate.SessionFactory;
import org.hibernate.relational.SchemaManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.sql.DataSource;

@Service
@RequiredArgsConstructor
public class DatabaseMaintenanceService {

    private final EntityManagerFactory entityManagerFactory;
    private final DataSource dataSource;
    private final ResourceLoader resourceLoader;

    @Value("${app.maintenance.seed-script:classpath:db/seed/mysql-seed.sql}")
    private String seedScript;

    public void resetDatabase() {
        recreateSchema();
        runSeedScript();
    }

    private void recreateSchema() {
        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        SchemaManager schemaManager = sessionFactory.getSchemaManager();
        schemaManager.dropMappedObjects(true);
        schemaManager.exportMappedObjects(true);
    }

    private void runSeedScript() {
        Resource script = resourceLoader.getResource(seedScript);
        Assert.state(script.exists(), "Seed script not found: " + seedScript);

        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScript(script);
        populator.setContinueOnError(false);
        populator.execute(dataSource);
    }
}
