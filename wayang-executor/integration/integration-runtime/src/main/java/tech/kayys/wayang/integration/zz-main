# ============================================================================
# KUBERNETES DEPLOYMENT FOR SILAT CAMEL EXECUTOR
# ============================================================================
#
# Complete production-ready Kubernetes manifests including:
# - Deployment with rolling updates
# - Horizontal Pod Autoscaler
# - Service and Ingress
# - ConfigMap and Secrets
# - ServiceMonitor for Prometheus
# - PodDisruptionBudget
#
# Usage:
#   kubectl apply -f kubernetes-deployment.yaml

---
# ==================== NAMESPACE ====================

apiVersion: v1
kind: Namespace
metadata:
  name: silat
  labels:
    name: silat
    environment: production

---
# ==================== CONFIGMAP ====================

apiVersion: v1
kind: ConfigMap
metadata:
  name: camel-executor-config
  namespace: silat
data:
  application.properties: |
    quarkus.application.name=silat-executor-camel
    quarkus.http.port=8082
    
    # Engine Connection
    silat.engine.grpc.host=${SILAT_ENGINE_HOST:silat-engine-service}
    silat.engine.grpc.port=${SILAT_ENGINE_PORT:9090}
    
    # Camel Configuration
    camel.context.name=silat-integration-context
    camel.main.auto-startup=true
    camel.threadpool.max-pool-size=100
    
    # Kafka
    camel.component.kafka.brokers=${KAFKA_BROKERS:kafka:9092}
    
    # Metrics
    quarkus.micrometer.enabled=true
    quarkus.micrometer.export.prometheus.enabled=true
    
    # Health Checks
    quarkus.health.extensions.enabled=true
    
    # OpenTelemetry
    quarkus.opentelemetry.enabled=true
    quarkus.opentelemetry.tracer.exporter.otlp.endpoint=${JAEGER_ENDPOINT:http://jaeger:4317}

---
# ==================== SECRET ====================

apiVersion: v1
kind: Secret
metadata:
  name: camel-executor-secret
  namespace: silat
type: Opaque
stringData:
  database-url: "jdbc:postgresql://postgres:5432/silat"
  database-username: "silat"
  database-password: "silat123"
  silat-api-key: "your-api-key-here"
  
  # AWS Credentials
  aws-access-key: "your-aws-access-key"
  aws-secret-key: "your-aws-secret-key"
  
  # Salesforce Credentials
  salesforce-client-id: "your-salesforce-client-id"
  salesforce-client-secret: "your-salesforce-client-secret"
  salesforce-username: "your-salesforce-username"
  salesforce-password: "your-salesforce-password"
  
  # Azure Credentials
  azure-storage-connection: "your-azure-storage-connection"
  azure-servicebus-connection: "your-azure-servicebus-connection"
  
  # GCP Credentials
  gcp-service-account-key: "your-gcp-service-account-json"

---
# ==================== DEPLOYMENT ====================

apiVersion: apps/v1
kind: Deployment
metadata:
  name: camel-executor
  namespace: silat
  labels:
    app: camel-executor
    version: v1
spec:
  replicas: 3
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
  selector:
    matchLabels:
      app: camel-executor
  template:
    metadata:
      labels:
        app: camel-executor
        version: v1
      annotations:
        prometheus.io/scrape: "true"
        prometheus.io/port: "8082"
        prometheus.io/path: "/metrics"
    spec:
      serviceAccountName: camel-executor
      securityContext:
        runAsNonRoot: true
        runAsUser: 1000
        fsGroup: 1000
      
      affinity:
        podAntiAffinity:
          preferredDuringSchedulingIgnoredDuringExecution:
          - weight: 100
            podAffinityTerm:
              labelSelector:
                matchExpressions:
                - key: app
                  operator: In
                  values:
                  - camel-executor
              topologyKey: kubernetes.io/hostname
      
      containers:
      - name: camel-executor
        image: silat/camel-executor:1.0.0
        imagePullPolicy: IfNotPresent
        
        ports:
        - name: http
          containerPort: 8082
          protocol: TCP
        
        env:
        - name: JAVA_OPTS
          value: >-
            -Xmx1g
            -Xms512m
            -XX:+UseG1GC
            -XX:MaxGCPauseMillis=100
            -XX:+UseContainerSupport
            -XX:MaxRAMPercentage=75.0
        
        - name: QUARKUS_DATASOURCE_JDBC_URL
          valueFrom:
            secretKeyRef:
              name: camel-executor-secret
              key: database-url
        
        - name: QUARKUS_DATASOURCE_USERNAME
          valueFrom:
            secretKeyRef:
              name: camel-executor-secret
              key: database-username
        
        - name: QUARKUS_DATASOURCE_PASSWORD
          valueFrom:
            secretKeyRef:
              name: camel-executor-secret
              key: database-password
        
        - name: SILAT_API_KEY
          valueFrom:
            secretKeyRef:
              name: camel-executor-secret
              key: silat-api-key
        
        - name: AWS_ACCESS_KEY
          valueFrom:
            secretKeyRef:
              name: camel-executor-secret
              key: aws-access-key
        
        - name: AWS_SECRET_KEY
          valueFrom:
            secretKeyRef:
              name: camel-executor-secret
              key: aws-secret-key
        
        - name: POD_NAME
          valueFrom:
            fieldRef:
              fieldPath: metadata.name
        
        - name: POD_NAMESPACE
          valueFrom:
            fieldRef:
              fieldPath: metadata.namespace
        
        resources:
          requests:
            memory: "512Mi"
            cpu: "500m"
          limits:
            memory: "2Gi"
            cpu: "2000m"
        
        livenessProbe:
          httpGet:
            path: /health/live
            port: http
          initialDelaySeconds: 30
          periodSeconds: 10
          timeoutSeconds: 5
          failureThreshold: 3
        
        readinessProbe:
          httpGet:
            path: /health/ready
            port: http
          initialDelaySeconds: 10
          periodSeconds: 5
          timeoutSeconds: 3
          failureThreshold: 3
        
        volumeMounts:
        - name: config
          mountPath: /deployments/config
          readOnly: true
        - name: logs
          mountPath: /deployments/logs
        - name: tmp
          mountPath: /tmp
      
      volumes:
      - name: config
        configMap:
          name: camel-executor-config
      - name: logs
        emptyDir: {}
      - name: tmp
        emptyDir: {}
      
      terminationGracePeriodSeconds: 60

---
# ==================== SERVICE ====================

apiVersion: v1
kind: Service
metadata:
  name: camel-executor-service
  namespace: silat
  labels:
    app: camel-executor
spec:
  type: ClusterIP
  ports:
  - port: 8082
    targetPort: http
    protocol: TCP
    name: http
  selector:
    app: camel-executor

---
# ==================== HEADLESS SERVICE (FOR STATEFUL FEATURES) ====================

apiVersion: v1
kind: Service
metadata:
  name: camel-executor-headless
  namespace: silat
  labels:
    app: camel-executor
spec:
  clusterIP: None
  ports:
  - port: 8082
    name: http
  selector:
    app: camel-executor

---
# ==================== INGRESS ====================

apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: camel-executor-ingress
  namespace: silat
  annotations:
    kubernetes.io/ingress.class: nginx
    cert-manager.io/cluster-issuer: letsencrypt-prod
    nginx.ingress.kubernetes.io/ssl-redirect: "true"
    nginx.ingress.kubernetes.io/proxy-body-size: "10m"
    nginx.ingress.kubernetes.io/proxy-connect-timeout: "600"
    nginx.ingress.kubernetes.io/proxy-send-timeout: "600"
    nginx.ingress.kubernetes.io/proxy-read-timeout: "600"
spec:
  tls:
  - hosts:
    - camel-executor.example.com
    secretName: camel-executor-tls
  rules:
  - host: camel-executor.example.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: camel-executor-service
            port:
              number: 8082

---
# ==================== HORIZONTAL POD AUTOSCALER ====================

apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: camel-executor-hpa
  namespace: silat
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: camel-executor
  minReplicas: 3
  maxReplicas: 20
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
  - type: Pods
    pods:
      metric:
        name: http_requests_per_second
      target:
        type: AverageValue
        averageValue: "1000"
  behavior:
    scaleDown:
      stabilizationWindowSeconds: 300
      policies:
      - type: Percent
        value: 50
        periodSeconds: 60
    scaleUp:
      stabilizationWindowSeconds: 60
      policies:
      - type: Percent
        value: 100
        periodSeconds: 30
      - type: Pods
        value: 2
        periodSeconds: 30
      selectPolicy: Max

---
# ==================== POD DISRUPTION BUDGET ====================

apiVersion: policy/v1
kind: PodDisruptionBudget
metadata:
  name: camel-executor-pdb
  namespace: silat
spec:
  minAvailable: 2
  selector:
    matchLabels:
      app: camel-executor

---
# ==================== SERVICE ACCOUNT ====================

apiVersion: v1
kind: ServiceAccount
metadata:
  name: camel-executor
  namespace: silat

---
# ==================== ROLE ====================

apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  name: camel-executor-role
  namespace: silat
rules:
- apiGroups: [""]
  resources: ["configmaps", "secrets"]
  verbs: ["get", "list", "watch"]
- apiGroups: [""]
  resources: ["pods"]
  verbs: ["get", "list"]

---
# ==================== ROLE BINDING ====================

apiVersion: rbac.authorization.k8s.io/v1
kind: RoleBinding
metadata:
  name: camel-executor-rolebinding
  namespace: silat
subjects:
- kind: ServiceAccount
  name: camel-executor
  namespace: silat
roleRef:
  kind: Role
  name: camel-executor-role
  apiGroup: rbac.authorization.k8s.io

---
# ==================== SERVICE MONITOR (PROMETHEUS) ====================

apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: camel-executor-metrics
  namespace: silat
  labels:
    app: camel-executor
spec:
  selector:
    matchLabels:
      app: camel-executor
  endpoints:
  - port: http
    path: /metrics
    interval: 30s
    scrapeTimeout: 10s

---
# ==================== POD MONITOR ====================

apiVersion: monitoring.coreos.com/v1
kind: PodMonitor
metadata:
  name: camel-executor-pod-metrics
  namespace: silat
spec:
  selector:
    matchLabels:
      app: camel-executor
  podMetricsEndpoints:
  - port: http
    path: /metrics

---
# ==================== NETWORK POLICY ====================

apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: camel-executor-network-policy
  namespace: silat
spec:
  podSelector:
    matchLabels:
      app: camel-executor
  policyTypes:
  - Ingress
  - Egress
  ingress:
  - from:
    - namespaceSelector:
        matchLabels:
          name: silat
    - podSelector:
        matchLabels:
          app: silat-engine
    ports:
    - protocol: TCP
      port: 8082
  egress:
  - to:
    - namespaceSelector: {}
    ports:
    - protocol: TCP
      port: 9090  # Silat engine
    - protocol: TCP
      port: 5432  # PostgreSQL
    - protocol: TCP
      port: 6379  # Redis
    - protocol: TCP
      port: 9092  # Kafka
    - protocol: TCP
      port: 27017 # MongoDB
  - to:
    - namespaceSelector: {}
    ports:
    - protocol: TCP
      port: 53    # DNS
    - protocol: UDP
      port: 53    # DNS
  - to:
    - namespaceSelector: {}
    ports:
    - protocol: TCP
      port: 443   # HTTPS

---
# ==================== RESOURCE QUOTAS ====================

apiVersion: v1
kind: ResourceQuota
metadata:
  name: camel-executor-quota
  namespace: silat
spec:
  hard:
    requests.cpu: "20"
    requests.memory: "40Gi"
    limits.cpu: "40"
    limits.memory: "80Gi"
    persistentvolumeclaims: "10"
    services.loadbalancers: "2"

---
# ==================== LIMIT RANGE ====================

apiVersion: v1
kind: LimitRange
metadata:
  name: camel-executor-limits
  namespace: silat
spec:
  limits:
  - max:
      cpu: "4"
      memory: "4Gi"
    min:
      cpu: "100m"
      memory: "128Mi"
    default:
      cpu: "1"
      memory: "1Gi"
    defaultRequest:
      cpu: "500m"
      memory: "512Mi"
    type: Container

---
# ==================== PERSISTENT VOLUME CLAIM ====================

apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: camel-executor-logs-pvc
  namespace: silat
spec:
  accessModes:
  - ReadWriteOnce
  resources:
    requests:
      storage: 10Gi
  storageClassName: standard

---
# ==================== CRON JOB FOR CLEANUP ====================

apiVersion: batch/v1
kind: CronJob
metadata:
  name: camel-executor-cleanup
  namespace: silat
spec:
  schedule: "0 2 * * *"  # Daily at 2 AM
  jobTemplate:
    spec:
      template:
        spec:
          containers:
          - name: cleanup
            image: silat/camel-executor:1.0.0
            command:
            - /bin/sh
            - -c
            - |
              echo "Running cleanup..."
              # Cleanup old routes
              # Cleanup old logs
              # Cleanup cache
              echo "Cleanup complete"
          restartPolicy: OnFailure


-----
version: '3.8'

# ============================================================================
# SILAT CAMEL EXECUTOR - COMPLETE DEPLOYMENT STACK
# ============================================================================
#
# This Docker Compose file includes:
# - Silat Camel Executor
# - PostgreSQL (for persistence)
# - Redis (for caching and idempotency)
# - Kafka + Zookeeper (for messaging)
# - Prometheus (for metrics)
# - Grafana (for visualization)
# - Jaeger (for distributed tracing)
# - MongoDB (for document storage)
#
# Usage:
#   docker-compose up -d

services:
  
  # ==================== SILAT CAMEL EXECUTOR ====================
  
  camel-executor:
    build:
      context: .
      dockerfile: src/main/docker/Dockerfile.jvm
    container_name: silat-camel-executor
    hostname: camel-executor
    ports:
      - "8082:8082"  # HTTP API
      - "5005:5005"  # Debug port
    environment:
      # Engine Connection
      SILAT_ENGINE_GRPC_HOST: silat-engine
      SILAT_ENGINE_GRPC_PORT: 9090
      SILAT_EXECUTOR_ID: camel-executor-01
      
      # Database
      QUARKUS_DATASOURCE_JDBC_URL: jdbc:postgresql://postgres:5432/silat
      QUARKUS_DATASOURCE_USERNAME: silat
      QUARKUS_DATASOURCE_PASSWORD: silat123
      
      # Redis
      QUARKUS_REDIS_HOSTS: redis://redis:6379
      
      # Kafka
      KAFKA_BOOTSTRAP_SERVERS: kafka:9092
      CAMEL_COMPONENT_KAFKA_BROKERS: kafka:9092
      
      # MongoDB
      QUARKUS_MONGODB_CONNECTION_STRING: mongodb://mongodb:27017
      
      # Observability
      QUARKUS_OPENTELEMETRY_TRACER_EXPORTER_OTLP_ENDPOINT: http://jaeger:4317
      QUARKUS_MICROMETER_EXPORT_PROMETHEUS_ENABLED: "true"
      
      # Java Options
      JAVA_OPTS: >-
        -Xmx1g
        -Xms512m
        -XX:+UseG1GC
        -XX:MaxGCPauseMillis=100
        -Djava.util.logging.manager=org.jboss.logmanager.LogManager
        -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005
      
      # Camel Configuration
      CAMEL_MAIN_ROUTES_INCLUDE_PATTERN: classpath:camel/routes/*.xml
      CAMEL_CONTEXT_STREAM_CACHING: "true"
      
    depends_on:
      - postgres
      - redis
      - kafka
      - mongodb
      - jaeger
    networks:
      - silat-network
    volumes:
      - ./logs:/deployments/logs
      - ./data:/deployments/data
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8082/health/ready"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s
    restart: unless-stopped
  
  # ==================== DATABASE ====================
  
  postgres:
    image: postgres:16-alpine
    container_name: silat-postgres
    hostname: postgres
    ports:
      - "5432:5432"
    environment:
      POSTGRES_DB: silat
      POSTGRES_USER: silat
      POSTGRES_PASSWORD: silat123
      POSTGRES_INITDB_ARGS: "-E UTF8"
    volumes:
      - postgres-data:/var/lib/postgresql/data
      - ./init-db.sql:/docker-entrypoint-initdb.d/init.sql
    networks:
      - silat-network
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U silat"]
      interval: 10s
      timeout: 5s
      retries: 5
    restart: unless-stopped
  
  # ==================== REDIS ====================
  
  redis:
    image: redis:7-alpine
    container_name: silat-redis
    hostname: redis
    ports:
      - "6379:6379"
    command: redis-server --appendonly yes --maxmemory 512mb --maxmemory-policy allkeys-lru
    volumes:
      - redis-data:/data
    networks:
      - silat-network
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5
    restart: unless-stopped
  
  # ==================== KAFKA ====================
  
  zookeeper:
    image: confluentinc/cp-zookeeper:7.5.0
    container_name: silat-zookeeper
    hostname: zookeeper
    ports:
      - "2181:2181"
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000
    volumes:
      - zookeeper-data:/var/lib/zookeeper/data
      - zookeeper-logs:/var/lib/zookeeper/log
    networks:
      - silat-network
    restart: unless-stopped
  
  kafka:
    image: confluentinc/cp-kafka:7.5.0
    container_name: silat-kafka
    hostname: kafka
    ports:
      - "9092:9092"
      - "9093:9093"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092,PLAINTEXT_HOST://localhost:9093
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT
      KAFKA_INTER_BROKER_LISTENER_NAME: PLAINTEXT
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_AUTO_CREATE_TOPICS_ENABLE: "true"
      KAFKA_LOG_RETENTION_HOURS: 168
      KAFKA_LOG_SEGMENT_BYTES: 1073741824
    volumes:
      - kafka-data:/var/lib/kafka/data
    depends_on:
      - zookeeper
    networks:
      - silat-network
    healthcheck:
      test: ["CMD", "kafka-broker-api-versions", "--bootstrap-server", "localhost:9092"]
      interval: 30s
      timeout: 10s
      retries: 5
    restart: unless-stopped
  
  # ==================== MONGODB ====================
  
  mongodb:
    image: mongo:7.0
    container_name: silat-mongodb
    hostname: mongodb
    ports:
      - "27017:27017"
    environment:
      MONGO_INITDB_ROOT_USERNAME: silat
      MONGO_INITDB_ROOT_PASSWORD: silat123
      MONGO_INITDB_DATABASE: silat
    volumes:
      - mongodb-data:/data/db
    networks:
      - silat-network
    healthcheck:
      test: ["CMD", "mongosh", "--eval", "db.adminCommand('ping')"]
      interval: 10s
      timeout: 5s
      retries: 5
    restart: unless-stopped
  
  # ==================== PROMETHEUS ====================
  
  prometheus:
    image: prom/prometheus:v2.48.0
    container_name: silat-prometheus
    hostname: prometheus
    ports:
      - "9090:9090"
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
      - '--web.console.libraries=/usr/share/prometheus/console_libraries'
      - '--web.console.templates=/usr/share/prometheus/consoles'
      - '--storage.tsdb.retention.time=30d'
    volumes:
      - ./monitoring/prometheus.yml:/etc/prometheus/prometheus.yml
      - prometheus-data:/prometheus
    networks:
      - silat-network
    restart: unless-stopped
  
  # ==================== GRAFANA ====================
  
  grafana:
    image: grafana/grafana:10.2.2
    container_name: silat-grafana
    hostname: grafana
    ports:
      - "3000:3000"
    environment:
      GF_SECURITY_ADMIN_USER: admin
      GF_SECURITY_ADMIN_PASSWORD: admin123
      GF_INSTALL_PLUGINS: grafana-piechart-panel
      GF_USERS_ALLOW_SIGN_UP: "false"
    volumes:
      - grafana-data:/var/lib/grafana
      - ./monitoring/grafana-dashboards:/etc/grafana/provisioning/dashboards
      - ./monitoring/grafana-datasources.yml:/etc/grafana/provisioning/datasources/datasources.yml
    depends_on:
      - prometheus
    networks:
      - silat-network
    restart: unless-stopped
  
  # ==================== JAEGER (TRACING) ====================
  
  jaeger:
    image: jaegertracing/all-in-one:1.52
    container_name: silat-jaeger
    hostname: jaeger
    ports:
      - "5775:5775/udp"   # Zipkin compact thrift
      - "6831:6831/udp"   # Jaeger compact thrift
      - "6832:6832/udp"   # Jaeger binary thrift
      - "5778:5778"       # Serve configs
      - "16686:16686"     # Jaeger UI
      - "14268:14268"     # Jaeger collector HTTP
      - "14250:14250"     # Jaeger gRPC
      - "9411:9411"       # Zipkin compatible endpoint
      - "4317:4317"       # OTLP gRPC
      - "4318:4318"       # OTLP HTTP
    environment:
      COLLECTOR_ZIPKIN_HOST_PORT: ":9411"
      COLLECTOR_OTLP_ENABLED: "true"
    networks:
      - silat-network
    restart: unless-stopped
  
  # ==================== KAFKA UI (OPTIONAL) ====================
  
  kafka-ui:
    image: provectuslabs/kafka-ui:latest
    container_name: silat-kafka-ui
    hostname: kafka-ui
    ports:
      - "8080:8080"
    environment:
      KAFKA_CLUSTERS_0_NAME: silat-cluster
      KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS: kafka:9092
      KAFKA_CLUSTERS_0_ZOOKEEPER: zookeeper:2181
    depends_on:
      - kafka
    networks:
      - silat-network
    restart: unless-stopped
  
  # ==================== PGADMIN (OPTIONAL) ====================
  
  pgadmin:
    image: dpage/pgadmin4:latest
    container_name: silat-pgadmin
    hostname: pgadmin
    ports:
      - "5050:80"
    environment:
      PGADMIN_DEFAULT_EMAIL: admin@silat.tech
      PGADMIN_DEFAULT_PASSWORD: admin123
    volumes:
      - pgadmin-data:/var/lib/pgadmin
    depends_on:
      - postgres
    networks:
      - silat-network
    restart: unless-stopped

# ==================== VOLUMES ====================

volumes:
  postgres-data:
    driver: local
  redis-data:
    driver: local
  mongodb-data:
    driver: local
  kafka-data:
    driver: local
  zookeeper-data:
    driver: local
  zookeeper-logs:
    driver: local
  prometheus-data:
    driver: local
  grafana-data:
    driver: local
  pgadmin-data:
    driver: local

# ==================== NETWORKS ====================

networks:
  silat-network:
    driver: bridge
    ipam:
      config:
        - subnet: 172.20.0.0/16version: '3.8'

# ============================================================================
# SILAT CAMEL EXECUTOR - COMPLETE DEPLOYMENT STACK
# ============================================================================
#
# This Docker Compose file includes:
# - Silat Camel Executor
# - PostgreSQL (for persistence)
# - Redis (for caching and idempotency)
# - Kafka + Zookeeper (for messaging)
# - Prometheus (for metrics)
# - Grafana (for visualization)
# - Jaeger (for distributed tracing)
# - MongoDB (for document storage)
#
# Usage:
#   docker-compose up -d

services:
  
  # ==================== SILAT CAMEL EXECUTOR ====================
  
  camel-executor:
    build:
      context: .
      dockerfile: src/main/docker/Dockerfile.jvm
    container_name: silat-camel-executor
    hostname: camel-executor
    ports:
      - "8082:8082"  # HTTP API
      - "5005:5005"  # Debug port
    environment:
      # Engine Connection
      SILAT_ENGINE_GRPC_HOST: silat-engine
      SILAT_ENGINE_GRPC_PORT: 9090
      SILAT_EXECUTOR_ID: camel-executor-01
      
      # Database
      QUARKUS_DATASOURCE_JDBC_URL: jdbc:postgresql://postgres:5432/silat
      QUARKUS_DATASOURCE_USERNAME: silat
      QUARKUS_DATASOURCE_PASSWORD: silat123
      
      # Redis
      QUARKUS_REDIS_HOSTS: redis://redis:6379
      
      # Kafka
      KAFKA_BOOTSTRAP_SERVERS: kafka:9092
      CAMEL_COMPONENT_KAFKA_BROKERS: kafka:9092
      
      # MongoDB
      QUARKUS_MONGODB_CONNECTION_STRING: mongodb://mongodb:27017
      
      # Observability
      QUARKUS_OPENTELEMETRY_TRACER_EXPORTER_OTLP_ENDPOINT: http://jaeger:4317
      QUARKUS_MICROMETER_EXPORT_PROMETHEUS_ENABLED: "true"
      
      # Java Options
      JAVA_OPTS: >-
        -Xmx1g
        -Xms512m
        -XX:+UseG1GC
        -XX:MaxGCPauseMillis=100
        -Djava.util.logging.manager=org.jboss.logmanager.LogManager
        -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005
      
      # Camel Configuration
      CAMEL_MAIN_ROUTES_INCLUDE_PATTERN: classpath:camel/routes/*.xml
      CAMEL_CONTEXT_STREAM_CACHING: "true"
      
    depends_on:
      - postgres
      - redis
      - kafka
      - mongodb
      - jaeger
    networks:
      - silat-network
    volumes:
      - ./logs:/deployments/logs
      - ./data:/deployments/data
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8082/health/ready"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s
    restart: unless-stopped
  
  # ==================== DATABASE ====================
  
  postgres:
    image: postgres:16-alpine
    container_name: silat-postgres
    hostname: postgres
    ports:
      - "5432:5432"
    environment:
      POSTGRES_DB: silat
      POSTGRES_USER: silat
      POSTGRES_PASSWORD: silat123
      POSTGRES_INITDB_ARGS: "-E UTF8"
    volumes:
      - postgres-data:/var/lib/postgresql/data
      - ./init-db.sql:/docker-entrypoint-initdb.d/init.sql
    networks:
      - silat-network
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U silat"]
      interval: 10s
      timeout: 5s
      retries: 5
    restart: unless-stopped
  
  # ==================== REDIS ====================
  
  redis:
    image: redis:7-alpine
    container_name: silat-redis
    hostname: redis
    ports:
      - "6379:6379"
    command: redis-server --appendonly yes --maxmemory 512mb --maxmemory-policy allkeys-lru
    volumes:
      - redis-data:/data
    networks:
      - silat-network
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5
    restart: unless-stopped
  
  # ==================== KAFKA ====================
  
  zookeeper:
    image: confluentinc/cp-zookeeper:7.5.0
    container_name: silat-zookeeper
    hostname: zookeeper
    ports:
      - "2181:2181"
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000
    volumes:
      - zookeeper-data:/var/lib/zookeeper/data
      - zookeeper-logs:/var/lib/zookeeper/log
    networks:
      - silat-network
    restart: unless-stopped
  
  kafka:
    image: confluentinc/cp-kafka:7.5.0
    container_name: silat-kafka
    hostname: kafka
    ports:
      - "9092:9092"
      - "9093:9093"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092,PLAINTEXT_HOST://localhost:9093
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT
      KAFKA_INTER_BROKER_LISTENER_NAME: PLAINTEXT
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_AUTO_CREATE_TOPICS_ENABLE: "true"
      KAFKA_LOG_RETENTION_HOURS: 168
      KAFKA_LOG_SEGMENT_BYTES: 1073741824
    volumes:
      - kafka-data:/var/lib/kafka/data
    depends_on:
      - zookeeper
    networks:
      - silat-network
    healthcheck:
      test: ["CMD", "kafka-broker-api-versions", "--bootstrap-server", "localhost:9092"]
      interval: 30s
      timeout: 10s
      retries: 5
    restart: unless-stopped
  
  # ==================== MONGODB ====================
  
  mongodb:
    image: mongo:7.0
    container_name: silat-mongodb
    hostname: mongodb
    ports:
      - "27017:27017"
    environment:
      MONGO_INITDB_ROOT_USERNAME: silat
      MONGO_INITDB_ROOT_PASSWORD: silat123
      MONGO_INITDB_DATABASE: silat
    volumes:
      - mongodb-data:/data/db
    networks:
      - silat-network
    healthcheck:
      test: ["CMD", "mongosh", "--eval", "db.adminCommand('ping')"]
      interval: 10s
      timeout: 5s
      retries: 5
    restart: unless-stopped
  
  # ==================== PROMETHEUS ====================
  
  prometheus:
    image: prom/prometheus:v2.48.0
    container_name: silat-prometheus
    hostname: prometheus
    ports:
      - "9090:9090"
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
      - '--web.console.libraries=/usr/share/prometheus/console_libraries'
      - '--web.console.templates=/usr/share/prometheus/consoles'
      - '--storage.tsdb.retention.time=30d'
    volumes:
      - ./monitoring/prometheus.yml:/etc/prometheus/prometheus.yml
      - prometheus-data:/prometheus
    networks:
      - silat-network
    restart: unless-stopped
  
  # ==================== GRAFANA ====================
  
  grafana:
    image: grafana/grafana:10.2.2
    container_name: silat-grafana
    hostname: grafana
    ports:
      - "3000:3000"
    environment:
      GF_SECURITY_ADMIN_USER: admin
      GF_SECURITY_ADMIN_PASSWORD: admin123
      GF_INSTALL_PLUGINS: grafana-piechart-panel
      GF_USERS_ALLOW_SIGN_UP: "false"
    volumes:
      - grafana-data:/var/lib/grafana
      - ./monitoring/grafana-dashboards:/etc/grafana/provisioning/dashboards
      - ./monitoring/grafana-datasources.yml:/etc/grafana/provisioning/datasources/datasources.yml
    depends_on:
      - prometheus
    networks:
      - silat-network
    restart: unless-stopped
  
  # ==================== JAEGER (TRACING) ====================
  
  jaeger:
    image: jaegertracing/all-in-one:1.52
    container_name: silat-jaeger
    hostname: jaeger
    ports:
      - "5775:5775/udp"   # Zipkin compact thrift
      - "6831:6831/udp"   # Jaeger compact thrift
      - "6832:6832/udp"   # Jaeger binary thrift
      - "5778:5778"       # Serve configs
      - "16686:16686"     # Jaeger UI
      - "14268:14268"     # Jaeger collector HTTP
      - "14250:14250"     # Jaeger gRPC
      - "9411:9411"       # Zipkin compatible endpoint
      - "4317:4317"       # OTLP gRPC
      - "4318:4318"       # OTLP HTTP
    environment:
      COLLECTOR_ZIPKIN_HOST_PORT: ":9411"
      COLLECTOR_OTLP_ENABLED: "true"
    networks:
      - silat-network
    restart: unless-stopped
  
  # ==================== KAFKA UI (OPTIONAL) ====================
  
  kafka-ui:
    image: provectuslabs/kafka-ui:latest
    container_name: silat-kafka-ui
    hostname: kafka-ui
    ports:
      - "8080:8080"
    environment:
      KAFKA_CLUSTERS_0_NAME: silat-cluster
      KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS: kafka:9092
      KAFKA_CLUSTERS_0_ZOOKEEPER: zookeeper:2181
    depends_on:
      - kafka
    networks:
      - silat-network
    restart: unless-stopped
  
  # ==================== PGADMIN (OPTIONAL) ====================
  
  pgadmin:
    image: dpage/pgadmin4:latest
    container_name: silat-pgadmin
    hostname: pgadmin
    ports:
      - "5050:80"
    environment:
      PGADMIN_DEFAULT_EMAIL: admin@silat.tech
      PGADMIN_DEFAULT_PASSWORD: admin123
    volumes:
      - pgadmin-data:/var/lib/pgadmin
    depends_on:
      - postgres
    networks:
      - silat-network
    restart: unless-stopped

# ==================== VOLUMES ====================

volumes:
  postgres-data:
    driver: local
  redis-data:
    driver: local
  mongodb-data:
    driver: local
  kafka-data:
    driver: local
  zookeeper-data:
    driver: local
  zookeeper-logs:
    driver: local
  prometheus-data:
    driver: local
  grafana-data:
    driver: local
  pgadmin-data:
    driver: local

# ==================== NETWORKS ====================

networks:
  silat-network:
    driver: bridge
    ipam:
      config:
        - subnet: 172.20.0.0/16


# =====


package tech.kayys.silat.executor.camel.observability;

import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.Timer;
import io.opentelemetry.api.trace.*;
import io.quarkus.scheduler.Scheduled;
import io.smallrye.health.checks.HealthCheck;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.*;
import org.eclipse.microprofile.health.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * ============================================================================
 * COMPREHENSIVE OBSERVABILITY & MONITORING
 * ============================================================================
 * 
 * Features:
 * 1. Real-time Metrics Collection
 * 2. Distributed Tracing with OpenTelemetry
 * 3. Health Checks (Liveness, Readiness)
 * 4. Performance Analytics
 * 5. SLA Monitoring
 * 6. Alert Management
 * 7. Audit Logging
 * 8. Dashboard API
 */

// ==================== METRICS COLLECTOR ====================

/**
 * Comprehensive metrics collection for Camel routes
 */
@ApplicationScoped
public class CamelMetricsCollector {
    
    private static final Logger LOG = LoggerFactory.getLogger(CamelMetricsCollector.class);
    
    @Inject
    MeterRegistry meterRegistry;
    
    @Inject
    CamelContext camelContext;
    
    private final Map<String, RouteMetricsData> routeMetrics = new ConcurrentHashMap<>();
    private final Map<String, ComponentMetrics> componentMetrics = new ConcurrentHashMap<>();
    
    /**
     * Initialize metrics for a route
     */
    public void initializeRouteMetrics(String routeId) {
        RouteMetricsData metrics = new RouteMetricsData(routeId, meterRegistry);
        routeMetrics.put(routeId, metrics);
        LOG.info("Initialized metrics for route: {}", routeId);
    }
    
    /**
     * Record route execution
     */
    public void recordRouteExecution(
            String routeId,
            Duration duration,
            boolean success,
            String tenantId) {
        
        RouteMetricsData metrics = routeMetrics.get(routeId);
        if (metrics != null) {
            metrics.recordExecution(duration, success, tenantId);
        }
    }
    
    /**
     * Get route metrics
     */
    public RouteMetricsData getRouteMetrics(String routeId) {
        return routeMetrics.get(routeId);
    }
    
    /**
     * Get all metrics
     */
    public Map<String, RouteMetricsData> getAllMetrics() {
        return new HashMap<>(routeMetrics);
    }
    
    /**
     * Get aggregated metrics summary
     */
    public MetricsSummary getMetricsSummary() {
        long totalExecutions = routeMetrics.values().stream()
            .mapToLong(m -> m.getTotalExecutions())
            .sum();
        
        long successfulExecutions = routeMetrics.values().stream()
            .mapToLong(m -> m.getSuccessfulExecutions())
            .sum();
        
        long failedExecutions = routeMetrics.values().stream()
            .mapToLong(m -> m.getFailedExecutions())
            .sum();
        
        double avgDuration = routeMetrics.values().stream()
            .mapToDouble(m -> m.getAverageDuration())
            .average()
            .orElse(0.0);
        
        return new MetricsSummary(
            totalExecutions,
            successfulExecutions,
            failedExecutions,
            avgDuration,
            routeMetrics.size()
        );
    }
}

/**
 * Route-specific metrics data
 */
class RouteMetricsData {
    private final String routeId;
    private final Counter totalCounter;
    private final Counter successCounter;
    private final Counter failureCounter;
    private final Timer executionTimer;
    private final Gauge activeExecutions;
    
    private final AtomicInteger activeCount = new AtomicInteger(0);
    private final AtomicLong totalExecutions = new AtomicLong(0);
    private final AtomicLong successfulExecutions = new AtomicLong(0);
    private final AtomicLong failedExecutions = new AtomicLong(0);
    private final List<Duration> recentDurations = Collections.synchronizedList(new ArrayList<>());
    private final Map<String, AtomicLong> tenantExecutions = new ConcurrentHashMap<>();
    
    public RouteMetricsData(String routeId, MeterRegistry registry) {
        this.routeId = routeId;
        
        // Register meters
        this.totalCounter = Counter.builder("camel.route.executions.total")
            .tag("route", routeId)
            .description("Total number of route executions")
            .register(registry);
        
        this.successCounter = Counter.builder("camel.route.executions.success")
            .tag("route", routeId)
            .description("Successful route executions")
            .register(registry);
        
        this.failureCounter = Counter.builder("camel.route.executions.failure")
            .tag("route", routeId)
            .description("Failed route executions")
            .register(registry);
        
        this.executionTimer = Timer.builder("camel.route.execution.duration")
            .tag("route", routeId)
            .description("Route execution duration")
            .register(registry);
        
        this.activeExecutions = Gauge.builder("camel.route.executions.active", 
                activeCount, AtomicInteger::get)
            .tag("route", routeId)
            .description("Active route executions")
            .register(registry);
    }
    
    public void recordExecution(Duration duration, boolean success, String tenantId) {
        totalExecutions.incrementAndGet();
        totalCounter.increment();
        
        if (success) {
            successfulExecutions.incrementAndGet();
            successCounter.increment();
        } else {
            failedExecutions.incrementAndGet();
            failureCounter.increment();
        }
        
        executionTimer.record(duration);
        recentDurations.add(duration);
        
        // Keep only last 100 durations
        if (recentDurations.size() > 100) {
            recentDurations.remove(0);
        }
        
        // Track per-tenant metrics
        tenantExecutions.computeIfAbsent(tenantId, k -> new AtomicLong(0))
            .incrementAndGet();
    }
    
    public void incrementActive() {
        activeCount.incrementAndGet();
    }
    
    public void decrementActive() {
        activeCount.decrementAndGet();
    }
    
    public long getTotalExecutions() {
        return totalExecutions.get();
    }
    
    public long getSuccessfulExecutions() {
        return successfulExecutions.get();
    }
    
    public long getFailedExecutions() {
        return failedExecutions.get();
    }
    
    public double getAverageDuration() {
        synchronized (recentDurations) {
            return recentDurations.stream()
                .mapToLong(Duration::toMillis)
                .average()
                .orElse(0.0);
        }
    }
    
    public double getSuccessRate() {
        long total = totalExecutions.get();
        if (total == 0) return 100.0;
        return (successfulExecutions.get() * 100.0) / total;
    }
    
    public Map<String, Long> getTenantMetrics() {
        return tenantExecutions.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> e.getValue().get()
            ));
    }
}

