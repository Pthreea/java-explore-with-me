package ru.practicum.ewm.admin.service;

import ru.practicum.ewm.admin.dto.CategoryDto;
import ru.practicum.ewm.admin.dto.NewCategoryDto;

public interface AdminCategoryService {
    CategoryDto createCategory(NewCategoryDto dto);

    void deleteCategory(Long catId);

    CategoryDto updateCategory(Long catId, CategoryDto dto);
}
