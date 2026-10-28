package nl.ticketflow.mapper;

import nl.ticketflow.dto.request.CreateCategoryRequest;
import nl.ticketflow.dto.request.UpdateCategoryRequest;
import nl.ticketflow.dto.response.CategoryResponse;
import nl.ticketflow.entity.Category;
import nl.ticketflow.entity.CategoryStatus;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {

    // Parent is resolved and validated in CategoryServiceImpl
    public Category toEntity(CreateCategoryRequest request) {
        Category category = new Category();
        category.setTitle(request.title());
        category.setDescription(request.description());
        category.setColor(request.color());
        category.setStatus(CategoryStatus.ACTIVE);
        return category;
    }

    public void updateEntity(Category category, UpdateCategoryRequest request) {
        category.setTitle(request.title());
        category.setDescription(request.description());
        category.setColor(request.color());
    }

    public CategoryResponse toResponse(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getTitle(),
                category.getDescription(),
                category.getColor(),
                category.getStatus(),
                category.getParent() == null ? null : category.getParent().getId()
        );
    }
}
