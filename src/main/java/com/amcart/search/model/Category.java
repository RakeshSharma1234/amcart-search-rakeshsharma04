package com.amcart.search.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Category{
    public String id;
    public String name;
    public String parent;
    public String path;
    public String createdAt;
    public String updatedAt;
}
