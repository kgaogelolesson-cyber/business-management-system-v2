package com.bms.backend.employee;

import java.security.Principal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 *   GET  /api/employees                    list (?search=text &includeInactive=true &department=Sales)
 *   GET  /api/employees/departments        the department names in use
 *   GET  /api/employees/5                  one employee
 *   POST /api/employees                    add an employee together with their login
 *   PUT  /api/employees/5                  change details and role
 *   POST /api/employees/5/deactivate       hide the employee and block their login
 *   POST /api/employees/5/reactivate       bring them back
 *   POST /api/employees/5/password         set a new password
 * Every one of these is administrator only (see SecurityConfig).
 */
@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    private final EmployeeService service;

    public EmployeeController(EmployeeService service) {
        this.service = service;
    }

    @GetMapping
    public List<EmployeeResponse> list(@RequestParam(required = false) String search,
                                       @RequestParam(defaultValue = "false") boolean includeInactive,
                                       @RequestParam(required = false) String department) {
        return service.list(search, includeInactive, department);
    }

    @GetMapping("/departments")
    public List<String> departments() {
        return service.departments();
    }

    @GetMapping("/{id}")
    public EmployeeResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EmployeeResponse create(@RequestBody EmployeeRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public EmployeeResponse update(@PathVariable Long id, @RequestBody EmployeeRequest request,
                                   Principal principal) {
        return service.update(id, request, principal.getName());
    }

    @PostMapping("/{id}/deactivate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable Long id, Principal principal) {
        service.deactivate(id, principal.getName());
    }

    @PostMapping("/{id}/reactivate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reactivate(@PathVariable Long id) {
        service.reactivate(id);
    }

    @PostMapping("/{id}/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@PathVariable Long id, @RequestBody PasswordRequest request) {
        service.resetPassword(id, request);
    }
}