record ComponentMetrics(
    String componentName,
    long messagesProcessed,
    long bytesProcessed,
    double avgProcessingTime
) {}

record MetricsSummary(
    long totalExecutions,
    long successfulExecutions,
    long failedExecutions,
    double averageDuration,
    int activeRoutes
) {}

// ==================== DISTRIBUTED TRACING ====================

/**
 * OpenTelemetry distributed tracing integration
 */
@ApplicationScoped
public class CamelTracingService {
    
    private static final Logger LOG = LoggerFactory.getLogger(CamelTracingService.class);
    
    @Inject
    Tracer tracer;
    
    private final Map<String, Span> activeSpans = new ConcurrentHashMap<>();
    
    /**
     * Start span for route execution
     */
    public Span startRouteSpan(String routeId, String tenantId, Map<String, Object> attributes) {
        Span span = tracer.spanBuilder("route.execute")
            .setAttribute("route.id", routeId)
            .setAttribute("tenant.id", tenantId)
            .setAttribute("component", "camel-executor")
            .startSpan();
        
        // Add custom attributes
        attributes.forEach((key, value) -> 
            span.setAttribute(key, value != null ? value.toString() : "null")
        );
        
        activeSpans.put(routeId, span);
        return span;
    }
    
    /**
     * End span
     */
    public void endRouteSpan(String routeId, boolean success, String errorMessage) {
        Span span = activeSpans.remove(routeId);
        if (span != null) {
            if (!success) {
                span.setStatus(StatusCode.ERROR, errorMessage);
                span.recordException(new Exception(errorMessage));
            } else {
                span.setStatus(StatusCode.OK);
            }
            span.end();
        }
    }
    
    /**
     * Add event to span
     */
    public void addSpanEvent(String routeId, String eventName, Map<String, String> attributes) {
        Span span = activeSpans.get(routeId);
        if (span != null) {
            span.addEvent(eventName, 
                io.opentelemetry.api.common.Attributes.builder()
                    .putAll(attributes.entrySet().stream()
                        .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            e -> io.opentelemetry.api.common.AttributeKey.stringKey(e.getKey())
                        )))
                    .build()
            );
        }
    }
}

// ==================== HEALTH CHECKS ====================

/**
 * Comprehensive health checks for Camel executor
 */
@Liveness
@ApplicationScoped
public class CamelLivenessCheck implements HealthCheck {
    
    @Inject
    CamelContext camelContext;
    
    @Override
    public HealthCheckResponse call() {
        HealthCheckResponseBuilder builder = HealthCheckResponse.named("camel-liveness");
        
        try {
            boolean alive = camelContext.getStatus().isStarted() || 
                           camelContext.getStatus().isStarting();
            
            if (alive) {
                return builder.up()
                    .withData("status", camelContext.getStatus().name())
                    .withData("uptime", camelContext.getUptime())
                    .build();
            } else {
                return builder.down()
                    .withData("status", camelContext.getStatus().name())
                    .build();
            }
        } catch (Exception e) {
            return builder.down()
                .withData("error", e.getMessage())
                .build();
        }
    }
}

/**
 * Readiness check for Camel executor
 */
