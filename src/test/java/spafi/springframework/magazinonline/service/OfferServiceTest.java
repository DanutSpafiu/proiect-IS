package spafi.springframework.magazinonline.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import spafi.springframework.magazinonline.model.*;
import spafi.springframework.magazinonline.repository.*;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OfferServiceTest {

    @Mock
    private OfferRepository offerRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SaleHistoryRepository saleHistoryRepository;

    @InjectMocks
    private OfferService offerService;

    @Test
    void trimiteOferta_PretSubMinim_AruncaExceptie() {
        // Arrange (Pregatim datele de test)
        Product produs = new Product();
        produs.setSaleType(SaleType.NEGOTIABLE);
        produs.setMinimumPrice(100.0);

        when(productRepository.findById(1L)).thenReturn(Optional.of(produs));

        // Act & Assert (Executam si verificam ca se arunca exceptia corecta)
        Exception exception = assertThrows(RuntimeException.class, () -> {
            offerService.trimiteOferta(1L, 1L, 50.0); // 50 e mai mic decat 100
        });

        assertTrue(exception.getMessage().contains("Pretul propus este sub pretul minim"));
        // Verificam ca NU s-a salvat nimic in baza de date
        verify(offerRepository, never()).save(any(Offer.class));
    }

    @Test
    void aprobaOferta_Succes() {
        // Arrange
        User vanzator = new User();
        vanzator.setId(10L);

        Product produs = new Product();
        produs.setSeller(vanzator);

        Offer oferta = new Offer();
        oferta.setProduct(produs);
        oferta.setStatus(OfferStatus.PENDING);

        when(offerRepository.findById(1L)).thenReturn(Optional.of(oferta));

        // Act
        offerService.aprobaOferta(10L, 1L);

        // Assert
        assertEquals(OfferStatus.APPROVED, oferta.getStatus());
        verify(offerRepository, times(1)).save(oferta);
    }
}