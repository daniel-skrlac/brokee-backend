package security;

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class SecurityUtils {
    @Inject
    SecurityIdentity identity;

    public String getCurrentUser() {
        return identity.getPrincipal().getName();
    }
}
