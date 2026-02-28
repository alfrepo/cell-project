package digital.alf.cells.physicalacesscontrolopa.generator;

import digital.alf.cells.physicalacesscontrolopa.model.AclEntry;
import digital.alf.cells.physicalacesscontrolopa.model.OpaEmployeeInfo;
import digital.alf.cells.physicalacesscontrolopa.model.OpaPolicyData;
import digital.alf.cells.physicalacesscontrolopa.model.OpaUserInfo;
import digital.alf.cells.physicalacesscontrolopa.service.OpaUserEvaluationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OpaAclGeneratorTest {

    @Mock
    private OpaUserEvaluationService userEvaluationService;

    private OpaAclGenerator generator;

    private OpaPolicyData policyData;

    @BeforeEach
    void setUp() {
        generator = new OpaAclGenerator(userEvaluationService);
        policyData = OpaPolicyData.builder()
                .policyName("test-policy")
                .packageName("physical_access_control")
                .operations(List.of("ENTER"))
                .resourceKind("Facility")
                .requiredGroup("training-vde-available-group")
                .timeWindowStart(Instant.parse("2024-10-20T08:00:00Z"))
                .timeWindowEnd(Instant.parse("2026-10-20T19:00:00Z"))
                .build();
    }

    // --- generateAcl (static) ---

    @Test
    void generateAcl_qualifiedEmployee_createsEntry() {
        List<OpaEmployeeInfo> employees = List.of(
                new OpaEmployeeInfo("ES-4902", "Anya Sharma", Map.of("training-vde-available-group", true))
        );

        List<AclEntry> entries = generator.generateAcl(policyData, employees);

        assertEquals(1, entries.size());
        assertEquals("<ES-4902:Anya Sharma>", entries.get(0).getPrincipal());
        assertEquals("ENTER", entries.get(0).getAction());
        assertEquals("Facility", entries.get(0).getResource());
    }

    @Test
    void generateAcl_unqualifiedEmployee_excluded() {
        List<OpaEmployeeInfo> employees = List.of(
                new OpaEmployeeInfo("BC-3115", "Ben Carter", Map.of("training-vde-available-group", false))
        );

        List<AclEntry> entries = generator.generateAcl(policyData, employees);

        assertTrue(entries.isEmpty());
    }

    @Test
    void generateAcl_mixedEmployees_onlyQualifiedIncluded() {
        List<OpaEmployeeInfo> employees = List.of(
                new OpaEmployeeInfo("ES-4902", "Anya Sharma", Map.of("training-vde-available-group", true)),
                new OpaEmployeeInfo("BC-3115", "Ben Carter",  Map.of("training-vde-available-group", false)),
                new OpaEmployeeInfo("DL-1020", "David Lee",   Map.of("training-vde-available-group", true))
        );

        List<AclEntry> entries = generator.generateAcl(policyData, employees);

        assertEquals(2, entries.size());
        List<String> principals = entries.stream().map(AclEntry::getPrincipal).toList();
        assertTrue(principals.contains("<ES-4902:Anya Sharma>"));
        assertTrue(principals.contains("<DL-1020:David Lee>"));
    }

    @Test
    void generateAcl_multipleOperations_oneEntryPerOperation() {
        OpaPolicyData multiOpPolicy = OpaPolicyData.builder()
                .operations(List.of("ENTER", "EXIT"))
                .resourceKind("Facility")
                .requiredGroup("training-vde-available-group")
                .build();

        List<OpaEmployeeInfo> employees = List.of(
                new OpaEmployeeInfo("ES-4902", "Anya Sharma", Map.of("training-vde-available-group", true))
        );

        List<AclEntry> entries = generator.generateAcl(multiOpPolicy, employees);

        assertEquals(2, entries.size());
        List<String> actions = entries.stream().map(AclEntry::getAction).toList();
        assertTrue(actions.contains("ENTER"));
        assertTrue(actions.contains("EXIT"));
    }

    @Test
    void generateAcl_emptyEmployeeList_returnsEmpty() {
        List<AclEntry> entries = generator.generateAcl(policyData, List.of());
        assertTrue(entries.isEmpty());
    }

    @Test
    void generateAcl_conditionContainsRequiredGroup() {
        List<OpaEmployeeInfo> employees = List.of(
                new OpaEmployeeInfo("ES-4902", "Anya Sharma", Map.of("training-vde-available-group", true))
        );

        List<AclEntry> entries = generator.generateAcl(policyData, employees);

        assertTrue(entries.get(0).getCondition().contains("training-vde-available-group"));
    }

    @Test
    void generateAcl_conditionContainsTimeWindow() {
        List<OpaEmployeeInfo> employees = List.of(
                new OpaEmployeeInfo("ES-4902", "Anya Sharma", Map.of("training-vde-available-group", true))
        );

        List<AclEntry> entries = generator.generateAcl(policyData, employees);

        String condition = entries.get(0).getCondition();
        assertTrue(condition.contains("2024-10-20T08:00:00Z"));
        assertTrue(condition.contains("2026-10-20T19:00:00Z"));
    }

    // --- generateAclWithDynamicEvaluation ---

    @Test
    void generateAclWithDynamicEvaluation_qualifiedUser_createsEntry() throws IOException {
        OpaUserInfo user = buildUserInfo("ES-4902", "Anya Sharma", "ENTER");
        when(userEvaluationService.evaluateUsersForAccess(anyString(), anyString()))
                .thenReturn(List.of(user));

        List<AclEntry> entries = generator.generateAclWithDynamicEvaluation(policyData, "policy.rego");

        assertEquals(1, entries.size());
        assertEquals("<ES-4902:Anya Sharma>", entries.get(0).getPrincipal());
        assertEquals("ENTER", entries.get(0).getAction());
        assertTrue(entries.get(0).getCondition().contains("Evaluated via OPA CLI"));
    }

    @Test
    void generateAclWithDynamicEvaluation_noQualifiedUsers_returnsEmpty() throws IOException {
        when(userEvaluationService.evaluateUsersForAccess(anyString(), anyString()))
                .thenReturn(List.of());

        List<AclEntry> entries = generator.generateAclWithDynamicEvaluation(policyData, "policy.rego");

        assertTrue(entries.isEmpty());
    }

    @Test
    void generateAclWithDynamicEvaluation_nullOperationInRequest_fallsBackToPolicyOperation() throws IOException {
        OpaUserInfo.UserInfo userInfo = new OpaUserInfo.UserInfo("Anya Sharma", "ES-4902", List.of());
        OpaUserInfo.Request request = new OpaUserInfo.Request(null, null, null, userInfo);
        OpaUserInfo user = new OpaUserInfo(request);

        when(userEvaluationService.evaluateUsersForAccess(anyString(), anyString()))
                .thenReturn(List.of(user));

        List<AclEntry> entries = generator.generateAclWithDynamicEvaluation(policyData, "policy.rego");

        assertEquals(1, entries.size());
        assertEquals("ENTER", entries.get(0).getAction());
    }

    @Test
    void generateAclWithDynamicEvaluation_userWithOnlyUid_principalContainsUid() throws IOException {
        OpaUserInfo.UserInfo userInfo = new OpaUserInfo.UserInfo(null, "ES-4902", List.of());
        OpaUserInfo.Request request = new OpaUserInfo.Request(null, "ENTER", null, userInfo);
        OpaUserInfo user = new OpaUserInfo(request);

        when(userEvaluationService.evaluateUsersForAccess(anyString(), anyString()))
                .thenReturn(List.of(user));

        List<AclEntry> entries = generator.generateAclWithDynamicEvaluation(policyData, "policy.rego");

        assertEquals(1, entries.size());
        assertTrue(entries.get(0).getPrincipal().contains("ES-4902"));
    }

    @Test
    void generateAclWithDynamicEvaluation_delegatesToEvaluationService() throws IOException {
        when(userEvaluationService.evaluateUsersForAccess("policy.rego", "physical_access_control"))
                .thenReturn(List.of());

        generator.generateAclWithDynamicEvaluation(policyData, "policy.rego");

        verify(userEvaluationService).evaluateUsersForAccess("policy.rego", "physical_access_control");
    }

    // --- helper ---

    private OpaUserInfo buildUserInfo(String uid, String username, String operation) {
        OpaUserInfo.UserInfo userInfo = new OpaUserInfo.UserInfo(username, uid, List.of());
        OpaUserInfo.Request request = new OpaUserInfo.Request(null, operation, null, userInfo);
        return new OpaUserInfo(request);
    }
}
