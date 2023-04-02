package com.amcart.search.controller;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.amcart.search.dto.CategoryRoot;
import com.amcart.search.model.Category;
import com.amcart.search.service.CategoryRepository;

@RestController
@RequestMapping("category")
public class CategoryController {

	@Autowired
	private CategoryRepository repository;

	@PostMapping
	public ResponseEntity<Object> createOrUpdateCategory(@RequestBody Category category) throws IOException {
		String response = repository.createOrUpdateCategory(category);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@PostMapping("/bulk")
	public ResponseEntity<Object> createOrUpdateCategories(@RequestBody List<Category> categories) throws IOException {
		String response = repository.createOrUpdateBulkCategories(categories);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@GetMapping("/{categoryId}")
	public ResponseEntity<Object> getCategoryById(@PathVariable String categoryId) throws IOException {
		Category category = repository.getCategoryById(categoryId);
		if (category != null)
			return new ResponseEntity<>(category, HttpStatus.OK);
		else
			return new ResponseEntity<>("Category not found", HttpStatus.NOT_FOUND);
	}

	@DeleteMapping("/{categoryId}")
	public ResponseEntity<Object> deleteCategoryById(@PathVariable String categoryId) throws IOException {
		String response = repository.deleteCategoryById(categoryId);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@GetMapping("/list")
	public ResponseEntity<Object> list() throws IOException {
		List<CategoryRoot> categories = repository.getAllCategories();
		return new ResponseEntity<>(categories, HttpStatus.OK);
	}

}