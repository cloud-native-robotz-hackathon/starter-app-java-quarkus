package com.redhat.robotics.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class InferencingResponse {
    
    @JsonProperty("outputs")
    private List<OutputData> outputs;

    public InferencingResponse() {}

    public List<OutputData> getOutputs() {
        return outputs;
    }

    public void setOutputs(List<OutputData> outputs) {
        this.outputs = outputs;
    }

    public static class OutputData {
        @JsonProperty("data")
        private List<Double> data;

        public OutputData() {}

        public List<Double> getData() { return data; }
        public void setData(List<Double> data) { this.data = data; }
    }
}