@Readiness
@ApplicationScoped
public class CamelReadinessCheck implements HealthCheck {
    
    @Inject
    CamelContext camelContext;
    
    @Inject
    CamelMetricsCollector metricsCollector;
    
    @Override
    public HealthCheckResponse call() {
        HealthCheckResponseBuilder builder = HealthCheckResponse.named("camel-readiness");
        
        try {
            boolean ready = camelContext.getStatus().isStarted();
            
            if (!ready) {
                return builder.down()
                    .withData("status", camelContext.getStatus().name())
                    .build();
            }
            
            // Check route health
            List<Route> routes = camelContext.getRoutes();
            long healthyRoutes = routes.stream()
                .filter(route -> {
                    try {
                        ServiceStatus status = camelContext.getRouteController()
                            .getRouteStatus(route.getId());
                        return status == ServiceStatus.Started;
                    } catch (Exception e) {
                        return false;
                    }
                })
                .count();
            
            boolean allRoutesHealthy = healthyRoutes == routes.size();
            
            // Check error rate
            MetricsSummary summary = metricsCollector.getMetricsSummary();
            double errorRate = summary.totalExecutions() > 0 ? 
                (summary.failedExecutions() * 100.0) / summary.totalExecutions() : 0;
            
            boolean errorRateAcceptable = errorRate < 10; // Less than 10% error rate
            
            if (allRoutesHealthy && errorRateAcceptable) {
                return builder.up()
                    .withData("totalRoutes", routes.size())
                    .withData("healthyRoutes", healthyRoutes)
                    .withData("errorRate", errorRate)
                    .withData("totalExecutions", summary.totalExecutions())
                    .build();
            } else {
                return builder.down()
                    .withData("totalRoutes", routes.size())
                    .withData("healthyRoutes", healthyRoutes)
                    .withData("errorRate", errorRate)
                    .withData("reason", !allRoutesHealthy ? 
                        "Unhealthy routes detected" : "High error rate")
                    .build();
            }
            
        } catch (Exception e) {
            return builder.down()
                .withData("error", e.getMessage())
                .build();
        }
    }
}

// ==================== SLA MONITORING ====================

/**
 * SLA monitoring and alerting
 */
@ApplicationScoped
public class SLAMonitoringService {
    
    private static final Logger LOG = LoggerFactory.getLogger(SLAMonitoringService.class);
    
    @Inject
    CamelMetricsCollector metricsCollector;
    
    private final Map<String, SLADefinition> slaDefinitions = new ConcurrentHashMap<>();
    private final List<SLAViolation> violations = Collections.synchronizedList(new ArrayList<>());
    
    /**
     * Define SLA for a route
     */
    public void defineSLA(String routeId, SLADefinition sla) {
        slaDefinitions.put(routeId, sla);
        LOG.info("SLA defined for route {}: {}", routeId, sla);
    }
    
    /**
     * Check SLA compliance
     */
    @Scheduled(every = "1m")
    public void checkSLACompliance() {
        slaDefinitions.forEach((routeId, sla) -> {
            RouteMetricsData metrics = metricsCollector.getRouteMetrics(routeId);
            if (metrics != null) {
                
                // Check success rate
                if (metrics.getSuccessRate() < sla.minSuccessRate()) {
                    recordViolation(routeId, "SUCCESS_RATE", 
                        metrics.getSuccessRate(), sla.minSuccessRate());
                }
                
                // Check average duration
                if (metrics.getAverageDuration() > sla.maxAverageDuration()) {
                    recordViolation(routeId, "AVG_DURATION", 
                        metrics.getAverageDuration(), sla.maxAverageDuration());
                }
            }
        });
    }
    
    private void recordViolation(
            String routeId,
            String metric,
            double actualValue,
            double expectedValue) {
        
        SLAViolation violation = new SLAViolation(
            routeId,
            metric,
            actualValue,
            expectedValue,
            Instant.now()
        );
        
        violations.add(violation);
        LOG.warn("SLA violation detected: {}", violation);
        
        // Keep only last 1000 violations
        if (violations.size() > 1000) {
            violations.remove(0);
        }
    }
    
    public List<SLAViolation> getViolations() {
        return new ArrayList<>(violations);
    }
    
    public List<SLAViolation> getViolations(String routeId) {
        return violations.stream()
            .filter(v -> v.routeId().equals(routeId))
            .collect(Collectors.toList());
    }
}

record SLADefinition(
    double minSuccessRate,
    double maxAverageDuration,
    long maxConcurrentExecutions
) {}

record SLAViolation(
    String routeId,
    String metric,
    double actualValue,
    double expectedValue,
    Instant occurredAt
) {}

// ==================== AUDIT LOGGING ====================

/**
 * Comprehensive audit logging service
 */
@ApplicationScoped
public class CamelAuditService {
    
    private static final Logger LOG = LoggerFactory.getLogger(CamelAuditService.class);
    private static final Logger AUDIT_LOG = LoggerFactory.getLogger("AUDIT");
    
    private final List<AuditEntry> auditTrail = Collections.synchronizedList(new ArrayList<>());
    
    /**
     * Log route execution
     */
    public void logRouteExecution(
            String routeId,
            String tenantId,
            String userId,
            Map<String, Object> input,
            Map<String, Object> output,
            boolean success,
            Duration duration) {
        
        AuditEntry entry = new AuditEntry(
            UUID.randomUUID().toString(),
            "ROUTE_EXECUTION",
            routeId,
            tenantId,
            userId,
            Map.of(
                "input", input,
                "output", output,
                "success", success,
                "duration", duration.toMillis()
            ),
            Instant.now()
        );
        
        auditTrail.add(entry);
        AUDIT_LOG.info("Audit: {}", entry);
        
        // Keep only last 10000 entries
        if (auditTrail.size() > 10000) {
            auditTrail.remove(0);
        }
    }
    
    /**
     * Query audit trail
     */
    public List<AuditEntry> queryAuditTrail(
            String tenantId,
            String eventType,
            Instant from,
            Instant to) {
        
        return auditTrail.stream()
            .filter(e -> tenantId == null || e.tenantId().equals(tenantId))
            .filter(e -> eventType == null || e.eventType().equals(eventType))
            .filter(e -> from == null || e.timestamp().isAfter(from))
            .filter(e -> to == null || e.timestamp().isBefore(to))
            .collect(Collectors.toList());
    }
}

record AuditEntry(
    String entryId,
    String eventType,
    String resourceId,
    String tenantId,
    String userId,
    Map<String, Object> details,
    Instant timestamp
) {}

// ==================== MONITORING DASHBOARD API ====================

/**
 * REST API for monitoring dashboard
 */
@Path("/api/v1/monitoring")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class MonitoringDashboardAPI {
    
    @Inject
    CamelMetricsCollector metricsCollector;
    
    @Inject
    SLAMonitoringService slaMonitor;
    
    @Inject
    CamelAuditService auditService;
    
    @Inject
    CamelContext camelContext;
    
    /**
     * Get metrics summary
     */
    @GET
    @Path("/metrics/summary")
    public MetricsSummary getMetricsSummary() {
        return metricsCollector.getMetricsSummary();
    }
    
    /**
     * Get route metrics
     */
    @GET
    @Path("/metrics/routes/{routeId}")
    public RouteMetricsData getRouteMetrics(@PathParam("routeId") String routeId) {
        return metricsCollector.getRouteMetrics(routeId);
    }
    
    /**
     * Get all route metrics
     */
    @GET
    @Path("/metrics/routes")
    public Map<String, RouteMetricsData> getAllRouteMetrics() {
        return metricsCollector.getAllMetrics();
    }
    
    /**
     * Get SLA violations
     */
    @GET
    @Path("/sla/violations")
    public List<SLAViolation> getSLAViolations(
            @QueryParam("routeId") String routeId) {
        
        if (routeId != null) {
            return slaMonitor.getViolations(routeId);
        }
        return slaMonitor.getViolations();
    }
    
    /**
     * Get audit trail
     */
    @GET
    @Path("/audit")
    public List<AuditEntry> getAuditTrail(
            @QueryParam("tenantId") String tenantId,
            @QueryParam("eventType") String eventType,
            @QueryParam("from") Long fromTimestamp,
            @QueryParam("to") Long toTimestamp) {
        
        Instant from = fromTimestamp != null ? 
            Instant.ofEpochMilli(fromTimestamp) : null;
        Instant to = toTimestamp != null ? 
            Instant.ofEpochMilli(toTimestamp) : null;
        
        return auditService.queryAuditTrail(tenantId, eventType, from, to);
    }
    
    /**
     * Get route status
     */
    @GET
    @Path("/routes/status")
    public List<RouteStatus> getRouteStatuses() {
        return camelContext.getRoutes().stream()
            .map(route -> {
                try {
                    ServiceStatus status = camelContext.getRouteController()
                        .getRouteStatus(route.getId());
                    return new RouteStatus(
                        route.getId(),
                        status.name(),
                        route.getUptime(),
                        true
                    );
                } catch (Exception e) {
                    return new RouteStatus(
                        route.getId(),
                        "UNKNOWN",
                        "",
                        false
                    );
                }
            })
            .collect(Collectors.toList());
    }
    
    /**
     * Start route
     */
    @POST
    @Path("/routes/{routeId}/start")
    public Uni<RouteControlResponse> startRoute(@PathParam("routeId") String routeId) {
        return Uni.createFrom().completionStage(() -> 
            java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                try {
                    camelContext.getRouteController().startRoute(routeId);
                    return new RouteControlResponse(routeId, "STARTED", true, null);
                } catch (Exception e) {
                    return new RouteControlResponse(routeId, "ERROR", false, e.getMessage());
                }
            })
        );
    }
    
    /**
     * Stop route
     */
    @POST
    @Path("/routes/{routeId}/stop")
    public Uni<RouteControlResponse> stopRoute(@PathParam("routeId") String routeId) {
        return Uni.createFrom().completionStage(() -> 
            java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                try {
                    camelContext.getRouteController().stopRoute(routeId);
                    return new RouteControlResponse(routeId, "STOPPED", true, null);
                } catch (Exception e) {
                    return new RouteControlResponse(routeId, "ERROR", false, e.getMessage());
                }
            })
        );
    }
}

record RouteStatus(
    String routeId,
    String status,
    String uptime,
    boolean healthy
) {}

record RouteControlResponse(
    String routeId,
    String newStatus,
    boolean success,
    String errorMessage
) {}


package tech.kayys.silat.executor.camel.enterprise;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.*;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.aws2.s3.AWS2S3Constants;
import org.apache.camel.component.salesforce.SalesforceEndpointConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * ============================================================================
 * ENTERPRISE SYSTEM INTEGRATIONS
 * ============================================================================
 * 
 * Production-ready integrations for enterprise systems:
 * 1. SAP Integration (IDoc, BAPI, RFC)
 * 2. Salesforce Integration (REST, Bulk API, Streaming)
 * 3. AWS Services (S3, SQS, SNS, DynamoDB, Lambda)
 * 4. Azure Services (Blob Storage, Service Bus, Event Hub)
 * 5. Google Cloud Platform (Cloud Storage, Pub/Sub, BigQuery)
 * 6. Database Federation (Multi-DB queries and transactions)
 * 7. File Transfer Protocols (FTP, SFTP, AS2, OFTP2)
 * 8. Message Transformation (EDI, HL7, SWIFT, ISO20022)
 * 9. API Management Integration (Kong, Apigee, AWS API Gateway)
 * 10. Legacy System Adapters (Mainframe, COBOL, AS/400)
 */

// ==================== SALESFORCE INTEGRATION ====================

/**
 * Comprehensive Salesforce integration with bulk operations support
 */
@ApplicationScoped
public class SalesforceIntegrationService {
    
    private static final Logger LOG = LoggerFactory.getLogger(SalesforceIntegrationService.class);
    
    @Inject
    CamelContext camelContext;
    
    @Inject
    ProducerTemplate producerTemplate;
    
    /**
     * Query Salesforce objects with SOQL
     */
    public Uni<List<Map<String, Object>>> querySalesforce(
            String soqlQuery,
            String tenantId) {
        
        return Uni.createFrom().completionStage(() -> {
            CompletableFuture<List<Map<String, Object>>> future = new CompletableFuture<>();
            
            try {
                String routeId = "salesforce-query-" + UUID.randomUUID();
                
                camelContext.addRoutes(new RouteBuilder() {
                    @Override
                    public void configure() {
                        from("direct:" + routeId)
                            .routeId(routeId)
                            .setHeader("tenantId", constant(tenantId))
                            
                            // Execute SOQL query
                            .toD("salesforce:query?sObjectQuery=" + soqlQuery +
                                 "&clientId={{salesforce.client-id}}" +
                                 "&clientSecret={{salesforce.client-secret}}" +
                                 "&userName={{salesforce.username}}" +
                                 "&password={{salesforce.password}}")
                            
                            .process(exchange -> {
                                List<Map<String, Object>> results = exchange.getIn()
                                    .getBody(List.class);
                                future.complete(results);
                            });
                    }
                });
                
                camelContext.getRouteController().startRoute(routeId);
                producerTemplate.sendBody("direct:" + routeId, null);
                
            } catch (Exception e) {
                LOG.error("Salesforce query failed", e);
                future.completeExceptionally(e);
            }
            
            return future;
        });
    }
    
    /**
     * Create/Update Salesforce records with bulk API
     */
    public Uni<BulkOperationResult> bulkUpsert(
            String sObjectType,
            List<Map<String, Object>> records,
            String externalIdField,
            String tenantId) {
        
        return Uni.createFrom().completionStage(() -> {
            CompletableFuture<BulkOperationResult> future = new CompletableFuture<>();
            
            try {
                String routeId = "salesforce-bulk-" + UUID.randomUUID();
                
                camelContext.addRoutes(new RouteBuilder() {
                    @Override
                    public void configure() {
                        from("direct:" + routeId)
                            .routeId(routeId)
                            .setHeader("tenantId", constant(tenantId))
                            
                            // Use Bulk API 2.0 for better performance
                            .toD("salesforce:bulk2?sObjectName=" + sObjectType +
                                 "&operation=upsert" +
                                 "&externalIdFieldName=" + externalIdField +
                                 "&clientId={{salesforce.client-id}}" +
                                 "&clientSecret={{salesforce.client-secret}}")
                            
                            .process(exchange -> {
                                String jobId = exchange.getIn()
                                    .getHeader("jobId", String.class);
                                
                                BulkOperationResult result = new BulkOperationResult(
                                    jobId,
                                    records.size(),
                                    0,  // Will be updated via status check
                                    0,
                                    Instant.now()
                                );
                                
                                future.complete(result);
                            });
                    }
                });
                
                camelContext.getRouteController().startRoute(routeId);
                producerTemplate.sendBody("direct:" + routeId, records);
                
            } catch (Exception e) {
                LOG.error("Salesforce bulk upsert failed", e);
                future.completeExceptionally(e);
            }
            
            return future;
        });
    }
    
    /**
     * Subscribe to Salesforce platform events (real-time)
     */
    public Uni<Void> subscribeToPlatformEvents(
            String eventName,
            String callbackEndpoint,
            String tenantId) {
        
        return Uni.createFrom().completionStage(() -> {
            CompletableFuture<Void> future = new CompletableFuture<>();
            
            try {
                String routeId = "salesforce-subscribe-" + eventName;
                
                camelContext.addRoutes(new RouteBuilder() {
                    @Override
                    public void configure() {
                        // Subscribe to platform events
                        from("salesforce:subscribe:topic/" + eventName +
                             "?replayId=-1" +  // Get all events
                             "&clientId={{salesforce.client-id}}" +
                             "&clientSecret={{salesforce.client-secret}}")
                            .routeId(routeId)
                            .setHeader("tenantId", constant(tenantId))
                            .log("Received Salesforce event: ${body}")
                            
                            // Forward to callback endpoint
                            .to(callbackEndpoint);
                    }
                });
                
                camelContext.getRouteController().startRoute(routeId);
                future.complete(null);
                
            } catch (Exception e) {
                LOG.error("Salesforce subscription failed", e);
                future.completeExceptionally(e);
            }
            
            return future;
        });
    }
}

record BulkOperationResult(
    String jobId,
    int totalRecords,
    int successfulRecords,
    int failedRecords,
    Instant completedAt
) {}

// ==================== AWS SERVICES INTEGRATION ====================

/**
 * Comprehensive AWS services integration
 */
@ApplicationScoped
public class AWSIntegrationService {
    
    private static final Logger LOG = LoggerFactory.getLogger(AWSIntegrationService.class);
    
    @Inject
    CamelContext camelContext;
    
    @Inject
    ProducerTemplate producerTemplate;
    
    /**
     * Upload file to S3 with metadata
     */
    public Uni<S3UploadResult> uploadToS3(
            String bucketName,
            String key,
            byte[] content,
            Map<String, String> metadata,
            String tenantId) {
        
        return Uni.createFrom().completionStage(() -> {
            CompletableFuture<S3UploadResult> future = new CompletableFuture<>();
            
            try {
                String routeId = "aws-s3-upload-" + UUID.randomUUID();
                
                camelContext.addRoutes(new RouteBuilder() {
                    @Override
                    public void configure() {
                        from("direct:" + routeId)
                            .routeId(routeId)
                            .setHeader("tenantId", constant(tenantId))
                            .setHeader(AWS2S3Constants.KEY, constant(key))
                            .setHeader(AWS2S3Constants.CONTENT_LENGTH, constant(content.length))
                            
                            // Set custom metadata
                            .process(exchange -> {
                                metadata.forEach((k, v) -> 
                                    exchange.getIn().setHeader("CamelAwsS3Metadata" + k, v)
                                );
                            })
                            
                            // Upload to S3
                            .toD("aws2-s3://" + bucketName +
                                 "?region={{aws.region}}" +
                                 "&accessKey={{aws.access-key}}" +
                                 "&secretKey={{aws.secret-key}}")
                            
                            .process(exchange -> {
                                String etag = exchange.getIn()
                                    .getHeader(AWS2S3Constants.E_TAG, String.class);
                                String versionId = exchange.getIn()
                                    .getHeader(AWS2S3Constants.VERSION_ID, String.class);
                                
                                S3UploadResult result = new S3UploadResult(
                                    bucketName,
                                    key,
                                    etag,
                                    versionId,
                                    content.length,
                                    Instant.now()
                                );
                                
                                future.complete(result);
                            });
                    }
                });
                
                camelContext.getRouteController().startRoute(routeId);
                producerTemplate.sendBody("direct:" + routeId, content);
                
            } catch (Exception e) {
                LOG.error("S3 upload failed", e);
                future.completeExceptionally(e);
            }
            
            return future;
        });
    }
    
    /**
     * Process SQS messages with batch support
     */
    public Uni<Void> processSQSQueue(
            String queueName,
            String processorEndpoint,
            int batchSize,
            String tenantId) {
        
        return Uni.createFrom().completionStage(() -> {
            CompletableFuture<Void> future = new CompletableFuture<>();
            
            try {
                String routeId = "aws-sqs-consumer-" + queueName;
                
                camelContext.addRoutes(new RouteBuilder() {
                    @Override
                    public void configure() {
                        from("aws2-sqs://" + queueName +
                             "?region={{aws.region}}" +
                             "&accessKey={{aws.access-key}}" +
                             "&secretKey={{aws.secret-key}}" +
                             "&maxMessagesPerPoll=" + batchSize +
                             "&deleteAfterRead=true" +
                             "&waitTimeSeconds=20")  // Long polling
                            .routeId(routeId)
                            .setHeader("tenantId", constant(tenantId))
                            .log("Processing SQS message: ${body}")
                            
                            // Process message
                            .to(processorEndpoint)
                            
                            // Error handling
                            .onException(Exception.class)
                                .handled(true)
                                .log(LoggingLevel.ERROR, "Failed to process SQS message")
                                // Send to DLQ
                                .to("aws2-sqs://{{aws.dlq.queue-name}}")
                            .end();
                    }
                });
                
                camelContext.getRouteController().startRoute(routeId);
                future.complete(null);
                
            } catch (Exception e) {
                LOG.error("SQS consumer setup failed", e);
                future.completeExceptionally(e);
            }
            
            return future;
        });
    }
    
