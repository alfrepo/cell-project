package digital.alf.cells.physicalacesscontrolopa.model;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OpaEmployeeInfoTest {

    @Test
    void hasGroup_nullGroups_returnsFalse() {
        OpaEmployeeInfo emp = new OpaEmployeeInfo("ES-4902", "Anya Sharma", null);
        assertFalse(emp.hasGroup("training-vde-available-group"));
    }

    @Test
    void hasGroup_groupPresentAndTrue_returnsTrue() {
        OpaEmployeeInfo emp = new OpaEmployeeInfo("ES-4902", "Anya Sharma",
                Map.of("training-vde-available-group", true, "employee-group", true));
        assertTrue(emp.hasGroup("training-vde-available-group"));
    }

    @Test
    void hasGroup_groupPresentButFalse_returnsFalse() {
        OpaEmployeeInfo emp = new OpaEmployeeInfo("BC-3115", "Ben Carter",
                Map.of("training-vde-available-group", false, "employee-group", true));
        assertFalse(emp.hasGroup("training-vde-available-group"));
    }

    @Test
    void hasGroup_groupAbsent_returnsFalse() {
        OpaEmployeeInfo emp = new OpaEmployeeInfo("BC-3115", "Ben Carter",
                Map.of("employee-group", true));
        assertFalse(emp.hasGroup("training-vde-available-group"));
    }

    @Test
    void hasGroup_emptyGroups_returnsFalse() {
        OpaEmployeeInfo emp = new OpaEmployeeInfo("BC-3115", "Ben Carter", Map.of());
        assertFalse(emp.hasGroup("training-vde-available-group"));
    }
}
