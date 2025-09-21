package digital.alf.cells.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EmployeeController {

    // Aggregate root
    // tag::get-aggregate-root[]
    @GetMapping("/")
    String all() {
        return "[]";
    }
}