    /**
     * Invoke AWS Lambda function
     */
    public Uni<Object> invokeLambda(
            String functionName,
            Object payload,
            String tenantId) {
        
        return Uni.createFrom().completionStage(() -> {
            CompletableFuture<Object> future = new CompletableFuture<>();
            
            try {
                String routeId = "aws-lambda-" + UUID.randomUUID();
                
                camelContext.addRoutes(new RouteBuilder() {
                    @Override
                    public void configure() {
                        from("direct:" + routeId)
                            .routeId(routeId)
                            .setHeader("tenantId", constant(tenantId))
                            
                            // Invoke Lambda
                            .toD("aws2-lambda://" + functionName +
                                 "?region={{aws.region}}" +
                                 "&accessKey={{aws.access-key}}" +
                                 "&secretKey={{aws.secret-key}}" +
                                 "&operation=invokeFunction")
                            
                            .process(exchange -> {
                                Object result = exchange.getIn().getBody();
                                future.complete(result);
                            });
                    }
                });
                
                camelContext.getRouteController().startRoute(routeId);
                producerTemplate.sendBody("direct:" + routeId, payload);
                
            } catch (Exception e) {
                LOG.error("Lambda invocation failed", e);
                future.completeExceptionally(e);
            }
            
            return future;
        });
    }
    
    /**
     * Write to DynamoDB with auto-retry
     */
    public Uni<Void> writeToDynamoDB(
            String tableName,
            Map<String, Object> item,
            String tenantId) {
        
        return Uni.createFrom().completionStage(() -> {
            CompletableFuture<Void> future = new CompletableFuture<>();
            
            try {
                String routeId = "aws-dynamodb-write-" + UUID.randomUUID();
                
                camelContext.addRoutes(new RouteBuilder() {
                    @Override
                    public void configure() {
                        from("direct:" + routeId)
                            .routeId(routeId)
                            .setHeader("tenantId", constant(tenantId))
                            
                            // Circuit breaker for DynamoDB
                            .circuitBreaker()
                                .resilience4jConfiguration()
                                    .failureRateThreshold(50)
                                    .waitDurationInOpenState(30000)
                                .end()
                                
                                // Put item
                                .toD("aws2-dynamodb://" + tableName +
                                     "?region={{aws.region}}" +
                                     "&accessKey={{aws.access-key}}" +
                                     "&secretKey={{aws.secret-key}}" +
                                     "&operation=PutItem")
                                
                                // Fallback
                                .onFallback()
                                    .log("DynamoDB write failed, using fallback")
                                    .to("direct:dynamodb-fallback")
                            .end()
                            
                            .process(exchange -> future.complete(null));
                    }
                });
                
                camelContext.getRouteController().startRoute(routeId);
                producerTemplate.sendBody("direct:" + routeId, item);
                
            } catch (Exception e) {
                LOG.error("DynamoDB write failed", e);
                future.completeExceptionally(e);
            }
            
            return future;
        });
    }
}

record S3UploadResult(
    String bucket,
    String key,
    String etag,
    String versionId,
    long size,
    Instant uploadedAt
) {}

// ==================== AZURE SERVICES INTEGRATION ====================

/**
 * Microsoft Azure services integration
 */
@ApplicationScoped
public class AzureIntegrationService {
    
    private static final Logger LOG = LoggerFactory.getLogger(AzureIntegrationService.class);
    
    @Inject
    CamelContext camelContext;
    
    @Inject
    ProducerTemplate producerTemplate;
    
    /**
     * Upload blob to Azure Storage
     */
    public Uni<BlobUploadResult> uploadBlob(
            String containerName,
            String blobName,
            byte[] content,
            String tenantId) {
        
        return Uni.createFrom().completionStage(() -> {
            CompletableFuture<BlobUploadResult> future = new CompletableFuture<>();
            
            try {
                String routeId = "azure-blob-upload-" + UUID.randomUUID();
                
                camelContext.addRoutes(new RouteBuilder() {
                    @Override
                    public void configure() {
                        from("direct:" + routeId)
                            .routeId(routeId)
                            .setHeader("tenantId", constant(tenantId))
                            .setHeader("CamelAzureStorageBlobBlobName", constant(blobName))
                            
                            // Upload blob
                            .toD("azure-storage-blob://" +
                                 "{{azure.storage.account-name}}/" +
                                 containerName +
                                 "?credentials={{azure.storage.credentials}}" +
                                 "&operation=uploadBlob")
                            
                            .process(exchange -> {
                                BlobUploadResult result = new BlobUploadResult(
                                    containerName,
                                    blobName,
                                    content.length,
                                    Instant.now()
                                );
                                future.complete(result);
                            });
                    }
                });
                
                camelContext.getRouteController().startRoute(routeId);
                producerTemplate.sendBody("direct:" + routeId, content);
                
            } catch (Exception e) {
                LOG.error("Azure blob upload failed", e);
                future.completeExceptionally(e);
            }
            
            return future;
        });
    }
    
    /**
     * Process Azure Service Bus messages
     */
    public Uni<Void> processServiceBusQueue(
            String queueName,
            String processorEndpoint,
            String tenantId) {
        
        return Uni.createFrom().completionStage(() -> {
            CompletableFuture<Void> future = new CompletableFuture<>();
            
            try {
                String routeId = "azure-servicebus-" + queueName;
                
                camelContext.addRoutes(new RouteBuilder() {
                    @Override
                    public void configure() {
                        from("azure-servicebus:" + queueName +
                             "?connectionString={{azure.servicebus.connection-string}}")
                            .routeId(routeId)
                            .setHeader("tenantId", constant(tenantId))
                            .log("Processing Service Bus message: ${body}")
                            
                            // Process message
                            .to(processorEndpoint)
                            
                            // Auto-complete on success
                            .setHeader("CamelAzureServiceBusDisposition", 
                                constant("COMPLETED"));
                    }
                });
                
                camelContext.getRouteController().startRoute(routeId);
                future.complete(null);
                
            } catch (Exception e) {
                LOG.error("Azure Service Bus consumer setup failed", e);
                future.completeExceptionally(e);
            }
            
            return future;
        });
    }
    
    /**
     * Publish to Azure Event Hub
     */
    public Uni<Void> publishToEventHub(
            String eventHubName,
            List<Map<String, Object>> events,
            String tenantId) {
        
        return Uni.createFrom().completionStage(() -> {
            CompletableFuture<Void> future = new CompletableFuture<>();
            
            try {
                String routeId = "azure-eventhub-publish-" + UUID.randomUUID();
                
                camelContext.addRoutes(new RouteBuilder() {
                    @Override
                    public void configure() {
                        from("direct:" + routeId)
                            .routeId(routeId)
                            .setHeader("tenantId", constant(tenantId))
                            
                            // Split events for batch sending
                            .split(body())
                                .parallelProcessing()
                                
                                // Send to Event Hub
                                .toD("azure-eventhubs://" + eventHubName +
                                     "?connectionString={{azure.eventhub.connection-string}}")
                            .end()
                            
                            .process(exchange -> future.complete(null));
                    }
                });
                
                camelContext.getRouteController().startRoute(routeId);
                producerTemplate.sendBody("direct:" + routeId, events);
                
            } catch (Exception e) {
                LOG.error("Azure Event Hub publish failed", e);
                future.completeExceptionally(e);
            }
            
            return future;
        });
    }
}

record BlobUploadResult(
    String container,
    String blobName,
    long size,
    Instant uploadedAt
) {}

// ==================== GOOGLE CLOUD PLATFORM INTEGRATION ====================

/**
 * Google Cloud Platform services integration
 */
@ApplicationScoped
public class GCPIntegrationService {
    
    private static final Logger LOG = LoggerFactory.getLogger(GCPIntegrationService.class);
    
    @Inject
    CamelContext camelContext;
    
    @Inject
    ProducerTemplate producerTemplate;
    
    /**
     * Upload to Google Cloud Storage
     */
    public Uni<GCSUploadResult> uploadToGCS(
            String bucketName,
            String objectName,
            byte[] content,
            String tenantId) {
        
        return Uni.createFrom().completionStage(() -> {
            CompletableFuture<GCSUploadResult> future = new CompletableFuture<>();
            
            try {
                String routeId = "gcp-gcs-upload-" + UUID.randomUUID();
                
                camelContext.addRoutes(new RouteBuilder() {
                    @Override
                    public void configure() {
                        from("direct:" + routeId)
                            .routeId(routeId)
                            .setHeader("tenantId", constant(tenantId))
                            .setHeader("CamelGoogleCloudStorageObjectName", constant(objectName))
                            
                            // Upload to GCS
                            .toD("google-storage://" + bucketName +
                                 "?serviceAccountKey={{gcp.service-account-key}}")
                            
                            .process(exchange -> {
                                GCSUploadResult result = new GCSUploadResult(
                                    bucketName,
                                    objectName,
                                    content.length,
                                    Instant.now()
                                );
                                future.complete(result);
                            });
                    }
                });
                
                camelContext.getRouteController().startRoute(routeId);
                producerTemplate.sendBody("direct:" + routeId, content);
                
            } catch (Exception e) {
                LOG.error("GCS upload failed", e);
                future.completeExceptionally(e);
            }
            
            return future;
        });
    }
    
    /**
     * Publish to Google Pub/Sub
     */
    public Uni<Void> publishToPubSub(
            String topicName,
            List<Map<String, Object>> messages,
            String tenantId) {
        
        return Uni.createFrom().completionStage(() -> {
            CompletableFuture<Void> future = new CompletableFuture<>();
            
            try {
                String routeId = "gcp-pubsub-publish-" + UUID.randomUUID();
                
                camelContext.addRoutes(new RouteBuilder() {
                    @Override
                    public void configure() {
                        from("direct:" + routeId)
                            .routeId(routeId)
                            .setHeader("tenantId", constant(tenantId))
                            
                            // Split messages
                            .split(body())
                                .parallelProcessing()
                                
                                // Publish to Pub/Sub
                                .toD("google-pubsub://{{gcp.project-id}}:" + topicName +
                                     "?serviceAccountKey={{gcp.service-account-key}}")
                            .end()
                            
                            .process(exchange -> future.complete(null));
                    }
                });
                
                camelContext.getRouteController().startRoute(routeId);
                producerTemplate.sendBody("direct:" + routeId, messages);
                
            } catch (Exception e) {
                LOG.error("Pub/Sub publish failed", e);
                future.completeExceptionally(e);
            }
            
            return future;
        });
    }
    
    /**
     * Query BigQuery
     */
    public Uni<List<Map<String, Object>>> queryBigQuery(
            String sql,
            String tenantId) {
        
        return Uni.createFrom().completionStage(() -> {
            CompletableFuture<List<Map<String, Object>>> future = new CompletableFuture<>();
            
            try {
                String routeId = "gcp-bigquery-query-" + UUID.randomUUID();
                
                camelContext.addRoutes(new RouteBuilder() {
                    @Override
                    public void configure() {
                        from("direct:" + routeId)
                            .routeId(routeId)
                            .setHeader("tenantId", constant(tenantId))
                            .setHeader("CamelGoogleBigQueryQuery", constant(sql))
                            
                            // Execute query
                            .toD("google-bigquery://{{gcp.project-id}}" +
                                 "?serviceAccountKey={{gcp.service-account-key}}")
                            
                            .process(exchange -> {
                                List<Map<String, Object>> results = exchange.getIn()
                                    .getBody(List.class);
                                future.complete(results);
                            });
                    }
                });
                
                camelContext.getRouteController().startRoute(routeId);
                producerTemplate.sendBody("direct:" + routeId, null);
                
            } catch (Exception e) {
                LOG.error("BigQuery query failed", e);
                future.completeExceptionally(e);
            }
            
            return future;
        });
    }
}

record GCSUploadResult(
    String bucket,
    String objectName,
    long size,
    Instant uploadedAt
) {}

// ==================== DATABASE FEDERATION ====================

/**
 * Multi-database query and transaction management
 */
@ApplicationScoped
public class DatabaseFederationService {
    
    private static final Logger LOG = LoggerFactory.getLogger(DatabaseFederationService.class);
    
    @Inject
    CamelContext camelContext;
    
    @Inject
    ProducerTemplate producerTemplate;
    
    /**
     * Execute federated query across multiple databases
     */
    public Uni<FederatedQueryResult> executeFederatedQuery(
            List<DatabaseQuery> queries,
            String tenantId) {
        
        return Uni.createFrom().completionStage(() -> {
            CompletableFuture<FederatedQueryResult> future = new CompletableFuture<>();
            
            try {
                String routeId = "federated-query-" + UUID.randomUUID();
                
                camelContext.addRoutes(new RouteBuilder() {
                    @Override
                    public void configure() {
                        from("direct:" + routeId)
                            .routeId(routeId)
                            .setHeader("tenantId", constant(tenantId))
                            
                            // Execute queries in parallel
                            .split(body())
                                .parallelProcessing()
                                .aggregationStrategy((oldExchange, newExchange) -> {
                                    if (oldExchange == null) {
                                        newExchange.setProperty("results", new ArrayList<>());
                                        return newExchange;
                                    }
                                    
                                    List<Object> results = oldExchange.getProperty("results", List.class);
                                    results.add(newExchange.getIn().getBody());
                                    oldExchange.setProperty("results", results);
                                    return oldExchange;
                                })
                                
                                // Route to appropriate database
                                .process(exchange -> {
                                    DatabaseQuery query = exchange.getIn()
                                        .getBody(DatabaseQuery.class);
                                    exchange.getIn().setHeader("databaseType", query.type());
                                    exchange.getIn().setHeader("query", query.sql());
                                })
                                
                                .choice()
                                    .when(header("databaseType").isEqualTo("POSTGRESQL"))
                                        .to("jdbc:postgresDataSource")
                                    .when(header("databaseType").isEqualTo("MYSQL"))
                                        .to("jdbc:mysqlDataSource")
                                    .when(header("databaseType").isEqualTo("MONGODB"))
                                        .toD("mongodb:{{mongodb.host}}")
                                    .when(header("databaseType").isEqualTo("REDIS"))
                                        .toD("redis:{{redis.host}}")
                                .end()
                            .end()
                            
                            .process(exchange -> {
                                List<Object> results = exchange.getProperty("results", List.class);
                                FederatedQueryResult result = new FederatedQueryResult(
                                    queries.size(),
                                    results,
                                    Instant.now()
                                );
                                future.complete(result);
                            });
                    }
                });
                
                camelContext.getRouteController().startRoute(routeId);
                producerTemplate.sendBody("direct:" + routeId, queries);
                
            } catch (Exception e) {
                LOG.error("Federated query failed", e);
                future.completeExceptionally(e);
            }
            
            return future;
        });
    }
    
    /**
     * Execute distributed transaction across multiple databases
     */
    public Uni<Void> executeDistributedTransaction(
            List<DatabaseOperation> operations,
            String tenantId) {
        
        return Uni.createFrom().completionStage(() -> {
            CompletableFuture<Void> future = new CompletableFuture<>();
            
            try {
                String routeId = "distributed-tx-" + UUID.randomUUID();
                
                camelContext.addRoutes(new RouteBuilder() {
                    @Override
                    public void configure() {
                        from("direct:" + routeId)
                            .routeId(routeId)
                            .setHeader("tenantId", constant(tenantId))
                            
                            // Use Saga pattern for distributed transaction
                            .saga()
                                .timeout(Duration.ofMinutes(5))
                                .compensation("direct:compensate-" + routeId)
                                
                                // Execute each operation
                                .split(body())
                                    .process(exchange -> {
                                        DatabaseOperation op = exchange.getIn()
                                            .getBody(DatabaseOperation.class);
                                        exchange.getIn().setHeader("operationType", op.type());
                                        exchange.getIn().setHeader("operationSql", op.sql());
                                    })
                                    
                                    .toD("jdbc:${header.operationType}DataSource")
                                        .compensation("direct:compensate-op-" + routeId)
                                .end()
                            .end()
                            
                            .process(exchange -> future.complete(null));
                        
                        // Compensation routes
                        from("direct:compensate-" + routeId)
                            .log("Compensating distributed transaction");
                        
                        from("direct:compensate-op-" + routeId)
                            .log("Compensating database operation");
                    }
                });
                
                camelContext.getRouteController().startRoute(routeId);
                producerTemplate.sendBody("direct:" + routeId, operations);
                
            } catch (Exception e) {
                LOG.error("Distributed transaction failed", e);
                future.completeExceptionally(e);
            }
            
            return future;
        });
    }
}

record DatabaseQuery(
    String type,
    String sql,
    Map<String, Object> parameters
) {}

record DatabaseOperation(
    String type,
    String sql,
    Map<String, Object> parameters,
    String compensationSql
) {}

record FederatedQueryResult(
    int queryCount,
    List<Object> results,
    Instant executedAt
) {}

// ==================== FILE TRANSFER PROTOCOLS ====================

/**
 * Secure file transfer protocol implementations
 */
@ApplicationScoped
public class FileTransferService {
    
    private static final Logger LOG = LoggerFactory.getLogger(FileTransferService.class);
    
    @Inject
    CamelContext camelContext;
    
    @Inject
    ProducerTemplate producerTemplate;
    
    /**
     * SFTP file transfer with automatic retry
     */
    public Uni<FileTransferResult> transferViaSFTP(
            String localPath,
            String remotePath,
            SFTPConfig config,
            String tenantId) {
        
        return Uni.createFrom().completionStage(() -> {
            CompletableFuture<FileTransferResult> future = new CompletableFuture<>();
            
            try {
                String routeId = "sftp-transfer-" + UUID.randomUUID();
                
                camelContext.addRoutes(new RouteBuilder() {
                    @Override
                    public void configure() {
                        from("file:" + localPath + "?delete=true")
                            .routeId(routeId)
                            .setHeader("tenantId", constant(tenantId))
                            
                            // Retry configuration
                            .errorHandler(deadLetterChannel("direct:sftp-error")
                                .maximumRedeliveries(3)
                                .redeliveryDelay(5000)
                                .useExponentialBackOff()
                                .backOffMultiplier(2))
                            
                            // Transfer via SFTP
                            .toD("sftp://" + config.host() + remotePath +
                                 "?username=" + config.username() +
                                 "&password=" + config.password() +
                                 "&knownHostsFile={{sftp.known-hosts}}" +
                                 "&strictHostKeyChecking=yes")
                            
                            .process(exchange -> {
                                String fileName = exchange.getIn()
                                    .getHeader("CamelFileName", String.class);
                                
                                FileTransferResult result = new FileTransferResult(
                                    fileName,
                                    "SFTP",
                                    config.host(),
                                    remotePath,
                                    true,
                                    Instant.now()
                                );
                                
                                future.complete(result);
                            });
                        
                        // Error handling
                        from("direct:sftp-error")
                            .log(LoggingLevel.ERROR, "SFTP transfer failed")
                            .process(exchange -> {
                                FileTransferResult result = new FileTransferResult(
                                    "",
                                    "SFTP",
                                    config.host(),
                                    remotePath,
                                    false,
                                    Instant.now()
                                );
                                future.complete(result);
                            });
                    }
                });
                
                camelContext.getRouteController().startRoute(routeId);
                
            } catch (Exception e) {
                LOG.error("SFTP transfer failed", e);
                future.completeExceptionally(e);
            }
            
            return future;
        });
    }
    
    /**
     * AS2 (Applicability Statement 2) file transfer
     */
    public Uni<FileTransferResult> transferViaAS2(
            byte[] content,
            AS2Config config,
            String tenantId) {
        
        return Uni.createFrom().completionStage(() -> {
            CompletableFuture<FileTransferResult> future = new CompletableFuture<>();
            
            try {
                String routeId = "as2-transfer-" + UUID.randomUUID();
                
                camelContext.addRoutes(new RouteBuilder() {
                    @Override
                    public void configure() {
                        from("direct:" + routeId)
                            .routeId(routeId)
                            .setHeader("tenantId", constant(tenantId))
                            .setHeader("CamelAS2.requestUri", constant(config.partnerUrl()))
                            .setHeader("CamelAS2.subject", constant(config.subject()))
                            .setHeader("CamelAS2.from", constant(config.fromAS2Id()))
                            .setHeader("CamelAS2.as2To", constant(config.toAS2Id()))
                            
                            // Send via AS2
                            .to("as2://client/send")
                            
                            .process(exchange -> {
                                FileTransferResult result = new FileTransferResult(
                                    config.subject(),
                                    "AS2",
                                    config.partnerUrl(),
                                    "",
                                    true,
                                    Instant.now()
                                );
                                future.complete(result);
                            });
                    }
                });
                
                camelContext.getRouteController().startRoute(routeId);
                producerTemplate.sendBody("direct:" + routeId, content);
                
            } catch (Exception e) {
                LOG.error("AS2 transfer failed", e);
                future.completeExceptionally(e);
            }
            
            return future;
        });
    }
}

record SFTPConfig(
    String host,
    int port,
    String username,
    String password,
    String privateKey
) {}

record AS2Config(
    String fromAS2Id,
    String toAS2Id,
    String partnerUrl,
    String subject,
    byte[] certificate
) {}

record FileTransferResult(
    String fileName,
    String protocol,
    String remoteHost,
    String remotePath,
    boolean success,
    Instant transferredAt
) {}

