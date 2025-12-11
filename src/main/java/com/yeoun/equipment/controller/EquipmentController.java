package com.yeoun.equipment.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/equipment")
@RequiredArgsConstructor
@Log4j2
public class EquipmentController {

    @GetMapping("/master")
    public String master(Model model) {
    	
        return "masterData/equipment_line";
    }
    
    @GetMapping("/list")
    public String list(Model model) {
    	return "equipment/list";
    }
    
    @GetMapping("/line")
    public String line(Model model) {
    	return "equipment/line";
    }


}
