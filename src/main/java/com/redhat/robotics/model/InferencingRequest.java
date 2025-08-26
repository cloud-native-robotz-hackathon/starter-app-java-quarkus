package com.redhat.robotics.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class InferencingRequest {
    
    @JsonProperty("inputs")
    private List<InputData> inputs;

    public InferencingRequest() {}

    public InferencingRequest(List<InputData> inputs) {
        this.inputs = inputs;
    }

    public List<InputData> getInputs() {
        return inputs;
    }

    public void setInputs(List<InputData> inputs) {
        this.inputs = inputs;
    }

    public static class InputData {
        @JsonProperty("name")
        private String name;
        
        @JsonProperty("shape")
        private List<Integer> shape;
        
        @JsonProperty("datatype")
        private String datatype;
        
        @JsonProperty("data")
        private List<Float> data;

        public InputData() {}

        public InputData(String name, List<Integer> shape, String datatype, List<Float> data) {
            this.name = name;
            this.shape = shape;
            this.datatype = datatype;
            this.data = data;
        }

        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public List<Integer> getShape() { return shape; }
        public void setShape(List<Integer> shape) { this.shape = shape; }
        public String getDatatype() { return datatype; }
        public void setDatatype(String datatype) { this.datatype = datatype; }
        public List<Float> getData() { return data; }
        public void setData(List<Float> data) { this.data = data; }
    }
}
