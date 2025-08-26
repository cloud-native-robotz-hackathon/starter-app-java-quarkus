package com.redhat.robotics.service;

import com.redhat.robotics.client.InferencingApiClient;
import com.redhat.robotics.config.InferencingConfig;
import com.redhat.robotics.config.ModelConfig;
import com.redhat.robotics.model.InferencingRequest;
import com.redhat.robotics.model.InferencingResponse;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.*;
import java.util.stream.IntStream;

@ApplicationScoped
public class ObjectDetectionService {

    @Inject
    @RestClient
    InferencingApiClient inferencingApiClient;

    @Inject
    InferencingConfig inferencingConfig;

    @Inject
    ModelConfig modelConfig;

    public List<double[]> detectObjects(float[][][] image) {
        return detectObjects(image, modelConfig.confidenceThreshold(), 0.2);
    }

    public List<double[]> detectObjects(float[][][] image, double confidenceThreshold, double iouThreshold) {
        try {
            InferencingRequest payload = serialize(image);
            InferencingResponse modelResponse = getModelResponse(payload);
            if (modelResponse == null) {
                return null;
            }
            return postprocess(modelResponse, confidenceThreshold, iouThreshold);
        } catch (Exception e) {
            Log.errorf("Error in object detection: %s", e.getMessage());
            return null;
        }
    }

    private InferencingRequest serialize(float[][][] image) {
        // Flatten the 3D array to 1D list
        List<Float> flatData = new ArrayList<>();
        for (int c = 0; c < image.length; c++) {
            for (int h = 0; h < image[c].length; h++) {
                for (int w = 0; w < image[c][h].length; w++) {
                    flatData.add(image[c][h][w]);
                }
            }
        }

        InferencingRequest.InputData inputData = new InferencingRequest.InputData(
            "images",
            Arrays.asList(1, 3, 640, 640),
            "FP32",
            flatData
        );

        return new InferencingRequest(Collections.singletonList(inputData));
    }

    private InferencingResponse getModelResponse(InferencingRequest payload) {
        try {
            String authHeader = "Bearer " + inferencingConfig.api().token();
            return inferencingApiClient.predict(payload, authHeader);
        } catch (Exception e) {
            Log.errorf("Failed to get model response: %s", e.getMessage());
            return null;
        }
    }

    private List<double[]> postprocess(InferencingResponse response, double confThres, double iouThres) {
        if (response.getOutputs() == null || response.getOutputs().isEmpty()) {
            return Collections.emptyList();
        }

        List<Double> outputData = response.getOutputs().get(0).getData();
        int classesCount = modelConfig.classLabels().size();
        int predictionColumnsNumber = 5 + classesCount;
        
        // Reshape the flat array prediction
        double[][] prediction = new double[outputData.size() / predictionColumnsNumber][predictionColumnsNumber];
        for (int i = 0; i < prediction.length; i++) {
            for (int j = 0; j < predictionColumnsNumber; j++) {
                prediction[i][j] = outputData.get(i * predictionColumnsNumber + j);
            }
        }

        return nonMaxSuppression(prediction, confThres, iouThres, classesCount);
    }

    private List<double[]> nonMaxSuppression(double[][] prediction, double confThres, double iouThres, int nc) {
        List<double[]> output = new ArrayList<>();
        
        for (double[] x : prediction) {
            // Check confidence
            if (x[4] <= confThres) continue;

            // Convert xywh to xyxy
            double[] box = xywh2xyxy(Arrays.copyOf(x, 4));
            
            // Get confidence and class
            double maxConf = 0;
            int bestClass = 0;
            for (int i = 5; i < 5 + nc; i++) {
                if (x[i] > maxConf) {
                    maxConf = x[i];
                    bestClass = i - 5;
                }
            }
            
            double conf = x[4] * maxConf;
            if (conf <= confThres) continue;

            // Create output entry [x1, y1, x2, y2, conf, class]
            double[] detection = new double[6];
            System.arraycopy(box, 0, detection, 0, 4);
            detection[4] = conf;
            detection[5] = bestClass;
            
            output.add(detection);
        }

        // Apply NMS
        return applyNMS(output, iouThres);
    }

    private double[] xywh2xyxy(double[] x) {
        double[] y = new double[4];
        y[0] = x[0] - x[2] / 2;  // top left x
        y[1] = x[1] - x[3] / 2;  // top left y
        y[2] = x[0] + x[2] / 2;  // bottom right x
        y[3] = x[1] + x[3] / 2;  // bottom right y
        return y;
    }

    private List<double[]> applyNMS(List<double[]> detections, double iouThres) {
        if (detections.isEmpty()) return detections;

        // Sort by confidence (descending)
        detections.sort((a, b) -> Double.compare(b[4], a[4]));

        List<double[]> keep = new ArrayList<>();
        boolean[] suppressed = new boolean[detections.size()];

        for (int i = 0; i < detections.size(); i++) {
            if (suppressed[i]) continue;
            
            keep.add(detections.get(i));
            
            for (int j = i + 1; j < detections.size(); j++) {
                if (suppressed[j]) continue;
                
                double iou = calculateIoU(detections.get(i), detections.get(j));
                if (iou > iouThres) {
                    suppressed[j] = true;
                }
            }
        }

        return keep;
    }

    private double calculateIoU(double[] box1, double[] box2) {
        double x1 = Math.max(box1[0], box2[0]);
        double y1 = Math.max(box1[1], box2[1]);
        double x2 = Math.min(box1[2], box2[2]);
        double y2 = Math.min(box1[3], box2[3]);

        double intersection = Math.max(0, x2 - x1) * Math.max(0, y2 - y1);
        
        double area1 = (box1[2] - box1[0]) * (box1[3] - box1[1]);
        double area2 = (box2[2] - box2[0]) * (box2[3] - box2[1]);
        double union = area1 + area2 - intersection;

        return union > 0 ? intersection / union : 0;
    }
}
