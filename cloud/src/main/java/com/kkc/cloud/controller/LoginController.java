package com.kkc.cloud.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.kkc.cloud.data.LoginData;

import jakarta.servlet.http.HttpSession;

@Controller
public class LoginController {

	@PostMapping("/main")
	public String login(@RequestParam("id") String id, @RequestParam("password") String password, HttpSession session) {
		if(id == null || password == null)
			return "redirect:/login/login.html";
		if(id.equals(LoginData.adminId) && password.equals(LoginData.adminPassword)) {
			session.setAttribute("id", id);
			return "main.html";
		}
		return "redirect:/login/login.html";
	}
	
	@RequestMapping(value = "/guest", method = {RequestMethod.GET, RequestMethod.POST})
	public String loginGuest(HttpSession session) {
		return "main.html";
	}
}
