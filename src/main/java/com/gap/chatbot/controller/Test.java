package com.gap.chatbot.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class Test {
    @RequestMapping(value = "/upload")
    public String index() {
        return "upload";
    }
}
