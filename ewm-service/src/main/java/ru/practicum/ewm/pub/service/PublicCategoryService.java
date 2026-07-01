package ru.practicum.ewm.pub.service;

import ru.practicum.ewm.admin.dto.CategoryDto;

import java.util.List;

public interface PublicCategoryService {
    List<CategoryDto> getCategories(int from, int size);

    CategoryDto getCategoryById(Long catId);
}
