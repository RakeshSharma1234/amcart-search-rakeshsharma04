package com.amcart.search.service;

import static com.amcart.search.fields.ProductFieldAttr.Aggregations.FACET_BRAND;
import static com.amcart.search.fields.ProductFieldAttr.Aggregations.FACET_BRAND_NAME;
import static com.amcart.search.fields.ProductFieldAttr.Aggregations.FACET_COLOR;
import static com.amcart.search.fields.ProductFieldAttr.Aggregations.FACET_COLOR_NAME;
import static com.amcart.search.fields.ProductFieldAttr.Aggregations.FACET_PRODUCT_DISCOUNT;
import static com.amcart.search.fields.ProductFieldAttr.Aggregations.FACET_PRODUCT_DISCOUNT_NAME;
import static com.amcart.search.fields.ProductFieldAttr.Aggregations.FACET_PRODUCT_SIZES;
import static com.amcart.search.fields.ProductFieldAttr.Aggregations.FACET_PRODUCT_SIZES_NAME;
import static com.amcart.search.fields.ProductFieldAttr.Aggregations.FACET_PRODUCT_TYPE;
import static com.amcart.search.fields.ProductFieldAttr.Aggregations.FACET_PRODUCT_TYPE_NAME;
import static org.apache.http.util.TextUtils.isEmpty;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Repository;

import com.amcart.search.dto.FacetsDTO;
import com.amcart.search.dto.ProductCatalogDTO;
import com.amcart.search.dto.ProductDTO;
import com.amcart.search.dto.ProductSearchDTO;
import com.amcart.search.fields.ProductFieldAttr;
import com.amcart.search.mappers.ProductMapper;
import com.amcart.search.model.Product;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.FieldSort;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.TermsAggregation;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchAllQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchPhraseQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MultiMatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQueryField;
import co.elastic.clients.elasticsearch._types.query_dsl.WildcardQuery;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.DeleteRequest;
import co.elastic.clients.elasticsearch.core.DeleteResponse;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchRequest.Builder;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Repository
@RequiredArgsConstructor
@ConfigurationProperties("elastic.product")
@Getter
@Setter
public class ProductRepository {

	Logger logger = LoggerFactory.getLogger(ProductRepository.class);

	@Autowired
	private ElasticsearchClient elasticsearchClient;

	private final ProductMapper mapper;
	private String indexName;

	public String createOrUpdateProduct(Product product) throws IOException {
		IndexResponse response = elasticsearchClient
				.index(i -> i.index(indexName).id(product.getId()).document(product));
		if (response.result().name().equals("Created")) {
			return new StringBuilder("Product has been successfully created.").toString();
		} else if (response.result().name().equals("Updated")) {
			return new StringBuilder("Product has been successfully updated.").toString();
		}
		return new StringBuilder("Error while performing the operation.").toString();
	}

	public String createOrUpdateBulkProducts(List<Product> products) throws IOException {

		BulkRequest.Builder br = new BulkRequest.Builder();

		for (Product product : products) {
			br.operations(op -> op.index(idx -> idx.index(indexName).id(product.getId()).document(product)));
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
		} else {
			return new StringBuilder("Products has been successfully created or updated.").toString();
		}
	}

	public Product getProductById(String productId) throws IOException {
		Product product = null;
		GetResponse<Product> response = elasticsearchClient.get(g -> g.index(indexName).id(productId), Product.class);

		if (response.found()) {
			product = response.source();
			logger.info("Product name " + product.getName());
		} else {
			logger.error("Product not found");
		}

		return product;
	}

	public String deleteProductById(String productId) throws IOException {

		DeleteRequest request = DeleteRequest.of(d -> d.index(indexName).id(productId));

		DeleteResponse deleteResponse = elasticsearchClient.delete(request);
		if (Objects.nonNull(deleteResponse.result()) && !deleteResponse.result().name().equals("NotFound")) {
			return new StringBuilder("Product with id " + deleteResponse.id() + " has been deleted.").toString();
		}
		logger.error("Product not found");
		return new StringBuilder("Product with id " + deleteResponse.id() + " does not exist.").toString();

	}

	public ProductCatalogDTO searchByText(ProductSearchDTO searchDTO) throws IOException {
		SearchRequest searchRequest = getSearchRequest(searchDTO.getText(), searchDTO.getSize(), searchDTO.getFrom(),
				Collections.emptyMap(), searchDTO.getMapFilters());
		var response = elasticsearchClient.search(searchRequest, Product.class);
		var products = new ArrayList<ProductDTO>();
		getResultDocuments(response, products);
		return ProductCatalogDTO.builder().products(products).size(searchDTO.getSize()).total(getTotalHits(response))
				.build();
	}

	public ProductCatalogDTO searchByCategory(ProductSearchDTO searchDTO) throws ElasticsearchException, IOException {
		Query query = MatchQuery.of(m -> m.field("category").query(searchDTO.getText()))._toQuery();
		SearchResponse<Product> searchResponse = elasticsearchClient
				.search(s -> s.index(indexName).size(searchDTO.getSize()).query(query), Product.class);
		var products = new ArrayList<ProductDTO>();
		getResultDocuments(searchResponse, products);
		return ProductCatalogDTO.builder().products(products).size(searchDTO.getSize())
				.total(getTotalHits(searchResponse)).build();
	}

