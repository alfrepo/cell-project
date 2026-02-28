package digital.alf.cells.physicalacesscontrolopa.service;

import digital.alf.cells.physicalacesscontrolopa.model.OpaEvalResult;
import digital.alf.cells.physicalacesscontrolopa.model.OpaUserInfo;
import digital.alf.cells.physicalacesscontrolopa.parser.OpaUserInfoParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Service to evaluate users from the pip-users directory against an OPA policy.
 *
 * Equivalent of UserEvaluationService for the OPA engine.
 * Iterates over all JSON user files in classpath:physical-access-control-opa/pip-users/,
 * runs opa eval on each, and returns those for whom allow == true.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OpaUserEvaluationService {

    private final OpaUserInfoParser userInfoParser;
    private final OpaCliExecutor cliExecutor;

    /**
     * Evaluates all users from the OPA pip-users directory against the given policy.
     *
     * @param policyPath  Path to the .rego file (relative to resources, e.g. "physical-access-control-opa/policy.rego")
     * @param packageName OPA package name extracted from the rego file (e.g. "physical_access_control")
     * @return List of OpaUserInfo objects for users where allow == true
     */
    public List<OpaUserInfo> evaluateUsersForAccess(String policyPath, String packageName) throws IOException {
        List<OpaUserInfo> qualifiedUsers = new ArrayList<>();

        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] userResources = resolver.getResources("classpath:physical-access-control-opa/pip-users/*.json");

        log.info("Found {} OPA user files to evaluate", userResources.length);

        for (Resource userResource : userResources) {
            try {
                OpaUserInfo userInfo = userInfoParser.parse(userResource.getInputStream());
                String username = userInfo.getUsername();
                String userFilePath = "physical-access-control-opa/pip-users/" + userResource.getFilename();

                log.debug("Evaluating user: {} from file: {}", username, userResource.getFilename());

                OpaEvalResult result = cliExecutor.evaluate(policyPath, userFilePath, packageName);

                if (result.isAllow()) {
                    log.info("User {} PASSED OPA evaluation (allow = true)", username);
                    qualifiedUsers.add(userInfo);
                } else {
                    log.info("User {} FAILED OPA evaluation (allow = false)", username);
                }

            } catch (Exception e) {
                log.error("Error evaluating user from file: {}", userResource.getFilename(), e);
                // Continue with next user
            }
        }

        return qualifiedUsers;
    }
}
