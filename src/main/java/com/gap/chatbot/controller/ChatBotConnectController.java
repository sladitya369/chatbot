package com.gap.chatbot.controller;

import com.gap.chatbot.service.ChatBotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/chatBot")
@CrossOrigin
public class ChatBotConnectController {

    @Autowired
    private ChatBotService chatBotService;

    @GetMapping("/response")
    public String response(@RequestParam(value = "message", required = true) final String message) throws IOException, InterruptedException {

        return chatBotService.connectWithChatBot(message);
    }

    @PostMapping("/updateFile")
    public String updateTagFile(@RequestParam("file") MultipartFile file,
                                   RedirectAttributes redirectAttributes) {

        String UPLOADED_FOLDER = System.getProperty("user.dir");
        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("message", "Please select a file to upload");
            return "redirect:uploadStatus";
        }

        try {

            // Get the file and save it somewhere
            byte[] bytes = file.getBytes();
            Path path = Paths.get(UPLOADED_FOLDER + "/" + file.getOriginalFilename());
            Files.write(path, bytes);

            redirectAttributes.addFlashAttribute("message",
                    "You successfully uploaded '" + file.getOriginalFilename() + "'");

        } catch (IOException e) {
            e.printStackTrace();
        }
        return "Uploaded Successfully";
    }
}
