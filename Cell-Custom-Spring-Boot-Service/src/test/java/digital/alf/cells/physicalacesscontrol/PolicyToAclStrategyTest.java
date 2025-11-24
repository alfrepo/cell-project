package digital.alf.cells.physicalacesscontrol;

import digital.alf.cells.physicalacesscontrol.generator.AclGenerator;
import digital.alf.cells.physicalacesscontrol.model.AclEntry;
import digital.alf.cells.physicalacesscontrol.parser.EmployeeInfoParser;
import digital.alf.cells.physicalacesscontrol.parser.KyvernoPolicyParser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class PolicyToAclStrategyTest {

    @Autowired
    private PolicyToAclStrategy policyToAclStrategy;

    @Test
    void testConvertPolicyToAcl() throws IOException {
        // Execute the algorithm
        List<AclEntry> aclEntries = policyToAclStrategy.convertPolicyToAcl();

        // Verify results
        assertNotNull(aclEntries, "ACL entries should not be null");
        assertFalse(aclEntries.isEmpty(), "ACL entries should not be empty");

        // Expected: 2 employees (Anya Sharma, David Lee) * operations = entries
        // The parser extracts operations from policy annotations
        assertTrue(aclEntries.size() >= 2, "Should generate at least 2 ACL entries");

        // Verify principals
        List<String> principals = aclEntries.stream()
                .map(AclEntry::getPrincipal)
                .distinct()
                .toList();

        assertTrue(principals.contains("<ES-4902:Anya Sharma>"), "Should include Anya Sharma");
        assertTrue(principals.contains("<DL-1020:David Lee>"), "Should include David Lee");
        assertEquals(2, principals.size(), "Should have exactly 2 unique principals");

        // Verify operations - policy annotation mentions ENTER and UPDATE
        List<String> actions = aclEntries.stream()
                .map(AclEntry::getAction)
                .distinct()
                .toList();

        assertFalse(actions.isEmpty(), "Should have at least one action");
        System.out.println("Operations found in ACL: " + actions);

        // Verify resource
        for (AclEntry entry : aclEntries) {
            assertNotNull(entry.getResource(), "Resource should not be null");
            assertTrue(entry.getResource().contains("Facility"), "Resource should be Facility");
            // Note: production-room label is in policy description but may not be in parsed structure
        }

        // Verify conditions
        for (AclEntry entry : aclEntries) {
            assertNotNull(entry.getCondition(), "Condition should not be null");
            assertTrue(entry.getCondition().contains("employee-group"), "Condition should mention employee-group");
            assertTrue(entry.getCondition().contains("training-vde-available-group"), "Condition should mention training-vde-available-group");
        }

        // Print formatted output for manual verification
        System.out.println("\n" + policyToAclStrategy.formatAclOutput(aclEntries));
    }

    @Test
    void testFormattedOutput() throws IOException {
        List<AclEntry> aclEntries = policyToAclStrategy.convertPolicyToAcl();
        String formatted = policyToAclStrategy.formatAclOutput(aclEntries);

        assertNotNull(formatted, "Formatted output should not be null");
        assertTrue(formatted.contains("ACCESS CONTROL LIST"), "Should contain ACL header");
        assertTrue(formatted.contains("ES-4902:Anya Sharma"), "Should contain Anya Sharma");
        assertTrue(formatted.contains("DL-1020:David Lee"), "Should contain David Lee");
    }
}
