package service;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import mapper.CategoryMapper;
import model.entity.Category;
import model.home.CategoryResponseDTO;
import model.response.ServiceResponseDTO;
import org.junit.jupiter.api.Test;
import repository.CategoryRepository;
import utils.NoDbProfile;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

@QuarkusTest
@TestProfile(NoDbProfile.class)
class CategoryServiceTest {

    @InjectMock
    CategoryRepository repo;
    @InjectMock
    CategoryMapper map;

    @Inject
    CategoryService svc;

    @Test
    void listAll_RepositoryReturnsEntities_ReturnsMappedDTOs() {
        var c1 = new Category();
        var c2 = new Category();
        when(repo.listAll()).thenReturn(List.of(c1, c2));
        when(map.toDto(c1)).thenReturn(new CategoryResponseDTO(1L, "Food"));
        when(map.toDto(c2)).thenReturn(new CategoryResponseDTO(2L, "Travel"));

        ServiceResponseDTO<List<CategoryResponseDTO>> res = svc.listAll();

        assertThat(res.isSuccess()).isTrue();
        assertThat(res.getData()).extracting("name").containsExactly("Food", "Travel");
    }

    @Test
    void search_TermProvided_ReturnsMappedResults() {
        when(repo.findByNameLike("foo")).thenReturn(List.of(new Category()));
        when(map.toDto(any())).thenReturn(new CategoryResponseDTO(3L, "Foo"));

        var res = svc.search("foo");

        assertThat(res.isSuccess()).isTrue();
        assertThat(res.getData()).hasSize(1);
        assertThat(res.getData().get(0).name()).isEqualTo("Foo");
    }

    @Test
    void getOrCreateRevolutCategory_Missing_ReturnsNull() {
        when(repo.findByName("Revolut")).thenReturn(Optional.empty());
        assertThat(svc.getOrCreateRevolutCategory()).isNull();
    }

    @Test
    void getOrCreateRevolutCategory_Existing_ReturnsEntity() {
        var c = new Category();
        c.setId(99L);
        when(repo.findByName("Revolut")).thenReturn(Optional.of(c));
        assertThat(svc.getOrCreateRevolutCategory()).isSameAs(c);
    }
}
