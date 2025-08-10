package utils;

import io.quarkus.test.junit.QuarkusTestProfile;
import java.util.Map;

public class NoDbProfile implements QuarkusTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
                "quarkus.http.test-port", "0",
                "quarkus.oidc.enabled", "false",
                "quarkus.datasource.devservices.enabled", "false",
                "quarkus.hibernate-orm.enabled", "false",
                "quarkus.flyway.migrate-at-start", "false",
                "quarkus.scheduler.enabled", "false"
        );
    }

}
