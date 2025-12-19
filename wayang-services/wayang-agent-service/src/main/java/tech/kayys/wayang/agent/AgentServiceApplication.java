package tech.kayys.wayang.agent;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.annotations.QuarkusMain;

@QuarkusMain
public class AgentServiceApplication {

    public static void main(String[] args) {
        Quarkus.run(args);
    }
}