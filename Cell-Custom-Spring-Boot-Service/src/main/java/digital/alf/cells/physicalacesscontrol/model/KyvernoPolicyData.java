package digital.alf.cells.physicalacesscontrol.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KyvernoPolicyData {
    private String policyName;
    private List<String> operations;
    private String resourceKind;
    private Map<String, String> resourceLabels;
    private String matchGroup;
    private String requiredGroup;
    private Instant timeWindowStart;
    private Instant timeWindowEnd;
    private String validationMessage;
}
