package digital.alf.cells.physicalacesscontrolopa;

import digital.alf.cells.physicalacesscontrolopa.generator.OpaAclGenerator;
import digital.alf.cells.physicalacesscontrolopa.model.AclEntry;
import digital.alf.cells.physicalacesscontrolopa.model.OpaEmployeeInfo;
import digital.alf.cells.physicalacesscontrolopa.model.OpaPolicyData;
import digital.alf.cells.physicalacesscontrolopa.parser.OpaEmployeeInfoParser;
import digital.alf.cells.physicalacesscontrolopa.parser.OpaPolicyParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OpaPolicyToAclStrategy.
 *
 * Parsers and generator are mocked. ClassPathResource opens the actual files
 * from src/main/resources (which are on the test classpath), and the mocked
 * parsers return pre-built test data.
 */
@ExtendWith(MockitoExtension.class)
class OpaPolicyToAclStrategyTest {

    @Mock
    private OpaPolicyParser policyParser;

    @Mock
    private OpaEmployeeInfoParser employeeParser;

    @Mock
    private OpaAclGenerator aclGenerator;

    private OpaPolicyToAclStrategy strategy;

    private OpaPolicyData samplePolicy;
    private List<OpaEmployeeInfo> sampleEmployees;
    private List<AclEntry> sampleEntries;

    @BeforeEach
    void setUp() {
        strategy = new OpaPolicyToAclStrategy(policyParser, employeeParser, aclGenerator);

        samplePolicy = OpaPolicyData.builder()
                .policyName("test-policy")
                .packageName("physical_access_control")
                .operations(List.of("ENTER"))
                .resourceKind("Facility")
                .requiredGroup("training-vde-available-group")
                .timeWindowStart(Instant.parse("2024-10-20T08:00:00Z"))
                .timeWindowEnd(Instant.parse("2026-10-20T19:00:00Z"))
                .build();

        sampleEmployees = List.of(
                new OpaEmployeeInfo("ES-4902", "Anya Sharma", Map.of("training-vde-available-group", true)),
                new OpaEmployeeInfo("DL-1020", "David Lee",   Map.of("training-vde-available-group", true))
        );

        sampleEntries = List.of(
                AclEntry.builder().principal("<ES-4902:Anya Sharma>").action("ENTER").resource("Facility").condition("test").build(),
                AclEntry.builder().principal("<DL-1020:David Lee>").action("ENTER").resource("Facility").condition("test").build()
        );
    }

    @Test
    void convertPolicyToAcl_delegatesToParsersAndGenerator() throws IOException {
        when(policyParser.parse(any(InputStream.class))).thenReturn(samplePolicy);
        when(employeeParser.parse(any(InputStream.class))).thenReturn(sampleEmployees);
        when(aclGenerator.generateAcl(samplePolicy, sampleEmployees)).thenReturn(sampleEntries);

        List<AclEntry> result = strategy.convertPolicyToAcl();

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(policyParser).parse(any(InputStream.class));
        verify(employeeParser).parse(any(InputStream.class));
        verify(aclGenerator).generateAcl(samplePolicy, sampleEmployees);
    }

    @Test
    void convertPolicyToAcl_returnsGeneratorResult() throws IOException {
        when(policyParser.parse(any(InputStream.class))).thenReturn(samplePolicy);
        when(employeeParser.parse(any(InputStream.class))).thenReturn(sampleEmployees);
        when(aclGenerator.generateAcl(any(), any())).thenReturn(sampleEntries);

        List<AclEntry> result = strategy.convertPolicyToAcl();

        assertEquals(sampleEntries, result);
    }

    @Test
    void convertPolicyToAcl_emptyEntries_returnsEmptyList() throws IOException {
        when(policyParser.parse(any(InputStream.class))).thenReturn(samplePolicy);
        when(employeeParser.parse(any(InputStream.class))).thenReturn(List.of());
        when(aclGenerator.generateAcl(any(), any())).thenReturn(List.of());

        List<AclEntry> result = strategy.convertPolicyToAcl();

        assertTrue(result.isEmpty());
    }

    @Test
    void convertPolicyToAcl_parserThrows_propagatesIOException() throws IOException {
        when(policyParser.parse(any(InputStream.class))).thenThrow(new IOException("parse failed"));

        assertThrows(IOException.class, () -> strategy.convertPolicyToAcl());
    }

    // --- formatAclOutput ---

    @Test
    void formatAclOutput_containsHeader() {
        String output = strategy.formatAclOutput(sampleEntries);
        assertTrue(output.contains("ACCESS CONTROL LIST (ACL)"));
        assertTrue(output.contains("Generated from OPA Rego Policy"));
    }

    @Test
    void formatAclOutput_containsEntryCount() {
        String output = strategy.formatAclOutput(sampleEntries);
        assertTrue(output.contains("Total entries: 2"));
    }

    @Test
    void formatAclOutput_containsPrincipals() {
        String output = strategy.formatAclOutput(sampleEntries);
        assertTrue(output.contains("ES-4902:Anya Sharma"));
        assertTrue(output.contains("DL-1020:David Lee"));
    }

    @Test
    void formatAclOutput_numbersEntries() {
        String output = strategy.formatAclOutput(sampleEntries);
        assertTrue(output.contains("Entry #1"));
        assertTrue(output.contains("Entry #2"));
    }

    @Test
    void formatAclOutput_emptyList_showsNoEmployeesMessage() {
        String output = strategy.formatAclOutput(List.of());
        assertTrue(output.contains("No employees meet the policy requirements"));
    }
}
