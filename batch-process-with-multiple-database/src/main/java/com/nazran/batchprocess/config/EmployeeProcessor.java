package com.nazran.batchprocess.config;

import com.nazran.batchprocess.entity.Employee;
import org.springframework.batch.item.ItemProcessor;

public class EmployeeProcessor implements ItemProcessor<Employee, Employee> {

    @Override
    public Employee process(Employee employee) throws Exception {
        employee.setFirstName(employee.getFirstName().toUpperCase());
        return employee;
    }
}

