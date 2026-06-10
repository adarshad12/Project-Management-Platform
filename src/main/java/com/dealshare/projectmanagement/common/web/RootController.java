package com.dealshare.projectmanagement.common.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class RootController {

    @GetMapping("/")
    String index() {
        return "redirect:/swagger-ui.html";
    }
}
