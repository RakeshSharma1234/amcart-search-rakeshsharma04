package com.amcart.search.mappers;

import org.mapstruct.Mapper;

import com.amcart.search.dto.ProductDTO;
import com.amcart.search.model.Product;

@Mapper(componentModel = "spring")
public interface ProductMapper {
	ProductDTO toDto(Product product);
}
