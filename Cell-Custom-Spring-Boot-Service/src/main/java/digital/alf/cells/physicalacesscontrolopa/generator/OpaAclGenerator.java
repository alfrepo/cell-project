package digital.alf.cells.physicalacesscontrolopa.generator;

import digital.alf.cells.physicalacesscontrolopa.model.AclEntry;
import digital.alf.cells.physicalacesscontrolopa.model.OpaEmployeeInfo;
import digital.alf.cells.physicalacesscontrolopa.model.OpaPolicyData;
import digital.alf.cells.physicalacesscontrolopa.model.OpaUserInfo;
import digital.alf.cells.physicalacesscontrolopa.service.OpaUserEvaluationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Generates ACL entries from OPA policy data and employee/user information.
 *
 * Equivalent of AclGenerator for the OPA engine.
 * Supports two modes:
 * 1. Static: filters employees from JSON employee file based on policy group requirements.
 * 2. Dynamic: evaluates users via opa eval CLI and uses the allow decision.
 */
@Component
@RequiredArgsConstructor
public class OpaAclGenerator {

    private final OpaUserEvaluationService userEvaluationService;

    /**
     * Generates ACL entries by statically filtering employees against policy group requirements.
     *
     * The OPA policy DENIES access when the user does NOT have the required group,
     * so we generate ACL entries for employees who DO have the required group.
     *
     * @param policyData Parsed OPA policy metadata
     * @param employees  List of employees with their group memberships
     * @return List of ACL entries granting access
     */
    public List<AclEntry> generateAcl(OpaPolicyData policyData, List<OpaEmployeeInfo> employees) {
        List<AclEntry> aclEntries = new ArrayList<>();

        List<OpaEmployeeInfo> qualifiedEmployees = employees.stream()
                .filter(emp -> emp.hasGroup(policyData.getRequiredGroup()))
                .collect(Collectors.toList());

        String resourceDescription = buildResourceDescription(policyData);
        String conditionDescription = buildConditionDescription(policyData);

        for (OpaEmployeeInfo employee : qualifiedEmployees) {
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
     * Generates ACL entries by dynamically evaluating users via the OPA CLI.
     *
     * Algorithm:
     * 1. Read all user JSON files from resources/physical-access-control-opa/pip-users/
     * 2. For each user file run: opa eval -d <policy.rego> --input <userinfo.json> 'data.<pkg>.allow'
     * 3. If allow == true, include the user in the ACL
     *
     * @param policyData  Parsed OPA policy metadata (provides packageName and operations)
     * @param policyPath  Path to the .rego file (relative to resources)
     * @return List of ACL entries for users who passed OPA evaluation
     */
    public List<AclEntry> generateAclWithDynamicEvaluation(
            OpaPolicyData policyData,
            String policyPath) throws IOException {

        List<AclEntry> aclEntries = new ArrayList<>();

        List<OpaUserInfo> qualifiedUsers = userEvaluationService.evaluateUsersForAccess(
                policyPath,
                policyData.getPackageName()
        );

        String resourceDescription = buildResourceDescription(policyData);
        String conditionDescription = buildConditionDescriptionForDynamicEval(policyData);

        for (OpaUserInfo userInfo : qualifiedUsers) {
            // Each user file has one embedded operation; use it directly
            String operation = (userInfo.getRequest() != null && userInfo.getRequest().getOperation() != null)
                    ? userInfo.getRequest().getOperation()
                    : (policyData.getOperations() != null && !policyData.getOperations().isEmpty()
                            ? policyData.getOperations().get(0) : "ENTER");

            AclEntry entry = AclEntry.builder()
                    .principal(formatPrincipalFromUserInfo(userInfo))
                    .action(operation)
                    .resource(resourceDescription)
                    .condition(conditionDescription)
                    .build();

            aclEntries.add(entry);
        }

        return aclEntries;
    }

    private String formatPrincipal(OpaEmployeeInfo employee) {
        return String.format("<%s:%s>", employee.getId(), employee.getName());
    }

    private String formatPrincipalFromUserInfo(OpaUserInfo userInfo) {
        String uid = userInfo.getUserId();
        String username = userInfo.getUsername();
        if (uid != null && username != null) {
            return String.format("<%s:%s>", uid, username);
        }
        return String.format("<%s>", uid != null ? uid : username);
    }

    private String buildResourceDescription(OpaPolicyData policyData) {
        return policyData.getResourceKind() != null ? policyData.getResourceKind() : "Unknown";
    }

    private String buildConditionDescription(OpaPolicyData policyData) {
        StringBuilder sb = new StringBuilder();
        sb.append("Subject must hold group '").append(policyData.getRequiredGroup()).append("'");

        if (policyData.getTimeWindowStart() != null && policyData.getTimeWindowEnd() != null) {
            sb.append("; Time window: ")
              .append(policyData.getTimeWindowStart())
              .append(" to ")
              .append(policyData.getTimeWindowEnd());
        }

        return sb.toString();
    }

    private String buildConditionDescriptionForDynamicEval(OpaPolicyData policyData) {
        StringBuilder sb = new StringBuilder();
        sb.append("Evaluated via OPA CLI");

        if (policyData.getRequiredGroup() != null) {
            sb.append("; Subject must hold group '").append(policyData.getRequiredGroup()).append("'");
        }

        if (policyData.getTimeWindowStart() != null && policyData.getTimeWindowEnd() != null) {
            sb.append("; Time window: ")
              .append(policyData.getTimeWindowStart())
              .append(" to ")
              .append(policyData.getTimeWindowEnd());
        }

        return sb.toString();
    }
}
