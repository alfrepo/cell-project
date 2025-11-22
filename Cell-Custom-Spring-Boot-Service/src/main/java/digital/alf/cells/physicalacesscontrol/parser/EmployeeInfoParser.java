package digital.alf.cells.physicalacesscontrol.parser;

import digital.alf.cells.physicalacesscontrol.model.EmployeeInfo;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class EmployeeInfoParser {

    /**
     * Parses the employee VDE training YAML file and returns a list of EmployeeInfo objects.
     *
     * @param inputStream InputStream of the YAML file
     * @return List of EmployeeInfo objects
     */
    public List<EmployeeInfo> parse(InputStream inputStream) {
        Yaml yaml = new Yaml();
        Map<String, Object> data = yaml.load(inputStream);

        List<EmployeeInfo> employees = new ArrayList<>();

        if (data != null && data.containsKey("employees")) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> employeeList = (List<Map<String, Object>>) data.get("employees");

            for (Map<String, Object> empData : employeeList) {
                EmployeeInfo employee = new EmployeeInfo();
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
