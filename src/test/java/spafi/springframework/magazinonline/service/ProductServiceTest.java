package spafi.springframework.magazinonline.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import spafi.springframework.magazinonline.dto.ProductCreateRequest;
import spafi.springframework.magazinonline.dto.ProductResponse;
import spafi.springframework.magazinonline.model.Product;
import spafi.springframework.magazinonline.model.Role;
import spafi.springframework.magazinonline.model.SaleType;
import spafi.springframework.magazinonline.model.User;
import spafi.springframework.magazinonline.repository.ProductRepository;
import spafi.springframework.magazinonline.repository.UserRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProductServiceTest {

    private ProductRepository productRepository;
    private UserRepository userRepository;
    private ProductService productService;

    @BeforeEach
    void setUp() {
        productRepository = mock(ProductRepository.class);
        userRepository = mock(UserRepository.class);
        productService = new ProductService(productRepository, userRepository);
    }

    @Test
    void sellerCanAddFixedPriceProduct() {
        User seller = createSeller();

        ProductCreateRequest request = createRequest(
                "Laptop Lenovo",
                2500.0,
                "Laptop in stare buna",
                SaleType.FIXED_PRICE,
                null
        );

        when(userRepository.findByEmail("seller@email.com")).thenReturn(Optional.of(seller));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponse response = productService.addProduct(request, "seller@email.com");

        assertEquals("Laptop Lenovo", response.getName());
        assertEquals(2500.0, response.getPrice());
        assertEquals("seller@email.com", response.getSellerEmail());
        assertEquals(SaleType.FIXED_PRICE, response.getSaleType());

        verify(productRepository).save(any(Product.class));
    }

    @Test
    void sellerCanAddNegotiableProduct() {
        User seller = createSeller();

        ProductCreateRequest request = createRequest(
                "Telefon Samsung",
                1800.0,
                "Telefon aproape nou",
                SaleType.NEGOTIABLE,
                1500.0
        );

        when(userRepository.findByEmail("seller@email.com")).thenReturn(Optional.of(seller));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponse response = productService.addProduct(request, "seller@email.com");

        assertEquals("Telefon Samsung", response.getName());
        assertEquals(1800.0, response.getPrice());
        assertEquals("seller@email.com", response.getSellerEmail());
        assertEquals(SaleType.NEGOTIABLE, response.getSaleType());

        verify(productRepository).save(any(Product.class));
    }

    @Test
    void negotiableProductNeedsMinimumPrice() {
        User seller = createSeller();

        ProductCreateRequest request = createRequest(
                "Telefon Samsung",
                1800.0,
                "Telefon aproape nou",
                SaleType.NEGOTIABLE,
                null
        );

        when(userRepository.findByEmail("seller@email.com")).thenReturn(Optional.of(seller));

        assertThrows(RuntimeException.class, () -> {
            productService.addProduct(request, "seller@email.com");
        });

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void unapprovedSellerCannotAddProduct() {
        User seller = createSeller();
        seller.setApproved(false);

        ProductCreateRequest request = createRequest(
                "Laptop Lenovo",
                2500.0,
                "Laptop in stare buna",
                SaleType.FIXED_PRICE,
                null
        );

        when(userRepository.findByEmail("seller@email.com")).thenReturn(Optional.of(seller));

        assertThrows(RuntimeException.class, () -> {
            productService.addProduct(request, "seller@email.com");
        });

        verify(productRepository, never()).save(any(Product.class));
    }

    private ProductCreateRequest createRequest(String name, Double price, String description,
                                               SaleType saleType, Double minimumPrice) {
        ProductCreateRequest request = new ProductCreateRequest();

        ReflectionTestUtils.setField(request, "name", name);
        ReflectionTestUtils.setField(request, "price", price);
        ReflectionTestUtils.setField(request, "description", description);
        ReflectionTestUtils.setField(request, "saleType", saleType);
        ReflectionTestUtils.setField(request, "minimumPrice", minimumPrice);

        return request;
    }

    private User createSeller() {
        User seller = new User();
        seller.setEmail("seller@email.com");
        seller.setRole(Role.SELLER);
        seller.setApproved(true);
        seller.setActive(true);
        return seller;
    }
}