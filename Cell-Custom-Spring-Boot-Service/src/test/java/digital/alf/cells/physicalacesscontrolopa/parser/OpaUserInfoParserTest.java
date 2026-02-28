package digital.alf.cells.physicalacesscontrolopa.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import digital.alf.cells.physicalacesscontrolopa.model.OpaUserInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OpaUserInfoParserTest {

    private OpaUserInfoParser parser;

    @BeforeEach
    void setUp() {
        parser = new OpaUserInfoParser(new ObjectMapper());
    }

    private InputStream json(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void parse_validUserJson_returnsUserInfo() throws IOException {
        String content = """
                {
                  "request": {
                    "admissionTime": "2025-10-20T08:30:00Z",
                    "operation": "ENTER",
                    "resource": {
                      "apiVersion": "v1",
                      "kind": "Facility",
                      "metadata": { "name": "main-entrance-door" }
                    },
                    "userInfo": {
                      "username": "Anya Sharma",
                      "uid": "ES-4902",
                      "groups": ["employee-group", "training-vde-available-group"]
                    }
                  }
                }
                """;

        OpaUserInfo result = parser.parse(json(content));

        assertNotNull(result);
        assertEquals("ES-4902", result.getUserId());
        assertEquals("Anya Sharma", result.getUsername());
        assertEquals(List.of("employee-group", "training-vde-available-group"), result.getUserGroups());
    }

    @Test
    void parse_validUserJson_requestFieldsPopulated() throws IOException {
        String content = """
                {
                  "request": {
                    "admissionTime": "2025-10-20T08:30:00Z",
                    "operation": "ENTER",
                    "resource": { "kind": "Facility", "metadata": {} },
                    "userInfo": { "username": "Anya Sharma", "uid": "ES-4902", "groups": [] }
                  }
                }
                """;

        OpaUserInfo result = parser.parse(json(content));

        assertNotNull(result.getRequest());
        assertEquals("ENTER", result.getRequest().getOperation());
        assertEquals("2025-10-20T08:30:00Z", result.getRequest().getAdmissionTime());
        assertEquals("Facility", result.getRequest().getResource().getKind());
    }

    @Test
    void parse_userWithNoGroups_returnsEmptyGroupList() throws IOException {
        String content = """
                {
                  "request": {
                    "userInfo": { "username": "Ben Carter", "uid": "BC-3115", "groups": [] }
                  }
                }
                """;

        OpaUserInfo result = parser.parse(json(content));

        assertEquals("BC-3115", result.getUserId());
        assertTrue(result.getUserGroups().isEmpty());
    }

    @Test
    void parse_unknownFieldsIgnored_doesNotThrow() throws IOException {
        String content = """
                {
                  "unknownField": "some value",
                  "request": {
                    "userInfo": { "username": "Anya Sharma", "uid": "ES-4902", "groups": [] }
                  }
                }
                """;

        assertDoesNotThrow(() -> parser.parse(json(content)));
    }
}
