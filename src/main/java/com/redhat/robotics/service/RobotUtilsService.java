package com.redhat.robotics.service;

import com.redhat.robotics.client.RobotApiClient;
import com.redhat.robotics.config.*;
import com.redhat.robotics.model.Coordinates;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

@ApplicationScoped
public class RobotUtilsService {

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Inject
    @RestClient
    RobotApiClient robotApiClient;

    @Inject
    RobotConfig robotConfig;

    @Inject
    ParametersConfig parametersConfig;

    @Inject
    ObjectDetectionService objectDetectionService;

    @Inject
    ImageProcessingService imageProcessingService;

    public void logWithTimestamp(String message) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        Log.infof("[%s] %s", timestamp, message);
    }

    public CompletableFuture<Void> writeFileAsync(String filename, byte[] data) {
        return CompletableFuture.runAsync(() -> {
            try {
                Path path = Paths.get(filename);
                Files.createDirectories(path.getParent());
                Files.write(path, data);
                logWithTimestamp("Async write to " + filename + " successful.");
            } catch (Exception e) {
                logWithTimestamp("Async write to " + filename + " failed: " + e.getMessage());
            }
        });
    }

    public String takePicture(String imageFileName) {
        logWithTimestamp("Executing takePicture for '" + imageFileName + "'...");
        String imageResponse = robotApiClient.getCamera(robotConfig.name());
        
        byte[] imageBytes = decodeBase64WithPadding(imageResponse);
        writeFileAsync(imageFileName, imageBytes);
        
        logWithTimestamp("takePicture finished (file writing started in background).");
        return imageResponse;
    }

    public List<double[]> takePictureAndDetectObjects() {
        logWithTimestamp("Executing takePictureAndDetectObjects...");
        String imageResponse = takePicture("src/main/resources/META-INF/resources/static/current_view.jpg");

        try {
            var preprocessResult = imageProcessingService.preprocessEncodedImage(imageResponse);
            float[][][] imageData = preprocessResult.imageData();
            double ratio = preprocessResult.ratio();
            double[] dwdh = preprocessResult.dwdh();

            logWithTimestamp("Detecting objects...");
            List<double[]> objects = objectDetectionService.detectObjects(imageData);
            logWithTimestamp("Detection finished. Found " + (objects != null ? objects.size() : 0) + " objects.");

            // Process and save image with detections
            byte[] imageBytes = decodeBase64WithPadding(imageResponse);
            byte[] imageWithDetections = imageProcessingService.drawDetections(imageBytes, objects, ratio, dwdh);
            if (imageWithDetections != null) {
                writeFileAsync("src/main/resources/META-INF/resources/static/current_view_box.jpg", imageWithDetections);
            }

            return objects;
        } catch (Exception e) {
            logWithTimestamp("ERROR: Cannot process image file from robot response: " + e.getMessage());
            return null;
        }
    }

    public Coordinates findHighestScore(List<double[]> objects) {
        if (objects == null || objects.isEmpty()) {
            return null;
        }

        double[] detectedObject = {0, 0, 0, 0, 0, 0};
        for (double[] obj : objects) {
            if (obj.length >= 6 && obj[5] == 0 && detectedObject[4] < obj[4]) {
                detectedObject = obj;
            }
        }

        if (detectedObject[4] > 0) {
            return new Coordinates(detectedObject[4], detectedObject[0], detectedObject[1], 
                                 detectedObject[2], detectedObject[3], (int) detectedObject[5]);
        }
        return null;
    }

    public boolean bypassObstacle() {
        logWithTimestamp("bypassObstacle: Checking distance...");
        int dist = distanceInt();
        logWithTimestamp("bypassObstacle: Distance is " + dist + "mm.");

        int minDistanceToObstacle = parametersConfig.minDistanceToObstacle();
        int angleDelta = parametersConfig.angleDelta();

        if (dist <= minDistanceToObstacle) {
            logWithTimestamp("bypassObstacle: Obstacle detected.");
            takePicture("src/main/resources/META-INF/resources/static/current_view.jpg");
            int distanceToObject = distanceInt();
            turnLeft(angleDelta);
            if (distanceInt() > minDistanceToObstacle) {
                moveForward(20);
                turnRight(angleDelta);
            }
            if (distanceInt() > minDistanceToObstacle) {
                moveForward((int) Math.ceil(distanceToObject / 10.0) + 40);
            }
            return true;
        }
        return false;
    }

    public int searchForHatStep(int turnCounter, AtomicReference<Boolean> hatFoundAndInterceptedRef) {
        List<double[]> objects = takePictureAndDetectObjects();

        if (objects == null) {
            logWithTimestamp("searchForHatStep: Skipping due to image processing error.");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return turnCounter;
        }

        Coordinates coordinates = findHighestScore(objects);

        if (coordinates != null && coordinates.confidenceScore() > 0.3) {
            logWithTimestamp("Hat candidate found. Aligning and approaching.");
            double centerX = coordinates.getCenterX();
            int imageResolutionX = parametersConfig.imageResolutionX();
            int deltaThreshold = parametersConfig.deltaThreshold();

            if (Math.abs(imageResolutionX / 2.0 - centerX) >= 20) {
                if (centerX < 320) {
                    turnLeft(10);
                } else {
                    turnRight(9);
                }
            } else {
                double delta = coordinates.getDelta();
                if (delta < deltaThreshold) {
                    moveForward(10);
                } else {
                    hatFoundAndInterceptedRef.set(true);
                    logWithTimestamp("### Hat Intercepted! ###");
                }
            }
        } else {
            logWithTimestamp("No hat found. Continuing search pattern.");
            if (turnCounter <= 360) {
                turnRight(10);
                turnCounter += 10;
            } else {
                moveForward(40);
                turnCounter = 0;
            }
        }

        return turnCounter;
    }

    public void moveForward(int length) {
        logWithTimestamp("Sending command: moveForward(" + length + ")");
        robotApiClient.moveForward(length, robotConfig.name());
    }

    public void moveBackward(int length) {
        logWithTimestamp("Sending command: moveBackward(" + length + ")");
        robotApiClient.moveBackward(length, robotConfig.name());
    }

    public void turnLeft(int degrees) {
        logWithTimestamp("Sending command: turnLeft(" + degrees + ")");
        robotApiClient.turnLeft(degrees, robotConfig.name());
    }

    public void turnRight(int degrees) {
        logWithTimestamp("Sending command: turnRight(" + degrees + ")");
        robotApiClient.turnRight(degrees, robotConfig.name());
    }

    public String distance() {
        return robotApiClient.getDistance(robotConfig.name());
    }

    public int distanceInt() {
        String distanceStr = distance().replaceAll("[^0-9]", "");
        return distanceStr.isEmpty() ? 0 : Integer.parseInt(distanceStr);
    }

    private byte[] decodeBase64WithPadding(String base64String) {
        base64String = base64String.strip();

        // Add padding if needed
        int missingPadding = base64String.length() % 4;
        if (missingPadding != 0) {
            base64String += "=".repeat(4 - missingPadding);
        }

        try {
            return Base64.getUrlDecoder().decode(base64String);
        } catch (Exception e) {
            try {
                return Base64.getDecoder().decode(base64String);
            } catch (Exception ex) {
                logWithTimestamp("Base64 decode failed for string (length " + base64String.length() + "): " + ex.getMessage());
                logWithTimestamp("First 100 chars: " + base64String.substring(0, Math.min(100, base64String.length())));
                throw ex;
            }
        }
    }
}
