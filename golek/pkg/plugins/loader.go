package plugins

import (
	"fmt"

	"github.com/bhangun/golek/pkg/core"
	"go.uber.org/zap"
)

// ============================================================================
// PLUGIN LOADER
// ============================================================================

// PluginLoader loads plugins from configuration
type PluginLoader struct {
	registry *DefaultPluginRegistry
	logger   *zap.Logger
}

// NewPluginLoader creates a new plugin loader
func NewPluginLoader(registry *DefaultPluginRegistry, logger *zap.Logger) *PluginLoader {
	return &PluginLoader{
		registry: registry,
		logger:   logger,
	}
}

// LoadFromConfig loads plugins from configuration
func (l *PluginLoader) LoadFromConfig(config *PluginConfig) error {
	l.logger.Info("Loading plugins from configuration",
		zap.Int("count", len(config.Plugins)),
	)

	for _, pluginCfg := range config.Plugins {
		plugin, err := l.createPlugin(pluginCfg)
		if err != nil {
			l.logger.Error("Failed to create plugin",
				zap.String("name", pluginCfg.Name),
				zap.Error(err),
			)
			continue
		}

		if err := l.registry.Register(plugin); err != nil {
			l.logger.Error("Failed to register plugin",
				zap.String("name", pluginCfg.Name),
				zap.Error(err),
			)
			continue
		}

		// Enable if configured
		if pluginCfg.Enabled {
			if err := l.registry.EnablePlugin(pluginCfg.Name); err != nil {
				l.logger.Error("Failed to enable plugin",
					zap.String("name", pluginCfg.Name),
					zap.Error(err),
				)
			}
		}
	}

	return nil
}

// createPlugin creates a plugin instance from configuration
func (l *PluginLoader) createPlugin(cfg PluginConfigEntry) (core.Plugin, error) {
	// Factory pattern for creating plugins based on type
	switch cfg.Type {
	case "security":
		return NewSecurityPlugin(cfg.Name, cfg.Version, cfg.Config, l.logger), nil
	case "storage":
		return NewStoragePlugin(cfg.Name, cfg.Version, cfg.Config, l.logger), nil
	case "messaging":
		return NewMessagingPlugin(cfg.Name, cfg.Version, cfg.Config, l.logger), nil
	case "observability":
		return NewObservabilityPlugin(cfg.Name, cfg.Version, cfg.Config, l.logger), nil
	default:
		return nil, fmt.Errorf("unknown plugin type: %s", cfg.Type)
	}
}

// PluginConfig represents plugin configuration
type PluginConfig struct {
	Plugins []PluginConfigEntry `json:"plugins" yaml:"plugins"`
}

// PluginConfigEntry represents a single plugin configuration
type PluginConfigEntry struct {
	Name    string         `json:"name" yaml:"name"`
	Type    string         `json:"type" yaml:"type"`
	Version string         `json:"version" yaml:"version"`
	Enabled bool           `json:"enabled" yaml:"enabled"`
	Config  map[string]any `json:"config" yaml:"config"`
}
