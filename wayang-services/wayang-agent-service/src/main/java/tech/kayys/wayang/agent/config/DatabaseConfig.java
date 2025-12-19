package tech.kayys.wayang.agent.config;

import io.quarkus.arc.config.ConfigProperties;
import jakarta.enterprise.context.ApplicationScoped;

@ConfigProperties(prefix = "wayang.db")
@ApplicationScoped
public class DatabaseConfig {
    
    public String host = "localhost";
    public int port = 5432;
    public String database = "wayang_agent";
    public String username = "wayang";
    public String password = "wayang";
    public int maxPoolSize = 20;
    public int minPoolSize = 5;
    public boolean enableConnectionPool = true;
    public String driver = "org.postgresql.Driver";
    public String dialect = "org.hibernate.dialect.PostgreSQLDialect";
}