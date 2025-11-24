package digital.alf.cells.physicalacesscontrol.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KyvernoClusterReport {
    private String kind;
    private String apiVersion;
    private Metadata metadata;
    private String source;
    private Summary summary;
    private List<Result> results;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Metadata {
        private String name;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Summary {
        private int pass;
        private int fail;
        private int warn;
        private int error;
        private int skip;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Result {
        private String source;
        private String policy;
        private String rule;
        private Timestamp timestamp;
        private String result;  // "pass", "fail", "warn", "error", "skip"
        private boolean scored;
        private List<Resource> resources;
        private String message;
        private Properties properties;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Timestamp {
        private long seconds;
        private int nanos;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Resource {
        private String kind;
        private String namespace;
        private String name;
        private String apiVersion;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Properties {
        private String process;
    }

    /**
     * Check if the report contains any failed results.
     */
    public boolean hasFailed() {
        if (results == null || results.isEmpty()) {
            return false;
        }
        return results.stream().anyMatch(r -> "fail".equalsIgnoreCase(r.result));
    }

    /**
     * Check if the report passed (no failures).
     */
    public boolean hasPassed() {
        return !hasFailed();
    }
}
