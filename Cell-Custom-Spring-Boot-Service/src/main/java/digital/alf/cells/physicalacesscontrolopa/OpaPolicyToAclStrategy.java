package digital.alf.cells.physicalacesscontrolopa;

import digital.alf.cells.physicalacesscontrolopa.generator.OpaAclGenerator;
import digital.alf.cells.physicalacesscontrolopa.model.AclEntry;
import digital.alf.cells.physicalacesscontrolopa.model.OpaEmployeeInfo;
import digital.alf.cells.physicalacesscontrolopa.model.OpaPolicyData;
import digital.alf.cells.physicalacesscontrolopa.parser.OpaEmployeeInfoParser;
import digital.alf.cells.physicalacesscontrolopa.parser.OpaPolicyParser;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Main strategy class for converting OPA rego policies to ACL format.
 *
 * Algorithm Overview:
 * ==================
 *
 * 1. PARSE INPUTS
 *    - Load and parse OPA rego policy (policy.rego) using text/regex extraction
 *      * Extract: operations (ENTER, EXIT, ...)
 *      * Extract: resource kind (Facility)
 *      * Extract: required group (training-vde-available-group)
 *      * Extract: temporal conditions (time window)
 *      * Extract: OPA package name (physical_access_control)
 *
 *    - Load and parse employee VDE training JSON (pip-info-employee-vde-trainings.json)
 *      * Extract: employee ID, name, group memberships
 *
 * 2. EVALUATE POLICY LOGIC
 *    The OPA policy uses DENY semantics (same as the Kyverno version):
 *      deny IF: resource.kind == "Facility" AND time_within_window AND user NOT IN training-vde-available-group
 *      allow IF: not deny
 *
 *    Static ACL grant logic:
 *      ALLOW IF: user IN training-vde-available-group
 *
 * 3. GENERATE ACL ENTRIES
 *    For each employee meeting the requirements:
 *      - Create ACL entry for each operation
 *      - Format principal as <UserId:name>
 *      - Include resource description
 *      - Include all applicable conditions
 *
 * 4. OUTPUT
 *    Return list of ACL entries in target format:
 *      - Principal <UserId:name>
 *      - Action
 *      - Resource
 *      - Condition
 *
 * Equivalent of PolicyToAclStrategy for the OPA engine.
 * Reads from: src/main/resources/physical-access-control-opa/
 */
@Service
@RequiredArgsConstructor
public class OpaPolicyToAclStrategy {

    private static final String DEFAULT_POLICY_PATH = "physical-access-control-opa/policy.rego";
    private static final String DEFAULT_EMPLOYEE_INFO_PATH = "physical-access-control-opa/pip-info-employee-vde-trainings.json";

    private final OpaPolicyParser policyParser;
    private final OpaEmployeeInfoParser employeeParser;
    private final OpaAclGenerator aclGenerator;

    /**
     * Main entry point: static policy-to-ACL conversion using default files.
     */
    public List<AclEntry> convertPolicyToAcl() throws IOException {
        return convertPolicyToAcl(DEFAULT_POLICY_PATH, DEFAULT_EMPLOYEE_INFO_PATH);
    }

    /**
     * Static policy-to-ACL conversion with custom file paths.
     */
    public List<AclEntry> convertPolicyToAcl(String policyPath, String employeeInfoPath) throws IOException {
        OpaPolicyData policyData = parseOpaPolicy(policyPath);
        List<OpaEmployeeInfo> employees = parseEmployeeInfo(employeeInfoPath);
        return aclGenerator.generateAcl(policyData, employees);
    }

    /**
     * Dynamic evaluation using the OPA CLI with default files.
     *
     * For each user file in physical-access-control-opa/pip-users/ runs:
     *   opa eval -d policy.rego --input <userinfo.json> 'data.<packageName>.allow'
     * and includes users where allow == true in the resulting ACL.
     */
    public List<AclEntry> convertPolicyToAclWithDynamicEvaluation() throws IOException {
        return convertPolicyToAclWithDynamicEvaluation(DEFAULT_POLICY_PATH);
    }

    /**
     * Dynamic evaluation using the OPA CLI with a custom policy path.
     */
    public List<AclEntry> convertPolicyToAclWithDynamicEvaluation(String policyPath) throws IOException {
        OpaPolicyData policyData = parseOpaPolicy(policyPath);
        return aclGenerator.generateAclWithDynamicEvaluation(policyData, policyPath);
    }

    /**
     * Formats ACL entries as a human-readable string.
     */
    public String formatAclOutput(List<AclEntry> aclEntries) {
        StringBuilder output = new StringBuilder();
        output.append("=".repeat(80)).append("\n");
        output.append("ACCESS CONTROL LIST (ACL)\n");
        output.append("Generated from OPA Rego Policy\n");
        output.append("=".repeat(80)).append("\n\n");

        if (aclEntries.isEmpty()) {
            output.append("No employees meet the policy requirements for access.\n");
        } else {
            output.append("Total entries: ").append(aclEntries.size()).append("\n\n");

            for (int i = 0; i < aclEntries.size(); i++) {
                output.append("Entry #").append(i + 1).append("\n");
                output.append(aclEntries.get(i).toString());
                output.append("\n");
            }
        }

        output.append("=".repeat(80)).append("\n");
        return output.toString();
    }

    private OpaPolicyData parseOpaPolicy(String resourcePath) throws IOException {
        try (InputStream inputStream = new ClassPathResource(resourcePath).getInputStream()) {
            return policyParser.parse(inputStream);
        }
    }

    private List<OpaEmployeeInfo> parseEmployeeInfo(String resourcePath) throws IOException {
        try (InputStream inputStream = new ClassPathResource(resourcePath).getInputStream()) {
            return employeeParser.parse(inputStream);
        }
    }
}
