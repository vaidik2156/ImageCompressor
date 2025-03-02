package com.example.imgcomp.model;

public class ImageResponse {
    private String originalImageBase64;
    private long originalSize;
    private String compressedImageBase64;
    private long compressedSize;
    private String imageId;
    private String fileName;

    public ImageResponse(String originalImageBase64, long originalSize, String compressedImageBase64, long compressedSize, String imageId, String fileName) {
        this.originalImageBase64 = originalImageBase64;
        this.originalSize = originalSize;
        this.compressedImageBase64 = compressedImageBase64;
        this.compressedSize = compressedSize;
        this.imageId = imageId;
        this.fileName = fileName;
    }

    // Getters
    public String getOriginalImageBase64() { return originalImageBase64; }
    public long getOriginalSize() { return originalSize; }
    public String getCompressedImageBase64() { return compressedImageBase64; }
    public long getCompressedSize() { return compressedSize; }
    public String getImageId() { return imageId; }
    public String getFileName() { return fileName; }
}