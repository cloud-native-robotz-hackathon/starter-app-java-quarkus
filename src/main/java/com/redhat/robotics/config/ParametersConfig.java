package com.redhat.robotics.config;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "parameters")
public interface ParametersConfig {
    int imageResolutionX();
    int deltaThreshold();
    int minDistanceToObstacle();
    int angleDelta();
}
