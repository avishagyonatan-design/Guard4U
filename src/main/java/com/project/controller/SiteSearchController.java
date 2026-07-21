package com.project.controller;

import com.project.model.SiteSearchResult;
import com.project.service.DatabaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/search")
@RequiredArgsConstructor
public class SiteSearchController {

    private final DatabaseService db;

    @GetMapping
    public String searchForm() {
        return "site-search";
    }

    @GetMapping("/result")
    public String searchResult(
            @RequestParam(required = false) Integer customerId,
            @RequestParam(required = false) Integer siteId,
            Model model) {

        if (customerId == null || siteId == null) {
            model.addAttribute("error", "יש להזין מספר לקוח ומספר אתר.");
            return "site-search";
        }

        model.addAttribute("customerId", customerId);
        model.addAttribute("siteId", siteId);

        SiteSearchResult result = db.searchSite(customerId, siteId);
        if (result == null) {
            model.addAttribute("error", "לקוח " + customerId + " עם אתר " + siteId + " לא נמצאו במערכת.");
        } else {
            model.addAttribute("result", result);
        }
        return "site-search";
    }
}