// ==================== MESSAGE TRANSFORMATION ====================

/**
 * Industry standard message format transformations
 */
@ApplicationScoped
public class MessageTransformationService {
    
    private static final Logger LOG = LoggerFactory.getLogger(MessageTransformationService.class);
    
    @Inject
    CamelContext camelContext;
    
    @Inject
    ProducerTemplate producerTemplate;
    
    /**
     * Transform EDI (X12, EDIFACT) to JSON
     */
    public Uni<Object> transformEDIToJSON(
            String ediMessage,
            String ediStandard,
            String tenantId) {
        
        return Uni.createFrom().completionStage(() -> {
            CompletableFuture<Object> future = new CompletableFuture<>();
            
            try {
                String routeId = "edi-transform-" + UUID.randomUUID();
                
                camelContext.addRoutes(new RouteBuilder() {
                    @Override
                    public void configure() {
                        from("direct:" + routeId)
                            .routeId(routeId)
                            .setHeader("tenantId", constant(tenantId))
                            
                            // Parse EDI
                            .unmarshal().edi(ediStandard)
                            
                            // Convert to JSON
                            .marshal().json()
                            
                            .process(exchange -> {
                                Object result = exchange.getIn().getBody();
                                future.complete(result);
                            });
                    }
                });
                
                camelContext.getRouteController().startRoute(routeId);
                producerTemplate.sendBody("direct:" + routeId, ediMessage);
                
            } catch (Exception e) {
                LOG.error("EDI transformation failed", e);
                future.completeExceptionally(e);
            }
            
            return future;
        });
    }
    
    /**
     * Transform HL7 message
     */
    public Uni<Object> transformHL7(
            String hl7Message,
            String targetFormat,
            String tenantId) {
        
        return Uni.createFrom().completionStage(() -> {
            CompletableFuture<Object> future = new CompletableFuture<>();
            
            try {
                String routeId = "hl7-transform-" + UUID.randomUUID();
                
                camelContext.addRoutes(new RouteBuilder() {
                    @Override
                    public void configure() {
                        from("direct:" + routeId)
                            .routeId(routeId)
                            .setHeader("tenantId", constant(tenantId))
                            
                            // Parse HL7
                            .unmarshal().hl7()
                            
                            // Transform based on target format
                            .choice()
                                .when(simple("${header.targetFormat} == 'json'"))
                                    .marshal().json()
                                .when(simple("${header.targetFormat} == 'xml'"))
                                    .marshal().jacksonXml()
                            .end()
                            
                            .process(exchange -> {
                                Object result = exchange.getIn().getBody();
                                future.complete(result);
                            });
                    }
                });
                
                camelContext.getRouteController().startRoute(routeId);
                producerTemplate.sendBodyAndHeader("direct:" + routeId, 
                    hl7Message, "targetFormat", targetFormat);
                
            } catch (Exception e) {
                LOG.error("HL7 transformation failed", e);
                future.completeExceptionally(e);
            }
            
            return future;
        });
    }
    
    /**
     * Transform SWIFT message (ISO 15022/20022)
     */
    public Uni<Object> transformSWIFT(
            String swiftMessage,
            String messageType,
            String tenantId) {
        
        return Uni.createFrom().completionStage(() -> {
            CompletableFuture<Object> future = new CompletableFuture<>();
            
            try {
                String routeId = "swift-transform-" + UUID.randomUUID();
                
                camelContext.addRoutes(new RouteBuilder() {
                    @Override
                    public void configure() {
                        from("direct:" + routeId)
                            .routeId(routeId)
                            .setHeader("tenantId", constant(tenantId))
                            
                            // Parse SWIFT message
                            // Custom processor for SWIFT format
                            .process(exchange -> {
                                String message = exchange.getIn().getBody(String.class);
                                // SWIFT parsing logic
                                Map<String, Object> parsed = parseSWIFTMessage(message, messageType);
                                exchange.getIn().setBody(parsed);
                            })
                            
                            // Convert to JSON
                            .marshal().json()
                            
                            .process(exchange -> {
                                Object result = exchange.getIn().getBody();
                                future.complete(result);
                            });
                    }
                });
                
                camelContext.getRouteController().startRoute(routeId);
                producerTemplate.sendBody("direct:" + routeId, swiftMessage);
                
            } catch (Exception e) {
                LOG.error("SWIFT transformation failed", e);
                future.completeExceptionally(e);
            }
            
            return future;
        });
    }
    
    private Map<String, Object> parseSWIFTMessage(String message, String messageType) {
        // Simplified SWIFT parser
        Map<String, Object> parsed = new HashMap<>();
        parsed.put("messageType", messageType);
        parsed.put("rawMessage", message);
        // Add actual parsing logic here
        return parsed;
    }
}

// ==================== API MANAGEMENT INTEGRATION ====================

/**
 * Integration with API management platforms
 */
@ApplicationScoped
public class APIManagementService {
    
    private static final Logger LOG = LoggerFactory.getLogger(APIManagementService.class);
    
    @Inject
    CamelContext camelContext;
    
    @Inject
    ProducerTemplate producerTemplate;
    
    /**
     * Register API with Kong Gateway
     */
    public Uni<APIRegistrationResult> registerWithKong(
            APIDefinition apiDef,
            String tenantId) {
        
        return Uni.createFrom().completionStage(() -> {
            CompletableFuture<APIRegistrationResult> future = new CompletableFuture<>();
            
            try {
                String routeId = "kong-register-" + UUID.randomUUID();
                
                camelContext.addRoutes(new RouteBuilder() {
                    @Override
                    public void configure() {
                        from("direct:" + routeId)
                            .routeId(routeId)
                            .setHeader("tenantId", constant(tenantId))
                            .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                            .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                            
                            // Register service
                            .toD("http://{{kong.admin.host}}/services")
                            
                            // Register route
                            .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                            .toD("http://{{kong.admin.host}}/services/${header.serviceId}/routes")
                            
                            .process(exchange -> {
                                APIRegistrationResult result = new APIRegistrationResult(
                                    apiDef.name(),
                                    "Kong",
                                    exchange.getIn().getHeader("serviceId", String.class),
                                    true,
                                    Instant.now()
                                );
                                future.complete(result);
                            });
                    }
                });
                
                camelContext.getRouteController().startRoute(routeId);
                producerTemplate.sendBody("direct:" + routeId, apiDef);
                
            } catch (Exception e) {
                LOG.error("Kong registration failed", e);
                future.completeExceptionally(e);
            }
            
            return future;
        });
    }
}

record APIDefinition(
    String name,
    String upstreamUrl,
    List<String> paths,
    Map<String, Object> plugins
) {}

record APIRegistrationResult(
    String apiName,
    String gateway,
    String apiId,
    boolean success,
    Instant registeredAt
) {}

--------
# ============================================================================
# SILAT CAMEL INTEGRATION EXECUTOR CONFIGURATION
# ============================================================================

# Application
quarkus.application.name=silat-executor-camel
quarkus.application.version=1.0.0

# HTTP Server
quarkus.http.port=8082
quarkus.http.host=0.0.0.0

# Logging
quarkus.log.level=INFO
quarkus.log.category."tech.kayys.silat".level=DEBUG
quarkus.log.category."org.apache.camel".level=INFO
quarkus.log.console.format=%d{HH:mm:ss} %-5p [%c{2.}] (%t) %s%e%n

# ==================== CAMEL CONFIGURATION ====================

# Camel Context
camel.context.name=silat-integration-context
camel.context.stream-caching=true
camel.context.tracing=true
camel.context.message-history=true
camel.context.load-type-converters=true

# Camel Main
camel.main.auto-startup=true
camel.main.duration-max-messages=0
camel.main.duration-max-idle-seconds=0
camel.main.shutdown-timeout=30
camel.main.shutdown-suppress-logging-on-timeout=false

# Route Configuration
camel.main.routes-include-pattern=classpath:camel/routes/*.xml,classpath:camel/routes/*.java
camel.main.auto-configuration=true

# Thread Pools
camel.threadpool.pool-size=10
camel.threadpool.max-pool-size=50
camel.threadpool.max-queue-size=1000
camel.threadpool.keep-alive-time=60
camel.threadpool.time-unit=SECONDS
camel.threadpool.allow-core-thread-timeout=true

# Error Handling
camel.error-handler.maximum-redeliveries=3
camel.error-handler.redelivery-delay=1000
camel.error-handler.backoff-multiplier=2
camel.error-handler.maximum-redelivery-delay=60000
camel.error-handler.use-exponential-backoff=true

# Health Check
camel.health.enabled=true
camel.health.check.routes.enabled=true
camel.health.check.registry.enabled=true
camel.health.check.consumers.enabled=true

# Metrics
camel.metrics.enabled=true
camel.metrics.route-policy-enabled=true

# ==================== COMPONENT CONFIGURATIONS ====================

# HTTP Component
camel.component.http.connection-timeout=30000
camel.component.http.socket-timeout=60000
camel.component.http.max-total-connections=200
camel.component.http.connections-per-route=20
camel.component.http.connection-request-timeout=10000

# Kafka Component
camel.component.kafka.brokers=localhost:9092
camel.component.kafka.consumer-streams=1
camel.component.kafka.max-poll-records=500
camel.component.kafka.session-timeout-ms=30000
camel.component.kafka.request-timeout-ms=60000
camel.component.kafka.auto-offset-reset=earliest
camel.component.kafka.enable-auto-commit=true
camel.component.kafka.allow-manual-commit=false

# JDBC Component
camel.component.jdbc.data-source=#bean:dataSource
camel.component.jdbc.use-headers-as-parameters=true

# File Component
camel.component.file.buffer-size=131072
camel.component.file.read-lock=changed
camel.component.file.read-lock-check-interval=5000
camel.component.file.read-lock-timeout=30000

# FTP Component
camel.component.ftp.connect-timeout=30000
camel.component.ftp.socket-timeout=60000
camel.component.ftp.passive-mode=true

# REST Component
camel.component.rest.host=0.0.0.0
camel.component.rest.port=8082
camel.component.rest.binding-mode=json
camel.component.rest.enable-cors=true

# ==================== SILAT SPECIFIC CONFIGURATION ====================

# Silat Engine Connection
silat.engine.grpc.host=localhost
silat.engine.grpc.port=9090
silat.engine.rest.url=http://localhost:8080
silat.engine.communication-type=GRPC

# Executor Configuration
silat.executor.id=${EXECUTOR_ID:camel-executor-${random.uuid}}
silat.executor.type=camel-integration
silat.executor.max-concurrent-tasks=50
silat.executor.heartbeat-interval=10000
silat.executor.registration-retry-interval=5000

# Tenant Configuration
silat.tenant.isolation.enabled=true
silat.tenant.default-id=default-tenant

# Integration Defaults
silat.camel.default-timeout=30000
silat.camel.enable-auto-cleanup=true
silat.camel.route-cleanup-delay=300000
silat.camel.max-active-routes=100

# Cache Configuration
silat.camel.cache.enabled=true
silat.camel.cache.max-size=1000
silat.camel.cache.ttl=300

# Performance Tuning
silat.camel.throttle.max-requests-per-minute=1000
silat.camel.circuit-breaker.threshold=5
silat.camel.circuit-breaker.half-open-after=30000

# ==================== DATA SOURCE CONFIGURATION ====================

# PostgreSQL (for idempotency, saga, etc.)
quarkus.datasource.db-kind=postgresql
quarkus.datasource.username=silat
quarkus.datasource.password=silat123
quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/silat
quarkus.datasource.jdbc.min-size=5
quarkus.datasource.jdbc.max-size=20

# Flyway Migration
quarkus.flyway.migrate-at-start=true
quarkus.flyway.locations=classpath:db/migration

# ==================== OBSERVABILITY ====================

# Metrics
quarkus.micrometer.enabled=true
quarkus.micrometer.registry-enabled-default=true
quarkus.micrometer.export.prometheus.enabled=true
quarkus.micrometer.export.prometheus.path=/metrics

# Health
quarkus.health.extensions.enabled=true
quarkus.health.openapi.included=true

# OpenTelemetry
quarkus.opentelemetry.enabled=true
quarkus.opentelemetry.tracer.exporter.otlp.endpoint=http://localhost:4317

# ==================== SECURITY ====================

# TLS/SSL
quarkus.http.ssl.certificate.key-files=
quarkus.http.ssl.certificate.files=
quarkus.http.ssl-port=8443

# Authentication
silat.executor.api-key=${SILAT_API_KEY:}
silat.executor.enable-auth=true

# ==================== CLUSTERING & HIGH AVAILABILITY ====================

# Hazelcast (for distributed idempotency, locks, etc.)
quarkus.hazelcast-client.cluster-name=silat-cluster
quarkus.hazelcast-client.cluster-members=localhost:5701

# ==================== PROFILE-SPECIFIC CONFIGS ====================

# Development
%dev.quarkus.log.level=DEBUG
%dev.camel.context.tracing=true
%dev.silat.engine.grpc.host=localhost
%dev.silat.engine.grpc.port=9090

# Testing
%test.quarkus.log.level=WARN
%test.camel.context.stream-caching=false
%test.silat.engine.grpc.host=localhost
%test.silat.engine.grpc.port=19090

# Production
%prod.quarkus.log.level=INFO
%prod.camel.context.tracing=false
%prod.silat.camel.max-active-routes=500
%prod.silat.executor.max-concurrent-tasks=100

# ==================== NATIVE BUILD ====================

quarkus.native.resources.includes=camel/**,db/migration/**
quarkus.native.additional-build-args=--initialize-at-run-time=org.apache.camel

# ==================== ADVANCED FEATURES ====================

# Saga LRA (Long Running Actions)
camel.saga.lra.coordinator-url=http://localhost:8080/lra-coordinator

# Content Enricher Cache
silat.camel.enricher.cache-ttl=600

# Dead Letter Queue
silat.camel.dlq.endpoint=kafka:dlq-topic?brokers=localhost:9092
silat.camel.dlq.max-redeliveries=3

# Wire Tap
silat.camel.wiretap.enabled=true
silat.camel.wiretap.endpoint=log:wiretap?level=DEBUG

# Claim Check
silat.camel.claimcheck.storage=redis
silat.camel.claimcheck.ttl=3600

# Load Balancing
silat.camel.loadbalancer.strategy=ROUND_ROBIN
silat.camel.loadbalancer.max-attempts=3

# ==================== CUSTOM ENDPOINTS ====================

# Custom REST APIs
silat.camel.custom.api.base-url=${CUSTOM_API_URL:}
silat.camel.custom.api.api-key=${CUSTOM_API_KEY:}
silat.camel.custom.api.timeout=30000

# Custom Kafka Topics
silat.camel.custom.kafka.input-topic=silat-integration-input
silat.camel.custom.kafka.output-topic=silat-integration-output
silat.camel.custom.kafka.error-topic=silat-integration-error

# Custom Database
silat.camel.custom.db.connection-string=${CUSTOM_DB_URL:}
silat.camel.custom.db.username=${CUSTOM_DB_USER:}
silat.camel.custom.db.password=${CUSTOM_DB_PASSWORD:}

--------



package tech.kayys.silat.executor.camel.advanced;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.quarkus.cache.CacheResult;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.*;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.spi.RoutePolicy;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ============================================================================
 * ADVANCED CAMEL FEATURES & ENHANCEMENTS
 * ============================================================================
 * 
 * Enterprise-grade features:
 * 1. Dynamic Route Template System
 * 2. Advanced Error Handling & Retry Strategies
 * 3. Performance Monitoring & Metrics
 * 4. Circuit Breaker with Bulkhead Pattern
 * 5. Message Throttling & Rate Limiting
 * 6. Idempotent Consumer Pattern
 * 7. Claim Check Pattern
 * 8. Request-Reply Pattern with Correlation
 * 9. Load Balancing Strategies
 * 10. Transaction Management
 * 11. Compensating Transaction (Saga)
 * 12. Service Orchestration
 */

// ==================== DYNAMIC ROUTE TEMPLATE SYSTEM ====================

/**
 * Route template manager for reusable route patterns
 */
@ApplicationScoped
public class CamelRouteTemplateManager {
    
    private static final Logger LOG = LoggerFactory.getLogger(CamelRouteTemplateManager.class);
    
    @Inject
    CamelContext camelContext;
    
    private final Map<String, RouteTemplateDefinition> templates = new ConcurrentHashMap<>();
    
    /**
     * Register a route template
     */
    public void registerTemplate(String templateId, RouteTemplateDefinition template) {
        templates.put(templateId, template);
        LOG.info("Registered route template: {}", templateId);
    }
    
    /**
     * Create route from template with parameters
     */
    public String createFromTemplate(
            String templateId,
            String routeId,
            Map<String, Object> parameters) throws Exception {
        
        RouteTemplateDefinition template = templates.get(templateId);
        if (template == null) {
            throw new IllegalArgumentException("Template not found: " + templateId);
        }
        
        // Create route from template
        camelContext.addRouteFromTemplate(routeId, templateId, parameters);
        camelContext.getRouteController().startRoute(routeId);
        
        LOG.info("Created route {} from template {}", routeId, templateId);
        return routeId;
    }
    
    /**
     * Pre-built templates
     */
    public void initializeDefaultTemplates() throws Exception {
        
        // REST API Integration Template
        camelContext.addRouteTemplateDefinitions(
            new RouteBuilder() {
                @Override
                public void configure() {
                    routeTemplate("rest-api-integration")
                        .templateParameter("url")
                        .templateParameter("method", "GET")
                        .templateParameter("timeout", "30000")
                        .from("direct:{{routeId}}")
                        .setHeader(Exchange.HTTP_METHOD, constant("{{method}}"))
                        .setHeader("Content-Type", constant("application/json"))
                        .toD("http:{{url}}?timeout={{timeout}}")
                        .convertBodyTo(String.class);
                }
            }
        );
        
        // Database Query Template
        camelContext.addRouteTemplateDefinitions(
            new RouteBuilder() {
                @Override
                public void configure() {
                    routeTemplate("database-query")
                        .templateParameter("dataSource")
                        .templateParameter("sql")
                        .from("direct:{{routeId}}")
                        .toD("jdbc:{{dataSource}}?useHeadersAsParameters=true")
                        .process(exchange -> {
                            List<Map<String, Object>> result = exchange.getIn()
                                .getBody(List.class);
                            exchange.getIn().setBody(result);
                        });
                }
            }
        );
        
        // Kafka Producer Template
        camelContext.addRouteTemplateDefinitions(
            new RouteBuilder() {
                @Override
                public void configure() {
                    routeTemplate("kafka-producer")
                        .templateParameter("topic")
                        .templateParameter("brokers")
                        .templateParameter("key")
                        .from("direct:{{routeId}}")
                        .setHeader("kafka.KEY", simple("{{key}}"))
                        .toD("kafka:{{topic}}?brokers={{brokers}}");
                }
            }
        );
        
        // File Processing Template
        camelContext.addRouteTemplateDefinitions(
            new RouteBuilder() {
                @Override
                public void configure() {
                    routeTemplate("file-processor")
                        .templateParameter("inputDir")
                        .templateParameter("outputDir")
                        .templateParameter("pattern", "*")
                        .from("file:{{inputDir}}?include={{pattern}}&delete=true")
                        .log("Processing file: ${header.CamelFileName}")
                        .to("file:{{outputDir}}");
                }
            }
        );
        
        LOG.info("Initialized default route templates");
    }
}

/**
 * Route template definition
 */
class RouteTemplateDefinition {
    private final String templateId;
    private final List<String> parameters;
    private final RouteBuilder builder;
    
    public RouteTemplateDefinition(String templateId, List<String> parameters, RouteBuilder builder) {
        this.templateId = templateId;
        this.parameters = parameters;
        this.builder = builder;
    }
    
    public String getTemplateId() { return templateId; }
    public List<String> getParameters() { return parameters; }
    public RouteBuilder getBuilder() { return builder; }
}

// ==================== ADVANCED ERROR HANDLING ====================

/**
 * Smart error handling with adaptive retry strategies
 */
@ApplicationScoped
public class CamelSmartErrorHandler {
    
    private static final Logger LOG = LoggerFactory.getLogger(CamelSmartErrorHandler.class);
    
    private final Map<String, ErrorStatistics> errorStats = new ConcurrentHashMap<>();
    
    /**
     * Create adaptive error handler
     */
    public org.apache.camel.processor.errorhandler.ErrorHandlerBuilder createAdaptiveErrorHandler() {
        return org.apache.camel.builder.DeadLetterChannelBuilder
            .deadLetterChannel("direct:error-dlq")
            .maximumRedeliveries(5)
            .redeliveryDelay(1000)
            .backOffMultiplier(2.0)
            .maximumRedeliveryDelay(60000)
            .useExponentialBackOff()
            .retryAttemptedLogLevel(LoggingLevel.WARN)
            .retriesExhaustedLogLevel(LoggingLevel.ERROR)
            .onRedelivery(this::onRedelivery)
            .onExceptionOccurred(this::onException);
    }
    
