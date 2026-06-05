package spafi.springframework.magazinonline.model;

/**
 * The three kinds of account supported by the store.
 * Stored on the {@link User} entity to distinguish Admin, Seller and Buyer
 * (the "role enum" approach mentioned in the requirements).
 */
public enum Role {
    ADMIN,
    SELLER,
    BUYER
}