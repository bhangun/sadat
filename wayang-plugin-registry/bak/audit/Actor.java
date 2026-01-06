/**
 * Actor in audit event
 */
@Data
@Builder
class Actor {
    private ActorType type;
    private String id;
    private String name;
}
