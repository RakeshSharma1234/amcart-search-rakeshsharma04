package com.amcart.search.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Repository;

import com.amcart.search.dto.CategoryChild;
import com.amcart.search.dto.CategoryRoot;
import com.amcart.search.model.Category;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.DeleteRequest;
import co.elastic.clients.elasticsearch.core.DeleteResponse;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.elasticsearch.core.search.Hit;
import lombok.Getter;
import lombok.Setter;

@Repository
@ConfigurationProperties("elastic.category")
@Getter
@Setter
public class CategoryRepository {

	Logger logger = LoggerFactory.getLogger(CategoryRepository.class);
	
	@Autowired
	private ElasticsearchClient elasticsearchClient;

	private String indexName;

	public String createOrUpdateCategory(Category category) throws IOException {

		IndexResponse response = elasticsearchClient
				.index(i -> i.index(indexName).id(category.getId()).document(category));
		if (response.result().name().equals("Created")) {
			return new StringBuilder("Category has been successfully created.").toString();
		} else if (response.result().name().equals("Updated")) {
			return new StringBuilder("Category has been successfully updated.").toString();
		}
		return new StringBuilder("Error while performing the operation.").toString();
	}
	
	public String createOrUpdateBulkCategories(List<Category> categories) throws IOException {

		BulkRequest.Builder br = new BulkRequest.Builder();

		for (Category category : categories) {
			br.operations(op -> op.index(idx -> idx.index(indexName).id(category.getId()).document(category)));
		}

		BulkResponse result = elasticsearchClient.bulk(br.build());

		// Log errors, if any
		if (result.errors()) {
			 logger.error("Bulk operation had errors");
			for (BulkResponseItem item : result.items()) {
				if (item.error() != null) {
					logger.error(item.error().reason());
				}
			}
			return new StringBuilder("Error while performing the operation.").toString();
		}else {
			return new StringBuilder("Categories has been successfully created or updated.").toString();
		}
	}

	public Category getCategoryById(String categoryId) throws IOException {
		Category category = null;
		GetResponse<Category> response = elasticsearchClient.get(g -> g.index(indexName).id(categoryId),
				Category.class);

		if (response.found()) {
			category = response.source();
			logger.info("Category name " + category.getName());
		} else {
			logger.error("Category not found");
		}

		return category;
	}

	public String deleteCategoryById(String categoryId) throws IOException {

		DeleteRequest request = DeleteRequest.of(d -> d.index(indexName).id(categoryId));

		DeleteResponse deleteResponse = elasticsearchClient.delete(request);
		if (Objects.nonNull(deleteResponse.result()) && !deleteResponse.result().name().equals("NotFound")) {
			return new StringBuilder("Category with id " + deleteResponse.id() + " has been deleted.").toString();
		}
		logger.error("Category not found");
		return new StringBuilder("Category with id " + deleteResponse.id() + " does not exist.").toString();

	}
	
	public List<CategoryRoot> getAllCategories() throws IOException {
		Map<String, List<Category>> map = new HashMap<>();
		SearchRequest searchRequest = SearchRequest.of(s -> s.index(indexName).size(100));
		SearchResponse<Category> searchResponse = elasticsearchClient.search(searchRequest, Category.class);
		List<Hit<Category>> hits = searchResponse.hits().hits();
		List<Category> categories = new ArrayList<>();
		
		for (Hit<Category> object : hits) {
			Category category = object.source();
			categories.add(category);	
			if(map.containsKey(category.getParent())) {
				map.get(category.getParent()).add(category);
			}else {
				List<Category> list = new ArrayList<>();
				list.add(category);
				map.put(category.getParent(),list);
			}
		}
		
		List<CategoryRoot> categoryRoots = new ArrayList<>();
		for(Category L1 : map.get("/")) {
			List<CategoryChild> childs = new ArrayList<>();
            for(Category L2:map.get(L1.getPath())) {
            	childs.add(new CategoryChild(L2,map.get(L2.getPath())));
            }
			CategoryRoot root = new CategoryRoot(L1,childs);
			categoryRoots.add(root);
		}
		
		return categoryRoots;
	}
}