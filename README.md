# מערכת ניהול אבטחה — ממשק ווב

ממשק גרפי ל-Spring Boot + Thymeleaf המתחבר ל-SQL Server מקומי.

## דרישות מוקדמות

- Java 17 ומעלה
- Maven 3.8 ומעלה
- SQL Server מקומי עם:
  - **TCP/IP מופעל** על פורט 1433 (SQL Server Configuration Manager → Protocols for MSSQLSERVER → TCP/IP → Enable → Restart)
  - **SQL Server and Windows Authentication mode** מופעל (Server Properties → Security)
  - משתמש `springapp` עם גישה למסד `project`:
    ```sql
    CREATE LOGIN springapp WITH PASSWORD = '${DB_PASSWORD}';
    USE project;
    CREATE USER springapp FOR LOGIN springapp;
    ALTER ROLE db_datareader ADD MEMBER springapp;
    ALTER ROLE db_datawriter ADD MEMBER springapp;
    GRANT EXECUTE TO springapp;
    ```

## הרצה

```bash
mvn spring-boot:run
```

## כניסה למערכת

פתחו דפדפן בכתובת: **http://localhost:8080**

## מסכים

| מסך | כתובת |
|-----|--------|
| תפריט ראשי | `/` |
| הצגת לקוחות ואתרים | `/customers` |
| חיפוש אתר של לקוח | `/search` |
| שאילתות ודוחות | `/queries` |
