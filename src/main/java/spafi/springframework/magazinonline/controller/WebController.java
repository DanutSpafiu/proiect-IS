package spafi.springframework.magazinonline.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebController {

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/auth")
    public String auth() {
        return "auth";
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        return "dashboard";
    }
}
