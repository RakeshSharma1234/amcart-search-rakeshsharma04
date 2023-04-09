package com.amcart.search.fields;

public final class ProductFieldAttr {

  private ProductFieldAttr() {
  }

  public static class Product {

    private Product() {
    	
    }
	public String name;
	public String category;
	public String description;
	public String brand;
	public String color;
	
    public static final String NAME_FIELD = "name.keyword";
    public static final String CATEGORY_FIELD = "category.keyword";
    public static final String DESCRIPTION_FIELD = "description.keyword";
    
    public static final String BRAND_FIELD = "brand.keyword";
    public static final String COLOR_FIELD = "color.keyword";
    public static final String PRODUCT_TYPE_FIELD = "productType.keyword";
    public static final String PRODUCT_SIZES_FIELD = "productSizes.keyword";
    public static final String PRODUCT_DISCOUNT_FIELD = "productDiscount.keyword";
  }

  public static class Aggregations {

    private Aggregations() {
    }

    public static final String FACET_BRAND_NAME = "agg_brand";
    public static final String FACET_COLOR_NAME = "agg_color";
    public static final String FACET_PRODUCT_SIZES_NAME = "agg_productSizes";
    public static final String FACET_PRODUCT_TYPE_NAME = "agg_productType";
    public static final String FACET_PRODUCT_DISCOUNT_NAME = "agg_productDiscount";
    
    public static final String FACET_BRAND = "brand.keyword";
    public static final String FACET_COLOR = "color.keyword";
    public static final String FACET_PRODUCT_SIZES = "productSizes.keyword";
    public static final String FACET_PRODUCT_TYPE = "productType.keyword";
    public static final String FACET_PRODUCT_DISCOUNT = "productDiscount.keyword";
  }
}