	public Object getFacets(@Valid ProductSearchDTO searchDTO) throws IOException {
		var response = getFacets(searchDTO.getText(), Collections.emptyMap());
		return parseResults(response, List.of(FACET_BRAND_NAME, FACET_COLOR_NAME, FACET_PRODUCT_TYPE_NAME,
				FACET_PRODUCT_SIZES_NAME, FACET_PRODUCT_DISCOUNT_NAME));
	}

	private SearchResponse<Void> getFacets(String term, Map<String, List<String>> filters) throws IOException {
		Map<String, Aggregation> map = new HashMap<>();
		map.put(FACET_BRAND_NAME,
				new Aggregation.Builder().terms(new TermsAggregation.Builder().field(FACET_BRAND).build()).build());
		map.put(FACET_COLOR_NAME,
				new Aggregation.Builder().terms(new TermsAggregation.Builder().field(FACET_COLOR).build()).build());
		map.put(FACET_PRODUCT_TYPE_NAME, new Aggregation.Builder()
				.terms(new TermsAggregation.Builder().field(FACET_PRODUCT_TYPE).build()).build());
		map.put(FACET_PRODUCT_SIZES_NAME, new Aggregation.Builder()
				.terms(new TermsAggregation.Builder().field(FACET_PRODUCT_SIZES).build()).build());
		map.put(FACET_PRODUCT_DISCOUNT_NAME, new Aggregation.Builder()
				.terms(new TermsAggregation.Builder().field(FACET_PRODUCT_DISCOUNT).build()).build());
		SearchRequest searchRequest = getSearchRequest(term, 0, 0, map, filters);
		return elasticsearchClient.search(searchRequest, Void.class);
	}

	private SearchRequest getSearchRequest(String term, int size, int from, Map<String, Aggregation> map,
			Map<String, List<String>> filters) {
		return SearchRequest.of(s -> {
			s.index(indexName);
			s.from(from);
			s.size(size);
			s.sort(sort -> sort.field(FieldSort.of(f -> f.field("id.keyword").order(SortOrder.Asc))));
			addQuery(s, term, filters);
			addAggregation(map, s);
			return s;
		});
	}

	private void addAggregation(Map<String, Aggregation> map, Builder s) {
		if (!map.isEmpty()) {
			s.aggregations(map);
		}
	}

	private void addQuery(Builder builder, String term, Map<String, List<String>> filters) {
		if (isEmpty(term)) {
			queryMatchAll(builder, filters);
		} else {
			buildBoolQuery(builder, term, filters);
		}
	}

	private void buildBoolQuery(Builder builder, String term, Map<String, List<String>> mapFilters) {
		var filters = getFilters(mapFilters);

		var matchQuery = Query.of(
				q -> q.match(MatchQuery.of(m -> m.field(ProductFieldAttr.Product.NAME_FIELD).query(term).boost(10f))));

		var matchCategoryQuery = Query.of(q -> q
				.match(MatchQuery.of(m -> m.field(ProductFieldAttr.Product.CATEGORY_FIELD).query(term).boost(10f))));

		var multiMatchQuery = Query.of(q -> q.multiMatch(MultiMatchQuery.of(m -> m
				.fields(applyFieldBoost(ProductFieldAttr.Product.NAME_FIELD, 5),
						applyFieldBoost(ProductFieldAttr.Product.CATEGORY_FIELD, 3),
						applyFieldBoost(ProductFieldAttr.Product.BRAND_FIELD, 4),
						applyFieldBoost(ProductFieldAttr.Product.COLOR_FIELD, 3),
						applyFieldBoost(ProductFieldAttr.Product.PRODUCT_TYPE_FIELD, 3))
				.operator(Operator.And).query(term))));

		var wildCardNameQuery = Query.of(q -> q.wildcard(WildcardQuery.of(w -> w.caseInsensitive(true)
				.field(ProductFieldAttr.Product.NAME_FIELD).value(term + "*").boost(5.0f))));
		var wildCardBrandQuery = Query.of(q -> q.wildcard(WildcardQuery.of(w -> w.caseInsensitive(true)
				.field(ProductFieldAttr.Product.BRAND_FIELD).value(term + "*").boost(4.0f))));
		var wildCardColorQuery = Query.of(q -> q.wildcard(WildcardQuery.of(w -> w.caseInsensitive(true)
				.field(ProductFieldAttr.Product.COLOR_FIELD).value(term + "*").boost(2.0f))));
		var wildCardTypeQuery = Query.of(q -> q.wildcard(WildcardQuery.of(w -> w.caseInsensitive(true)
				.field(ProductFieldAttr.Product.PRODUCT_TYPE_FIELD).value(term + "*").boost(3.0f))));

		var matchPhraseQuery = Query.of(q -> q.matchPhrase(MatchPhraseQuery.of(p -> p.field("name").query(term))));

		var boolQuery = BoolQuery.of(bq -> {
			bq.filter(filters);
			bq.should(matchQuery, matchCategoryQuery, matchPhraseQuery, multiMatchQuery, wildCardNameQuery,
					wildCardBrandQuery, wildCardTypeQuery, wildCardColorQuery);
			bq.minimumShouldMatch("1");
			return bq;
		});
		builder.query(Query.of(q -> q.bool(boolQuery)));
	}

