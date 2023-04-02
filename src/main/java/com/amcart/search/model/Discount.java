package com.amcart.search.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Discount{

    public Long flatDiscount;
    public String flatDiscountCurrency;
    public float percentageDiscount;
}

