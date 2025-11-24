package digital.alf.cells.physicalacesscontrol.parser;

import digital.alf.cells.physicalacesscontrol.model.KyvernoUserInfo;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class KyvernoUserInfoParser {

    /**
     * Parses a Kyverno userinfo YAML file.
     *
     * @param inputStream InputStream of the YAML file
     * @return KyvernoUserInfo object
     */
    public KyvernoUserInfo parse(InputStream inputStream) {
        Yaml yaml = new Yaml();
        Map<String, Object> data = yaml.load(inputStream);

        KyvernoUserInfo userInfo = new KyvernoUserInfo();

        if (data != null) {
            userInfo.setApiVersion((String) data.get("apiVersion"));
            userInfo.setKind((String) data.get("kind"));

            // Parse clusterRoleBinding (if present)
            @SuppressWarnings("unchecked")
            Map<String, Object> clusterRoleBinding = (Map<String, Object>) data.get("clusterRoleBinding");
            if (clusterRoleBinding != null) {
                userInfo.setClusterRoleBinding(parseClusterRoleBinding(clusterRoleBinding));
            }

            // Parse requestInfo
            @SuppressWarnings("unchecked")
            Map<String, Object> requestInfo = (Map<String, Object>) data.get("requestInfo");
            if (requestInfo != null) {
                userInfo.setRequestInfo(parseRequestInfo(requestInfo));
            }
        }

        return userInfo;
    }

    private KyvernoUserInfo.ClusterRoleBinding parseClusterRoleBinding(Map<String, Object> data) {
        KyvernoUserInfo.ClusterRoleBinding binding = new KyvernoUserInfo.ClusterRoleBinding();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> subjects = (List<Map<String, Object>>) data.get("subjects");
        if (subjects != null) {
            List<KyvernoUserInfo.Subject> subjectList = new ArrayList<>();
            for (Map<String, Object> subjectData : subjects) {
                KyvernoUserInfo.Subject subject = new KyvernoUserInfo.Subject();
                subject.setKind((String) subjectData.get("kind"));
                subject.setName((String) subjectData.get("name"));
                subject.setApiGroup((String) subjectData.get("apiGroup"));
                subjectList.add(subject);
            }
            binding.setSubjects(subjectList);
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> roleRefData = (Map<String, Object>) data.get("roleRef");
        if (roleRefData != null) {
            KyvernoUserInfo.RoleRef roleRef = new KyvernoUserInfo.RoleRef();
            roleRef.setKind((String) roleRefData.get("kind"));
            roleRef.setName((String) roleRefData.get("name"));
            roleRef.setApiGroup((String) roleRefData.get("apiGroup"));
            binding.setRoleRef(roleRef);
        }

        return binding;
    }

    private KyvernoUserInfo.RequestInfo parseRequestInfo(Map<String, Object> data) {
        KyvernoUserInfo.RequestInfo requestInfo = new KyvernoUserInfo.RequestInfo();

        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) data.get("roles");
        requestInfo.setRoles(roles != null ? roles : new ArrayList<>());

        @SuppressWarnings("unchecked")
        List<String> clusterRoles = (List<String>) data.get("clusterRoles");
        requestInfo.setClusterRoles(clusterRoles != null ? clusterRoles : new ArrayList<>());

        @SuppressWarnings("unchecked")
        Map<String, Object> userInfoData = (Map<String, Object>) data.get("userInfo");
        if (userInfoData != null) {
            KyvernoUserInfo.UserInfo userInfo = new KyvernoUserInfo.UserInfo();
            userInfo.setUsername((String) userInfoData.get("username"));

            @SuppressWarnings("unchecked")
            List<String> groups = (List<String>) userInfoData.get("groups");
            userInfo.setGroups(groups != null ? groups : new ArrayList<>());

            requestInfo.setUserInfo(userInfo);
        }

        return requestInfo;
    }
}
