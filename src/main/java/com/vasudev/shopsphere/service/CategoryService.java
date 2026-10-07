package com.vasudev.shopsphere.service;

import com.vasudev.shopsphere.entity.Category;
import com.vasudev.shopsphere.repository.CategoryRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @CacheEvict(value = "categories", key = "#category.id", condition = "#category != null && #category.id != null")
    public Category saveCategory(Category category) {
        return categoryRepository.save(category);
    }

    @Cacheable(value = "categories", key = "#id")
    public Optional<Category> getCategoryById(Long id) {
        return categoryRepository.findById(id);
    }

    public Optional<Category> getCategoryByName(String name) {
        return categoryRepository.findByName(name);
    }

    public List<Category> getActiveCategories() {
        return categoryRepository.findByActiveTrue();
    }

    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    @CacheEvict(value = "categories", key = "#id")
    public Category updateCategory(Long id, Category category) {
        category.setId(id);
        return categoryRepository.save(category);
    }

    @CacheEvict(value = "categories", key = "#id")
    public void deleteCategory(Long id) {
        categoryRepository.deleteById(id);
    }
}

