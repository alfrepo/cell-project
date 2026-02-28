package digital.alf.cells.physicalacesscontrolopa.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpaPolicyData {
    private String policyName;
    private String packageName;
    private List<String> operations;
    private String resourceKind;
    private String requiredGroup;
    private Instant timeWindowStart;
    private Instant timeWindowEnd;
    private String validationMessage;
}
