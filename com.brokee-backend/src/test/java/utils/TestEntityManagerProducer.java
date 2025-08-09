package utils;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import jakarta.persistence.EntityManager;
import org.mockito.Mockito;

@Singleton
public class TestEntityManagerProducer {
    @Produces
    EntityManager entityManager() {
        return Mockito.mock(EntityManager.class);
    }
}