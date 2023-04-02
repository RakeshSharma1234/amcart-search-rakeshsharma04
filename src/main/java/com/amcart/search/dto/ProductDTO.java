package com.amcart.search.dto;

import java.util.List;
import java.util.Map;

import com.amcart.search.model.Discount;
import com.amcart.search.model.Image;
import com.amcart.search.model.PriceInfo;
import com.amcart.search.model.SellerInfo;
import com.amcart.search.model.SkuInfo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDTO {
	public String id;
	public String name;
	public String category;
	public String description;
	public String brand;
	public String color;
	public Discount discount;
	public Map<String, PriceInfo> price;
	public SellerInfo sellerInfo;
	public List<Image> images;
	public List<Map<String, String>> attributes;
	public List<SkuInfo> skus;
	public String productType;
	public List<String> productSizes;
	public String productDiscount;

//	@JsonProperty("search_after")
//	private Object[] searchAfter;
}
