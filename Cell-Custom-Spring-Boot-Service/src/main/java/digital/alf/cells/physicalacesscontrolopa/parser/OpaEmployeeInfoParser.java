package digital.alf.cells.physicalacesscontrolopa.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import digital.alf.cells.physicalacesscontrolopa.model.OpaEmployeeInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Parses the OPA employee VDE training JSON file into a list of OpaEmployeeInfo objects.
 *
 * Expected JSON format:
 * {
 *   "employees": [
 *     { "id": "ES-4902", "name": "Anya Sharma", "groups": { "employee-group": true, "training-vde-available-group": true } }
 *   ]
 * }
 */
@Component
@RequiredArgsConstructor
public class OpaEmployeeInfoParser {

    private final ObjectMapper objectMapper;

    public List<OpaEmployeeInfo> parse(InputStream inputStream) throws IOException {
        @SuppressWarnings("unchecked")
        Map<String, Object> data = objectMapper.readValue(inputStream, Map.class);

        List<OpaEmployeeInfo> employees = new ArrayList<>();

        if (data != null && data.containsKey("employees")) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> employeeList = (List<Map<String, Object>>) data.get("employees");

            for (Map<String, Object> empData : employeeList) {
                OpaEmployeeInfo employee = new OpaEmployeeInfo();
                employee.setId((String) empData.get("id"));
                employee.setName((String) empData.get("name"));

                @SuppressWarnings("unchecked")
                Map<String, Boolean> groups = (Map<String, Boolean>) empData.get("groups");
                employee.setGroups(groups);

                employees.add(employee);
            }
        }

        return employees;
    }
}
