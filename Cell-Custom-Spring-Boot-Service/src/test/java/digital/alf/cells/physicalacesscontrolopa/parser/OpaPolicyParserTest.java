package digital.alf.cells.physicalacesscontrolopa.parser;

import digital.alf.cells.physicalacesscontrolopa.model.OpaPolicyData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OpaPolicyParserTest {

    private OpaPolicyParser parser;

    @BeforeEach
    void setUp() {
        parser = new OpaPolicyParser();
    }

    private InputStream rego(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }

    private static final String SAMPLE_REGO = """
            # Policy: abac-enroll-restriction-time-bound
            # Restricts the ENTER operation on Facility resources to users possessing
            # 'training-vde-available-group'.
            package physical_access_control

            import rego.v1

            default allow := false

            deny if {
                input.request.resource.kind == "Facility"
                not "training-vde-available-group" in input.request.userInfo.groups
            }

            time_within_window if {
                window_start := time.parse_rfc3339_ns("2024-10-20T08:00:00Z")
                window_end   := time.parse_rfc3339_ns("2026-10-20T19:00:00Z")
            }

            violation contains msg if {
                deny
                msg := "Access is denied during the critical window."
            }
            """;

    @Test
    void parse_extractsPackageName() throws IOException {
        OpaPolicyData data = parser.parse(rego(SAMPLE_REGO));
        assertEquals("physical_access_control", data.getPackageName());
    }

    @Test
    void parse_extractsPolicyName() throws IOException {
        OpaPolicyData data = parser.parse(rego(SAMPLE_REGO));
        assertEquals("abac-enroll-restriction-time-bound", data.getPolicyName());
    }

    @Test
    void parse_extractsOperationFromComment() throws IOException {
        OpaPolicyData data = parser.parse(rego(SAMPLE_REGO));
        assertNotNull(data.getOperations());
        assertTrue(data.getOperations().contains("ENTER"));
    }

    @Test
    void parse_extractsResourceKind() throws IOException {
        OpaPolicyData data = parser.parse(rego(SAMPLE_REGO));
        assertEquals("Facility", data.getResourceKind());
    }

    @Test
    void parse_extractsRequiredGroup() throws IOException {
        OpaPolicyData data = parser.parse(rego(SAMPLE_REGO));
        assertEquals("training-vde-available-group", data.getRequiredGroup());
    }

    @Test
    void parse_extractsTimeWindowStart() throws IOException {
        OpaPolicyData data = parser.parse(rego(SAMPLE_REGO));
        assertEquals(Instant.parse("2024-10-20T08:00:00Z"), data.getTimeWindowStart());
    }

    @Test
    void parse_extractsTimeWindowEnd() throws IOException {
        OpaPolicyData data = parser.parse(rego(SAMPLE_REGO));
        assertEquals(Instant.parse("2026-10-20T19:00:00Z"), data.getTimeWindowEnd());
    }

    @Test
    void parse_extractsViolationMessage() throws IOException {
        OpaPolicyData data = parser.parse(rego(SAMPLE_REGO));
        assertEquals("Access is denied during the critical window.", data.getValidationMessage());
    }

    @Test
    void parse_noOperationsInComment_defaultsToEnter() throws IOException {
        String rego = """
                package some_policy
                deny if {
                    input.request.resource.kind == "Facility"
                }
                """;

        OpaPolicyData data = parser.parse(rego(rego));

        assertEquals(List.of("ENTER"), data.getOperations());
    }

    @Test
    void parse_multipleOperationsInComment_extractsAll() throws IOException {
        // Each operation must appear on its own comment line because the regex anchors on '#'
        String rego = """
                # Policy: multi-op
                # Restricts the ENTER operation
                # Also restricts the EXIT operation
                package some_policy
                """;

        OpaPolicyData data = parser.parse(rego(rego));

        assertTrue(data.getOperations().contains("ENTER"));
        assertTrue(data.getOperations().contains("EXIT"));
    }

    @Test
    void parse_duplicateOperationInComment_deduplicates() throws IOException {
        String rego = """
                # Restricts the ENTER operation
                # Also restricts ENTER for another reason
                package some_policy
                """;

        OpaPolicyData data = parser.parse(rego(rego));

        assertEquals(1, data.getOperations().stream().filter("ENTER"::equals).count());
    }

    @Test
    void parse_noTimeWindow_timeWindowIsNull() throws IOException {
        String rego = """
                package some_policy
                deny if {
                    input.request.resource.kind == "Room"
                }
                """;

        OpaPolicyData data = parser.parse(rego(rego));

        assertNull(data.getTimeWindowStart());
        assertNull(data.getTimeWindowEnd());
    }

    @Test
    void parse_realPolicyFile_parsesSuccessfully() throws IOException {
        // Parses the actual policy.rego from the classpath
        try (InputStream is = getClass().getClassLoader()
                .getResourceAsStream("physical-access-control-opa/policy.rego")) {
            assertNotNull(is, "policy.rego must be on the test classpath");
            OpaPolicyData data = parser.parse(is);
            assertEquals("physical_access_control", data.getPackageName());
            assertEquals("Facility", data.getResourceKind());
            assertEquals("training-vde-available-group", data.getRequiredGroup());
        }
    }
}
