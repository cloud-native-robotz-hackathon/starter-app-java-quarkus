package com.redhat.robotics.config;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "inferencing")
public interface InferencingConfig {
    Api api();
    
    interface Api {
        String url();
        String token();
    }
}
