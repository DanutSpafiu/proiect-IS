package spafi.springframework.magazinonline.controller;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import spafi.springframework.magazinonline.dto.UserResponse;
import spafi.springframework.magazinonline.service.AdminService;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/sellers")
    public ResponseEntity<List<UserResponse>> listSellers() {
        List<UserResponse> sellers = adminService.listSellers().stream()
                .map(UserResponse::from)
                .toList();
        return ResponseEntity.ok(sellers);
    }

    @PostMapping("/sellers/{email}/approve")
    public ResponseEntity<UserResponse> approveSeller(@PathVariable String email) {
        return ResponseEntity.ok(UserResponse.from(adminService.approveSeller(email)));
    }

    @PostMapping("/sellers/{email}/deactivate")
    public ResponseEntity<UserResponse> deactivateSeller(@PathVariable String email) {
        return ResponseEntity.ok(UserResponse.from(adminService.deactivateSeller(email)));
    }
}
