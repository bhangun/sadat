package tech.kayys.wayang.agent.service;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.Priorities;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;
import tech.kayys.wayang.agent.dto.AgentPrincipal;

/**
 * Rate Limiting Filter
 * Implements token bucket algorithm
 */
@Provider
@Priority(Priorities.AUTHORIZATION + 1)
public class RateLimitingFilter implements ContainerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(RateLimitingFilter.class);

    @Inject
    RateLimiter rateLimiter;

    @Inject
    SecurityContext securityContext;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        AgentPrincipal principal = securityContext.getPrincipal();

        if (principal == null) {
            return; // Skip if not authenticated
        }

        String key = makeRateLimitKey(principal);

        if (!rateLimiter.allowRequest(key)) {
            LOG.warn("Rate limit exceeded for: {}", key);
            requestContext.abortWith(
                    Response.status(429) // Too Many Requests
                            .entity(Map.of(
                                    "error", "rate_limit_exceeded",
                                    "message", "Too many requests. Please try again later."))
                            .build());
        }
    }

    private String makeRateLimitKey(AgentPrincipal principal) {
        return principal.tenantId() + ":" + principal.userId();
    }
}