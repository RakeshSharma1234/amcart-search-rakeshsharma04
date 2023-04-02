package com.amcart.search.model;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Product{
	
	public String id;
	public String name;
	public String category;
	public String description;
	public String brand;
	public String color;
	public Discount discount;
	public Map<String,PriceInfo> price;
	public SellerInfo sellerInfo;
	public List<Image> images;
	public List<Map<String, String>> attributes;
	public List<SkuInfo> skus;
	public String productType;
	public List<String> productSizes;
	public String productDiscount;
	public String createdAt;
	public String updatedAt;
}