    private void onRedelivery(Exchange exchange) {
        String routeId = exchange.getFromRouteId();
        int attempt = exchange.getProperty(Exchange.REDELIVERY_COUNTER, Integer.class);
        
        LOG.warn("Redelivery attempt {} for route {}", attempt, routeId);
        
        // Track error statistics
        ErrorStatistics stats = errorStats.computeIfAbsent(
            routeId, 
            k -> new ErrorStatistics()
        );
        stats.incrementRetries();
    }
    
    private void onException(Exchange exchange) {
        String routeId = exchange.getFromRouteId();
        Exception exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
        
        LOG.error("Exception in route {}: {}", routeId, exception.getMessage());
        
        // Track error statistics
        ErrorStatistics stats = errorStats.computeIfAbsent(
            routeId,
            k -> new ErrorStatistics()
        );
        stats.incrementErrors();
        stats.recordException(exception.getClass().getSimpleName());
    }
    
    /**
     * Get error statistics for monitoring
     */
    public Map<String, ErrorStatistics> getErrorStatistics() {
        return new HashMap<>(errorStats);
    }
}

/**
 * Error statistics tracker
 */
class ErrorStatistics {
    private final AtomicLong totalErrors = new AtomicLong(0);
    private final AtomicLong totalRetries = new AtomicLong(0);
    private final Map<String, AtomicInteger> exceptionCounts = new ConcurrentHashMap<>();
    private volatile Instant lastError;
    
    public void incrementErrors() {
        totalErrors.incrementAndGet();
        lastError = Instant.now();
    }
    
    public void incrementRetries() {
        totalRetries.incrementAndGet();
    }
    
    public void recordException(String exceptionType) {
        exceptionCounts.computeIfAbsent(exceptionType, k -> new AtomicInteger(0))
            .incrementAndGet();
    }
    
    public long getTotalErrors() { return totalErrors.get(); }
    public long getTotalRetries() { return totalRetries.get(); }
    public Map<String, Integer> getExceptionCounts() {
        Map<String, Integer> result = new HashMap<>();
        exceptionCounts.forEach((k, v) -> result.put(k, v.get()));
        return result;
    }
    public Instant getLastError() { return lastError; }
}

// ==================== PERFORMANCE MONITORING ====================

/**
 * Comprehensive performance monitoring
 */
@ApplicationScoped
public class CamelPerformanceMonitor {
    
    private static final Logger LOG = LoggerFactory.getLogger(CamelPerformanceMonitor.class);
    
    @Inject
    MeterRegistry meterRegistry;
    
    private final Map<String, RouteMetrics> routeMetrics = new ConcurrentHashMap<>();
    
    /**
     * Create monitoring route policy
     */
    public RoutePolicy createMonitoringPolicy(String routeId) {
        return new RoutePolicy() {
            private Timer.Sample sample;
            
            @Override
            public void onInit(Route route) {
                LOG.info("Initializing monitoring for route: {}", routeId);
                routeMetrics.put(routeId, new RouteMetrics(routeId, meterRegistry));
            }
            
            @Override
            public void onExchangeBegin(Route route, Exchange exchange) {
                sample = Timer.start(meterRegistry);
                RouteMetrics metrics = routeMetrics.get(routeId);
                if (metrics != null) {
                    metrics.incrementActiveExchanges();
                    metrics.incrementTotalExchanges();
                }
            }
            
            @Override
            public void onExchangeDone(Route route, Exchange exchange) {
                if (sample != null) {
                    sample.stop(meterRegistry.timer(
                        "camel.route.duration",
                        "route", routeId
                    ));
                }
                
                RouteMetrics metrics = routeMetrics.get(routeId);
                if (metrics != null) {
                    metrics.decrementActiveExchanges();
                    
                    if (exchange.isFailed()) {
                        metrics.incrementFailedExchanges();
                    } else {
                        metrics.incrementCompletedExchanges();
                    }
                }
            }
        };
    }
    
    /**
     * Get route metrics
     */
    public Map<String, RouteMetrics> getRouteMetrics() {
        return new HashMap<>(routeMetrics);
    }
    
    /**
     * Get aggregated metrics
     */
    public AggregatedMetrics getAggregatedMetrics() {
        long totalExchanges = routeMetrics.values().stream()
            .mapToLong(m -> m.getTotalExchanges())
            .sum();
        
        long activeExchanges = routeMetrics.values().stream()
            .mapToLong(m -> m.getActiveExchanges())
            .sum();
        
        long completedExchanges = routeMetrics.values().stream()
            .mapToLong(m -> m.getCompletedExchanges())
            .sum();
        
        long failedExchanges = routeMetrics.values().stream()
            .mapToLong(m -> m.getFailedExchanges())
            .sum();
        
        return new AggregatedMetrics(
            totalExchanges,
            activeExchanges,
            completedExchanges,
            failedExchanges,
            routeMetrics.size()
        );
    }
}

/**
 * Route-specific metrics
 */
class RouteMetrics {
    private final String routeId;
    private final AtomicLong totalExchanges = new AtomicLong(0);
    private final AtomicInteger activeExchanges = new AtomicInteger(0);
    private final AtomicLong completedExchanges = new AtomicLong(0);
    private final AtomicLong failedExchanges = new AtomicLong(0);
    private final MeterRegistry registry;
    
    public RouteMetrics(String routeId, MeterRegistry registry) {
        this.routeId = routeId;
        this.registry = registry;
        
        // Register gauges
        registry.gauge("camel.route.active", 
            List.of("route", routeId), activeExchanges);
        registry.counter("camel.route.total", 
            "route", routeId);
        registry.counter("camel.route.completed", 
            "route", routeId);
        registry.counter("camel.route.failed", 
            "route", routeId);
    }
    
    public void incrementTotalExchanges() {
        totalExchanges.incrementAndGet();
        registry.counter("camel.route.total", "route", routeId).increment();
    }
    
    public void incrementActiveExchanges() {
        activeExchanges.incrementAndGet();
    }
    
    public void decrementActiveExchanges() {
        activeExchanges.decrementAndGet();
    }
    
    public void incrementCompletedExchanges() {
        completedExchanges.incrementAndGet();
        registry.counter("camel.route.completed", "route", routeId).increment();
    }
    
    public void incrementFailedExchanges() {
        failedExchanges.incrementAndGet();
        registry.counter("camel.route.failed", "route", routeId).increment();
    }
    
    public long getTotalExchanges() { return totalExchanges.get(); }
    public int getActiveExchanges() { return activeExchanges.get(); }
    public long getCompletedExchanges() { return completedExchanges.get(); }
    public long getFailedExchanges() { return failedExchanges.get(); }
}

/**
 * Aggregated metrics across all routes
 */
record AggregatedMetrics(
    long totalExchanges,
    long activeExchanges,
    long completedExchanges,
    long failedExchanges,
    int activeRoutes
) {}

// ==================== IDEMPOTENT CONSUMER ====================

/**
 * Idempotent consumer implementation
 */
@ApplicationScoped
public class CamelIdempotentConsumer {
    
    private static final Logger LOG = LoggerFactory.getLogger(CamelIdempotentConsumer.class);
    
    @Inject
    CamelContext camelContext;
    
    /**
     * Create idempotent route
     */
    public RouteBuilder createIdempotentRoute(
            String routeId,
            String sourceEndpoint,
            String targetEndpoint,
            String messageIdExpression) {
        
        return new RouteBuilder() {
            @Override
            public void configure() {
                from(sourceEndpoint)
                    .routeId(routeId)
                    .idempotentConsumer(simple(messageIdExpression))
                    .messageIdRepository(createMemoryRepository())
                    .skipDuplicate(true)
                    .log("Processing unique message: ${header.messageId}")
                    .to(targetEndpoint)
                    .end();
            }
        };
    }
    
    private org.apache.camel.spi.IdempotentRepository createMemoryRepository() {
        return org.apache.camel.support.processor.idempotent.MemoryIdempotentRepository.memoryIdempotentRepository(1000);
    }
}

// ==================== CLAIM CHECK PATTERN ====================

/**
 * Claim check pattern for large message handling
 */
@ApplicationScoped
public class CamelClaimCheckManager {
    
    private static final Logger LOG = LoggerFactory.getLogger(CamelClaimCheckManager.class);
    
    private final Map<String, Object> claimCheckStore = new ConcurrentHashMap<>();
    
    /**
     * Store payload and return claim check
     */
    public String storePayload(Object payload) {
        String claimCheckId = UUID.randomUUID().toString();
        claimCheckStore.put(claimCheckId, payload);
        LOG.debug("Stored payload with claim check: {}", claimCheckId);
        return claimCheckId;
    }
    
    /**
     * Retrieve payload using claim check
     */
    public Object retrievePayload(String claimCheckId) {
        Object payload = claimCheckStore.remove(claimCheckId);
        LOG.debug("Retrieved payload for claim check: {}", claimCheckId);
        return payload;
    }
    
    /**
     * Create claim check route
     */
    public RouteBuilder createClaimCheckRoute(String routeId) {
        return new RouteBuilder() {
            @Override
            public void configure() {
                from("direct:" + routeId)
                    .routeId(routeId)
                    // Store original body
                    .process(exchange -> {
                        Object body = exchange.getIn().getBody();
                        String claimCheck = storePayload(body);
                        exchange.getIn().setHeader("claimCheckId", claimCheck);
                        exchange.getIn().setBody(Map.of("claimCheckId", claimCheck));
                    })
                    .log("Created claim check: ${header.claimCheckId}")
                    // Send lightweight message
                    .to("direct:lightweight-processing")
                    // Retrieve original body
                    .process(exchange -> {
                        String claimCheck = exchange.getIn()
                            .getHeader("claimCheckId", String.class);
                        Object originalBody = retrievePayload(claimCheck);
                        exchange.getIn().setBody(originalBody);
                    })
                    .log("Retrieved original payload");
            }
        };
    }
}

// ==================== THROTTLING & RATE LIMITING ====================

/**
 * Advanced throttling and rate limiting
 */
@ApplicationScoped
public class CamelThrottlingManager {
    
    private static final Logger LOG = LoggerFactory.getLogger(CamelThrottlingManager.class);
    
    /**
     * Create throttled route
     */
    public RouteBuilder createThrottledRoute(
            String routeId,
            String sourceEndpoint,
            String targetEndpoint,
            int maxRequestsPerPeriod,
            long periodInMillis) {
        
        return new RouteBuilder() {
            @Override
            public void configure() {
                from(sourceEndpoint)
                    .routeId(routeId)
                    .throttle(maxRequestsPerPeriod)
                        .timePeriodMillis(periodInMillis)
                        .rejectExecution(false) // Queue instead of reject
                    .log("Processing throttled request")
                    .to(targetEndpoint);
            }
        };
    }
    
    /**
     * Create rate-limited route with tenant isolation
     */
    public RouteBuilder createTenantRateLimitedRoute(
            String routeId,
            String sourceEndpoint,
            String targetEndpoint,
            int maxRequestsPerTenant) {
        
        return new RouteBuilder() {
            @Override
            public void configure() {
                from(sourceEndpoint)
                    .routeId(routeId)
                    .throttle(maxRequestsPerTenant)
                        .correlationExpression(header("tenantId"))
                        .timePeriodMillis(60000) // Per minute
                    .log("Processing request for tenant: ${header.tenantId}")
                    .to(targetEndpoint);
            }
        };
    }
}

// ==================== LOAD BALANCING ====================

/**
 * Advanced load balancing strategies
 */
@ApplicationScoped
public class CamelLoadBalancingManager {
    
    private static final Logger LOG = LoggerFactory.getLogger(CamelLoadBalancingManager.class);
    
    /**
     * Create load-balanced route with multiple endpoints
     */
    public RouteBuilder createLoadBalancedRoute(
            String routeId,
            String sourceEndpoint,
            List<String> targetEndpoints,
            LoadBalancingStrategy strategy) {
        
        return new RouteBuilder() {
            @Override
            public void configure() {
                RouteDefinition route = from(sourceEndpoint)
                    .routeId(routeId);
                
                switch (strategy) {
                    case ROUND_ROBIN:
                        route.loadBalance().roundRobin()
                            .to(targetEndpoints.toArray(new String[0]))
                        .end();
                        break;
                    
                    case RANDOM:
                        route.loadBalance().random()
                            .to(targetEndpoints.toArray(new String[0]))
                        .end();
                        break;
                    
                    case WEIGHTED:
                        route.loadBalance().weighted(true, "1,2,3")
                            .to(targetEndpoints.toArray(new String[0]))
                        .end();
                        break;
                    
                    case FAILOVER:
                        route.loadBalance().failover()
                            .to(targetEndpoints.toArray(new String[0]))
                        .end();
                        break;
                    
                    case STICKY:
                        route.loadBalance().sticky(header("sessionId"))
                            .to(targetEndpoints.toArray(new String[0]))
                        .end();
                        break;
                }
            }
        };
    }
}

enum LoadBalancingStrategy {
    ROUND_ROBIN,
    RANDOM,
    WEIGHTED,
    FAILOVER,
    STICKY
}

// ==================== CORRELATION & REQUEST-REPLY ====================

/**
 * Request-reply pattern with correlation
 */
@ApplicationScoped
public class CamelRequestReplyManager {
    
    private static final Logger LOG = LoggerFactory.getLogger(CamelRequestReplyManager.class);
    
    /**
     * Create request-reply route
     */
    public RouteBuilder createRequestReplyRoute(
            String routeId,
            String requestEndpoint,
            String replyEndpoint) {
        
        return new RouteBuilder() {
            @Override
            public void configure() {
                from(requestEndpoint)
                    .routeId(routeId)
                    // Generate correlation ID
                    .setHeader("correlationId", simple("${id}"))
                    .log("Sending request with correlation ID: ${header.correlationId}")
                    // Send and wait for reply
                    .inOut(replyEndpoint)
                    .log("Received reply for correlation ID: ${header.correlationId}");
            }
        };
    }
}

// ==================== CACHE INTEGRATION ====================

/**
 * Intelligent caching for integration results
 */
@ApplicationScoped
public class CamelCacheManager {
    
    private static final Logger LOG = LoggerFactory.getLogger(CamelCacheManager.class);
    
    private final Map<String, CachedResponse> cache = new ConcurrentHashMap<>();
    
    /**
     * Create cached route
     */
    public RouteBuilder createCachedRoute(
            String routeId,
            String sourceEndpoint,
            String targetEndpoint,
            Duration ttl) {
        
        return new RouteBuilder() {
            @Override
            public void configure() {
                from(sourceEndpoint)
                    .routeId(routeId)
                    .process(exchange -> {
                        String cacheKey = generateCacheKey(exchange);
                        CachedResponse cached = cache.get(cacheKey);
                        
                        if (cached != null && !cached.isExpired()) {
                            LOG.debug("Cache hit for key: {}", cacheKey);
                            exchange.getIn().setBody(cached.response());
                            exchange.getIn().setHeader("X-Cache", "HIT");
                        } else {
                            LOG.debug("Cache miss for key: {}", cacheKey);
                            exchange.getIn().setHeader("X-Cache", "MISS");
                            exchange.getIn().setHeader("cacheKey", cacheKey);
                        }
                    })
                    .choice()
                        .when(header("X-Cache").isEqualTo("MISS"))
                            .to(targetEndpoint)
                            .process(exchange -> {
                                String cacheKey = exchange.getIn()
                                    .getHeader("cacheKey", String.class);
                                Object response = exchange.getIn().getBody();
                                cache.put(cacheKey, new CachedResponse(
                                    response,
                                    Instant.now().plus(ttl)
                                ));
                                LOG.debug("Cached response for key: {}", cacheKey);
                            })
                    .end();
            }
        };
    }
    
    private String generateCacheKey(Exchange exchange) {
        return String.valueOf(Objects.hash(
            exchange.getIn().getBody(),
            exchange.getIn().getHeaders()
        ));
    }
}

record CachedResponse(Object response, Instant expiresAt) {
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}


package tech.kayys.silat.executor.camel;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.*;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.silat.core.domain.*;
import tech.kayys.silat.core.engine.NodeExecutionResult;
import tech.kayys.silat.core.engine.NodeExecutionTask;
import tech.kayys.silat.executor.*;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * ============================================================================
 * APACHE CAMEL INTEGRATION EXECUTOR
 * ============================================================================
 * 
 * Full-featured enterprise integration executor using Apache Camel.
 * 
 * Features:
 * - All EIP patterns (Router, Splitter, Aggregator, Enricher, etc.)
 * - 300+ Camel components support
 * - Dynamic route creation
 * - Multi-tenant route isolation
 * - Error handling with dead letter channel
 * - Monitoring and metrics
 * - Transaction support
 * - Compensation logic
 * 
 * Supported Patterns:
 * - Message Router (Content-Based, Recipient List, Dynamic)
 * - Message Translator
 * - Content-Based Router
 * - Splitter/Aggregator
 * - Content Enricher
 * - Message Filter
 * - Resequencer
 * - Claim Check
 * - Wire Tap
 * - Multicast
 * - Load Balancer
 * - Circuit Breaker
 * - Saga Pattern
 * 
 * Communication Endpoints:
 * - REST APIs (HTTP, HTTPS)
 * - Message Queues (Kafka, RabbitMQ, ActiveMQ)
 * - Databases (JDBC, MongoDB, Redis)
 * - File Systems (FTP, SFTP, File, S3)
 * - Cloud Services (AWS, Azure, GCP)
 * - Enterprise Systems (SAP, Salesforce, etc.)
 */
@Executor(
    executorType = "camel-integration",
    communicationType = tech.kayys.silat.core.scheduler.CommunicationType.GRPC,
    maxConcurrentTasks = 50,
    supportedNodeTypes = {"INTEGRATION", "EIP_ROUTER", "EIP_TRANSFORMER", "EIP_SPLITTER", 
                          "EIP_AGGREGATOR", "EIP_ENRICHER", "MESSAGE_ENDPOINT"},
    version = "1.0.0"
)
@ApplicationScoped
public class CamelIntegrationExecutor extends AbstractWorkflowExecutor {
    
    private static final Logger LOG = LoggerFactory.getLogger(CamelIntegrationExecutor.class);
    
    @Inject
    CamelContext camelContext;
    
    @Inject
    ProducerTemplate producerTemplate;
    
    @Inject
    ConsumerTemplate consumerTemplate;
    
    @Inject
    CamelRouteManager routeManager;
    
    @Inject
    CamelTenantIsolationManager tenantIsolationManager;
    
    @Inject
    CamelEndpointFactory endpointFactory;
    
    @Inject
    CamelTransformationService transformationService;
    
    @Inject
    CamelErrorHandlingService errorHandlingService;
    
    @ConfigProperty(name = "silat.camel.default-timeout", defaultValue = "30000")
    long defaultTimeout;
    
    // Track active integrations for monitoring
    private final Map<String, ActiveIntegration> activeIntegrations = new ConcurrentHashMap<>();
    
    @Override
    public Uni<NodeExecutionResult> execute(NodeExecutionTask task) {
        LOG.info("Executing Camel integration: run={}, node={}, attempt={}", 
            task.runId().value(), task.nodeId().value(), task.attempt());
        
        try {
            // Extract integration configuration
            Map<String, Object> config = task.context();
            IntegrationConfig integrationConfig = parseIntegrationConfig(config);
            
            // Get tenant context
            String tenantId = (String) config.get("tenantId");
            
            // Execute based on pattern type
            return executeIntegrationPattern(task, integrationConfig, tenantId);
            
        } catch (Exception e) {
            LOG.error("Failed to execute Camel integration", e);
            return Uni.createFrom().item(NodeExecutionResult.failure(
                task.runId(),
                task.nodeId(),
                task.attempt(),
                ErrorInfo.of(e),
                task.token()
            ));
        }
    }
    
    /**
     * Execute integration pattern
     */
    private Uni<NodeExecutionResult> executeIntegrationPattern(
            NodeExecutionTask task,
            IntegrationConfig config,
            String tenantId) {
        
        return switch (config.patternType()) {
            case CONTENT_BASED_ROUTER -> executeContentBasedRouter(task, config, tenantId);
            case MESSAGE_TRANSLATOR -> executeMessageTranslator(task, config, tenantId);
            case SPLITTER -> executeSplitter(task, config, tenantId);
            case AGGREGATOR -> executeAggregator(task, config, tenantId);
            case CONTENT_ENRICHER -> executeContentEnricher(task, config, tenantId);
            case MESSAGE_FILTER -> executeMessageFilter(task, config, tenantId);
            case RECIPIENT_LIST -> executeRecipientList(task, config, tenantId);
            case WIRE_TAP -> executeWireTap(task, config, tenantId);
            case MULTICAST -> executeMulticast(task, config, tenantId);
            case RESEQUENCER -> executeResequencer(task, config, tenantId);
            case SAGA -> executeSaga(task, config, tenantId);
            case CIRCUIT_BREAKER -> executeCircuitBreaker(task, config, tenantId);
            default -> executeGenericEndpoint(task, config, tenantId);
        };
    }
    
