package utils;

import io.quarkus.test.junit.QuarkusTestProfile;

import java.util.Map;

public class NoDbProfileWithPermitOidc implements QuarkusTestProfile {
    @Override
    public String getConfigProfile() { return "no-db"; }

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
                "quarkus.http.test-port", "0",
                "quarkus.datasource.devservices.enabled", "false",
                "quarkus.hibernate-orm.enabled", "false",
                "quarkus.flyway.migrate-at-start", "false",
                "quarkus.scheduler.enabled", "false",
                "quarkus.oidc.enabled", "false",
                "quarkus.http.auth.permission.secured.policy", "permit"
        );
    }
}
