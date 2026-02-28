package digital.alf.cells.physicalacesscontrolopa.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AclEntry {
    private String principal;  // Format: <UserId:name>
    private String action;
    private String resource;
    private String condition;

    @Override
    public String toString() {
        return String.format("""
                - Principal: %s
                - Action: %s
                - Resource: %s
                - Condition: %s
                """, principal, action, resource, condition);
    }
}
