package com.redhat.robotics.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class StreamResponse {
    
    @JsonProperty("image")
    private String image;
    
    @JsonProperty("error")
    private String error;

    public StreamResponse() {}

    public StreamResponse(String image) {
        this.image = image;
    }

    public StreamResponse(String error, boolean isError) {
        this.error = error;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
