package digital.alf.cells.physicalacesscontrol.service;

import digital.alf.cells.physicalacesscontrol.model.KyvernoClusterReport;
import digital.alf.cells.physicalacesscontrol.model.KyvernoUserInfo;
import digital.alf.cells.physicalacesscontrol.parser.KyvernoUserInfoParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Service to evaluate users against Kyverno policies.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UserEvaluationService {

    private final KyvernoUserInfoParser userInfoParser;
    private final KyvernoCliExecutor cliExecutor;

    /**
     * Evaluates all users from the pip-users directory against a policy and resource.
     *
     * @param policyPath Path to policy YAML (relative to resources)
     * @param resourcePath Path to resource YAML (relative to resources)
     * @param operation Operation to evaluate
     * @param admissionTime Admission time for evaluation
     * @return List of KyvernoUserInfo objects for users who passed evaluation
     */
    public List<KyvernoUserInfo> evaluateUsersForAccess(
            String policyPath,
            String resourcePath,
            String operation,
            String admissionTime) throws IOException {

        List<KyvernoUserInfo> qualifiedUsers = new ArrayList<>();

        // Find all user info files
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] userResources = resolver.getResources("classpath:physical-access-control/pip-users/*.yml");

        log.info("Found {} user files to evaluate", userResources.length);

        for (Resource userResource : userResources) {
            try {
                // Parse user info
                KyvernoUserInfo userInfo = userInfoParser.parse(userResource.getInputStream());
                String username = userInfo.getUserId();
                String userFilePath = "physical-access-control/pip-users/" + userResource.getFilename();

                log.debug("Evaluating user: {} from file: {}", username, userResource.getFilename());

                // Evaluate user against policy
                KyvernoClusterReport report = cliExecutor.evaluate(
                        policyPath,
                        resourcePath,
                        userFilePath,
                        operation,
                        admissionTime
                );

                // Check if user passed evaluation
                if (report.hasPassed()) {
                    log.info("User {} PASSED evaluation for operation {}", username, operation);
                    qualifiedUsers.add(userInfo);
                } else {
                    log.info("User {} FAILED evaluation for operation {}", username, operation);
                    if (report.getResults() != null && !report.getResults().isEmpty()) {
                        log.debug("Failure reason: {}", report.getResults().get(0).getMessage());
                    }
                }

            } catch (Exception e) {
                log.error("Error evaluating user from file: {}", userResource.getFilename(), e);
                // Continue with next user
            }
        }

        return qualifiedUsers;
    }

    /**
     * Evaluates all users for multiple operations.
     *
     * @param policyPath Path to policy YAML
     * @param resourcePath Path to resource YAML
     * @param operations List of operations to evaluate
     * @param admissionTime Admission time for evaluation
     * @return List of users who passed evaluation for at least one operation
     */
    public List<KyvernoUserInfo> evaluateUsersForMultipleOperations(
            String policyPath,
            String resourcePath,
            List<String> operations,
            String admissionTime) throws IOException {

        List<KyvernoUserInfo> qualifiedUsers = new ArrayList<>();

        for (String operation : operations) {
            List<KyvernoUserInfo> usersForOperation = evaluateUsersForAccess(
                    policyPath,
                    resourcePath,
                    operation,
                    admissionTime
            );

            // Add users that aren't already in the list
            for (KyvernoUserInfo user : usersForOperation) {
                if (qualifiedUsers.stream().noneMatch(u -> u.getUserId().equals(user.getUserId()))) {
                    qualifiedUsers.add(user);
                }
            }
        }

        return qualifiedUsers;
    }
}
