//xia


package spafi.springframework.magazinonline.dto;

import spafi.springframework.magazinonline.model.SaleType;

public class ProductCreateRequest {

    private String name;
    private Double price;
    private String description;
    private SaleType saleType;
    private Double minimumPrice;

    public String getName() {
        return name;
    }

    public Double getPrice() {
        return price;
    }

    public String getDescription() {
        return description;
    }

    public SaleType getSaleType() {
        return saleType;
    }

    public Double getMinimumPrice() {
        return minimumPrice;
    }

    public void setMinimumPrice(Double minimumPrice) {
        this.minimumPrice = minimumPrice;
    }
}