package com.yeoun.orgchart.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/orgchart")
public class OrgChartController {
	
	// 조직도 화면
	@GetMapping("")
	public String orgchartPage() {
		return "org/orgchart";
	}
	

}

