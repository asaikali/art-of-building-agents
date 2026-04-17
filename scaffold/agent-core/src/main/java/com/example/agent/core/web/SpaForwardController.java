package com.example.agent.core.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaForwardController {

  @GetMapping(value = {"/", "/sessions/**"})
  public String forward() {
    return "forward:/index.html";
  }
}
