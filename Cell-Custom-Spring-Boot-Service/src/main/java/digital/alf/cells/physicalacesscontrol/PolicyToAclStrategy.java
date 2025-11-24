package digital.alf.cells.physicalacesscontrol;

import digital.alf.cells.physicalacesscontrol.generator.AclGenerator;
import digital.alf.cells.physicalacesscontrol.model.AclEntry;
import digital.alf.cells.physicalacesscontrol.model.EmployeeInfo;
import digital.alf.cells.physicalacesscontrol.model.KyvernoPolicyData;
import digital.alf.cells.physicalacesscontrol.parser.EmployeeInfoParser;
import digital.alf.cells.physicalacesscontrol.parser.KyvernoPolicyParser;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Main strategy class for converting Kyverno ABAC policies to ACL format.
 *
 * Algorithm Overview:
 * ==================
 *
 * 1. PARSE INPUTS
 *    - Load and parse Kyverno policy YAML (pip-abac-policy1.yml)
 *      * Extract: operations (ENTER, EXIT)
 *      * Extract: resource types and labels (Facility, location=production-room)
 *      * Extract: subject requirements (employee-group)
 *      * Extract: attribute requirements (training-vde-available-group)
 *      * Extract: temporal conditions (time window)
 *
 *    - Load and parse employee VDE training data (pip-info-employee-vde-trainings.yml)
 *      * Extract: employee ID, name, group memberships
 *
 * 2. EVALUATE POLICY LOGIC
 *    The Kyverno policy uses DENY semantics:
 *      DENY IF: (user IN employee-group) AND (user NOT IN training-vde-available-group)
 *
 *    Therefore, ALLOW (ACL grant) logic is:
 *      ALLOW IF: (user IN employee-group) AND (user IN training-vde-available-group)
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
 */
@Service
@RequiredArgsConstructor
public class PolicyToAclStrategy {

    private final KyvernoPolicyParser policyParser;
    private final EmployeeInfoParser employeeParser;
    private final AclGenerator aclGenerator;

    /**
     * Main entry point for the policy-to-ACL conversion algorithm.
     *
     * @return List of ACL entries for all employees with granted access
     * @throws IOException if policy or employee files cannot be read
     */
    public List<AclEntry> convertPolicyToAcl() throws IOException {
        // Step 1: Parse Kyverno policy
        KyvernoPolicyData policyData = parseKyvernoPolicy(
                "physical-access-control/pip-abac-policy1.yml"
        );

        // Step 2: Parse employee VDE training information
        List<EmployeeInfo> employees = parseEmployeeInfo(
                "physical-access-control/pip-info-employee-vde-trainings.yml"
        );

        // Step 3 & 4: Generate ACL entries based on policy rules and employee data
        return aclGenerator.generateAcl(policyData, employees);
    }

    /**
     * Convenience method with custom file paths.
     */
    public List<AclEntry> convertPolicyToAcl(String policyPath, String employeeInfoPath) throws IOException {
        KyvernoPolicyData policyData = parseKyvernoPolicy(policyPath);
        List<EmployeeInfo> employees = parseEmployeeInfo(employeeInfoPath);
        return aclGenerator.generateAcl(policyData, employees);
    }

    /**
     * NEW: Dynamic evaluation using kyverno-cli.
     *
     * Generates ACL entries by evaluating users from pip-users/ directory against the policy
     * using kyverno-cli. Only users who pass the evaluation (results.result != "fail") are included.
     *
     * Algorithm:
     * 1. Parse Kyverno policy to extract operations and metadata
     * 2. For each user in resources/physical-access-control/pip-users/:
     *    - Execute: kyverno apply <policy> --resource <resource> --userinfo <user> --set request.operation=<op>,request.time.admissionTime=<time>
     *    - Parse JSON output (ClusterReport)
     *    - If no "fail" results, add user to ACL
     * 3. Generate ACL entries for qualified users
     *
     * @param policyPath Path to policy YAML (e.g., "physical-access-control/pip-abac-policy1.yml")
     * @param resourcePath Path to resource YAML (e.g., "physical-access-control/pip-resources/pip-resource-room.yml")
     * @param admissionTime Time for evaluation (e.g., "2025-10-20T08:30:00Z")
     * @return List of ACL entries for users who passed kyverno evaluation
     * @throws IOException if execution fails
     */
    public List<AclEntry> convertPolicyToAclWithDynamicEvaluation(
            String policyPath,
            String resourcePath,
            String admissionTime) throws IOException {

        // Parse policy to extract operations and metadata
        KyvernoPolicyData policyData = parseKyvernoPolicy(policyPath);

        // Use dynamic evaluation with kyverno-cli
        return aclGenerator.generateAclWithDynamicEvaluation(
                policyData,
                policyPath,
                resourcePath,
                admissionTime
        );
    }

    /**
     * Default dynamic evaluation with standard paths.
     */
    public List<AclEntry> convertPolicyToAclWithDynamicEvaluation() throws IOException {
        return convertPolicyToAclWithDynamicEvaluation(
                "physical-access-control/pip-abac-policy1.yml",
                "physical-access-control/pip-resources/pip-resource-room.yml",
                "2025-10-20T08:30:00Z"
        );
    }

    /**
     * Formats the ACL entries as a string output.
     */
    public String formatAclOutput(List<AclEntry> aclEntries) {
        StringBuilder output = new StringBuilder();
        output.append("=".repeat(80)).append("\n");
        output.append("ACCESS CONTROL LIST (ACL)\n");
        output.append("Generated from Kyverno ABAC Policy\n");
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

    private KyvernoPolicyData parseKyvernoPolicy(String resourcePath) throws IOException {
        try (InputStream inputStream = new ClassPathResource(resourcePath).getInputStream()) {
            return policyParser.parse(inputStream);
        }
    }

    private List<EmployeeInfo> parseEmployeeInfo(String resourcePath) throws IOException {
        try (InputStream inputStream = new ClassPathResource(resourcePath).getInputStream()) {
            return employeeParser.parse(inputStream);
        }
    }
}
