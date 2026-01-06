// Package plugins provides a modular plugin system for extending golek functionality

package plugins

import (
	"context"
	"fmt"
	"sync"

	"go.uber.org/zap"

	"tech.kayys.golek/pkg/core"
)

// ============================================================================
// PLUGIN REGISTRY
// ============================================================================

// DefaultPluginRegistry manages plugin lifecycle and registration
type DefaultPluginRegistry struct {
	plugins        map[string]core.Plugin
	enabledPlugins map[string]bool
	logger         *zap.Logger
	mu             sync.RWMutex
}

// NewPluginRegistry creates a new plugin registry
func NewPluginRegistry(logger *zap.Logger) *DefaultPluginRegistry {
	return &DefaultPluginRegistry{
		plugins:        make(map[string]core.Plugin),
		enabledPlugins: make(map[string]bool),
		logger:         logger,
	}
}

// Register registers a new plugin
func (r *DefaultPluginRegistry) Register(plugin core.Plugin) error {
	r.mu.Lock()
	defer r.mu.Unlock()

	name := plugin.Name()

	if _, exists := r.plugins[name]; exists {
		return fmt.Errorf("plugin already registered: %s", name)
	}

	// Validate configuration
	if err := plugin.ValidateConfig(plugin.DefaultConfig()); err != nil {
		return fmt.Errorf("invalid default config for plugin %s: %w", name, err)
	}

	r.plugins[name] = plugin
	r.enabledPlugins[name] = false // Disabled by default

	r.logger.Info("Plugin registered",
		zap.String("name", name),
		zap.String("version", plugin.Version()),
		zap.String("type", string(plugin.Type())),
	)

	return nil
}

// Unregister removes a plugin
func (r *DefaultPluginRegistry) Unregister(name string) error {
	r.mu.Lock()
	defer r.mu.Unlock()

	plugin, exists := r.plugins[name]
	if !exists {
		return fmt.Errorf("plugin not found: %s", name)
	}

	// Stop plugin if enabled
	if r.enabledPlugins[name] {
		ctx := context.Background()
		if err := plugin.Stop(ctx); err != nil {
			r.logger.Error("Failed to stop plugin during unregister",
				zap.String("plugin", name),
				zap.Error(err),
			)
		}
	}

	delete(r.plugins, name)
	delete(r.enabledPlugins, name)

	r.logger.Info("Plugin unregistered", zap.String("name", name))

	return nil
}

// Get retrieves a plugin by name
func (r *DefaultPluginRegistry) Get(name string) (core.Plugin, error) {
	r.mu.RLock()
	defer r.mu.RUnlock()

	plugin, exists := r.plugins[name]
	if !exists {
		return nil, fmt.Errorf("plugin not found: %s", name)
	}

	return plugin, nil
}

// List returns all registered plugins
func (r *DefaultPluginRegistry) List() []core.Plugin {
	r.mu.RLock()
	defer r.mu.RUnlock()

	plugins := make([]core.Plugin, 0, len(r.plugins))
	for _, plugin := range r.plugins {
		plugins = append(plugins, plugin)
	}

	return plugins
}

// EnablePlugin enables and starts a plugin
func (r *DefaultPluginRegistry) EnablePlugin(name string) error {
	r.mu.Lock()
	defer r.mu.Unlock()

	plugin, exists := r.plugins[name]
	if !exists {
		return fmt.Errorf("plugin not found: %s", name)
	}

	if r.enabledPlugins[name] {
		return nil // Already enabled
	}

	// Initialize plugin
	config := plugin.DefaultConfig()
	if err := plugin.Initialize(config); err != nil {
		return fmt.Errorf("failed to initialize plugin %s: %w", name, err)
	}

	// Start plugin
	ctx := context.Background()
	if err := plugin.Start(ctx); err != nil {
		return fmt.Errorf("failed to start plugin %s: %w", name, err)
	}

	r.enabledPlugins[name] = true

	r.logger.Info("Plugin enabled", zap.String("name", name))

	return nil
}

// DisablePlugin stops and disables a plugin
func (r *DefaultPluginRegistry) DisablePlugin(name string) error {
	r.mu.Lock()
	defer r.mu.Unlock()

	plugin, exists := r.plugins[name]
	if !exists {
		return fmt.Errorf("plugin not found: %s", name)
	}

	if !r.enabledPlugins[name] {
		return nil // Already disabled
	}

	// Stop plugin
	ctx := context.Background()
	if err := plugin.Stop(ctx); err != nil {
		return fmt.Errorf("failed to stop plugin %s: %w", name, err)
	}

	r.enabledPlugins[name] = false

	r.logger.Info("Plugin disabled", zap.String("name", name))

	return nil
}

// IsEnabled checks if a plugin is enabled
func (r *DefaultPluginRegistry) IsEnabled(name string) bool {
	r.mu.RLock()
	defer r.mu.RUnlock()

	return r.enabledPlugins[name]
}

// StartAll starts all enabled plugins
func (r *DefaultPluginRegistry) StartAll(ctx context.Context) error {
	r.mu.Lock()
	defer r.mu.Unlock()

	for name, plugin := range r.plugins {
		if r.enabledPlugins[name] {
			if err := plugin.Start(ctx); err != nil {
				r.logger.Error("Failed to start plugin",
					zap.String("plugin", name),
					zap.Error(err),
				)
				// Continue with other plugins
			}
		}
	}

	return nil
}

// StopAll stops all plugins
func (r *DefaultPluginRegistry) StopAll(ctx context.Context) error {
	r.mu.Lock()
	defer r.mu.Unlock()

	for name, plugin := range r.plugins {
		if r.enabledPlugins[name] {
			if err := plugin.Stop(ctx); err != nil {
				r.logger.Error("Failed to stop plugin",
					zap.String("plugin", name),
					zap.Error(err),
				)
			}
		}
	}

	return nil
}

// HealthCheckAll performs health check on all enabled plugins
func (r *DefaultPluginRegistry) HealthCheckAll(ctx context.Context) map[string]error {
	r.mu.RLock()
	defer r.mu.RUnlock()

	results := make(map[string]error)

	for name, plugin := range r.plugins {
		if r.enabledPlugins[name] {
			results[name] = plugin.HealthCheck(ctx)
		}
	}

	return results
}
