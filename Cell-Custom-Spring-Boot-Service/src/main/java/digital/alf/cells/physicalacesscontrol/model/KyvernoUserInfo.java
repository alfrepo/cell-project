package digital.alf.cells.physicalacesscontrol.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KyvernoUserInfo {
    private String apiVersion;
    private String kind;
    private ClusterRoleBinding clusterRoleBinding;
    private RequestInfo requestInfo;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClusterRoleBinding {
        private List<Subject> subjects;
        private RoleRef roleRef;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Subject {
        private String kind;
        private String name;
        private String apiGroup;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoleRef {
        private String kind;
        private String name;
        private String apiGroup;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RequestInfo {
        private List<String> roles;
        private List<String> clusterRoles;
        private UserInfo userInfo;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private String username;
        private List<String> groups;
    }

    public String getUserId() {
        if (requestInfo != null && requestInfo.userInfo != null) {
            return requestInfo.userInfo.username;
        }
        return null;
    }

    public List<String> getUserGroups() {
        if (requestInfo != null && requestInfo.userInfo != null) {
            return requestInfo.userInfo.groups;
        }
        return List.of();
    }
}
