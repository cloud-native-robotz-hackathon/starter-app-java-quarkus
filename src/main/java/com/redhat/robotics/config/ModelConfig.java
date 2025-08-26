package com.redhat.robotics.config;

import io.smallrye.config.ConfigMapping;
import java.util.List;

@ConfigMapping(prefix = "model")
public interface ModelConfig {
    double confidenceThreshold();
    List<String> classLabels();
}