	private void queryMatchAll(Builder builder, Map<String, List<String>> filters) {
		var filter = getFilters(filters);
		var matchAll = Query.of(q -> q.matchAll(MatchAllQuery.of(ma -> ma)));
		var boolQuery = BoolQuery.of(bq -> {
			if (filter.size() > 0) {
				bq.filter(filter);
			}
			bq.must(matchAll);
			return bq;
		});
		builder.query(Query.of(q -> q.bool(boolQuery)));
	}

	private List<Query> getFilters(Map<String, List<String>> mapFilters) {
		if (!mapFilters.isEmpty()) {
			var queries = new ArrayList<Query>();
			var brands = mapFilters.get("brands");
			if (!brands.isEmpty()) {
				var filters = brands.stream().map(g -> FieldValue.of(fv -> fv.stringValue(g)))
						.collect(Collectors.toList());
				queries.add(getTermsQuery(filters, ProductFieldAttr.Product.BRAND_FIELD));
			}

			var colors = mapFilters.get("colors");
			if (!colors.isEmpty()) {
				var filters = colors.stream().map(g -> FieldValue.of(fv -> fv.stringValue(g)))
						.collect(Collectors.toList());
				queries.add(getTermsQuery(filters, ProductFieldAttr.Product.COLOR_FIELD));
			}

			var productTypes = mapFilters.get("productType");
			if (!productTypes.isEmpty()) {
				var filters = productTypes.stream().map(g -> FieldValue.of(fv -> fv.stringValue(g)))
						.collect(Collectors.toList());
				queries.add(getTermsQuery(filters, ProductFieldAttr.Product.PRODUCT_TYPE_FIELD));
			}

			var productSizes = mapFilters.get("productSize");
			if (!productSizes.isEmpty()) {
				var filters = productSizes.stream().map(g -> FieldValue.of(fv -> fv.stringValue(g)))
						.collect(Collectors.toList());
				queries.add(getTermsQuery(filters, ProductFieldAttr.Product.PRODUCT_SIZES_FIELD));
			}

			var productDiscounts = mapFilters.get("productDiscount");
			if (!productDiscounts.isEmpty()) {
				var filters = productDiscounts.stream().map(g -> FieldValue.of(fv -> fv.stringValue(g)))
						.collect(Collectors.toList());
				queries.add(getTermsQuery(filters, ProductFieldAttr.Product.PRODUCT_DISCOUNT_FIELD));
			}
			return queries;
		} else {
			return new ArrayList<Query>();
		}
	}

	private Query getTermsQuery(List<FieldValue> filters, String field) {
		var termsQuery = Query.of(q -> q
				.terms(TermsQuery.of(tsq -> tsq.field(field).terms(TermsQueryField.of(tf -> tf.value(filters))))));
		return termsQuery;
	}

	private Map<String, List<FacetsDTO>> parseResults(SearchResponse<Void> response, List<String> aggNames) {
		Map<String, List<FacetsDTO>> facets = new HashMap<>();
		for (var name : aggNames) {
			var list = response.aggregations().get(name).sterms().buckets().array();
			var facetsList = list.stream().map(l -> new FacetsDTO(l.key().stringValue(), l.docCount()))
					.collect(Collectors.toList());
			facets.put(name, facetsList);
		}
		return facets;
	}

	private void getResultDocuments(SearchResponse<Product> response, ArrayList<ProductDTO> products) {
		for (var hit : response.hits().hits()) {
			var dto = mapper.toDto(hit.source());
			products.add(dto);
		}
	}

	private long getTotalHits(SearchResponse<Product> response) {
		return Objects.nonNull(response.hits().total()) ? response.hits().total().value() : 0;
	}

	private String applyFieldBoost(String field, int boost) {
		return String.format("%s^%s", field, boost);
	}

	public Set<String> getSuggestions(ProductSearchDTO searchDTO) throws IOException {
		var suggestionProducts = new HashSet<String>();
		SearchRequest searchRequest = SearchRequest.of(s -> s.index(indexName).size(searchDTO.getSize())
				.query(q -> q.matchPhrasePrefix(m -> m.field("name").query(searchDTO.getText()))));
		SearchResponse<Product> response = elasticsearchClient.search(searchRequest, Product.class);
		getProductNames(response, suggestionProducts);
		return suggestionProducts;
	}

	private void getProductNames(SearchResponse<Product> response, Set<String> products) {
		for (var hit : response.hits().hits()) {
			var dto = mapper.toDto(hit.source());
			products.add(dto.getName());
		}
	}
}