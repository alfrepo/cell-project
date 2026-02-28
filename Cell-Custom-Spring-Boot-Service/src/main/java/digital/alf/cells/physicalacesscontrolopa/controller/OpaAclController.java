package digital.alf.cells.physicalacesscontrolopa.controller;

import digital.alf.cells.physicalacesscontrolopa.OpaPolicyToAclStrategy;
import digital.alf.cells.physicalacesscontrolopa.model.AclEntry;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

/**
 * REST controller for OPA-based ACL generation.
 *
 * Equivalent of AclController for the OPA engine.
 * Exposes endpoints under /api/acl/opa instead of /api/acl.
 */
@RestController
@RequestMapping("/api/acl/opa")
@RequiredArgsConstructor
public class OpaAclController {

    private final OpaPolicyToAclStrategy opaPolicyToAclStrategy;

    /**
     * Generates ACL from OPA rego policy and employee JSON data (static evaluation).
     *
     * @return ACL entries in JSON format
     */
    @GetMapping("/generate")
    public ResponseEntity<List<AclEntry>> generateAcl() {
        try {
            List<AclEntry> aclEntries = opaPolicyToAclStrategy.convertPolicyToAcl();
            return ResponseEntity.ok(aclEntries);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Returns formatted ACL output as plain text (static evaluation).
     *
     * @return Formatted ACL string
     */
    @GetMapping("/generate/formatted")
    public ResponseEntity<String> generateFormattedAcl() {
        try {
            List<AclEntry> aclEntries = opaPolicyToAclStrategy.convertPolicyToAcl();
            String formatted = opaPolicyToAclStrategy.formatAclOutput(aclEntries);
            return ResponseEntity.ok(formatted);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Generates ACL using dynamic OPA CLI evaluation.
     * Evaluates each user file under pip-users/ via 'opa eval' and includes
     * only those for whom allow == true.
     *
     * @return ACL entries in JSON format
     */
    @GetMapping("/generate/dynamic")
    public ResponseEntity<List<AclEntry>> generateAclDynamic() {
        try {
            List<AclEntry> aclEntries = opaPolicyToAclStrategy.convertPolicyToAclWithDynamicEvaluation();
            return ResponseEntity.ok(aclEntries);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Returns formatted ACL output using dynamic OPA CLI evaluation.
     *
     * @return Formatted ACL string
     */
    @GetMapping("/generate/dynamic/formatted")
    public ResponseEntity<String> generateFormattedAclDynamic() {
        try {
            List<AclEntry> aclEntries = opaPolicyToAclStrategy.convertPolicyToAclWithDynamicEvaluation();
            String formatted = opaPolicyToAclStrategy.formatAclOutput(aclEntries);
            return ResponseEntity.ok(formatted);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
