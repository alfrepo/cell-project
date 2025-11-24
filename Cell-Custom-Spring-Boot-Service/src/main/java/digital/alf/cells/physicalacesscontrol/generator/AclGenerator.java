package digital.alf.cells.physicalacesscontrol.generator;

import digital.alf.cells.physicalacesscontrol.model.AclEntry;
import digital.alf.cells.physicalacesscontrol.model.EmployeeInfo;
import digital.alf.cells.physicalacesscontrol.model.KyvernoPolicyData;
import digital.alf.cells.physicalacesscontrol.model.KyvernoUserInfo;
import digital.alf.cells.physicalacesscontrol.service.UserEvaluationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AclGenerator {

    private final UserEvaluationService userEvaluationService;

    /**
     * Generates ACL entries based on the Kyverno policy and employee information.
     *
     * Algorithm:
     * 1. Extract policy requirements (required groups, operations, resources, conditions)
     * 2. Filter employees who meet all group requirements
     * 3. For each qualifying employee, generate ACL entries for each operation
     * 4. Format entries according to target ACL format
     *
     * @param policyData Parsed Kyverno policy data
     * @param employees List of employees with their group memberships
     * @return List of ACL entries granting access
     */
    public List<AclEntry> generateAcl(KyvernoPolicyData policyData, List<EmployeeInfo> employees) {
        List<AclEntry> aclEntries = new ArrayList<>();

        // Filter employees who meet the policy requirements
        // The policy DENIES access to those without training, so we generate ACL for those WITH training
        List<EmployeeInfo> qualifiedEmployees = employees.stream()
                .filter(emp -> emp.hasGroup(policyData.getMatchGroup()))  // Must be in employee-group
                .filter(emp -> emp.hasGroup(policyData.getRequiredGroup())) // Must have training-vde-available-group
                .collect(Collectors.toList());

        // Build resource string from policy
        String resourceDescription = buildResourceDescription(policyData);

        // Build condition string from policy
        String conditionDescription = buildConditionDescription(policyData);

        // Generate ACL entry for each qualified employee and each operation
        for (EmployeeInfo employee : qualifiedEmployees) {
            for (String operation : policyData.getOperations()) {
                AclEntry entry = AclEntry.builder()
                        .principal(formatPrincipal(employee))
                        .action(operation)
                        .resource(resourceDescription)
                        .condition(conditionDescription)
                        .build();

                aclEntries.add(entry);
            }
        }

        return aclEntries;
    }

    /**
     * Formats the principal according to target format: <UserId:name>
     */
    private String formatPrincipal(EmployeeInfo employee) {
        return String.format("<%s:%s>", employee.getId(), employee.getName());
    }

    /**
     * Builds a human-readable resource description from policy data.
     */
    private String buildResourceDescription(KyvernoPolicyData policyData) {
        StringBuilder sb = new StringBuilder();
        sb.append(policyData.getResourceKind());

        if (policyData.getResourceLabels() != null && !policyData.getResourceLabels().isEmpty()) {
            String labels = policyData.getResourceLabels().entrySet().stream()
                    .map(e -> e.getKey() + "='" + e.getValue() + "'")
                    .collect(Collectors.joining(", "));
            sb.append(" [").append(labels).append("]");
        }

        return sb.toString();
    }

    /**
     * Builds a human-readable condition description from policy data.
     */
    private String buildConditionDescription(KyvernoPolicyData policyData) {
        StringBuilder sb = new StringBuilder();

        // Add group requirements
        sb.append("Subject must be member of '")
          .append(policyData.getMatchGroup())
          .append("' AND '")
          .append(policyData.getRequiredGroup())
          .append("'");

        // Add time window if present
        if (policyData.getTimeWindowStart() != null && policyData.getTimeWindowEnd() != null) {
            sb.append("; Time window: ")
              .append(policyData.getTimeWindowStart())
              .append(" to ")
              .append(policyData.getTimeWindowEnd());
        }

        return sb.toString();
    }

    /**
     * Generates ACL entries by dynamically evaluating users against Kyverno policy using kyverno-cli.
     *
     * NEW Algorithm:
     * 1. Read all user files from resources/physical-access-control/pip-users/
     * 2. For each user file and each operation:
     *    - Execute kyverno-cli to evaluate policy
     *    - Parse the ClusterReport JSON output
     *    - If results.result != "fail", user has access
     * 3. Generate ACL entries for qualified users
     *
     * @param policyData Parsed Kyverno policy data
     * @param policyPath Path to policy YAML file
     * @param resourcePath Path to resource YAML file (e.g., pip-resources/pip-resource-room.yml)
     * @param admissionTime Admission time for evaluation (e.g., "2025-10-20T08:30:00Z")
     * @return List of ACL entries for users who passed kyverno evaluation
     */
    public List<AclEntry> generateAclWithDynamicEvaluation(
            KyvernoPolicyData policyData,
            String policyPath,
            String resourcePath,
            String admissionTime) throws IOException {

        List<AclEntry> aclEntries = new ArrayList<>();

        // Evaluate each operation separately
        for (String operation : policyData.getOperations()) {
            // Get users who pass evaluation for this operation
            List<KyvernoUserInfo> qualifiedUsers = userEvaluationService.evaluateUsersForAccess(
                    policyPath,
                    resourcePath,
                    operation,
                    admissionTime
            );

            // Build resource and condition descriptions
            String resourceDescription = buildResourceDescription(policyData);
            String conditionDescription = buildConditionDescriptionForDynamicEval(policyData, admissionTime);

            // Generate ACL entries for qualified users
            for (KyvernoUserInfo userInfo : qualifiedUsers) {
                AclEntry entry = AclEntry.builder()
                        .principal(formatPrincipalFromUserInfo(userInfo))
                        .action(operation)
                        .resource(resourceDescription)
                        .condition(conditionDescription)
                        .build();

                aclEntries.add(entry);
            }
        }

        return aclEntries;
    }

    /**
     * Formats the principal from KyvernoUserInfo: <UserId:name>
     */
    private String formatPrincipalFromUserInfo(KyvernoUserInfo userInfo) {
        String userId = userInfo.getUserId();
        // Extract name from username if available, otherwise use username
        String displayName = userId != null ? userId : "unknown";
        return String.format("<%s>", displayName);
    }

    /**
     * Builds condition description for dynamic evaluation, including admission time.
     */
    private String buildConditionDescriptionForDynamicEval(KyvernoPolicyData policyData, String admissionTime) {
        StringBuilder sb = new StringBuilder();

        sb.append("Evaluated via Kyverno CLI");

        // Add group requirements if present
        if (policyData.getMatchGroup() != null) {
            sb.append("; Subject must be member of '")
              .append(policyData.getMatchGroup())
              .append("'");
        }

        if (policyData.getRequiredGroup() != null) {
            sb.append(" AND '")
              .append(policyData.getRequiredGroup())
              .append("'");
        }

        // Add admission time
        sb.append("; Admission time: ").append(admissionTime);

        // Add time window if present
        if (policyData.getTimeWindowStart() != null && policyData.getTimeWindowEnd() != null) {
            sb.append("; Time window: ")
              .append(policyData.getTimeWindowStart())
              .append(" to ")
              .append(policyData.getTimeWindowEnd());
        }

        return sb.toString();
    }
}
