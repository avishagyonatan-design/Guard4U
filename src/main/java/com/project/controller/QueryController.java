package com.project.controller;

import com.project.service.DatabaseService;
import com.project.service.DatabaseService.QueryResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/queries")
@RequiredArgsConstructor
public class QueryController {

    private final DatabaseService db;

    @GetMapping
    public String queriesPage() {
        return "queries";
    }

    @GetMapping("/inactive-sites")
    public String inactiveSites(Model model) {
        QueryResult r = db.runSimpleView("SELECT * FROM vw_InactiveSites");
        model.addAttribute("title", "אתרים לא פעילים");
        model.addAttribute("result", r);
        return "queries";
    }

    @GetMapping("/patrol-incentives")
    public String patrolIncentives(Model model) {
        QueryResult r = db.runSimpleView("SELECT * FROM vw_PatrolIncentives");
        model.addAttribute("title", "תמריצי סיירים");
        model.addAttribute("result", r);
        return "queries";
    }

    @GetMapping("/monthly-charge")
    public String monthlyCharge(@RequestParam(required = false) Integer customerId, Model model) {
        model.addAttribute("title", "חישוב חיוב חודשי ללקוח");
        model.addAttribute("showMonthlyChargeForm", true);
        if (customerId != null) {
            model.addAttribute("customerIdInput", customerId);
            QueryResult r = db.runProcedureMonthlyCharge(customerId);
            model.addAttribute("result", r);
        }
        return "queries";
    }

    @GetMapping("/patrol-stats")
    public String patrolStats(
            @RequestParam(required = false) Integer minRequest,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            Model model) {
        model.addAttribute("title", "סטטיסטיקות סיור חודשיות");
        model.addAttribute("showPatrolStatsForm", true);
        if (minRequest != null && month != null && year != null) {
            model.addAttribute("minRequestInput", minRequest);
            model.addAttribute("monthInput", month);
            model.addAttribute("yearInput", year);
            QueryResult r = db.runProcedurePatrolStats(minRequest, month, year);
            model.addAttribute("result", r);
        }
        return "queries";
    }

    @GetMapping("/repairs-billing")
    public String repairsBilling(Model model) {
        QueryResult r = db.runSimpleView("SELECT * FROM vw_RepairsBilling");
        model.addAttribute("title", "חיוב תיקונים");
        model.addAttribute("result", r);
        return "queries";
    }

    @GetMapping("/items-stock")
    public String itemsStock(Model model) {
        QueryResult r = db.runSimpleView("SELECT * FROM vw_ItemsStock");
        model.addAttribute("title", "מלאי פריטים");
        model.addAttribute("result", r);
        return "queries";
    }
}
