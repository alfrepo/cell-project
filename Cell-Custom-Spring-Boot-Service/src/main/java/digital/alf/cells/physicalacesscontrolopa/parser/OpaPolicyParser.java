package digital.alf.cells.physicalacesscontrolopa.parser;

import digital.alf.cells.physicalacesscontrolopa.model.OpaPolicyData;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Parses an OPA rego policy file and extracts relevant ABAC metadata.
 *
 * Extracts from the rego source text using regex:
 * - package name (e.g. "physical_access_control")
 * - policy name from comment "# Policy: <name>"
 * - operations from comment "# Restricts the ENTER operation" or similar
 * - resource kind from rule body: input.request.resource.kind == "Facility"
 * - required group from rule body: not "group-name" in input.request.userInfo.groups
 * - time window from: time.parse_rfc3339_ns("2024-...")
 * - violation message from: msg := "..."
 */
@Component
public class OpaPolicyParser {

    private static final Pattern PACKAGE_PATTERN =
            Pattern.compile("^package\\s+(\\S+)", Pattern.MULTILINE);

    private static final Pattern POLICY_NAME_PATTERN =
            Pattern.compile("#\\s*Policy:\\s*(.+)$", Pattern.MULTILINE);

    private static final Pattern OPERATION_PATTERN =
            Pattern.compile("#.*?\\b(ENTER|EXIT|UPDATE|CREATE|DELETE)\\b", Pattern.MULTILINE);

    private static final Pattern RESOURCE_KIND_PATTERN =
            Pattern.compile("input\\.request\\.resource\\.kind\\s*==\\s*\"(\\w+)\"");

    private static final Pattern REQUIRED_GROUP_PATTERN =
            Pattern.compile("not\\s+\"([^\"]+)\"\\s+in\\s+input\\.request\\.userInfo\\.groups");

    private static final Pattern TIME_PATTERN =
            Pattern.compile("time\\.parse_rfc3339_ns\\(\"(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z)\"\\)");

    private static final Pattern VIOLATION_MSG_PATTERN =
            Pattern.compile("msg\\s*:=\\s*\"([^\"]+)\"");

    /**
     * Parses a rego policy file from the given InputStream.
     *
     * @param inputStream InputStream of the .rego file
     * @return OpaPolicyData with extracted metadata
     */
    public OpaPolicyData parse(InputStream inputStream) throws IOException {
        String regoText;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            regoText = reader.lines().collect(Collectors.joining("\n"));
        }

        OpaPolicyData.OpaPolicyDataBuilder builder = OpaPolicyData.builder();

        // Package name
        Matcher m = PACKAGE_PATTERN.matcher(regoText);
        if (m.find()) {
            builder.packageName(m.group(1));
        }

        // Policy name from comment
        m = POLICY_NAME_PATTERN.matcher(regoText);
        if (m.find()) {
            builder.policyName(m.group(1).trim());
        }

        // Operations from comments
        List<String> operations = new ArrayList<>();
        m = OPERATION_PATTERN.matcher(regoText);
        while (m.find()) {
            String op = m.group(1);
            if (!operations.contains(op)) {
                operations.add(op);
            }
        }
        if (operations.isEmpty()) {
            operations.add("ENTER");
        }
        builder.operations(operations);

        // Resource kind
        m = RESOURCE_KIND_PATTERN.matcher(regoText);
        if (m.find()) {
            builder.resourceKind(m.group(1));
        }

        // Required group
        m = REQUIRED_GROUP_PATTERN.matcher(regoText);
        if (m.find()) {
            builder.requiredGroup(m.group(1));
        }

        // Time window — first match is window_start, second is window_end
        m = TIME_PATTERN.matcher(regoText);
        List<String> timestamps = new ArrayList<>();
        while (m.find()) {
            timestamps.add(m.group(1));
        }
        if (timestamps.size() >= 2) {
            builder.timeWindowStart(Instant.parse(timestamps.get(0)));
            builder.timeWindowEnd(Instant.parse(timestamps.get(1)));
        }

        // Violation message
        m = VIOLATION_MSG_PATTERN.matcher(regoText);
        if (m.find()) {
            builder.validationMessage(m.group(1));
        }

        return builder.build();
    }
}