    // ==================== CONTENT-BASED ROUTER ====================
    
    /**
     * Content-Based Router Pattern
     * Routes messages to different destinations based on message content
     */
    private Uni<NodeExecutionResult> executeContentBasedRouter(
            NodeExecutionTask task,
            IntegrationConfig config,
            String tenantId) {
        
        LOG.info("Executing Content-Based Router for tenant: {}", tenantId);
        
        return Uni.createFrom().completionStage(() -> {
            CompletableFuture<NodeExecutionResult> future = new CompletableFuture<>();
            
            try {
                // Create dynamic route
                String routeId = createRouteId(task, "cbr");
                Object payload = config.payload();
                
                camelContext.addRoutes(new RouteBuilder() {
                    @Override
                    public void configure() {
                        // Dead Letter Channel for error handling
                        errorHandler(deadLetterChannel("direct:error-handler")
                            .maximumRedeliveries(3)
                            .redeliveryDelay(1000)
                            .retryAttemptedLogLevel(LoggingLevel.WARN));
                        
                        from("direct:" + routeId)
                            .routeId(routeId)
                            .setHeader("tenantId", constant(tenantId))
                            .setHeader("taskId", constant(task.nodeId().value()))
                            
                            // Content-based routing
                            .choice()
                                .when(simple(config.routingRules().get("rule1")))
                                    .log("Routing to destination 1")
                                    .to(config.targetEndpoints().get(0))
                                .when(simple(config.routingRules().get("rule2")))
                                    .log("Routing to destination 2")
                                    .to(config.targetEndpoints().get(1))
                                .otherwise()
                                    .log("Routing to default destination")
                                    .to(config.defaultEndpoint())
                            .end()
                            
                            .process(exchange -> {
                                // Collect result
                                Object result = exchange.getIn().getBody();
                                Map<String, Object> output = Map.of(
                                    "result", result,
                                    "routedTo", exchange.getIn().getHeader("CamelToEndpoint", String.class),
                                    "timestamp", Instant.now()
                                );
                                
                                future.complete(NodeExecutionResult.success(
                                    task.runId(),
                                    task.nodeId(),
                                    task.attempt(),
                                    output,
                                    task.token()
                                ));
                            });
                    }
                });
                
                // Start the route
                camelContext.getRouteController().startRoute(routeId);
                producerTemplate.sendBody("direct:" + routeId, payload);
                trackIntegration(task, routeId, Instant.now());
                
            } catch (Exception e) {
                LOG.error("Recipient List failed", e);
                future.complete(NodeExecutionResult.failure(
                    task.runId(),
                    task.nodeId(),
                    task.attempt(),
                    ErrorInfo.of(e),
                    task.token()
                ));
            }
            
            return future;
        });
    }
    
    // ==================== WIRE TAP ====================
    
    private Uni<NodeExecutionResult> executeWireTap(
            NodeExecutionTask task,
            IntegrationConfig config,
            String tenantId) {
        
        LOG.info("Executing Wire Tap for tenant: {}", tenantId);
        
        return Uni.createFrom().completionStage(() -> {
            CompletableFuture<NodeExecutionResult> future = new CompletableFuture<>();
            
            try {
                String routeId = createRouteId(task, "wiretap");
                Object payload = config.payload();
                
                camelContext.addRoutes(new RouteBuilder() {
                    @Override
                    public void configure() {
                        from("direct:" + routeId)
                            .routeId(routeId)
                            .setHeader("tenantId", constant(tenantId))
                            
                            // Wire tap to monitoring endpoint
                            .wireTap(config.wireTapEndpoint())
                                .newExchangeBody(constant("Tapped message"))
                            
                            // Continue with main flow
                            .to(config.targetEndpoints().get(0))
                            
                            .process(exchange -> {
                                Object result = exchange.getIn().getBody();
                                Map<String, Object> output = Map.of(
                                    "result", result,
                                    "tappedTo", config.wireTapEndpoint(),
                                    "timestamp", Instant.now()
                                );
                                
                                future.complete(NodeExecutionResult.success(
                                    task.runId(),
                                    task.nodeId(),
                                    task.attempt(),
                                    output,
                                    task.token()
                                ));
                            });
                    }
                });
                
                camelContext.getRouteController().startRoute(routeId);
                producerTemplate.sendBody("direct:" + routeId, payload);
                trackIntegration(task, routeId, Instant.now());
                
            } catch (Exception e) {
                future.complete(NodeExecutionResult.failure(
                    task.runId(), task.nodeId(), task.attempt(),
                    ErrorInfo.of(e), task.token()
                ));
            }
            
            return future;
        });
    }
    
    // ==================== MULTICAST ====================
    
    private Uni<NodeExecutionResult> executeMulticast(
            NodeExecutionTask task,
            IntegrationConfig config,
            String tenantId) {
        
        return Uni.createFrom().completionStage(() -> {
            CompletableFuture<NodeExecutionResult> future = new CompletableFuture<>();
            
            try {
                String routeId = createRouteId(task, "multicast");
                Object payload = config.payload();
                
                camelContext.addRoutes(new RouteBuilder() {
                    @Override
                    public void configure() {
                        from("direct:" + routeId)
                            .routeId(routeId)
                            .multicast()
                                .parallelProcessing()
                                .to(config.targetEndpoints().toArray(new String[0]))
                            .end()
                            .process(exchange -> {
                                future.complete(NodeExecutionResult.success(
                                    task.runId(), task.nodeId(), task.attempt(),
                                    Map.of("multicastTo", config.targetEndpoints(), 
                                           "timestamp", Instant.now()),
                                    task.token()
                                ));
                            });
                    }
                });
                
                camelContext.getRouteController().startRoute(routeId);
                producerTemplate.sendBody("direct:" + routeId, payload);
                trackIntegration(task, routeId, Instant.now());
                
            } catch (Exception e) {
                future.complete(NodeExecutionResult.failure(
                    task.runId(), task.nodeId(), task.attempt(),
                    ErrorInfo.of(e), task.token()
                ));
            }
            
            return future;
        });
    }
    
    // ==================== RESEQUENCER ====================
    
    private Uni<NodeExecutionResult> executeResequencer(
            NodeExecutionTask task,
            IntegrationConfig config,
            String tenantId) {
        
        return Uni.createFrom().completionStage(() -> {
            CompletableFuture<NodeExecutionResult> future = new CompletableFuture<>();
            
            try {
                String routeId = createRouteId(task, "resequencer");
                List<Object> messages = (List<Object>) config.payload();
                
                camelContext.addRoutes(new RouteBuilder() {
                    @Override
                    public void configure() {
                        from("direct:" + routeId)
                            .routeId(routeId)
                            .resequence(header("sequenceNumber"))
                                .batch()
                                .timeout(5000)
                            .to(config.targetEndpoints().get(0))
                            .process(exchange -> {
                                future.complete(NodeExecutionResult.success(
                                    task.runId(), task.nodeId(), task.attempt(),
                                    Map.of("resequenced", exchange.getIn().getBody()),
                                    task.token()
                                ));
                            });
                    }
                });
                
                camelContext.getRouteController().startRoute(routeId);
                
                // Send messages with sequence numbers
                for (int i = 0; i < messages.size(); i++) {
                    producerTemplate.sendBodyAndHeader("direct:" + routeId, 
                        messages.get(i), "sequenceNumber", i);
                }
                
                trackIntegration(task, routeId, Instant.now());
                
            } catch (Exception e) {
                future.complete(NodeExecutionResult.failure(
                    task.runId(), task.nodeId(), task.attempt(),
                    ErrorInfo.of(e), task.token()
                ));
            }
            
            return future;
        });
    }
    
    // ==================== SAGA PATTERN ====================
    
    private Uni<NodeExecutionResult> executeSaga(
            NodeExecutionTask task,
            IntegrationConfig config,
            String tenantId) {
        
        LOG.info("Executing Saga Pattern for tenant: {}", tenantId);
        
        return Uni.createFrom().completionStage(() -> {
            CompletableFuture<NodeExecutionResult> future = new CompletableFuture<>();
            
            try {
                String routeId = createRouteId(task, "saga");
                Object payload = config.payload();
                
                camelContext.addRoutes(new RouteBuilder() {
                    @Override
                    public void configure() {
                        from("direct:" + routeId)
                            .routeId(routeId)
                            .saga()
                                .timeout(Duration.ofSeconds(config.sagaTimeout()))
                                .compensation("direct:compensate-" + routeId)
                                
                                // Step 1
                                .to(config.targetEndpoints().get(0))
                                    .compensation("direct:compensate-step1-" + routeId)
                                
                                // Step 2
                                .to(config.targetEndpoints().get(1))
                                    .compensation("direct:compensate-step2-" + routeId)
                                
                                // Step 3 (if exists)
                                .choice()
                                    .when(simple("${body.step3} != null"))
                                        .to(config.targetEndpoints().get(2))
                                            .compensation("direct:compensate-step3-" + routeId)
                                .end()
                            .end()
                            
                            .process(exchange -> {
                                future.complete(NodeExecutionResult.success(
                                    task.runId(), task.nodeId(), task.attempt(),
                                    Map.of("sagaCompleted", true, 
                                           "result", exchange.getIn().getBody()),
                                    task.token()
                                ));
                            });
                        
                        // Compensation routes
                        from("direct:compensate-" + routeId)
                            .log("Saga compensation triggered");
                        
                        from("direct:compensate-step1-" + routeId)
                            .log("Compensating step 1")
                            .to(config.compensationEndpoints().get(0));
                        
                        from("direct:compensate-step2-" + routeId)
                            .log("Compensating step 2")
                            .to(config.compensationEndpoints().get(1));
                    }
                });
                
                camelContext.getRouteController().startRoute(routeId);
                producerTemplate.sendBody("direct:" + routeId, payload);
                trackIntegration(task, routeId, Instant.now());
                
            } catch (Exception e) {
                future.complete(NodeExecutionResult.failure(
                    task.runId(), task.nodeId(), task.attempt(),
                    ErrorInfo.of(e), task.token()
                ));
            }
            
            return future;
        });
    }
    
    // ==================== CIRCUIT BREAKER ====================
    
    private Uni<NodeExecutionResult> executeCircuitBreaker(
            NodeExecutionTask task,
            IntegrationConfig config,
            String tenantId) {
        
        return Uni.createFrom().completionStage(() -> {
            CompletableFuture<NodeExecutionResult> future = new CompletableFuture<>();
            
            try {
                String routeId = createRouteId(task, "circuit-breaker");
                Object payload = config.payload();
                
                camelContext.addRoutes(new RouteBuilder() {
                    @Override
                    public void configure() {
                        from("direct:" + routeId)
                            .routeId(routeId)
                            .circuitBreaker()
                                .threshold(config.circuitBreakerThreshold())
                                .halfOpenAfter(config.halfOpenAfter())
                                
                                // Protected call
                                .to(config.targetEndpoints().get(0))
                                
                                // Fallback
                                .onFallback()
                                    .log("Circuit breaker activated - using fallback")
                                    .setBody(constant(config.fallbackResponse()))
                            .end()
                            
                            .process(exchange -> {
                                future.complete(NodeExecutionResult.success(
                                    task.runId(), task.nodeId(), task.attempt(),
                                    Map.of("result", exchange.getIn().getBody()),
                                    task.token()
                                ));
                            });
                    }
                });
                
                camelContext.getRouteController().startRoute(routeId);
                producerTemplate.sendBody("direct:" + routeId, payload);
                trackIntegration(task, routeId, Instant.now());
                
            } catch (Exception e) {
                future.complete(NodeExecutionResult.failure(
                    task.runId(), task.nodeId(), task.attempt(),
                    ErrorInfo.of(e), task.token()
                ));
            }
            
            return future;
        });
    }
    
    // ==================== GENERIC ENDPOINT ====================
    
    private Uni<NodeExecutionResult> executeGenericEndpoint(
            NodeExecutionTask task,
            IntegrationConfig config,
            String tenantId) {
        
        LOG.info("Executing generic endpoint integration for tenant: {}", tenantId);
        
        return Uni.createFrom().completionStage(() -> {
            CompletableFuture<NodeExecutionResult> future = new CompletableFuture<>();
            
            try {
                String endpoint = config.targetEndpoints().get(0);
                Object payload = config.payload();
                
                // Direct endpoint call
                Object result = producerTemplate.requestBody(endpoint, payload);
                
                future.complete(NodeExecutionResult.success(
                    task.runId(),
                    task.nodeId(),
                    task.attempt(),
                    Map.of(
                        "result", result,
                        "endpoint", endpoint,
                        "timestamp", Instant.now()
                    ),
                    task.token()
                ));
                
            } catch (Exception e) {
                LOG.error("Generic endpoint failed", e);
                future.complete(NodeExecutionResult.failure(
                    task.runId(),
                    task.nodeId(),
                    task.attempt(),
                    ErrorInfo.of(e),
                    task.token()
                ));
            }
            
            return future;
        });
    }
    
    // ==================== HELPER METHODS ====================
    
    private IntegrationConfig parseIntegrationConfig(Map<String, Object> context) {
        return new IntegrationConfig(
            EIPPatternType.valueOf(
                context.getOrDefault("patternType", "GENERIC").toString().toUpperCase()
            ),
            context.get("payload"),
            (List<String>) context.getOrDefault("targetEndpoints", List.of()),
            (Map<String, String>) context.getOrDefault("routingRules", Map.of()),
            context.getOrDefault("defaultEndpoint", "").toString(),
            context.getOrDefault("transformationType", "json").toString(),
            context.getOrDefault("transformationScript", "").toString(),
            context.getOrDefault("splitDelimiter", "\n").toString(),
            (boolean) context.getOrDefault("parallelProcessing", false),
            (int) context.getOrDefault("batchSize", 10),
            (long) context.getOrDefault("aggregationTimeout", 5000L),
            context.getOrDefault("aggregationStrategy", "collect").toString(),
            context.getOrDefault("reduceFunction", "").toString(),
            context.getOrDefault("enrichmentEndpoint", "").toString(),
            context.getOrDefault("enrichmentStrategy", "merge").toString(),
            context.getOrDefault("enrichmentKey", "enrichment").toString(),
            context.getOrDefault("filterExpression", "${body} != null").toString(),
            context.getOrDefault("wireTapEndpoint", "").toString(),
            (long) context.getOrDefault("sagaTimeout", 300L),
            (List<String>) context.getOrDefault("compensationEndpoints", List.of()),
            (int) context.getOrDefault("circuitBreakerThreshold", 5),
            (long) context.getOrDefault("halfOpenAfter", 30000L),
            context.get("fallbackResponse")
        );
    }
    
    private String createRouteId(NodeExecutionTask task, String pattern) {
        return String.format("%s-%s-%s-%d",
            pattern,
            task.runId().value().substring(0, 8),
            task.nodeId().value(),
            task.attempt()
        );
    }
    
    private void trackIntegration(NodeExecutionTask task, String routeId, Instant startTime) {
        activeIntegrations.put(routeId, new ActiveIntegration(
            task.runId().value(),
            task.nodeId().value(),
            routeId,
            startTime
        ));
        
        // Schedule cleanup
        CompletableFuture.delayedExecutor(defaultTimeout, TimeUnit.MILLISECONDS)
            .execute(() -> cleanupRoute(routeId));
    }
    
    private void cleanupRoute(String routeId) {
        try {
            if (camelContext.getRouteController().getRouteStatus(routeId) != null) {
                camelContext.getRouteController().stopRoute(routeId);
                camelContext.removeRoute(routeId);
                activeIntegrations.remove(routeId);
                LOG.debug("Cleaned up route: {}", routeId);
            }
        } catch (Exception e) {
            LOG.warn("Failed to cleanup route: {}", routeId, e);
        }
    }
    
    private Object mergeObjects(List<Object> items) {
        Map<String, Object> merged = new HashMap<>();
        items.forEach(item -> {
            if (item instanceof Map) {
                merged.putAll((Map) item);
            }
        });
        return merged;
    }
    
    private Object reduceItems(List<Object> items, String function) {
        // Simple reduce implementation
        if (function.equals("sum") && !items.isEmpty() && items.get(0) instanceof Number) {
            return items.stream()
                .map(o -> ((Number) o).doubleValue())
                .reduce(0.0, Double::sum);
        }
        return items;
    }
    
    public Map<String, Object> getMetrics() {
        return Map.of(
            "activeIntegrations", activeIntegrations.size(),
            "totalRoutes", camelContext.getRoutes().size(),
            "integrations", activeIntegrations.values().stream()
                .map(ai -> Map.of(
                    "routeId", ai.routeId(),
                    "runId", ai.runId(),
                    "duration", Duration.between(ai.startTime(), Instant.now()).toMillis()
                ))
                .toList()
        );
    }
}

// ==================== SUPPORTING CLASSES ====================

/**
 * Integration configuration model
 */
record IntegrationConfig(
    EIPPatternType patternType,
    Object payload,
    List<String> targetEndpoints,
    Map<String, String> routingRules,
    String defaultEndpoint,
    String transformationType,
    String transformationScript,
    String splitDelimiter,
    boolean parallelProcessing,
    int batchSize,
    long aggregationTimeout,
    String aggregationStrategy,
    String reduceFunction,
    String enrichmentEndpoint,
    String enrichmentStrategy,
    String enrichmentKey,
    String filterExpression,
    String wireTapEndpoint,
    long sagaTimeout,
    List<String> compensationEndpoints,
    int circuitBreakerThreshold,
    long halfOpenAfter,
    Object fallbackResponse
) {}

/**
 * EIP Pattern types
 */
enum EIPPatternType {
    CONTENT_BASED_ROUTER,
    MESSAGE_TRANSLATOR,
    SPLITTER,
    AGGREGATOR,
    CONTENT_ENRICHER,
    MESSAGE_FILTER,
    RECIPIENT_LIST,
    WIRE_TAP,
    MULTICAST,
    RESEQUENCER,
    SAGA,
    CIRCUIT_BREAKER,
    GENERIC
}

/**
 * Active integration tracking
 */
record ActiveIntegration(
    String runId,
    String nodeId,
    String routeId,
    Instant startTime
) {}

/**
 * Route manager for dynamic route management
 */
@ApplicationScoped
class CamelRouteManager {
    
    private static final Logger LOG = LoggerFactory.getLogger(CamelRouteManager.class);
    
    @Inject
    CamelContext camelContext;
    
    public void createRoute(String routeId, RouteBuilder builder) throws Exception {
        camelContext.addRoutes(builder);
        camelContext.getRouteController().startRoute(routeId);
        LOG.info("Created and started route: {}", routeId);
    }
    
    public void stopRoute(String routeId) throws Exception {
        camelContext.getRouteController().stopRoute(routeId);
        camelContext.removeRoute(routeId);
        LOG.info("Stopped and removed route: {}", routeId);
    }
}

/**
 * Tenant isolation manager
 */
@ApplicationScoped
class CamelTenantIsolationManager {
    
    private static final Logger LOG = LoggerFactory.getLogger(CamelTenantIsolationManager.class);
    
    private final Map<String, Set<String>> tenantRoutes = new ConcurrentHashMap<>();
    
    public void registerRoute(String tenantId, String routeId) {
        tenantRoutes.computeIfAbsent(tenantId, k -> ConcurrentHashMap.newKeySet())
            .add(routeId);
        LOG.debug("Registered route {} for tenant {}", routeId, tenantId);
    }
    
    public Set<String> getRoutesForTenant(String tenantId) {
        return tenantRoutes.getOrDefault(tenantId, Set.of());
    }
    
    public void cleanupTenant(String tenantId) {
        Set<String> routes = tenantRoutes.remove(tenantId);
        if (routes != null) {
            LOG.info("Cleaned up {} routes for tenant {}", routes.size(), tenantId);
        }
    }
}

/**
 * Endpoint factory for creating Camel endpoints
 */
@ApplicationScoped
class CamelEndpointFactory {
    
    private static final Logger LOG = LoggerFactory.getLogger(CamelEndpointFactory.class);
    
    @Inject
    CamelContext camelContext;
    
    public Endpoint createEndpoint(String uri) throws Exception {
        return camelContext.getEndpoint(uri);
    }
    
