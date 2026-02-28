package digital.alf.cells.physicalacesscontrolopa.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import digital.alf.cells.physicalacesscontrolopa.model.OpaEmployeeInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OpaEmployeeInfoParserTest {

    private OpaEmployeeInfoParser parser;

    @BeforeEach
    void setUp() {
        parser = new OpaEmployeeInfoParser(new ObjectMapper());
    }

    private InputStream json(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void parse_validJson_returnsAllEmployees() throws IOException {
        String content = """
                {
                  "employees": [
                    { "id": "ES-4902", "name": "Anya Sharma", "groups": { "employee-group": true, "training-vde-available-group": true } },
                    { "id": "BC-3115", "name": "Ben Carter",  "groups": { "employee-group": true, "training-vde-available-group": false } }
                  ]
                }
                """;

        List<OpaEmployeeInfo> result = parser.parse(json(content));

        assertEquals(2, result.size());
    }

    @Test
    void parse_firstEmployee_hasCorrectIdAndName() throws IOException {
        String content = """
                {
                  "employees": [
                    { "id": "ES-4902", "name": "Anya Sharma", "groups": { "training-vde-available-group": true } }
                  ]
                }
                """;

        OpaEmployeeInfo emp = parser.parse(json(content)).get(0);

        assertEquals("ES-4902", emp.getId());
        assertEquals("Anya Sharma", emp.getName());
    }

    @Test
    void parse_groupTrueValue_hasGroupReturnsTrue() throws IOException {
        String content = """
                {
                  "employees": [
                    { "id": "ES-4902", "name": "Anya Sharma", "groups": { "training-vde-available-group": true } }
                  ]
                }
                """;

        OpaEmployeeInfo emp = parser.parse(json(content)).get(0);

        assertTrue(emp.hasGroup("training-vde-available-group"));
    }

    @Test
    void parse_groupFalseValue_hasGroupReturnsFalse() throws IOException {
        String content = """
                {
                  "employees": [
                    { "id": "BC-3115", "name": "Ben Carter", "groups": { "training-vde-available-group": false } }
                  ]
                }
                """;

        OpaEmployeeInfo emp = parser.parse(json(content)).get(0);

        assertFalse(emp.hasGroup("training-vde-available-group"));
    }

    @Test
    void parse_emptyEmployeesList_returnsEmptyList() throws IOException {
        String content = """
                { "employees": [] }
                """;

        List<OpaEmployeeInfo> result = parser.parse(json(content));

        assertTrue(result.isEmpty());
    }

    @Test
    void parse_missingEmployeesKey_returnsEmptyList() throws IOException {
        String content = """
                { "_comment": "no employees key" }
                """;

        List<OpaEmployeeInfo> result = parser.parse(json(content));

        assertTrue(result.isEmpty());
    }
}
