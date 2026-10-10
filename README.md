# Business Management System - Version 2 (work in progress)

A web version of the [Version 1 desktop application](https://github.com/kgaogelolesson-cyber/business-management-system),
being built step by step with **React**, **Spring Boot** and **PostgreSQL**.

> **Status: early. Only the pieces listed under "What works today" exist.** Everything else
> is planned, not built.

## What works today

- PostgreSQL schema (9 tables, 3 views) converted from the Version 1 SQLite schema: `database/postgres-schema.sql`
- Spring Boot backend that connects to PostgreSQL
- Customers API: list, get one, create, edit, delete (hidden, and blocked when the customer has orders), with the same validation rules as Version 1

## What is NOT done yet

- Login, roles and passwords (the security setup is **temporary and open**: do not expose this to a network)
- Products, suppliers, employees, inventory, orders and reports
- The React front end
- Automated tests

## Technology

| Part | Choice |
|---|---|
| Backend | Java 17, Spring Boot 4.1, Spring Web, Spring Data JPA |
| Database | PostgreSQL 18 |
| Build | Maven (the included `mvnw` wrapper, nothing to install) |

## How to run it

1. Install PostgreSQL and create an empty database called `bms`.
2. Run `database/postgres-schema.sql` in that database (pgAdmin: Query Tool, open file, F5).
3. Start the backend. The database password is read from an environment variable, never from a file:
   ```
   cd backend
   $env:DB_PASSWORD="your-postgres-password"
   .\mvnw.cmd spring-boot:run
   ```
4. Open http://localhost:8080/api/customers

## Folder structure

```
database/    PostgreSQL schema script
backend/     Spring Boot project
  src/main/java/com/bms/backend/
    customer/   entity, repository, service, controller for customers
    config/     security configuration (temporary)
```
