package digital.alf.cells.physicalacesscontrolopa.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OpaEmployeeInfo {
    private String id;
    private String name;
    private Map<String, Boolean> groups;

    public boolean hasGroup(String groupName) {
        return groups != null && groups.getOrDefault(groupName, false);
    }
}
