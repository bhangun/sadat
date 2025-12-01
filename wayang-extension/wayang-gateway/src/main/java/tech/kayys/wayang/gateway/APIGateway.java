public interface APIGateway {
    Response route(Request request);
    void registerRoute(RouteDefinition route);
    void unregisterRoute(String routeId);
    List<RouteDefinition> listRoutes();
}
