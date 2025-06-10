package com.example.localshare.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DropController {

    @GetMapping("/")
    public String home() {
        return "index";
    }
    
    @GetMapping("/file-share")
    public String fileShare() {
        return "index";
    }
} 