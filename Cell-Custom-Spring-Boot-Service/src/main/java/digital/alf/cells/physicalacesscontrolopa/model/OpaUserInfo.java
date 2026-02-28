package digital.alf.cells.physicalacesscontrolopa.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Represents the OPA input JSON structure used as --input for opa eval.
 * Each user file already embeds admissionTime, operation, resource, and userInfo.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OpaUserInfo {

    private Request request;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Request {
        private String admissionTime;
        private String operation;
        private Resource resource;
        private UserInfo userInfo;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Resource {
        private String apiVersion;
        private String kind;
        private Metadata metadata;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Metadata {
        private String name;
        private Map<String, String> labels;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UserInfo {
        private String username;
        private String uid;
        private List<String> groups;
    }

    public String getUserId() {
        if (request != null && request.userInfo != null) {
            return request.userInfo.uid;
        }
        return null;
    }

    public String getUsername() {
        if (request != null && request.userInfo != null) {
            return request.userInfo.username;
        }
        return null;
    }

    public List<String> getUserGroups() {
        if (request != null && request.userInfo != null && request.userInfo.groups != null) {
            return request.userInfo.groups;
        }
        return List.of();
    }
}
