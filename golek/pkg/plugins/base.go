package plugins

import (
	"context"
	"sync"

	"github.com/bhangun/golek/pkg/core"
	"go.uber.org/zap"
)

// ============================================================================
// BASE PLUGIN IMPLEMENTATION
// ============================================================================

// BasePlugin provides common plugin functionality
type BasePlugin struct {
	name    string
	version string
	pType   core.PluginType
	config  map[string]any
	logger  *zap.Logger
	mu      sync.RWMutex
}

// NewBasePlugin creates a new base plugin
func NewBasePlugin(name, version string, pType core.PluginType, logger *zap.Logger) *BasePlugin {
	return &BasePlugin{
		name:    name,
		version: version,
		pType:   pType,
		logger:  logger,
	}
}

func (p *BasePlugin) Name() string          { return p.name }
func (p *BasePlugin) Version() string       { return p.version }
func (p *BasePlugin) Type() core.PluginType { return p.pType }

func (p *BasePlugin) Initialize(config map[string]any) error {
	p.mu.Lock()
	defer p.mu.Unlock()

	p.config = config
	p.logger.Info("Plugin initialized", zap.String("name", p.name))
	return nil
}

func (p *BasePlugin) Start(ctx context.Context) error {
	p.logger.Info("Plugin started", zap.String("name", p.name))
	return nil
}

func (p *BasePlugin) Stop(ctx context.Context) error {
	p.logger.Info("Plugin stopped", zap.String("name", p.name))
	return nil
}

func (p *BasePlugin) HealthCheck(ctx context.Context) error {
	return nil
}

func (p *BasePlugin) DefaultConfig() map[string]any {
	return make(map[string]any)
}

func (p *BasePlugin) ValidateConfig(config map[string]any) error {
	return nil
}

func (p *BasePlugin) GetConfig() map[string]any {
	p.mu.RLock()
	defer p.mu.RUnlock()

	// Return copy
	configCopy := make(map[string]any)
	for k, v := range p.config {
		configCopy[k] = v
	}
	return configCopy
}
