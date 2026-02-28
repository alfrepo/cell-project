package digital.alf.cells.physicalacesscontrolopa.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import digital.alf.cells.physicalacesscontrolopa.model.OpaUserInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

/**
 * Parses an OPA user info JSON file into an OpaUserInfo object.
 *
 * Expected JSON format (OPA input structure):
 * {
 *   "request": {
 *     "admissionTime": "2025-10-20T08:30:00Z",
 *     "operation": "ENTER",
 *     "resource": { "kind": "Facility", ... },
 *     "userInfo": { "username": "Anya Sharma", "uid": "ES-4902", "groups": [...] }
 *   }
 * }
 */
@Component
@RequiredArgsConstructor
public class OpaUserInfoParser {

    private final ObjectMapper objectMapper;

    public OpaUserInfo parse(InputStream inputStream) throws IOException {
        return objectMapper.readValue(inputStream, OpaUserInfo.class);
    }
}
