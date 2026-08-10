package com.financemanager.repository;

import com.financemanager.entity.Category;
import com.financemanager.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByOwnerIsNull();

    List<Category> findByOwner(User owner);

    Optional<Category> findByOwnerIsNullAndNameIgnoreCase(String name);

    Optional<Category> findByOwnerAndNameIgnoreCase(User owner, String name);

    boolean existsByOwnerAndNameIgnoreCase(User owner, String name);
}
