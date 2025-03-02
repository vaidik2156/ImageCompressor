package com.example.imgcomp.model;

import java.util.Date;
import java.util.UUID;

/**
 * Represents a compressed image model with metadata.
 * Used to store image data, format, dimensions, and timestamps for tracking.
 */
public class ImageModel {
    private String name; // Name of the image file
    private byte[] data; // Compressed image data
    private String format; // Format of the image (e.g., jpg, png)
    private int width; // Custom width (-1 for original width)
    private int height; // Custom height (-1 for original height)
    private String id; // Unique identifier for the image (e.g., for caching or download tracking)
    private Date timestamp; // When the image was compressed (optional, for tracking)

    // No-args constructor
    public ImageModel() {
        this.id = UUID.randomUUID().toString(); // Generate a unique ID by default
        this.timestamp = new Date(); // Set current timestamp by default
        this.width = -1; // Default to original width
        this.height = -1; // Default to original height
    }

    // Constructor with all required fields
    public ImageModel(String name, byte[] data, String format, int width, int height) {
        this.name = name;
        this.data = data != null ? data.clone() : null; // Defensive copy to prevent external modification
        this.format = format;
        this.width = width > 0 ? width : -1; // Use -1 for original dimensions if 0 or negative
        this.height = height > 0 ? height : -1; // Use -1 for original dimensions if 0 or negative
        this.id = UUID.randomUUID().toString(); // Generate a unique ID
        this.timestamp = new Date(); // Set current timestamp
    }

    // Getters and Setters with basic validation
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the compressed image data.
     * @return byte array containing the image data, or null if not set.
     */
    public byte[] getData() {
        return data != null ? data.clone() : null; // Return a copy to prevent external modification
    }

    public void setData(byte[] data) {
        this.data = data != null ? data.clone() : null; // Defensive copy
    }

    /**
     * Gets the image format.
     * @return String representing the image format (e.g., "jpg", "png").
     */
    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        if (format == null || format.trim().isEmpty()) {
            throw new IllegalArgumentException("Format cannot be null or empty");
        }
        this.format = format.toLowerCase(); // Normalize to lowercase for consistency
    }

    /**
     * Gets the width of the image (-1 indicates original width).
     * @return int representing the width in pixels, or -1 for original width.
     */
    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width > 0 ? width : -1; // Ensure width is positive or -1 for original
    }

    /**
     * Gets the height of the image (-1 indicates original height).
     * @return int representing the height in pixels, or -1 for original height.
     */
    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height > 0 ? height : -1; // Ensure height is positive or -1 for original
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id != null ? id : UUID.randomUUID().toString(); // Generate new ID if null
    }

    /**
     * Gets the timestamp when the image was compressed.
     * @return Date representing the compression timestamp.
     */
    public Date getTimestamp() {
        return timestamp != null ? (Date) timestamp.clone() : null; // Return a copy to prevent modification
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp != null ? (Date) timestamp.clone() : new Date(); // Defensive copy or default to now
    }
}