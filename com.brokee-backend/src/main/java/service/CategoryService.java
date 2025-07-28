package service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import mapper.CategoryMapper;
import model.entity.Category;
import model.home.CategoryResponseDTO;
import model.response.ServiceResponseDTO;
import model.response.ServiceResponseDirector;
import repository.CategoryRepository;

import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class CategoryService {

    @Inject
    CategoryRepository repo;

    @Inject
    CategoryMapper map;

    public ServiceResponseDTO<List<CategoryResponseDTO>> listAll() {
        var dtos = repo.listAll()
                .stream()
                .map(map::toDto)
                .collect(Collectors.toList());
        return ServiceResponseDirector.successOk(dtos, "All categories");
    }

    public ServiceResponseDTO<List<CategoryResponseDTO>> search(String term) {
        var dtos = repo.findByNameLike(term)
                .stream()
                .map(map::toDto)
                .collect(Collectors.toList());
        return ServiceResponseDirector.successOk(dtos, "Search results");
    }

    @Transactional
    public Category getOrCreateRevolutCategory() {
        return repo.findByName("Revolut").orElse(null);
    }
}
