package mapper;

import model.entity.Category;
import model.home.CategoryResponseDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "cdi")
public interface CategoryMapper {
    CategoryResponseDTO toDto(Category entity);
}
