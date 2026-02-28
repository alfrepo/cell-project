package digital.alf.cells.physicalacesscontrolopa.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OpaEvalResultTest {

    @Test
    void isAllow_nullResult_returnsFalse() {
        OpaEvalResult result = new OpaEvalResult(null);
        assertFalse(result.isAllow());
    }

    @Test
    void isAllow_emptyResult_returnsFalse() {
        OpaEvalResult result = new OpaEvalResult(List.of());
        assertFalse(result.isAllow());
    }

    @Test
    void isAllow_nullExpressions_returnsFalse() {
        OpaEvalResult.ResultItem item = new OpaEvalResult.ResultItem(null);
        OpaEvalResult result = new OpaEvalResult(List.of(item));
        assertFalse(result.isAllow());
    }

    @Test
    void isAllow_emptyExpressions_returnsFalse() {
        OpaEvalResult.ResultItem item = new OpaEvalResult.ResultItem(List.of());
        OpaEvalResult result = new OpaEvalResult(List.of(item));
        assertFalse(result.isAllow());
    }

    @Test
    void isAllow_valueTrue_returnsTrue() {
        OpaEvalResult.Expression expr = new OpaEvalResult.Expression(Boolean.TRUE, "data.physical_access_control.allow", null);
        OpaEvalResult.ResultItem item = new OpaEvalResult.ResultItem(List.of(expr));
        OpaEvalResult result = new OpaEvalResult(List.of(item));
        assertTrue(result.isAllow());
    }

    @Test
    void isAllow_valueFalse_returnsFalse() {
        OpaEvalResult.Expression expr = new OpaEvalResult.Expression(Boolean.FALSE, "data.physical_access_control.allow", null);
        OpaEvalResult.ResultItem item = new OpaEvalResult.ResultItem(List.of(expr));
        OpaEvalResult result = new OpaEvalResult(List.of(item));
        assertFalse(result.isAllow());
    }

    @Test
    void isAllow_valueIsStringNotBoolean_returnsFalse() {
        OpaEvalResult.Expression expr = new OpaEvalResult.Expression("true", "data.physical_access_control.allow", null);
        OpaEvalResult.ResultItem item = new OpaEvalResult.ResultItem(List.of(expr));
        OpaEvalResult result = new OpaEvalResult(List.of(item));
        assertFalse(result.isAllow());
    }

    @Test
    void isAllow_valueIsNull_returnsFalse() {
        OpaEvalResult.Expression expr = new OpaEvalResult.Expression(null, "data.physical_access_control.allow", null);
        OpaEvalResult.ResultItem item = new OpaEvalResult.ResultItem(List.of(expr));
        OpaEvalResult result = new OpaEvalResult(List.of(item));
        assertFalse(result.isAllow());
    }

    @Test
    void isAllow_onlyFirstExpressionConsidered() {
        // First expression is false, second is true - result should be false
        OpaEvalResult.Expression falseExpr = new OpaEvalResult.Expression(Boolean.FALSE, "data.allow", null);
        OpaEvalResult.Expression trueExpr  = new OpaEvalResult.Expression(Boolean.TRUE,  "data.allow", null);
        OpaEvalResult.ResultItem item = new OpaEvalResult.ResultItem(List.of(falseExpr, trueExpr));
        OpaEvalResult result = new OpaEvalResult(List.of(item));
        assertFalse(result.isAllow());
    }
}
