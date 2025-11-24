package digital.alf.cells.physicalacesscontrol.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import digital.alf.cells.physicalacesscontrol.model.KyvernoClusterReport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Service to execute Kyverno CLI commands and parse results.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class KyvernoCliExecutor {

    private final ObjectMapper objectMapper;

    /**
     * Evaluates a user against a policy and resource using kyverno-cli.
     *
     * @param policyPath Path to policy YAML file (relative to resources directory)
     * @param resourcePath Path to resource YAML file (relative to resources directory)
     * @param userInfoPath Path to userinfo YAML file (relative to resources directory)
     * @param operation The operation to evaluate (e.g., "ENTER", "EXIT", "UPDATE")
     * @param admissionTime The admission time for evaluation
     * @return KyvernoClusterReport with evaluation results
     * @throws IOException if execution fails
     */
    public KyvernoClusterReport evaluate(
            String policyPath,
            String resourcePath,
            String userInfoPath,
            String operation,
            String admissionTime) throws IOException {

        // Get the resources directory path
        File resourcesDir = getResourcesDirectory();

        // Build the command
        List<String> command = buildCommand(
                new File(resourcesDir, policyPath).getAbsolutePath(),
                new File(resourcesDir, resourcePath).getAbsolutePath(),
                new File(resourcesDir, userInfoPath).getAbsolutePath(),
                operation,
                admissionTime
        );

        log.debug("Executing kyverno command: {}", String.join(" ", command));

        // Execute the command
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();

        // Read output
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }

        // Wait for process to complete
        try {
            int exitCode = process.waitFor();
            log.debug("Kyverno CLI exit code: {}", exitCode);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Process interrupted", e);
        }

        String jsonOutput = output.toString();
        log.debug("Kyverno CLI output: {}", jsonOutput);

        // Parse JSON output
        try {
            return objectMapper.readValue(jsonOutput, KyvernoClusterReport.class);
        } catch (Exception e) {
            log.error("Failed to parse kyverno output: {}", jsonOutput, e);
            throw new IOException("Failed to parse kyverno output", e);
        }
    }

    /**
     * Builds the kyverno CLI command.
     */
    private List<String> buildCommand(
            String policyPath,
            String resourcePath,
            String userInfoPath,
            String operation,
            String admissionTime) {

        List<String> command = new ArrayList<>();
        command.add("kyverno");
        command.add("apply");
        command.add(policyPath);
        command.add("--resource");
        command.add(resourcePath);
        command.add("--userinfo");
        command.add(userInfoPath);
        command.add("--set");
        command.add(String.format("request.operation=%s,request.time.admissionTime=%s", operation, admissionTime));
        command.add("-t");
        command.add("--policy-report");
        command.add("--output-format");
        command.add("json");

        return command;
    }

    /**
     * Gets the resources directory from the classpath.
     */
    private File getResourcesDirectory() throws IOException {
        // Try to get resources directory from classpath
        String classPath = System.getProperty("java.class.path");
        String[] entries = classPath.split(System.getProperty("path.separator"));

        for (String entry : entries) {
            File file = new File(entry);
            // Look for src/main/resources in the path
            if (file.getPath().contains("src" + File.separator + "main")) {
                File resourcesDir = new File(file.getParentFile().getParentFile().getParentFile(),
                        "src" + File.separator + "main" + File.separator + "resources");
                if (resourcesDir.exists() && resourcesDir.isDirectory()) {
                    return resourcesDir;
                }
            }
        }

        // Fallback: try current directory
        File resourcesDir = new File("src/main/resources");
        if (resourcesDir.exists() && resourcesDir.isDirectory()) {
            return resourcesDir;
        }

        throw new IOException("Could not locate resources directory");
    }
}
