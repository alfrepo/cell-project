package digital.alf.cells.physicalacesscontrol.controller;

import digital.alf.cells.physicalacesscontrol.PolicyToAclStrategy;
import digital.alf.cells.physicalacesscontrol.model.AclEntry;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/acl")
@RequiredArgsConstructor
public class AclController {

    private final PolicyToAclStrategy policyToAclStrategy;

    /**
     * Endpoint to generate ACL from Kyverno policy and employee data.
     *
     * @return ACL entries in JSON format
     */
    @GetMapping("/generate")
    public ResponseEntity<List<AclEntry>> generateAcl() {
        try {
            List<AclEntry> aclEntries = policyToAclStrategy.convertPolicyToAcl();
            return ResponseEntity.ok(aclEntries);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Endpoint to get formatted ACL output as plain text.
     *
     * @return Formatted ACL string
     */
    @GetMapping("/generate/formatted")
    public ResponseEntity<String> generateFormattedAcl() {
        try {
            List<AclEntry> aclEntries = policyToAclStrategy.convertPolicyToAcl();
            String formatted = policyToAclStrategy.formatAclOutput(aclEntries);
            return ResponseEntity.ok(formatted);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
