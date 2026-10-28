package nl.ticketflow.service;

import nl.ticketflow.dto.request.ChangeCategoryStatusRequest;
import nl.ticketflow.dto.request.CreateCategoryRequest;
import nl.ticketflow.dto.request.UpdateCategoryRequest;
import nl.ticketflow.dto.response.CategoryResponse;

import java.util.List;
import java.util.UUID;

public interface CategoryService {

    List<CategoryResponse> getAll();

    List<CategoryResponse> getRoots();

    CategoryResponse getById(UUID id);

    List<CategoryResponse> getChildren(UUID parentId);

    CategoryResponse create(CreateCategoryRequest request);

    CategoryResponse update(UUID id, UpdateCategoryRequest request);

    CategoryResponse changeStatus(UUID id, ChangeCategoryStatusRequest request);

    void delete(UUID id);
}
