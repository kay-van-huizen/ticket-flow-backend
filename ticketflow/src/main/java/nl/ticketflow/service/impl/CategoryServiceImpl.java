package nl.ticketflow.service.impl;

import nl.ticketflow.dto.request.ChangeCategoryStatusRequest;
import nl.ticketflow.dto.request.CreateCategoryRequest;
import nl.ticketflow.dto.request.UpdateCategoryRequest;
import nl.ticketflow.dto.response.CategoryResponse;
import nl.ticketflow.entity.Category;
import nl.ticketflow.entity.CategoryStatus;
import nl.ticketflow.exception.BusinessRuleException;
import nl.ticketflow.exception.ResourceNotFoundException;
import nl.ticketflow.mapper.CategoryMapper;
import nl.ticketflow.repository.CategoryRepository;
import nl.ticketflow.repository.TicketRepository;
import nl.ticketflow.service.CategoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final TicketRepository ticketRepository;
    private final CategoryMapper categoryMapper;

    public CategoryServiceImpl(CategoryRepository categoryRepository, TicketRepository ticketRepository,
                               CategoryMapper categoryMapper) {
        this.categoryRepository = categoryRepository;
        this.ticketRepository = ticketRepository;
        this.categoryMapper = categoryMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAll() {
        return categoryRepository.findAll().stream()
                .map(categoryMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getRoots() {
        return categoryRepository.findByParentIsNull().stream()
                .map(categoryMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getById(UUID id) {
        return categoryMapper.toResponse(findCategoryOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getChildren(UUID parentId) {
        // Unknown parent gives 404 instead of an empty list
        findCategoryOrThrow(parentId);
        return categoryRepository.findByParentId(parentId).stream()
                .map(categoryMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public CategoryResponse create(CreateCategoryRequest request) {
        Category parent = request.parentId() == null ? null : findCategoryOrThrow(request.parentId());
        if (parent != null && parent.getStatus() == CategoryStatus.INACTIVE) {
            throw new BusinessRuleException("Een inactieve categorie kan niet als ouder worden gekozen");
        }

        Category category = categoryMapper.toEntity(request);
        category.setParent(parent);
        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public CategoryResponse update(UUID id, UpdateCategoryRequest request) {
        Category category = findCategoryOrThrow(id);

        // PUT is a full replacement, so a missing parentId makes this a root category
        Category newParent = null;
        if (request.parentId() != null) {
            if (request.parentId().equals(id)) {
                throw new BusinessRuleException("Een categorie kan niet zijn eigen ouder zijn");
            }
            newParent = findCategoryOrThrow(request.parentId());

            if (category.getStatus() == CategoryStatus.ACTIVE && newParent.getStatus() == CategoryStatus.INACTIVE) {
                throw new BusinessRuleException("Een inactieve categorie kan niet als ouder worden gekozen");
            }
            // Walk up from the new parent; finding this category means the move would create a cycle
            for (Category ancestor = newParent; ancestor != null; ancestor = ancestor.getParent()) {
                if (ancestor.getId().equals(id)) {
                    throw new BusinessRuleException("Een categorie kan geen subcategorie van een eigen subcategorie als ouder hebben");
                }
            }
        }

        categoryMapper.updateEntity(category, request);
        category.setParent(newParent);
        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public CategoryResponse changeStatus(UUID id, ChangeCategoryStatusRequest request) {
        Category category = findCategoryOrThrow(id);

        if (request.status() == CategoryStatus.INACTIVE
                && categoryRepository.existsByParentIdAndStatus(id, CategoryStatus.ACTIVE)) {
            throw new BusinessRuleException("Categorie kan niet worden gedeactiveerd zolang er actieve subcategorieën zijn");
        }
        if (request.status() == CategoryStatus.ACTIVE
                && category.getParent() != null && category.getParent().getStatus() == CategoryStatus.INACTIVE) {
            throw new BusinessRuleException("Een subcategorie kan niet worden geactiveerd zolang de bovenliggende categorie inactief is");
        }

        category.setStatus(request.status());
        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Category category = findCategoryOrThrow(id);

        if (categoryRepository.existsByParentId(id)) {
            throw new BusinessRuleException("Categorie kan niet worden verwijderd zolang er subcategorieën zijn");
        }
        if (ticketRepository.existsByCategoryId(id)) {
            throw new BusinessRuleException("Categorie kan niet worden verwijderd zolang er tickets aan gekoppeld zijn");
        }

        categoryRepository.delete(category);
    }

    private Category findCategoryOrThrow(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categorie met id " + id + " niet gevonden"));
    }
}
