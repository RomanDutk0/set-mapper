package com.epam.rd.autocode;

import com.epam.rd.autocode.domain.Employee;

import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import com.epam.rd.autocode.domain.FullName;
import com.epam.rd.autocode.domain.Position;
import java.math.BigDecimal;
import java.math.BigInteger;


public class SetMapperFactory {
    public static SetMapper<Set<Employee>> employeesSetMapper() {
        return resultSet -> {
            Map<BigInteger, Employee> employeesMap = new HashMap<>();
            Set<Employee> employees = new HashSet<>();

            try {
                // Перший прохід: створення всіх співробітників без менеджерів
                while (resultSet.next()) {
                    BigInteger id = BigInteger.valueOf(resultSet.getInt("ID"));
                    if (!employeesMap.containsKey(id)) {
                        Employee employee = createEmployeeWithoutManager(resultSet);
                        employeesMap.put(id, employee);
                    }
                }

                // Повертаємо курсор до початку ResultSet
                resultSet.beforeFirst();

                // Другий прохід: встановлення зв'язків з менеджерами
                while (resultSet.next()) {
                    BigInteger id = BigInteger.valueOf(resultSet.getInt("ID"));
                    Employee employee = employeesMap.get(id);
                    Integer managerId = resultSet.getObject("MANAGER", Integer.class);

                    if (managerId != null) {
                        BigInteger managerKey = BigInteger.valueOf(managerId);
                        Employee manager = employeesMap.get(managerKey);
                        if (manager == null) {
                            System.out.println("Manager with ID " + managerId + " not found in employeesMap.");
                        } else {
                            // Створюємо нового Employee з менеджером
                            employee = new Employee(
                                    employee.getId(),
                                    employee.getFullName(),
                                    employee.getPosition(),
                                    employee.getHired(),
                                    employee.getSalary(),
                                    manager
                            );
                            employeesMap.put(id, employee); // Оновлюємо мапу
                        }
                    }

                    // Якщо менеджер не був знайдений, додамо співробітника в результат без змін
                    employees.add(employee);
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }

            return employees;
        };
    }

    private static Employee createEmployeeWithoutManager(ResultSet resultSet) throws SQLException {
        BigInteger id = BigInteger.valueOf(resultSet.getInt("ID"));
        FullName fullName = new FullName(
                resultSet.getString("FIRSTNAME"),
                resultSet.getString("LASTNAME"),
                resultSet.getString("MIDDLENAME")
        );
        Position position = Position.valueOf(resultSet.getString("POSITION").toUpperCase());
        LocalDate hiredDate = resultSet.getDate("HIREDATE").toLocalDate();
        BigDecimal salary = BigDecimal.valueOf(resultSet.getDouble("SALARY"))
                .setScale(5, RoundingMode.HALF_UP);

        // Повертаємо об'єкт Employee без менеджера
        return new Employee(id, fullName, position, hiredDate, salary, null);
    }
}

