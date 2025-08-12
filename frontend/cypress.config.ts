import { defineConfig } from "cypress";
import { environment } from "./src/app/environments/environment";

export default defineConfig({
  e2e: {
    baseUrl: "http://localhost:4200",
    retries: { runMode: 2, openMode: 0 },
    video: true,
    setupNodeEvents(on, config) {},
  },

  env: {
    keycloakUrl: environment.keycloak.url,
    keycloakUser: environment.keycloak.user,
    keycloakPass: environment.keycloak.password,
  },

  component: {
    devServer: {
      framework: "angular",
      bundler: "webpack",
    },
    specPattern: "**/*.cy.ts",
  },
});
