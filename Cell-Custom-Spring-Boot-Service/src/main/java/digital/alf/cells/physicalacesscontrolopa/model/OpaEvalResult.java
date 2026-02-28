package digital.alf.cells.physicalacesscontrolopa.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Represents the JSON output of the OPA CLI eval command.
 *
 * Example output for a truthy result:
 * {
 *   "result": [
 *     {
 *       "expressions": [
 *         {
 *           "value": true,
 *           "text": "data.physical_access_control.allow",
 *           "location": { "row": 1, "col": 1 }
 *         }
 *       ]
 *     }
 *   ]
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OpaEvalResult {

    private List<ResultItem> result;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ResultItem {
        private List<Expression> expressions;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Expression {
        private Object value;
        private String text;
        private Location location;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Location {
        private int row;
        private int col;
    }

    /**
     * Returns true if the first expression in the result evaluates to boolean true.
     */
    public boolean isAllow() {
        if (result == null || result.isEmpty()) return false;
        ResultItem firstResult = result.get(0);
        if (firstResult.expressions == null || firstResult.expressions.isEmpty()) return false;
        return Boolean.TRUE.equals(firstResult.expressions.get(0).value);
    }
}
