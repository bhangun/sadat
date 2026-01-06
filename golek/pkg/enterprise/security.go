package enterprise

import (
	"context"
	"crypto/tls"
	"fmt"
	"sync"
	"time"

	"github.com/golang-jwt/jwt/v5"
	"go.uber.org/zap"
	"golang.org/x/time/rate"

	"tech.kayys.golek/pkg/core"
	"tech.kayys.golek/pkg/plugins"
)

// ============================================================================
// ADVANCED SECURITY PLUGIN
// ============================================================================

// AdvancedSecurityPlugin provides enterprise-grade security features
type AdvancedSecurityPlugin struct {
	*plugins.BasePlugin
	jwtSecret        []byte
	tokenExpiry      time.Duration
	rateLimiters     map[string]*rate.Limiter
	rateLimiterMutex sync.RWMutex
	tlsConfig        *tls.Config
	auditLog         *AuditLogger
	rbac             *RBACManager
}

// NewAdvancedSecurityPlugin creates a new security plugin
func NewAdvancedSecurityPlugin(config map[string]any, logger *zap.Logger) *AdvancedSecurityPlugin {
	plugin := &AdvancedSecurityPlugin{
		BasePlugin:   plugins.NewBasePlugin("advanced-security", "1.0.0", core.PluginTypeSecurity, logger),
		rateLimiters: make(map[string]*rate.Limiter),
		auditLog:     NewAuditLogger(logger),
		rbac:         NewRBACManager(),
	}

	plugin.Initialize(config)

	return plugin
}

func (p *AdvancedSecurityPlugin) Initialize(config map[string]any) error {
	p.BasePlugin.Initialize(config)

	// JWT configuration
	if secret, ok := config["jwt_secret"].(string); ok {
		p.jwtSecret = []byte(secret)
	}

	if expiry, ok := config["token_expiry"].(int); ok {
		p.tokenExpiry = time.Duration(expiry) * time.Hour
	} else {
		p.tokenExpiry = 24 * time.Hour
	}

	// Rate limiting configuration
	if rps, ok := config["rate_limit_rps"].(int); ok {
		p.configureDefaultRateLimit(rps)
	}

	// TLS configuration
	if requireTLS, ok := config["require_tls"].(bool); ok && requireTLS {
		p.configureTLS(config)
	}

	// RBAC configuration
	p.rbac.LoadPolicies(config)

	return nil
}

func (p *AdvancedSecurityPlugin) Start(ctx context.Context) error {
	p.BasePlugin.Start(ctx)
	p.auditLog.Start()
	return nil
}

func (p *AdvancedSecurityPlugin) Stop(ctx context.Context) error {
	p.auditLog.Stop()
	return p.BasePlugin.Stop(ctx)
}

// JWT Token Management
func (p *AdvancedSecurityPlugin) GenerateToken(tenantID core.TenantID, userID string, roles []string) (string, error) {
	claims := jwt.MapClaims{
		"tenant_id": tenantID,
		"user_id":   userID,
		"roles":     roles,
		"exp":       time.Now().Add(p.tokenExpiry).Unix(),
		"iat":       time.Now().Unix(),
	}

	token := jwt.NewWithClaims(jwt.SigningMethodHS256, claims)
	return token.SignedString(p.jwtSecret)
}

func (p *AdvancedSecurityPlugin) ValidateToken(tokenString string) (*TokenClaims, error) {
	token, err := jwt.Parse(tokenString, func(token *jwt.Token) (interface{}, error) {
		if _, ok := token.Method.(*jwt.SigningMethodHMAC); !ok {
			return nil, fmt.Errorf("unexpected signing method: %v", token.Header["alg"])
		}
		return p.jwtSecret, nil
	})

	if err != nil {
		return nil, err
	}

	if claims, ok := token.Claims.(jwt.MapClaims); ok && token.Valid {
		return &TokenClaims{
			TenantID: core.TenantID(claims["tenant_id"].(string)),
			UserID:   claims["user_id"].(string),
			Roles:    interfaceSliceToStringSlice(claims["roles"].([]interface{})),
		}, nil
	}

	return nil, fmt.Errorf("invalid token")
}

// Rate Limiting
func (p *AdvancedSecurityPlugin) CheckRateLimit(key string) bool {
	p.rateLimiterMutex.RLock()
	limiter, exists := p.rateLimiters[key]
	p.rateLimiterMutex.RUnlock()

	if !exists {
		p.rateLimiterMutex.Lock()
		limiter = rate.NewLimiter(rate.Every(time.Second), 100)
		p.rateLimiters[key] = limiter
		p.rateLimiterMutex.Unlock()
	}

	return limiter.Allow()
}

func (p *AdvancedSecurityPlugin) configureDefaultRateLimit(rps int) {
	p.rateLimiters["default"] = rate.NewLimiter(rate.Every(time.Second/time.Duration(rps)), rps)
}

func (p *AdvancedSecurityPlugin) configureTLS(config map[string]any) {
	// TLS configuration would go here
	p.tlsConfig = &tls.Config{
		MinVersion: tls.VersionTLS13,
	}
}

