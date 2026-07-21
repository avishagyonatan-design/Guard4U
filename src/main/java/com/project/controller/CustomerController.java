package com.project.controller;

import com.project.model.Customer;
import com.project.model.Site;
import com.project.service.DatabaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final DatabaseService db;

    @GetMapping
    public String listCustomers(Model model) {
        List<Customer> customers = db.getAllCustomers();
        model.addAttribute("customers", customers);
        return "customers";
    }

    @GetMapping("/{id}/sites")
    public String customerSites(@PathVariable int id, Model model) {
        Customer customer = db.getCustomer(id);
        if (customer == null) {
            model.addAttribute("error", "לקוח מספר " + id + " לא נמצא.");
            model.addAttribute("customers", db.getAllCustomers());
            return "customers";
        }
        List<Site> sites = db.getSitesWithLatestSubscription(id);
        model.addAttribute("customer", customer);
        model.addAttribute("sites", sites);
        model.addAttribute("customers", db.getAllCustomers());
        return "customers";
    }
}
