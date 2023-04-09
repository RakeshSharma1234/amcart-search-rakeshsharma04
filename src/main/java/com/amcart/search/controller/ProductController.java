package com.amcart.search.controller;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import javax.validation.Valid;

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

import com.amcart.search.dto.ProductCatalogDTO;
import com.amcart.search.dto.ProductSearchDTO;
import com.amcart.search.model.Product;
import com.amcart.search.service.ProductRepository;

@RestController
@RequestMapping("product")
public class ProductController {

	@Autowired
    private ProductRepository productRepository;

    @PostMapping
    public ResponseEntity<Object> createOrUpdateProduct(@RequestBody Product product) throws IOException {
          String response = productRepository.createOrUpdateProduct(product);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
    
    @PostMapping("/bulk")
    public ResponseEntity<Object> createOrUpdateProducts(@RequestBody List<Product> products) throws IOException {
          String response = productRepository.createOrUpdateBulkProducts(products);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/{productId}")
    public ResponseEntity<Object> getProductById(@PathVariable String productId) throws IOException {
       Product product =  productRepository.getProductById(productId);
       if(product!= null) 
    	   return new ResponseEntity<>(product, HttpStatus.OK);
       else
           return new ResponseEntity<>("Product not found", HttpStatus.NOT_FOUND);
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Object> deleteProductById(@PathVariable String productId) throws IOException {
        String response =  productRepository.deleteProductById(productId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    
    @GetMapping("/search")
    public ResponseEntity<Object> search(@Valid ProductSearchDTO searchDTO) throws IOException {
        ProductCatalogDTO catalog = productRepository.searchByText(searchDTO);
        return new ResponseEntity<>(catalog, HttpStatus.OK);
    }
    
    @GetMapping("/category/search")
    public ResponseEntity<Object> searchByCategory(@Valid ProductSearchDTO searchDTO) throws IOException {
    	ProductCatalogDTO catalog = productRepository.searchByCategory(searchDTO);
        return new ResponseEntity<>(catalog, HttpStatus.OK);
    }
    
    @GetMapping("/facets")
    public ResponseEntity<Object> getFacets(@Valid ProductSearchDTO searchDTO) throws IOException {
      return new ResponseEntity<>(productRepository.getFacets(searchDTO), HttpStatus.OK);
    }
    
    @GetMapping("/autocomplete")
    public ResponseEntity<Set<String>> getSuggestions(@Valid ProductSearchDTO searchDTO) throws IOException {
      return ResponseEntity.ok(productRepository.getSuggestions(searchDTO));
    }
}