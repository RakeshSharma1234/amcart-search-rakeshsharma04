package com.amcart.search.dto;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.springframework.util.CollectionUtils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Singular;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductSearchDTO {

	@NotNull
	private String text;

	@Min(10)
	@Max(20)
	private int size = 15;

	@Singular
	private List<String> brands;

	@Singular
	private List<String> colors;

	@Singular
	private List<String> productType;

	@Singular
	private List<String> productSize;

	@Singular
	private List<String> productDiscount;

	@SuppressWarnings("serial")
	public Map<String, List<String>> getMapFilters() {
		return new HashMap<>() {
			{
				put("brands", CollectionUtils.isEmpty(brands) ? Collections.emptyList() : brands);
				put("colors", CollectionUtils.isEmpty(colors) ? Collections.emptyList() : colors);
				put("productType", CollectionUtils.isEmpty(productType) ? Collections.emptyList() : productType);
				put("productSize", CollectionUtils.isEmpty(productSize) ? Collections.emptyList() : productSize);
				put("productDiscount", CollectionUtils.isEmpty(productDiscount) ? Collections.emptyList() : productDiscount);
			}
		};
	}
}
