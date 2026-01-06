package enterprise

import (
	"fmt"
	"sync"
	"time"

	"github.com/bhangun/golek/pkg/core"
	"go.uber.org/zap"
	"tech.kayys.golek/pkg/plugins"
)

// ============================================================================
// CIRCUIT BREAKER PLUGIN
// ============================================================================

type CircuitBreakerPlugin struct {
	*plugins.BasePlugin
	breakers map[string]*CircuitBreaker
	mu       sync.RWMutex
}

type CircuitBreaker struct {
	name            string
	maxFailures     int
	resetTimeout    time.Duration
	state           CircuitState
	failures        int
	lastFailureTime time.Time
	mu              sync.RWMutex
}

type CircuitState string

const (
	StateClosed   CircuitState = "CLOSED"
	StateOpen     CircuitState = "OPEN"
	StateHalfOpen CircuitState = "HALF_OPEN"
)

func NewCircuitBreakerPlugin(config map[string]any, logger *zap.Logger) *CircuitBreakerPlugin {
	plugin := &CircuitBreakerPlugin{
		BasePlugin: plugins.NewBasePlugin("circuit-breaker", "1.0.0", core.PluginTypeMiddleware, logger),
		breakers:   make(map[string]*CircuitBreaker),
	}

	plugin.Initialize(config)

	return plugin
}

func (p *CircuitBreakerPlugin) GetBreaker(name string) *CircuitBreaker {
	p.mu.RLock()
	breaker, exists := p.breakers[name]
	p.mu.RUnlock()

	if !exists {
		p.mu.Lock()
		breaker = &CircuitBreaker{
			name:         name,
			maxFailures:  5,
			resetTimeout: 60 * time.Second,
			state:        StateClosed,
		}
		p.breakers[name] = breaker
		p.mu.Unlock()
	}

	return breaker
}

func (cb *CircuitBreaker) Call(fn func() error) error {
	cb.mu.Lock()

	// Check if circuit is open
	if cb.state == StateOpen {
		if time.Since(cb.lastFailureTime) > cb.resetTimeout {
			// Try half-open
			cb.state = StateHalfOpen
			cb.mu.Unlock()
		} else {
			cb.mu.Unlock()
			return fmt.Errorf("circuit breaker open for %s", cb.name)
		}
	} else {
		cb.mu.Unlock()
	}

	// Execute function
	err := fn()

	cb.mu.Lock()
	defer cb.mu.Unlock()

	if err != nil {
		cb.failures++
		cb.lastFailureTime = time.Now()

		if cb.failures >= cb.maxFailures {
			cb.state = StateOpen
		}

		return err
	}

	// Success - reset
	if cb.state == StateHalfOpen {
		cb.state = StateClosed
	}
	cb.failures = 0

	return nil
}

// ============================================================================
// HELPER TYPES
// ============================================================================

type TokenClaims struct {
	TenantID core.TenantID
	UserID   string
	Roles    []string
}

func interfaceSliceToStringSlice(slice []interface{}) []string {
	result := make([]string, len(slice))
	for i, v := range slice {
		result[i] = v.(string)
	}
	return result
}
