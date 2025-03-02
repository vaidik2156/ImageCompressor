package com.example.imgcomp.controller;

import com.example.imgcomp.model.ImageModel;
import com.example.imgcomp.model.ImageResponse;
import com.example.imgcomp.service.ImageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller for handling image compression requests, responses, and feedback submissions.
 */
@Controller
public class ImageController {

    private static final Logger logger = LoggerFactory.getLogger(ImageController.class);

    @Autowired
    private ImageService imageService;

    @Autowired
    private JavaMailSender mailSender; // Inject JavaMailSender for feedback emails

    // Temporary storage for compressed images (use a cache or database in production)
    private Map<String, byte[]> imageCache = new HashMap<>();

    // Supported formats
    private static final String[] SUPPORTED_FORMATS = {"jpg", "png", "gif", "bmp", "webp"};

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("supportedFormats", SUPPORTED_FORMATS); // Pass supported formats to the frontend
        return "index"; // Return the name of your HTML file
    }

    @PostMapping("/compress")
    public String compressImages(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(value = "compressionType", required = false, defaultValue = "quality") String compressionType, // Default to quality
            @RequestParam(value = "maxFileSize", required = false) Integer maxFileSize, // In KB
            @RequestParam(value = "quality", required = false, defaultValue = "0.5") float quality, // Default quality if not using max size
            @RequestParam("format") String format,
            @RequestParam("width") int width,
            @RequestParam("height") int height,
            Model model) {

        // Validate the format
        if (!isFormatSupported(format)) {
            model.addAttribute("error", "Unsupported format: " + format);
            return "index";
        }

        // Validate maxFileSize if compressionType is maxSize
        if ("maxSize".equals(compressionType) && (maxFileSize == null || maxFileSize <= 0)) {
            model.addAttribute("error", "Max file size must be greater than 0 KB");
            return "index";
        }

        // Validate quality if compressionType is quality
        if ("quality".equals(compressionType) && (quality < 0.1f || quality > 1.0f)) {
            model.addAttribute("error", "Quality must be between 0.1 and 1.0");
            return "index";
        }

        List<ImageResponse> imageResponses = new ArrayList<>();
        List<String> errorMessages = new ArrayList<>();

        try {
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    try {
                        // Original image details
                        byte[] originalBytes = file.getBytes();
                        long originalSize = file.getSize();
                        String originalBase64 = java.util.Base64.getEncoder().encodeToString(originalBytes);

                        // Determine compression settings
                        ImageModel compressedImage;
                        if ("maxSize".equals(compressionType) && maxFileSize != null && maxFileSize > 0) {
                            // Compress to target max file size (in KB, convert to bytes)
                            long targetSizeBytes = maxFileSize * 1024L;
                            compressedImage = imageService.compressToMaxSize(file, targetSizeBytes, format, width, height);
                        } else {
                            // Use quality-based compression (default or user-specified)
                            compressedImage = imageService.compressImage(file, quality, format, width, height);
                        }

                        // Generate a unique ID for the image
                        String imageId = UUID.randomUUID().toString();

                        // Store the compressed image in the cache
                        imageCache.put(imageId, compressedImage.getData());

                        // Compressed image details
                        String compressedBase64 = java.util.Base64.getEncoder().encodeToString(compressedImage.getData());
                        long compressedSize = compressedImage.getData().length;

                        // Create response object
                        ImageResponse response = new ImageResponse(
                            originalBase64, originalSize, compressedBase64, compressedSize, imageId, compressedImage.getName()
                        );
                        imageResponses.add(response);

                    } catch (Exception e) {
                        errorMessages.add("Error compressing " + file.getOriginalFilename() + ": " + e.getMessage());
                    }
                } else {
                    errorMessages.add("Empty file skipped: " + file.getOriginalFilename());
                }
            }

            // Add attributes to the model
            model.addAttribute("imageResponses", imageResponses);
            model.addAttribute("format", format);
            model.addAttribute("width", width);
            model.addAttribute("height", height);
            model.addAttribute("errors", errorMessages);
            model.addAttribute("message", "Images compressed successfully!");
            model.addAttribute("supportedFormats", SUPPORTED_FORMATS);

            return "index";
        } catch (Exception e) {
            model.addAttribute("error", "Error processing images: " + e.getMessage());
            model.addAttribute("supportedFormats", SUPPORTED_FORMATS);
            return "index";
        }
    }

    @GetMapping("/download")
    public ResponseEntity<byte[]> downloadImage(
            @RequestParam("imageId") String imageId,
            @RequestParam("fileName") String fileName,
            @RequestParam("format") String format) {

        // Retrieve the compressed image from the cache
        byte[] imageData = imageCache.get(imageId);

        if (imageData == null) {
            return ResponseEntity.notFound().build(); // Image not found
        }

        // Return the image as a downloadable file
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "." + format + "\"")
                .contentType(MediaType.parseMediaType("image/" + format))
                .body(imageData);
    }

    // Check if the format is supported
    private boolean isFormatSupported(String format) {
        for (String supportedFormat : SUPPORTED_FORMATS) {
            if (supportedFormat.equalsIgnoreCase(format)) {
                return true;
            }
        }
        return false;
    }

    /**
 * Handles general feedback submission and sends it to a specified email via email service.
 *
 * @param feedback Map containing feedback details (rating, description, email).
 * @return Map with status and message indicating success or failure.
 */
@PostMapping("/api/general-feedback")
@ResponseBody
@CrossOrigin(origins = "http://localhost:8080") // Allow CORS for local testing
public Map<String, String> submitGeneralFeedback(@RequestBody Map<String, String> feedback) {
    Map<String, String> response = new HashMap<>();
    logger.info("Received general feedback submission");

    try {
        String rating = feedback.get("rating");
        String description = feedback.get("description");
        String userEmail = feedback.get("email"); // User-provided or fallback email (ignored here)

        // Validate input
        if (rating == null) {
            response.put("status", "error");
            response.put("message", "Missing required feedback fields");
            logger.error("General feedback submission failed: Missing required fields");
            return response;
        }

        // Hard-code the recipient to your personal email
        String recipientEmail = "vaidik627@gmail.com"; // Your email

        // Prepare email
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(recipientEmail); // Send to your personal email
        message.setSubject("New General Feedback for Image Compressor");
        message.setText("Feedback Details:\n" +
                "Rating: " + rating + " stars\n" +
                "Description: " + (description != null && !description.isEmpty() ? description : "No additional comments"));

        // Send email
        mailSender.send(message);
        logger.info("General feedback email sent successfully to: {}", recipientEmail);

        response.put("status", "success");
        response.put("message", "Feedback submitted successfully!");
    } catch (Exception e) {
        logger.error("Error submitting general feedback: {}", e.getMessage(), e);
        response.put("status", "error");
        response.put("message", "Error submitting feedback: " + e.getMessage());
    }

    return response;
}
}