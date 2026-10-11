# Business Management System - Version 2 (work in progress)

A web version of the [Version 1 desktop application](https://github.com/YOUR-USERNAME/business-management-system),
being built step by step with **React**, **Spring Boot** and **PostgreSQL**.

> **Status: early. Only the pieces listed under "What works today" exist.** Everything else
> is planned, not built.

## What works today

- PostgreSQL schema (9 tables, 3 views) converted from the Version 1 SQLite schema: `database/postgres-schema.sql`
- Spring Boot backend that connects to PostgreSQL
- Customers API: list, get one, create, edit, delete (hidden, and blocked when the customer has orders), with the same validation rules as Version 1
- Login and roles: users are stored in the database with BCrypt-hashed passwords; every `/api` URL needs a login; only an administrator may delete a customer
- `GET /api/auth/me` returns the logged-in user and role

## What is NOT done yet

- Products, suppliers, employees, inventory, orders and reports
- The React front end
- Automated tests
- HTTPS (the login method below is only safe over HTTPS)

## Login method

HTTP Basic authentication: every request carries the username and password and the server checks
them against the stored BCrypt hash. There are no sessions or cookies. This is simple and good for
learning, but a real deployment would use HTTPS and normally a token (JWT). That is a possible later improvement.

On the first start the server creates two demo accounts (the same as Version 1):

| Role | Username | Password |
|---|---|---|
| Administrator | `admin` | `Admin@123` |
| Employee | `thandi` | `Employee@123` |

These demo passwords are written here, so **change them before any real use**. The administrator
password can be set with the environment variable `BMS_ADMIN_PASSWORD` before the first start.

## Technology

| Part | Choice |
|---|---|
| Backend | Java 17, Spring Boot 4.1, Spring Web, Spring Data JPA, Spring Security |
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
4. Call the API with a login, for example in PowerShell:
```
   $pair = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes("admin:Admin@123"))
   Invoke-RestMethod http://localhost:8080/api/customers -Headers @{ Authorization = "Basic $pair" }
```

## Folder structure

```
database/    PostgreSQL schema script
backend/     Spring Boot project
  src/main/java/com/bms/backend/
    customer/   entity, repository, service, controller for customers
    auth/       users, login lookup, default account creation
    common/     error handling and shared text rules
    config/     security configuration
```