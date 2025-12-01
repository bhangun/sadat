
## 🎯 **2. API Gateway (wayang-gateway)**

### **Purpose**
Unified entry point with AuthN/AuthZ, rate limiting, tenant resolution, and routing.

### **Project Structure**

```
wayang-gateway/
├── pom.xml
└── src/main/
    ├── java/tech/kayys/wayang/gateway/
    │   ├── filter/
    │   │   ├── AuthenticationFilter.java
    │   │   ├── TenantResolverFilter.java
    │   │   └── RateLimitFilter.java
    │   ├── security/
    │   │   ├── JwtValidator.java
    │   │   └── PermissionEvaluator.java
    │   ├── route/
    │   │   └── RouteConfiguration.java
    │   └── health/
    │       └── GatewayHealthCheck.java
    └── resources/
        └── application.yml
```

### 12. WAYANG-GATEWAY

```
wayang-gateway/
├── routing/        # RequestRouter, RouteRegistry
├── auth/           # AuthenticationFilter, JWT validation
├── ratelimit/      # RateLimiter, ThrottleController
└── circuit/        # CircuitBreaker, FallbackHandler
```