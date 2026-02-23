package org.example.controller;

import org.example.model.source.SourceCategory;
import org.example.model.source.SourceCategoryDTO;
import org.example.service.CategoryService;
import org.example.service.SourceCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;
    private final SourceCategoryService sourceCategoryService;

    @Autowired
    public CategoryController(CategoryService categoryService, SourceCategoryService sourceCategoryService) {
        this.categoryService = categoryService;
        this.sourceCategoryService = sourceCategoryService;
    }

    @GetMapping("")
    public List<SourceCategoryDTO> getValidCategories() {
        return sourceCategoryService.getAvailableCategoryDTOs();
    }

    @GetMapping("/subcategories")
    public List<SourceCategoryDTO> getSubcategories(@RequestParam SourceCategory category) {
        return sourceCategoryService.getSubcategoriesByCategory(category);
    }

    @GetMapping("/device_types")
    public List<String> getDeviceTypes() {
        return categoryService.getDeviceTypes();
    }

    @GetMapping("/sources")
    public List<String> getSourcesByCategory(
            @RequestParam SourceCategory category,
            @RequestParam(required = false) String subCategory) {
        return categoryService.getSourcesByCategory(category, subCategory);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<String> handleInvalidCategory(MethodArgumentTypeMismatchException ex) {
        if (ex.getRequiredType() != null && ex.getRequiredType().equals(SourceCategory.class)) {
            String valid = sourceCategoryService.getAvailableCategoriesAsString();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Invalid category '" + ex.getValue() + "'. Valid categories are: " + valid);
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }
}
