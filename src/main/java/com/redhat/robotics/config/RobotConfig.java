package com.redhat.robotics.config;

import io.smallrye.config.ConfigMapping;
import java.util.List;

@ConfigMapping(prefix = "robot")
public interface RobotConfig {
    String name();
    Api api();
    
    interface Api {
        String url();
    }
}
