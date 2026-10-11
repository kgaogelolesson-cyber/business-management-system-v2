# Business Management System - Version 2 (work in progress)

A web version of the [Version 1 desktop application](https://github.com/kgaogelolesson-cyber/business-management-system),
being built step by step with **React**, **Spring Boot** and **PostgreSQL**.

> **Status: early. Only the pieces listed under "What works today" exist.** Everything else
> is planned, not built.

## What works today

- PostgreSQL schema (9 tables, 3 views) converted from the Version 1 SQLite schema: `database/postgres-schema.sql`
- Spring Boot backend that connects to PostgreSQL
- Customers API: list, get one, create, edit, delete (hidden, and blocked when the customer has orders), with the same validation rules and messages as Version 1
- Login and roles: users are stored in the database with BCrypt-hashed passwords; every `/api` URL needs a login; only an administrator may delete a customer
- React front end: login screen, customer list, and add / edit / delete customer (the Delete button is shown to administrators only)

## What is NOT done yet

- Products, suppliers, employees, inventory, orders, reports and the dashboard
- Searching and filtering in the React screens
- Automated tests
- HTTPS (the login method below is only safe over HTTPS)

## Login method

HTTP Basic authentication: every request carries the username and password and the server checks
them against the stored BCrypt hash. There are no sessions or cookies, and the browser keeps the
login in memory only (reloading the page logs you out). This is simple and good for learning, but a
real deployment would use HTTPS and normally a token (JWT). That is a possible later improvement.

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
| Front end | React with Vite (JavaScript) |
| Backend | Java 17, Spring Boot 4.1, Spring Web, Spring Data JPA, Spring Security |
| Database | PostgreSQL 18 |
| Build | Maven (the included `mvnw` wrapper) and npm |

## How to run it

You need: a JDK (17 or newer), PostgreSQL, and Node.js.

1. Create an empty PostgreSQL database called `bms`.
2. Run `database/postgres-schema.sql` in that database (pgAdmin: Query Tool, open file, F5).
3. Start the backend. The database password is read from an environment variable, never from a file:
```
   cd backend
   $env:DB_PASSWORD="your-postgres-password"
   .\mvnw.cmd spring-boot:run
```
4. In a second window, start the front end:
```
   cd frontend
   npm install
   npm run dev
```
5. Open http://localhost:5173 and sign in. The dev server forwards `/api` requests to the backend on port 8080.

## Folder structure

```
database/    PostgreSQL schema script
backend/     Spring Boot project
  src/main/java/com/bms/backend/
    customer/   entity, repository, service, controller for customers
    auth/       users, login lookup, default account creation
    common/     error handling and shared text rules
    config/     security configuration
frontend/    React project (Vite)
  src/
    api.js            the only file that talks to the API
    LoginForm.jsx     login screen
    CustomerList.jsx  customer table, add / edit / delete
    CustomerForm.jsx  add and edit form
```