package com.redhat.robotics.service;

import com.redhat.robotics.config.ModelConfig;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.*;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import javax.imageio.ImageIO;

@ApplicationScoped
public class ImageProcessingService {

    @Inject
    ModelConfig modelConfig;

    public record PreprocessResult(float[][][] imageData, double ratio, double[] dwdh) {}

    public PreprocessResult preprocessEncodedImage(String base64EncodedImage) throws IOException {
        byte[] imageBytes = Base64.getDecoder().decode(base64EncodedImage);
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
        return transform(image);
    }

    public PreprocessResult preprocessImageFile(String imagePath) throws IOException {
        BufferedImage image = ImageIO.read(new java.io.File(imagePath));
        return transform(image);
    }

    private PreprocessResult transform(BufferedImage image) {
        int imageSize = 640;
        
        // Letterbox the image
        var letterboxResult = letterboxImage(image, imageSize);
        BufferedImage letterboxedImage = letterboxResult.image();
        double ratio = letterboxResult.ratio();
        double[] dwdh = letterboxResult.dwdh();

        // Convert to CHW format and normalize
        float[][][] imageData = new float[3][imageSize][imageSize];
        
        for (int y = 0; y < imageSize; y++) {
            for (int x = 0; x < imageSize; x++) {
                int rgb = letterboxedImage.getRGB(x, y);
                Color color = new Color(rgb);
                
                // Normalize to [0, 1] and convert to CHW format
                imageData[0][y][x] = color.getRed() / 255.0f;   // R channel
                imageData[1][y][x] = color.getGreen() / 255.0f; // G channel
                imageData[2][y][x] = color.getBlue() / 255.0f;  // B channel
            }
        }

        return new PreprocessResult(imageData, ratio, dwdh);
    }

    private record LetterboxResult(BufferedImage image, double ratio, double[] dwdh) {}

    private LetterboxResult letterboxImage(BufferedImage image, int imageSize) {
        int width = image.getWidth();
        int height = image.getHeight();
        
        // Calculate scale ratio
        double ratio = Math.min((double) imageSize / width, (double) imageSize / height);
        
        // Calculate new dimensions
        int newWidth = (int) Math.round(width * ratio);
        int newHeight = (int) Math.round(height * ratio);
        
        // Calculate padding
        double dw = (imageSize - newWidth) / 2.0;
        double dh = (imageSize - newHeight) / 2.0;
        
        // Create new image with padding
        BufferedImage resized = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resized.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(image, 0, 0, newWidth, newHeight, null);
        g2d.dispose();
        
        // Add padding
        BufferedImage padded = new BufferedImage(imageSize, imageSize, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2dPadded = padded.createGraphics();
        g2dPadded.setColor(new Color(114, 114, 114)); // Gray padding
        g2dPadded.fillRect(0, 0, imageSize, imageSize);
        g2dPadded.drawImage(resized, (int) dw, (int) dh, null);
        g2dPadded.dispose();
        
        return new LetterboxResult(padded, ratio, new double[]{dw, dh});
    }

    public byte[] drawDetections(byte[] imageBytes, List<double[]> detections, double ratio, double[] dwdh) {
        if (detections == null || detections.isEmpty()) {
            return imageBytes;
        }

        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
            BufferedImage result = drawDetectionsOnImage(image, detections, ratio, dwdh);
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(result, "jpg", baos);
            return baos.toByteArray();
        } catch (IOException e) {
            Log.errorf("Error drawing detections: %s", e.getMessage());
            return imageBytes;
        }
    }

    private BufferedImage drawDetectionsOnImage(BufferedImage image, List<double[]> detections, double ratio, double[] dwdh) {
        BufferedImage result = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = result.createGraphics();
        g2d.drawImage(image, 0, 0, null);
        
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setStroke(new BasicStroke(2.0f));
        g2d.setColor(Color.GREEN);
        g2d.setFont(new Font("Arial", Font.BOLD, 12));

        double dw = dwdh[0];
        double dh = dwdh[1];
        double dwHalf = dw / 2;
        double dhHalf = dh / 2;
        
        int originalW = image.getWidth();
        int originalH = image.getHeight();
        
        double unpaddedW = 640 - dw;
        double unpaddedH = 640 - dh;
        
        if (unpaddedW == 0 || unpaddedH == 0) {
            Log.warn("Warning: Unpadded image dimensions are zero, cannot calculate scale factors.");
            g2d.dispose();
            return result;
        }
        
        double scaleX = originalW / unpaddedW;
        double scaleY = originalH / unpaddedH;

        for (double[] detection : detections) {
            double x1 = (detection[0] - dwHalf) * scaleX;
            double y1 = (detection[1] - dhHalf) * scaleY;
            double x2 = (detection[2] - dwHalf) * scaleX;
            double y2 = (detection[3] - dhHalf) * scaleY;
            
            // Clip to image bounds
            x1 = Math.max(0, Math.min(x1, originalW - 1));
            y1 = Math.max(0, Math.min(y1, originalH - 1));
            x2 = Math.max(0, Math.min(x2, originalW - 1));
            y2 = Math.max(0, Math.min(y2, originalH - 1));
            
            double conf = detection[4];
            int classId = (int) detection[5];
            
            // Draw rectangle
            g2d.drawRect((int) x1, (int) y1, (int) (x2 - x1), (int) (y2 - y1));
            
            // Draw label
            String label = classId < modelConfig.classLabels().size() 
                ? modelConfig.classLabels().get(classId) + ": " + String.format("%.2f", conf)
                : "Unknown: " + String.format("%.2f", conf);
                
            FontMetrics fm = g2d.getFontMetrics();
            int textWidth = fm.stringWidth(label);
            int textHeight = fm.getHeight();
            
            int textX = (int) x1;
            int textY = (int) (y1 - 10 > textHeight ? y1 - 10 : y1 + textHeight + 10);
            
            g2d.drawString(label, textX, textY);
        }
        
        g2d.dispose();
        return result;
    }
}
