package com.transcriptionProject.Controllers;

import com.transcriptionProject.Util.S3Util;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;


@Controller
public class UploadController {
    @GetMapping("")
    public String uploadPage() {
        return "upload";
    }

    @PostMapping("/upload")
    public String uploadFile(Model model, String description,
             @RequestParam("file") MultipartFile multipart) {
        String fileName = multipart.getOriginalFilename();

        System.out.println("Description: " + description);
        System.out.println("Filename: " + fileName);

        String message = "";

        try {
            S3Util.uploadFile(fileName, multipart.getInputStream());
        } catch (Exception ex) {
            message = "Error uploading file: " + ex.getMessage();
        }

        try {
            S3Util.transcribeObject(fileName);
        } catch (Exception ex) {
            message = "Error transcribing file: " + ex.getMessage();
        }

        try {
            message = S3Util.getTranscript(fileName);
        } catch (Exception ex) {
            message = "Error getting transcription" + ex.getMessage();
        }

        model.addAttribute("message", message);

        return "message";
    }
}
