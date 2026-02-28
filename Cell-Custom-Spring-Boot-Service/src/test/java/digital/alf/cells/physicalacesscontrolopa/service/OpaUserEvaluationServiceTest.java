package digital.alf.cells.physicalacesscontrolopa.service;

import digital.alf.cells.physicalacesscontrolopa.model.OpaEvalResult;
import digital.alf.cells.physicalacesscontrolopa.model.OpaUserInfo;
import digital.alf.cells.physicalacesscontrolopa.parser.OpaUserInfoParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OpaUserEvaluationService.
 *
 * PathMatchingResourcePatternResolver is created internally, so it resolves
 * real classpath resources (the 5 user files from src/main/resources).
 * OpaUserInfoParser and OpaCliExecutor are mocked.
 */
@ExtendWith(MockitoExtension.class)
class OpaUserEvaluationServiceTest {

    @Mock
    private OpaUserInfoParser userInfoParser;

    @Mock
    private OpaCliExecutor cliExecutor;

    private OpaUserEvaluationService service;

    @BeforeEach
    void setUp() {
        service = new OpaUserEvaluationService(userInfoParser, cliExecutor);
    }

    private OpaUserInfo buildUserInfo(String uid, String username) {
        OpaUserInfo.UserInfo info = new OpaUserInfo.UserInfo(username, uid, List.of("employee-group"));
        OpaUserInfo.Request request = new OpaUserInfo.Request("2025-10-20T08:30:00Z", "ENTER", null, info);
        return new OpaUserInfo(request);
    }

    private OpaEvalResult allowResult() {
        OpaEvalResult.Expression expr = new OpaEvalResult.Expression(Boolean.TRUE, "data.physical_access_control.allow", null);
        OpaEvalResult.ResultItem item = new OpaEvalResult.ResultItem(List.of(expr));
        return new OpaEvalResult(List.of(item));
    }

    private OpaEvalResult denyResult() {
        OpaEvalResult.Expression expr = new OpaEvalResult.Expression(Boolean.FALSE, "data.physical_access_control.allow", null);
        OpaEvalResult.ResultItem item = new OpaEvalResult.ResultItem(List.of(expr));
        return new OpaEvalResult(List.of(item));
    }

    @Test
    void evaluateUsersForAccess_allUsersAllow_returnsAllUsers() throws IOException {
        OpaUserInfo anyaUser = buildUserInfo("ES-4902", "Anya Sharma");
        when(userInfoParser.parse(any(InputStream.class))).thenReturn(anyaUser);
        when(cliExecutor.evaluate(anyString(), anyString(), anyString())).thenReturn(allowResult());

        List<OpaUserInfo> result = service.evaluateUsersForAccess(
                "physical-access-control-opa/policy.rego",
                "physical_access_control");

        assertFalse(result.isEmpty());
        assertTrue(result.stream().allMatch(u -> "Anya Sharma".equals(u.getUsername())));
    }

    @Test
    void evaluateUsersForAccess_allUsersDeny_returnsEmptyList() throws IOException {
        OpaUserInfo benUser = buildUserInfo("BC-3115", "Ben Carter");
        when(userInfoParser.parse(any(InputStream.class))).thenReturn(benUser);
        when(cliExecutor.evaluate(anyString(), anyString(), anyString())).thenReturn(denyResult());

        List<OpaUserInfo> result = service.evaluateUsersForAccess(
                "physical-access-control-opa/policy.rego",
                "physical_access_control");

        assertTrue(result.isEmpty());
    }

    @Test
    void evaluateUsersForAccess_parseException_skipsFileAndContinues() throws IOException {
        // First call throws, second succeeds with allow
        OpaUserInfo user = buildUserInfo("DL-1020", "David Lee");
        when(userInfoParser.parse(any(InputStream.class)))
                .thenThrow(new IOException("parse error"))
                .thenReturn(user);
        when(cliExecutor.evaluate(anyString(), anyString(), anyString())).thenReturn(allowResult());

        // Should not throw - error is caught internally
        assertDoesNotThrow(() ->
                service.evaluateUsersForAccess("physical-access-control-opa/policy.rego", "physical_access_control"));
    }

    @Test
    void evaluateUsersForAccess_cliExecutorException_skipsUserAndContinues() throws IOException {
        OpaUserInfo user = buildUserInfo("ES-4902", "Anya Sharma");
        when(userInfoParser.parse(any(InputStream.class))).thenReturn(user);
        when(cliExecutor.evaluate(anyString(), anyString(), anyString()))
                .thenThrow(new IOException("opa not found"));

        assertDoesNotThrow(() ->
                service.evaluateUsersForAccess("physical-access-control-opa/policy.rego", "physical_access_control"));
    }

    @Test
    void evaluateUsersForAccess_passesCorrectPackageName() throws IOException {
        OpaUserInfo user = buildUserInfo("ES-4902", "Anya Sharma");
        when(userInfoParser.parse(any(InputStream.class))).thenReturn(user);
        when(cliExecutor.evaluate(anyString(), anyString(), eq("physical_access_control")))
                .thenReturn(denyResult());

        service.evaluateUsersForAccess("physical-access-control-opa/policy.rego", "physical_access_control");

        verify(cliExecutor, atLeast(1)).evaluate(anyString(), anyString(), eq("physical_access_control"));
    }
}
