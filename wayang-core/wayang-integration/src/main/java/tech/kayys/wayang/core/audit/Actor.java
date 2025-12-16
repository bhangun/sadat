


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Actor {
    private ActorType type;
    private String id;
    private String name;
    
    public static Actor system() {
        return Actor.builder()
            .type(ActorType.SYSTEM)
            .id("system")
            .name("Wayang System")
            .build();
    }
    
    public static Actor user(String userId, String name) {
        return Actor.builder()
            .type(ActorType.USER)
            .id(userId)
            .name(name)
            .build();
    }
}