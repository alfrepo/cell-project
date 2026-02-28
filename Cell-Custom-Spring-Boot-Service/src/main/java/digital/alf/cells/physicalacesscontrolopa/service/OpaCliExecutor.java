package digital.alf.cells.physicalacesscontrolopa.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import digital.alf.cells.physicalacesscontrolopa.model.OpaEvalResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

/**
 * Service to execute OPA CLI eval commands and parse the JSON results.
 *
 * Equivalent of KyvernoCliExecutor for the OPA engine.
 * Runs: opa eval -d <policy.rego> --input <userinfo.json> 'data.<packageName>.allow'
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OpaCliExecutor {

    private final ObjectMapper objectMapper;

    /**
     * Evaluates a user input file against an OPA policy.
     *
     * @param policyPath   Path to the .rego file (relative to resources directory)
     * @param userInfoPath Path to the user input JSON file (relative to resources directory)
     * @param packageName  OPA package name (e.g. "physical_access_control")
     * @return OpaEvalResult containing the parsed JSON output of opa eval
     * @throws IOException if execution or parsing fails
     */
    public OpaEvalResult evaluate(String policyPath, String userInfoPath, String packageName) throws IOException {
        File resourcesDir = getResourcesDirectory();

        String absolutePolicyPath = new File(resourcesDir, policyPath).getAbsolutePath();
        String absoluteUserInfoPath = new File(resourcesDir, userInfoPath).getAbsolutePath();
        String query = "data." + packageName + ".allow";

        List<String> command = List.of(
                "opa", "eval",
                "-d", absolutePolicyPath,
                "--input", absoluteUserInfoPath,
                query
        );

        log.debug("Executing OPA command: {}", String.join(" ", command));

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }

        try {
            int exitCode = process.waitFor();
            log.debug("OPA CLI exit code: {}", exitCode);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Process interrupted", e);
        }

        String jsonOutput = output.toString();
        log.debug("OPA CLI output: {}", jsonOutput);

        try {
            return objectMapper.readValue(jsonOutput, OpaEvalResult.class);
        } catch (Exception e) {
            log.error("Failed to parse OPA output: {}", jsonOutput, e);
            throw new IOException("Failed to parse OPA output", e);
        }
    }

    /**
     * Gets the resources directory from the classpath.
     * Mirrors the approach used by KyvernoCliExecutor.
     */
    private File getResourcesDirectory() throws IOException {
        String classPath = System.getProperty("java.class.path");
        String[] entries = classPath.split(System.getProperty("path.separator"));

        for (String entry : entries) {
            File file = new File(entry);
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
