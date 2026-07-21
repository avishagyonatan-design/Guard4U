package com.project.controller;

import com.project.service.DatabaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final DatabaseService db;

    @GetMapping("/")
    public String home(Model model) {
        String dbName = db.testConnection();
        model.addAttribute("dbStatus", dbName);
        return "index";
    }
}
