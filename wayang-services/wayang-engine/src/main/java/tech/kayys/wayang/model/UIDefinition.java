package tech.kayys.wayang.model;

import java.util.ArrayList;
import java.util.List;

/**
 * UIDefinition - Visual layout information
 */
public class UIDefinition {
    public CanvasState canvas = new CanvasState();
    public List<NodeUI> nodes = new ArrayList<>();
    public List<ConnectionUI> connections = new ArrayList<>();

    public static class CanvasState {
        public double zoom = 1.0;
        public Point offset = new Point(0, 0);
        public String background = "grid";
        public boolean snapToGrid = true;
    }

    public static class NodeUI {
        public String ref; // Node ID
        public Point position;
        public Size size;
        public String icon;
        public String color;
        public String shape = "rectangle";
        public boolean collapsed = false;
        public int zIndex = 0;
    }

    public static class ConnectionUI {
        public String ref; // Connection ID
        public String color;
        public String pathStyle = "bezier";
    }

    public static class Point {
        public double x;
        public double y;

        public Point() {
        }

        public Point(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }

    public static class Size {
        public double width;
        public double height;
    }
}
