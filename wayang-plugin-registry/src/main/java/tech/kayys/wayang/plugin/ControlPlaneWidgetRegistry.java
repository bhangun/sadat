package tech.kayys.wayang.plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * UI Widget Registry - Independent from backend
 */
@ApplicationScoped
public class ControlPlaneWidgetRegistry {

    private static final Logger LOG = Logger.getLogger(ControlPlaneWidgetRegistry.class);

    private final Map<String, UIWidgetDefinition> widgetRegistry = new ConcurrentHashMap<>();

    public void register(UIWidgetDefinition widget) {
        widgetRegistry.put(widget.widgetId, widget);
        LOG.infof("Registered widget: %s (type: %s)", widget.widgetId, widget.type);
    }

    public void unregister(String widgetId) {
        widgetRegistry.remove(widgetId);
    }

    public UIWidgetDefinition get(String widgetId) {
        return widgetRegistry.get(widgetId);
    }

    public List<UIWidgetDefinition> getAll() {
        return new ArrayList<>(widgetRegistry.values());
    }
}