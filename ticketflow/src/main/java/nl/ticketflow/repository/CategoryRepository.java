package nl.ticketflow.repository;

import nl.ticketflow.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

    List<Category> findByParentIsNull();

    List<Category> findByParentId(UUID parentId);

    boolean existsByParentId(UUID parentId);
}