    public String createHttpEndpoint(String url, String method) {
        return String.format("http:%s?httpMethod=%s", url, method);
    }
    
    public String createKafkaEndpoint(String topic, String brokers) {
        return String.format("kafka:%s?brokers=%s", topic, brokers);
    }
    
    public String createDatabaseEndpoint(String dataSource, String sql) {
        return String.format("jdbc:%s?useHeadersAsParameters=true", dataSource);
    }
    
    public String createFileEndpoint(String directory) {
        return String.format("file:%s", directory);
    }
}

/**
 * Transformation service
 */
@ApplicationScoped
class CamelTransformationService {
    
    private static final Logger LOG = LoggerFactory.getLogger(CamelTransformationService.class);
    
    @Inject
    com.fasterxml.jackson.databind.ObjectMapper objectMapper;
    
    public Object transform(Object input, String script, String type) {
        try {
            return switch (type) {
                case "json" -> transformJson(input, script);
                case "xml" -> transformXml(input, script);
                case "javascript" -> executeJavaScript(input, script);
                default -> input;
            };
        } catch (Exception e) {
            LOG.error("Transformation failed", e);
            throw new RuntimeException("Transformation failed", e);
        }
    }
    
    private Object transformJson(Object input, String script) throws Exception {
        // Use Jackson for JSON transformation
        String json = objectMapper.writeValueAsString(input);
        // Apply transformation logic (simplified)
        return objectMapper.readValue(json, Map.class);
    }
    
    private Object transformXml(Object input, String script) {
        // XML transformation logic
        return input;
    }
    
    private Object executeJavaScript(Object input, String script) {
        // JavaScript execution logic using GraalVM or Nashorn
        return input;
    }
}

/**
 * Error handling service
 */
@ApplicationScoped
class CamelErrorHandlingService {
    
    private static final Logger LOG = LoggerFactory.getLogger(CamelErrorHandlingService.class);
    
    public void handleError(Exchange exchange, Exception exception) {
        LOG.error("Integration error occurred", exception);
        
        // Store error details
        exchange.setProperty("errorMessage", exception.getMessage());
        exchange.setProperty("errorTimestamp", Instant.now());
        exchange.setProperty("errorType", exception.getClass().getName());
    }
    
    public boolean shouldRetry(Exchange exchange, int attempt, int maxAttempts) {
        return attempt < maxAttempts;
    }
}

/**
 * Main application
 */
@QuarkusMain
public class CamelExecutorApplication implements QuarkusApplication {
    
    private static final Logger LOG = LoggerFactory.getLogger(CamelExecutorApplication.class);
    
    @Inject
    ExecutorRuntime executorRuntime;
    
    @Inject
    CamelIntegrationExecutor camelExecutor;
    
    @Override
    public int run(String... args) {
        LOG.info("Starting Silat Camel Integration Executor");
        
        // Register executor
        executorRuntime.registerExecutor(camelExecutor);
        
        // Start runtime
        executorRuntime.start();
        
        LOG.info("Camel Integration Executor started successfully");
        
        Quarkus.waitForExit();
        return 0;
    }
}startRoute(routeId);
                
                // Send message
                producerTemplate.sendBody("direct:" + routeId, payload);
                
                // Track integration
                trackIntegration(task, routeId, Instant.now());
                
            } catch (Exception e) {
                LOG.error("Content-Based Router failed", e);
                future.complete(NodeExecutionResult.failure(
                    task.runId(),
                    task.nodeId(),
                    task.attempt(),
                    ErrorInfo.of(e),
                    task.token()
                ));
            }
            
            return future;
        });
    }
    
    // ==================== MESSAGE TRANSLATOR ====================
    
    /**
     * Message Translator Pattern
     * Transforms messages from one format to another
     */
    private Uni<NodeExecutionResult> executeMessageTranslator(
            NodeExecutionTask task,
            IntegrationConfig config,
            String tenantId) {
        
        LOG.info("Executing Message Translator for tenant: {}", tenantId);
        
        return Uni.createFrom().completionStage(() -> {
            CompletableFuture<NodeExecutionResult> future = new CompletableFuture<>();
            
            try {
                String routeId = createRouteId(task, "translator");
                Object payload = config.payload();
                
                camelContext.addRoutes(new RouteBuilder() {
                    @Override
                    public void configure() {
                        from("direct:" + routeId)
                            .routeId(routeId)
                            .setHeader("tenantId", constant(tenantId))
                            
                            // Log input
                            .log("Input message: ${body}")
                            
                            // Apply transformation based on type
                            .choice()
                                .when(header("transformType").isEqualTo("json-to-xml"))
                                    .unmarshal().json(JsonLibrary.Jackson)
                                    .marshal().jacksonXml()
                                .when(header("transformType").isEqualTo("xml-to-json"))
                                    .unmarshal().jacksonXml()
                                    .marshal().json(JsonLibrary.Jackson)
                                .when(header("transformType").isEqualTo("csv-to-json"))
                                    .unmarshal().csv()
                                    .marshal().json(JsonLibrary.Jackson)
                                .otherwise()
                                    // Custom transformation using processor
                                    .process(exchange -> {
                                        Object body = exchange.getIn().getBody();
                                        Object transformed = transformationService.transform(
                                            body, 
                                            config.transformationScript(),
                                            config.transformationType()
                                        );
                                        exchange.getIn().setBody(transformed);
                                    })
                            .end()
                            
                            // Log output
                            .log("Transformed message: ${body}")
                            
                            // Send to target if specified
                            .choice()
                                .when(simple("${header.targetEndpoint} != null"))
                                    .toD("${header.targetEndpoint}")
                            .end()
                            
                            .process(exchange -> {
                                Object result = exchange.getIn().getBody();
                                Map<String, Object> output = Map.of(
                                    "transformed", result,
                                    "transformationType", config.transformationType(),
                                    "timestamp", Instant.now()
                                );
                                
                                future.complete(NodeExecutionResult.success(
                                    task.runId(),
                                    task.nodeId(),
                                    task.attempt(),
                                    output,
                                    task.token()
                                ));
                            });
                    }
                });
                
                camelContext.getRouteController().startRoute(routeId);
                
                // Send with transformation type header
                Map<String, Object> headers = Map.of(
                    "transformType", config.transformationType(),
                    "targetEndpoint", config.targetEndpoints().isEmpty() ? "" : config.targetEndpoints().get(0)
                );
                producerTemplate.sendBodyAndHeaders("direct:" + routeId, payload, headers);
                
                trackIntegration(task, routeId, Instant.now());
                
            } catch (Exception e) {
                LOG.error("Message Translator failed", e);
                future.complete(NodeExecutionResult.failure(
                    task.runId(),
                    task.nodeId(),
                    task.attempt(),
                    ErrorInfo.of(e),
                    task.token()
                ));
            }
            
            return future;
        });
    }
    
    // ==================== SPLITTER ====================
    
    /**
     * Splitter Pattern
     * Splits a single message into multiple messages
     */
    private Uni<NodeExecutionResult> executeSplitter(
            NodeExecutionTask task,
            IntegrationConfig config,
            String tenantId) {
        
        LOG.info("Executing Splitter for tenant: {}", tenantId);
        
        return Uni.createFrom().completionStage(() -> {
            CompletableFuture<NodeExecutionResult> future = new CompletableFuture<>();
            
            try {
                String routeId = createRouteId(task, "splitter");
                Object payload = config.payload();
                
                camelContext.addRoutes(new RouteBuilder() {
                    @Override
                    public void configure() {
                        from("direct:" + routeId)
                            .routeId(routeId)
                            .setHeader("tenantId", constant(tenantId))
                            
                            // Split the message
                            .split(body().tokenize(config.splitDelimiter()))
                                .streaming() // Process splits as they arrive
                                .parallelProcessing(config.parallelProcessing())
                                .aggregationStrategy((oldExchange, newExchange) -> {
                                    // Collect split results
                                    if (oldExchange == null) {
                                        newExchange.setProperty("splitResults", new ArrayList<>());
                                        return newExchange;
                                    }
                                    
                                    List<Object> results = oldExchange.getProperty("splitResults", List.class);
                                    results.add(newExchange.getIn().getBody());
                                    oldExchange.setProperty("splitResults", results);
                                    return oldExchange;
                                })
                                
                                // Process each split
                                .log("Processing split: ${body}")
                                
                                // Send to target endpoint if specified
                                .choice()
                                    .when(simple("'${header.targetEndpoint}' != ''"))
                                        .toD("${header.targetEndpoint}")
                                .end()
                            .end() // End split
                            
                            .process(exchange -> {
                                List<Object> results = exchange.getProperty("splitResults", List.class);
                                Map<String, Object> output = Map.of(
                                    "splits", results,
                                    "splitCount", results.size(),
                                    "timestamp", Instant.now()
                                );
                                
                                future.complete(NodeExecutionResult.success(
                                    task.runId(),
                                    task.nodeId(),
                                    task.attempt(),
                                    output,
                                    task.token()
                                ));
                            });
                    }
                });
                
                camelContext.getRouteController().startRoute(routeId);
                
                Map<String, Object> headers = Map.of(
                    "targetEndpoint", config.targetEndpoints().isEmpty() ? "" : config.targetEndpoints().get(0)
                );
                producerTemplate.sendBodyAndHeaders("direct:" + routeId, payload, headers);
                
                trackIntegration(task, routeId, Instant.now());
                
            } catch (Exception e) {
                LOG.error("Splitter failed", e);
                future.complete(NodeExecutionResult.failure(
                    task.runId(),
                    task.nodeId(),
                    task.attempt(),
                    ErrorInfo.of(e),
                    task.token()
                ));
            }
            
            return future;
        });
    }
    
    // ==================== AGGREGATOR ====================
    
    /**
     * Aggregator Pattern
     * Combines multiple messages into a single message
     */
    private Uni<NodeExecutionResult> executeAggregator(
            NodeExecutionTask task,
            IntegrationConfig config,
            String tenantId) {
        
        LOG.info("Executing Aggregator for tenant: {}", tenantId);
        
        return Uni.createFrom().completionStage(() -> {
            CompletableFuture<NodeExecutionResult> future = new CompletableFuture<>();
            
            try {
                String routeId = createRouteId(task, "aggregator");
                List<Object> messages = (List<Object>) config.payload();
                
                camelContext.addRoutes(new RouteBuilder() {
                    @Override
                    public void configure() {
                        from("direct:" + routeId)
                            .routeId(routeId)
                            .setHeader("tenantId", constant(tenantId))
                            
                            // Aggregate messages
                            .aggregate(constant(true), (oldExchange, newExchange) -> {
                                if (oldExchange == null) {
                                    List<Object> aggregated = new ArrayList<>();
                                    aggregated.add(newExchange.getIn().getBody());
                                    newExchange.getIn().setBody(aggregated);
                                    return newExchange;
                                }
                                
                                List<Object> aggregated = oldExchange.getIn().getBody(List.class);
                                aggregated.add(newExchange.getIn().getBody());
                                oldExchange.getIn().setBody(aggregated);
                                return oldExchange;
                            })
                            .completionSize(config.batchSize())
                            .completionTimeout(config.aggregationTimeout())
                            
                            // Process aggregated result
                            .log("Aggregated ${body.size} messages")
                            
                            // Apply aggregation strategy
                            .process(exchange -> {
                                List<Object> items = exchange.getIn().getBody(List.class);
                                Object aggregated = switch (config.aggregationStrategy()) {
                                    case "merge" -> mergeObjects(items);
                                    case "reduce" -> reduceItems(items, config.reduceFunction());
                                    case "collect" -> items;
                                    default -> items;
                                };
                                exchange.getIn().setBody(aggregated);
                            })
                            
                            // Send to target
                            .choice()
                                .when(simple("'${header.targetEndpoint}' != ''"))
                                    .toD("${header.targetEndpoint}")
                            .end()
                            
                            .process(exchange -> {
                                Object result = exchange.getIn().getBody();
                                Map<String, Object> output = Map.of(
                                    "aggregated", result,
                                    "aggregationStrategy", config.aggregationStrategy(),
                                    "itemCount", messages.size(),
                                    "timestamp", Instant.now()
                                );
                                
                                future.complete(NodeExecutionResult.success(
                                    task.runId(),
                                    task.nodeId(),
                                    task.attempt(),
                                    output,
                                    task.token()
                                ));
                            });
                    }
                });
                
                camelContext.getRouteController().startRoute(routeId);
                
                // Send all messages for aggregation
                Map<String, Object> headers = Map.of(
                    "targetEndpoint", config.targetEndpoints().isEmpty() ? "" : config.targetEndpoints().get(0)
                );
                
                for (Object message : messages) {
                    producerTemplate.sendBodyAndHeaders("direct:" + routeId, message, headers);
                }
                
                trackIntegration(task, routeId, Instant.now());
                
            } catch (Exception e) {
                LOG.error("Aggregator failed", e);
                future.complete(NodeExecutionResult.failure(
                    task.runId(),
                    task.nodeId(),
                    task.attempt(),
                    ErrorInfo.of(e),
                    task.token()
                ));
            }
            
            return future;
        });
    }
    
    // ==================== CONTENT ENRICHER ====================
    
    /**
     * Content Enricher Pattern
     * Enriches message with additional data from external source
     */
    private Uni<NodeExecutionResult> executeContentEnricher(
            NodeExecutionTask task,
            IntegrationConfig config,
            String tenantId) {
        
        LOG.info("Executing Content Enricher for tenant: {}", tenantId);
        
        return Uni.createFrom().completionStage(() -> {
            CompletableFuture<NodeExecutionResult> future = new CompletableFuture<>();
            
            try {
                String routeId = createRouteId(task, "enricher");
                Object payload = config.payload();
                
                camelContext.addRoutes(new RouteBuilder() {
                    @Override
                    public void configure() {
                        from("direct:" + routeId)
                            .routeId(routeId)
                            .setHeader("tenantId", constant(tenantId))
                            
                            // Store original body
                            .setProperty("originalBody", body())
                            
                            // Enrich with external data
                            .enrich(config.enrichmentEndpoint(), (original, resource) -> {
                                Map<String, Object> originalData = original.getIn().getBody(Map.class);
                                Map<String, Object> enrichmentData = resource.getIn().getBody(Map.class);
                                
                                // Merge data based on strategy
                                Map<String, Object> enriched = new HashMap<>(originalData);
                                
                                if (config.enrichmentStrategy().equals("merge")) {
                                    enriched.putAll(enrichmentData);
                                } else if (config.enrichmentStrategy().equals("nested")) {
                                    enriched.put(config.enrichmentKey(), enrichmentData);
                                }
                                
                                original.getIn().setBody(enriched);
                                return original;
                            })
                            
                            .log("Enriched message: ${body}")
                            
                            // Send to target
                            .choice()
                                .when(simple("'${header.targetEndpoint}' != ''"))
                                    .toD("${header.targetEndpoint}")
                            .end()
                            
                            .process(exchange -> {
                                Object result = exchange.getIn().getBody();
                                Map<String, Object> output = Map.of(
                                    "enriched", result,
                                    "enrichmentSource", config.enrichmentEndpoint(),
                                    "timestamp", Instant.now()
                                );
                                
                                future.complete(NodeExecutionResult.success(
                                    task.runId(),
                                    task.nodeId(),
                                    task.attempt(),
                                    output,
                                    task.token()
                                ));
                            });
                    }
                });
                
                camelContext.getRouteController().startRoute(routeId);
                
                Map<String, Object> headers = Map.of(
                    "targetEndpoint", config.targetEndpoints().isEmpty() ? "" : config.targetEndpoints().get(0)
                );
                producerTemplate.sendBodyAndHeaders("direct:" + routeId, payload, headers);
                
                trackIntegration(task, routeId, Instant.now());
                
            } catch (Exception e) {
                LOG.error("Content Enricher failed", e);
                future.complete(NodeExecutionResult.failure(
                    task.runId(),
                    task.nodeId(),
                    task.attempt(),
                    ErrorInfo.of(e),
                    task.token()
                ));
            }
            
            return future;
        });
    }
    
    // ==================== MESSAGE FILTER ====================
    
    /**
     * Message Filter Pattern
     * Filters messages based on criteria
     */
    private Uni<NodeExecutionResult> executeMessageFilter(
            NodeExecutionTask task,
            IntegrationConfig config,
            String tenantId) {
        
        LOG.info("Executing Message Filter for tenant: {}", tenantId);
        
        return Uni.createFrom().completionStage(() -> {
            CompletableFuture<NodeExecutionResult> future = new CompletableFuture<>();
            
            try {
                String routeId = createRouteId(task, "filter");
                Object payload = config.payload();
                
                camelContext.addRoutes(new RouteBuilder() {
                    @Override
                    public void configure() {
                        from("direct:" + routeId)
                            .routeId(routeId)
                            .setHeader("tenantId", constant(tenantId))
                            
                            // Apply filter
                            .filter(simple(config.filterExpression()))
                                .log("Message passed filter")
                                
                                // Send to target
                                .choice()
                                    .when(simple("'${header.targetEndpoint}' != ''"))
                                        .toD("${header.targetEndpoint}")
                                .end()
                                
                                .process(exchange -> {
                                    Object result = exchange.getIn().getBody();
                                    Map<String, Object> output = Map.of(
                                        "filtered", result,
                                        "passed", true,
                                        "timestamp", Instant.now()
                                    );
                                    
                                    future.complete(NodeExecutionResult.success(
                                        task.runId(),
                                        task.nodeId(),
                                        task.attempt(),
                                        output,
                                        task.token()
                                    ));
                                })
                            .end(); // End filter
                            
                        // Handle filtered out messages
                        from("direct:" + routeId + "-filtered")
                            .log("Message filtered out")
                            .process(exchange -> {
                                Map<String, Object> output = Map.of(
                                    "passed", false,
                                    "reason", "Did not match filter criteria",
                                    "timestamp", Instant.now()
                                );
                                
                                future.complete(NodeExecutionResult.success(
                                    task.runId(),
                                    task.nodeId(),
                                    task.attempt(),
                                    output,
                                    task.token()
                                ));
                            });
                    }
                });
                
                camelContext.getRouteController().startRoute(routeId);
                
                Map<String, Object> headers = Map.of(
                    "targetEndpoint", config.targetEndpoints().isEmpty() ? "" : config.targetEndpoints().get(0)
                );
                producerTemplate.sendBodyAndHeaders("direct:" + routeId, payload, headers);
                
                trackIntegration(task, routeId, Instant.now());
                
            } catch (Exception e) {
                LOG.error("Message Filter failed", e);
                future.complete(NodeExecutionResult.failure(
                    task.runId(),
                    task.nodeId(),
                    task.attempt(),
                    ErrorInfo.of(e),
                    task.token()
                ));
            }
            
            return future;
        });
    }
    
    // ==================== RECIPIENT LIST ====================
    
    /**
     * Recipient List Pattern
     * Routes message to multiple dynamic recipients
     */
    private Uni<NodeExecutionResult> executeRecipientList(
            NodeExecutionTask task,
            IntegrationConfig config,
            String tenantId) {
        
        LOG.info("Executing Recipient List for tenant: {}", tenantId);
        
        return Uni.createFrom().completionStage(() -> {
            CompletableFuture<NodeExecutionResult> future = new CompletableFuture<>();
            
            try {
                String routeId = createRouteId(task, "recipient-list");
                Object payload = config.payload();
                
                camelContext.addRoutes(new RouteBuilder() {
                    @Override
                    public void configure() {
                        from("direct:" + routeId)
                            .routeId(routeId)
                            .setHeader("tenantId", constant(tenantId))
                            
                            // Set recipient list header
                            .setHeader("recipients", constant(String.join(",", config.targetEndpoints())))
                            
                            // Route to all recipients
                            .recipientList(header("recipients"))
                                .parallelProcessing(config.parallelProcessing())
                                .aggregationStrategy((oldExchange, newExchange) -> {
                                    if (oldExchange == null) {
                                        newExchange.setProperty("responses", new ArrayList<>());
                                        return newExchange;
                                    }
                                    
                                    List<Object> responses = oldExchange.getProperty("responses", List.class);
                                    responses.add(newExchange.getIn().getBody());
                                    oldExchange.setProperty("responses", responses);
                                    return oldExchange;
                                })
                            .end()
                            
                            .process(exchange -> {
                                List<Object> responses = exchange.getProperty("responses", List.class);
                                Map<String, Object> output = Map.of(
                                    "responses", responses,
                                    "recipientCount", config.targetEndpoints().size(),
                                    "timestamp", Instant.now()
                                );
                                
                                future.complete(NodeExecutionResult.success(
                                    task.runId(),
                                    task.nodeId(),
                                    task.attempt(),
                                    output,
                                    task.token()
                                ));
                            });
                    }
                });
                
                camelContext.getRouteController().