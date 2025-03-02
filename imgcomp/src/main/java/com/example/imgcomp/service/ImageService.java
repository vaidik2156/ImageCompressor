package com.example.imgcomp.service;

import com.example.imgcomp.model.ImageModel;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Service class for handling image compression operations.
 */
@Service
public class ImageService {

    /**
     * Compresses an array of images using the specified quality, format, and dimensions.
     *
     * @param files   Array of MultipartFile objects containing the images to compress.
     * @param quality Compression quality (0.1 to 1.0) for quality-based compression.
     * @param format  Output format for the compressed images (e.g., "jpg", "png").
     * @param width   Desired width in pixels (0 to keep original width).
     * @param height  Desired height in pixels (0 to keep original height).
     * @return List of ImageModel objects representing the compressed images.
     * @throws IOException If an error occurs during image processing.
     */
    public List<ImageModel> compressImages(MultipartFile[] files, float quality, String format, int width, int height) throws IOException {
        List<ImageModel> compressedImages = new ArrayList<>();

        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                throw new IllegalArgumentException("Empty file skipped: " + (file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown"));
            }

            try {
                ImageModel compressedImage = compressImage(file, quality, format, width, height);
                compressedImages.add(compressedImage);
            } catch (IOException e) {
                throw new IOException("Error compressing image " + file.getOriginalFilename() + ": " + e.getMessage(), e);
            }
        }

        return compressedImages;
    }

    /**
     * Compresses a single image using the specified quality, format, and dimensions.
     *
     * @param file    MultipartFile object containing the image to compress.
     * @param quality Compression quality (0.1 to 1.0) for quality-based compression.
     * @param format  Output format for the compressed image (e.g., "jpg", "png").
     * @param width   Desired width in pixels (0 to keep original width).
     * @param height  Desired height in pixels (0 to keep original height).
     * @return ImageModel object representing the compressed image.
     * @throws IOException If an error occurs during image processing.
     */
    public ImageModel compressImage(MultipartFile file, float quality, String format, int width, int height) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty: " + (file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown"));
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Thumbnails.of(file.getInputStream())
                .size(width > 0 ? width : null, height > 0 ? height : null) // Use provided dimensions or null for original
                .outputQuality(quality) // Set compression quality (0.1 to 1.0)
                .outputFormat(format) // Set output format
                .toOutputStream(outputStream);

        ImageModel imageModel = new ImageModel();
        imageModel.setName(file.getOriginalFilename());
        imageModel.setData(outputStream.toByteArray());
        imageModel.setFormat(format);
        imageModel.setWidth(width > 0 ? width : -1); // Use -1 to indicate original width
        imageModel.setHeight(height > 0 ? height : -1); // Use -1 to indicate original height

        return imageModel;
    }

    /**
     * Compresses a single image to fit within a target file size in bytes, adjusting quality dynamically.
     *
     * @param file           MultipartFile object containing the image to compress.
     * @param targetSizeBytes Target file size in bytes.
     * @param format         Output format for the compressed image (e.g., "jpg", "png").
     * @param width          Desired width in pixels (0 to keep original width).
     * @param height         Desired height in pixels (0 to keep original height).
     * @return ImageModel object representing the compressed image.
     * @throws IOException If an error occurs during image processing.
     */
    public ImageModel compressToMaxSize(MultipartFile file, long targetSizeBytes, String format, int width, int height) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty: " + (file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown"));
        }

        float quality = 1.0f; // Start with maximum quality
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageModel result = null;

        while (true) {
            outputStream.reset(); // Clear the output stream
            Thumbnails.of(file.getInputStream())
                    .size(width > 0 ? width : null, height > 0 ? height : null) // Use provided dimensions or null for original
                    .outputQuality(quality)
                    .outputFormat(format)
                    .toOutputStream(outputStream);

            long currentSize = outputStream.size();
            if (currentSize <= targetSizeBytes) {
                result = new ImageModel();
                result.setName(file.getOriginalFilename());
                result.setData(outputStream.toByteArray());
                result.setFormat(format);
                result.setWidth(width > 0 ? width : -1); // Use -1 to indicate original width
                result.setHeight(height > 0 ? height : -1); // Use -1 to indicate original height
                break;
            }

            if (quality <= 0.1f) { // Minimum quality threshold
                break; // Stop if quality is too low
            }

            quality -= 0.05f; // Reduce quality in smaller increments for finer control
        }

        if (result == null) {
            // Fallback: Use lowest quality if target size not achievable
            outputStream.reset();
            Thumbnails.of(file.getInputStream())
                    .size(width > 0 ? width : null, height > 0 ? height : null)
                    .outputQuality(0.1f) // Minimum quality
                    .outputFormat(format)
                    .toOutputStream(outputStream);
            result = new ImageModel();
            result.setName(file.getOriginalFilename());
            result.setData(outputStream.toByteArray());
            result.setFormat(format);
            result.setWidth(width > 0 ? width : -1);
            result.setHeight(height > 0 ? height : -1);
        }

        return result;
    }
}