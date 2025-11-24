package digital.alf.cells.physicalacesscontrol.parser;

import digital.alf.cells.physicalacesscontrol.model.KyvernoPolicyData;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class KyvernoPolicyParser {

    /**
     * Parses the Kyverno policy YAML file and extracts relevant ABAC information.
     *
     * @param inputStream InputStream of the YAML file
     * @return KyvernoPolicyData object containing parsed policy information
     */
    public KyvernoPolicyData parse(InputStream inputStream) {
        Yaml yaml = new Yaml();
        Map<String, Object> data = yaml.load(inputStream);

        KyvernoPolicyData.KyvernoPolicyDataBuilder builder = KyvernoPolicyData.builder();

        // Extract metadata
        @SuppressWarnings("unchecked")
        Map<String, Object> metadata = (Map<String, Object>) data.get("metadata");
        if (metadata != null) {
            builder.policyName((String) metadata.get("name"));
        }

        // Extract spec
        @SuppressWarnings("unchecked")
        Map<String, Object> spec = (Map<String, Object>) data.get("spec");
        if (spec != null) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rules = (List<Map<String, Object>>) spec.get("rules");

            if (rules != null && !rules.isEmpty()) {
                Map<String, Object> rule = rules.get(0); // Process first rule

                // Extract match clause
                @SuppressWarnings("unchecked")
                Map<String, Object> match = (Map<String, Object>) rule.get("match");
                if (match != null) {
                    // Extract operations (optional)
                    @SuppressWarnings("unchecked")
                    List<String> operations = (List<String>) match.get("operations");
                    // If no operations specified, default to common ones based on annotations
                    if (operations == null || operations.isEmpty()) {
                        operations = extractOperationsFromAnnotations(metadata);
                    }
                    builder.operations(operations);

                    // Extract subject group (optional)
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> subjects = (List<Map<String, Object>>) match.get("subjects");
                    if (subjects != null && !subjects.isEmpty()) {
                        builder.matchGroup((String) subjects.get(0).get("name"));
                    } else {
                        // Default from annotations
                        builder.matchGroup("employee-group");
                    }

                    // Extract resource information
                    @SuppressWarnings("unchecked")
                    Map<String, Object> resources = (Map<String, Object>) match.get("resources");
                    if (resources != null) {
                        @SuppressWarnings("unchecked")
                        List<String> kinds = (List<String>) resources.get("kinds");
                        if (kinds != null && !kinds.isEmpty()) {
                            builder.resourceKind(kinds.get(0));
                        }

                        @SuppressWarnings("unchecked")
                        Map<String, Object> selector = (Map<String, Object>) resources.get("selector");
                        if (selector != null) {
                            @SuppressWarnings("unchecked")
                            Map<String, String> matchLabels = (Map<String, String>) selector.get("matchLabels");
                            builder.resourceLabels(matchLabels != null ? matchLabels : new HashMap<>());
                        } else {
                            builder.resourceLabels(new HashMap<>());
                        }
                    }
                }

                // Extract preconditions (time window)
                @SuppressWarnings("unchecked")
                Map<String, Object> preconditions = (Map<String, Object>) rule.get("preconditions");
                if (preconditions != null) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> allConditions = (List<Map<String, Object>>) preconditions.get("all");
                    if (allConditions != null && !allConditions.isEmpty()) {
                        String timeKey = (String) allConditions.get(0).get("key");
                        parseTimeWindow(timeKey, builder);
                    }
                }

                // Extract deny conditions from both old and new structures
                @SuppressWarnings("unchecked")
                Map<String, Object> deny = (Map<String, Object>) rule.get("deny");
                @SuppressWarnings("unchecked")
                Map<String, Object> validate = (Map<String, Object>) rule.get("validate");

                // Try old structure (deny at rule level)
                if (deny != null) {
                    extractRequiredGroupFromDeny(deny, builder);
                }

                // Try new structure (deny under validate)
                if (validate != null) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> validateDeny = (Map<String, Object>) validate.get("deny");
                    if (validateDeny != null) {
                        extractRequiredGroupFromDeny(validateDeny, builder);
                    }

                    // Extract message
                    builder.validationMessage((String) validate.get("message"));
                }

            }
        }

        return builder.build();
    }

    /**
     * Extract required group from deny conditions structure.
     */
    private void extractRequiredGroupFromDeny(Map<String, Object> deny, KyvernoPolicyData.KyvernoPolicyDataBuilder builder) {
        @SuppressWarnings("unchecked")
        Map<String, Object> conditions = (Map<String, Object>) deny.get("conditions");
        if (conditions != null) {
            // Try "all" conditions
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> allDenyConditions = (List<Map<String, Object>>) conditions.get("all");
            if (allDenyConditions != null) {
                extractGroupFromConditionList(allDenyConditions, builder);
            }

            // Try "any" conditions
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> anyDenyConditions = (List<Map<String, Object>>) conditions.get("any");
            if (anyDenyConditions != null) {
                extractGroupFromConditionList(anyDenyConditions, builder);
            }
        }
    }

    /**
     * Extract group from condition list.
     */
    private void extractGroupFromConditionList(List<Map<String, Object>> conditions, KyvernoPolicyData.KyvernoPolicyDataBuilder builder) {
        for (Map<String, Object> condition : conditions) {
            String operator = (String) condition.get("operator");
            if ("AnyNotIn".equals(operator) || "NotIn".equals(operator)) {
                String key = (String) condition.get("key");
                if (key != null && !key.isEmpty()) {
                    builder.requiredGroup(key);
                    break;
                }
            }
        }
    }

    /**
     * Extract operations from policy annotations/description if not explicitly stated.
     */
    private List<String> extractOperationsFromAnnotations(Map<String, Object> metadata) {
        List<String> operations = new ArrayList<>();
        if (metadata != null) {
            @SuppressWarnings("unchecked")
            Map<String, Object> annotations = (Map<String, Object>) metadata.get("annotations");
            if (annotations != null) {
                String description = (String) annotations.get("policies.kyverno.io/description");
                if (description != null) {
                    // Look for operation keywords in description
                    if (description.toUpperCase().contains("ENTER")) {
                        operations.add("ENTER");
                    }
                    if (description.toUpperCase().contains("EXIT")) {
                        operations.add("EXIT");
                    }
                    if (description.toUpperCase().contains("UPDATE")) {
                        operations.add("UPDATE");
                    }
                }
            }
        }
        // Default if nothing found
        if (operations.isEmpty()) {
            operations.add("UPDATE");
        }
        return operations;
    }

    /**
     * Extracts time window from the time_between expression.
     * Expected format: {{ time_between('{{ time_now_utc() }}', '2025-10-20T08:00:00Z', '2025-10-20T09:00:00Z') }}
     */
    private void parseTimeWindow(String timeExpression, KyvernoPolicyData.KyvernoPolicyDataBuilder builder) {
        if (timeExpression == null) return;

        Pattern pattern = Pattern.compile("'(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z)'");
        Matcher matcher = pattern.matcher(timeExpression);

        List<String> timestamps = new ArrayList<>();
        while (matcher.find()) {
            timestamps.add(matcher.group(1));
        }

        if (timestamps.size() >= 2) {
            builder.timeWindowStart(Instant.parse(timestamps.get(0)));
            builder.timeWindowEnd(Instant.parse(timestamps.get(1)));
        }
    }
}
