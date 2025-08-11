import { defineConfig } from 'cypress';
import { environment } from './app/environments/environment';

export default defineConfig({
    e2e: {
        baseUrl: 'https://web.test.example.com',
        retries: { runMode: 2, openMode: 0 },
        video: true,
        setupNodeEvents(on, config) {
        },
    },
    env: {
        keycloakUrl: environment.keycloak.url,
        keycloakUser: environment.keycloak.user,
        keycloakPass: environment.keycloak.password
    }
});