// RBAC Authorization
func (p *AdvancedSecurityPlugin) Authorize(claims *TokenClaims, resource, action string) error {
	return p.rbac.CheckPermission(claims.Roles, resource, action)
}

// Audit Logging
func (p *AdvancedSecurityPlugin) AuditLog(event AuditEvent) {
	p.auditLog.Log(event)
}

// ============================================================================
// AUDIT LOGGER
// ============================================================================

type AuditLogger struct {
	logger  *zap.Logger
	events  chan AuditEvent
	storage AuditStorage
	running bool
}

type AuditEvent struct {
	Timestamp  time.Time              `json:"timestamp"`
	TenantID   core.TenantID          `json:"tenant_id"`
	UserID     string                 `json:"user_id"`
	Action     string                 `json:"action"`
	Resource   string                 `json:"resource"`
	ResourceID string                 `json:"resource_id,omitempty"`
	Result     string                 `json:"result"` // success, failure
	Details    map[string]interface{} `json:"details,omitempty"`
	IPAddress  string                 `json:"ip_address,omitempty"`
}

type AuditStorage interface {
	Store(event AuditEvent) error
	Query(tenantID core.TenantID, from, to time.Time) ([]AuditEvent, error)
}

func NewAuditLogger(logger *zap.Logger) *AuditLogger {
	return &AuditLogger{
		logger:  logger,
		events:  make(chan AuditEvent, 1000),
		storage: &InMemoryAuditStorage{events: []AuditEvent{}},
	}
}

func (a *AuditLogger) Start() {
	a.running = true
	go a.processEvents()
}

func (a *AuditLogger) Stop() {
	a.running = false
	close(a.events)
}

func (a *AuditLogger) Log(event AuditEvent) {
	if a.running {
		event.Timestamp = time.Now()
		a.events <- event
	}
}

func (a *AuditLogger) processEvents() {
	for event := range a.events {
		if err := a.storage.Store(event); err != nil {
			a.logger.Error("Failed to store audit event", zap.Error(err))
		}

		// Also log to structured logger
		a.logger.Info("Audit event",
			zap.Time("timestamp", event.Timestamp),
			zap.String("tenant_id", event.TenantID.String()),
			zap.String("user_id", event.UserID),
			zap.String("action", event.Action),
			zap.String("resource", event.Resource),
			zap.String("result", event.Result),
		)
	}
}

type InMemoryAuditStorage struct {
	events []AuditEvent
	mu     sync.RWMutex
}

func (s *InMemoryAuditStorage) Store(event AuditEvent) error {
	s.mu.Lock()
	defer s.mu.Unlock()
	s.events = append(s.events, event)
	return nil
}

func (s *InMemoryAuditStorage) Query(tenantID core.TenantID, from, to time.Time) ([]AuditEvent, error) {
	s.mu.RLock()
	defer s.mu.RUnlock()

	var result []AuditEvent
	for _, event := range s.events {
		if event.TenantID == tenantID &&
			event.Timestamp.After(from) &&
			event.Timestamp.Before(to) {
			result = append(result, event)
		}
	}
	return result, nil
}

// ============================================================================
// RBAC MANAGER
// ============================================================================

type RBACManager struct {
	policies map[string]*Policy
	mu       sync.RWMutex
}

type Policy struct {
	Roles       []string
	Permissions []Permission
}

type Permission struct {
	Resource string
	Actions  []string
}

func NewRBACManager() *RBACManager {
	manager := &RBACManager{
		policies: make(map[string]*Policy),
	}

	// Default policies
	manager.AddPolicy("admin", Policy{
		Roles: []string{"admin"},
		Permissions: []Permission{
			{Resource: "*", Actions: []string{"*"}},
		},
	})

	manager.AddPolicy("operator", Policy{
		Roles: []string{"operator"},
		Permissions: []Permission{
			{Resource: "workflow", Actions: []string{"read", "execute", "cancel"}},
			{Resource: "definition", Actions: []string{"read"}},
		},
	})

	manager.AddPolicy("viewer", Policy{
		Roles: []string{"viewer"},
		Permissions: []Permission{
			{Resource: "*", Actions: []string{"read"}},
		},
	})

	return manager
}

func (r *RBACManager) AddPolicy(name string, policy Policy) {
	r.mu.Lock()
	defer r.mu.Unlock()
	r.policies[name] = &policy
}

func (r *RBACManager) LoadPolicies(config map[string]any) {
	// Load custom policies from configuration
}

func (r *RBACManager) CheckPermission(roles []string, resource, action string) error {
	r.mu.RLock()
	defer r.mu.RUnlock()

	for _, role := range roles {
		if policy, exists := r.policies[role]; exists {
			if r.hasPermission(policy, resource, action) {
				return nil
			}
		}
	}

	return fmt.Errorf("permission denied: %s %s for roles %v", action, resource, roles)
}

func (r *RBACManager) hasPermission(policy *Policy, resource, action string) bool {
	for _, perm := range policy.Permissions {
		if perm.Resource == "*" || perm.Resource == resource {
			for _, a := range perm.Actions {
				if a == "*" || a == action {
					return true
				}
			}
		}
	}
	return false
}
