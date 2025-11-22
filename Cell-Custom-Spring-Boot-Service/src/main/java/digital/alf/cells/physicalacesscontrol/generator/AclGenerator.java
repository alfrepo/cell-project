package digital.alf.cells.physicalacesscontrol.generator;

import digital.alf.cells.physicalacesscontrol.model.AclEntry;
import digital.alf.cells.physicalacesscontrol.model.EmployeeInfo;
import digital.alf.cells.physicalacesscontrol.model.KyvernoPolicyData;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class AclGenerator {

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
}
