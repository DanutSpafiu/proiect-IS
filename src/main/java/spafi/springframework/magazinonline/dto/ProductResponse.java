//xiaaa


package spafi.springframework.magazinonline.dto;

import spafi.springframework.magazinonline.model.Product;
import spafi.springframework.magazinonline.model.SaleType;

public class ProductResponse {

    private String name;
    private Double price;
    private String sellerEmail;
    private String description;
    private SaleType saleType;

    public ProductResponse(String name, Double price, String sellerEmail, String description, SaleType saleType) {
        this.name = name;
        this.price = price;
        this.sellerEmail = sellerEmail;
        this.description = description;
        this.saleType = saleType;
    }

    public static ProductResponse fromProduct(Product product) {
        return new ProductResponse(
                product.getName(),
                product.getPrice(),
                product.getSeller().getEmail(),
                product.getDescription(),
                product.getSaleType()
        );
    }

    public String getName() {
        return name;
    }

    public Double getPrice() {
        return price;
    }

    public String getSellerEmail() {
        return sellerEmail;
    }

    public String getDescription() {
        return description;
    }

    public SaleType getSaleType() {
        return saleType;
    }
}