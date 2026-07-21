package com.project.service;

import com.project.model.Customer;
import com.project.model.Site;
import com.project.model.SiteSearchResult;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DatabaseService {

    private final JdbcTemplate jdbc;

    // ── Connection test ──────────────────────────────────────────────────────

    public String testConnection() {
        try {
            return jdbc.queryForObject("SELECT DB_NAME()", String.class);
        } catch (Exception e) {
            return "שגיאה: " + e.getMessage();
        }
    }

    // ── Screen 1: Customers ──────────────────────────────────────────────────

    public List<Customer> getAllCustomers() {
        String sql = "SELECT CustomerID, FirstName, LastName, Address, PhoneNum, Email FROM tblCustomers ORDER BY CustomerID";
        return jdbc.query(sql, (rs, i) -> {
            Customer c = new Customer();
            c.setCustomerID(rs.getInt("CustomerID"));
            c.setFirstName(rs.getString("FirstName"));
            c.setLastName(rs.getString("LastName"));
            c.setAddress(rs.getString("Address"));
            c.setPhoneNum(rs.getString("PhoneNum"));
            c.setEmail(rs.getString("Email"));
            return c;
        });
    }

    public Customer getCustomer(int customerId) {
        String sql = "SELECT CustomerID, FirstName, LastName, Address, PhoneNum, Email FROM tblCustomers WHERE CustomerID = ?";
        List<Customer> result = jdbc.query(sql, (rs, i) -> {
            Customer c = new Customer();
            c.setCustomerID(rs.getInt("CustomerID"));
            c.setFirstName(rs.getString("FirstName"));
            c.setLastName(rs.getString("LastName"));
            c.setAddress(rs.getString("Address"));
            c.setPhoneNum(rs.getString("PhoneNum"));
            c.setEmail(rs.getString("Email"));
            return c;
        }, customerId);
        return result.isEmpty() ? null : result.get(0);
    }

    /**
     * Returns sites for a customer, each with the latest subscription (highest SerialNum).
     */
    public List<Site> getSitesWithLatestSubscription(int customerId) {
        String sql = """
            SELECT
                s.CustomerID, s.SiteID, s.StartDate, s.Location,
                sc.SerialNum, sc.ServiceStartDate, sc.Discount, sc.Valid,
                st.Description AS ServiceDescription
            FROM tblSites s
            LEFT JOIN (
                SELECT sc1.*
                FROM tblServiceCharge sc1
                INNER JOIN (
                    SELECT CustomerID, SiteID, MAX(SerialNum) AS MaxSerial
                    FROM tblServiceCharge
                    GROUP BY CustomerID, SiteID
                ) sc2 ON sc1.CustomerID = sc2.CustomerID
                      AND sc1.SiteID    = sc2.SiteID
                      AND sc1.SerialNum = sc2.MaxSerial
            ) sc ON s.CustomerID = sc.CustomerID AND s.SiteID = sc.SiteID
            LEFT JOIN tblServiceType st ON sc.ServiceTypeID = st.ServiceTypeID
            WHERE s.CustomerID = ?
            ORDER BY s.SiteID
            """;

        return jdbc.query(sql, (rs, i) -> {
            Site site = new Site();
            site.setCustomerID(rs.getInt("CustomerID"));
            site.setSiteID(rs.getInt("SiteID"));
            java.sql.Date sd = rs.getDate("StartDate");
            site.setStartDate(sd != null ? sd.toLocalDate() : null);
            site.setLocation(rs.getString("Location"));
            site.setSerialNum(rs.getObject("SerialNum") != null ? rs.getInt("SerialNum") : null);
            java.sql.Date ssd = rs.getDate("ServiceStartDate");
            site.setServiceStartDate(ssd != null ? ssd.toLocalDate() : null);
            site.setServiceDescription(rs.getString("ServiceDescription"));
            site.setDiscount(rs.getObject("Discount") != null ? rs.getDouble("Discount") : null);
            site.setValid(rs.getObject("Valid") != null ? rs.getInt("Valid") : null);
            return site;
        }, customerId);
    }

    // ── Screen 2: Site Search ────────────────────────────────────────────────

    public SiteSearchResult searchSite(int customerId, int siteId) {
        // Verify customer + site exist
        String checkSql = """
            SELECT c.CustomerID, c.FirstName, c.LastName,
                   (SELECT COUNT(*) FROM tblSites WHERE CustomerID = c.CustomerID) AS SiteCount
            FROM tblCustomers c
            INNER JOIN tblSites s ON c.CustomerID = s.CustomerID AND s.SiteID = ?
            WHERE c.CustomerID = ?
            """;

        List<SiteSearchResult> rows = jdbc.query(checkSql, (rs, i) -> {
            SiteSearchResult r = new SiteSearchResult();
            r.setCustomerID(rs.getInt("CustomerID"));
            r.setFullName(rs.getString("FirstName") + " " + rs.getString("LastName"));
            r.setSiteCount(rs.getInt("SiteCount"));
            return r;
        }, siteId, customerId);

        if (rows.isEmpty()) return null;

        SiteSearchResult result = rows.get(0);

        // Valid subscription serial
        String validSql = """
            SELECT SerialNum FROM tblServiceCharge
            WHERE CustomerID = ? AND SiteID = ? AND Valid = 1
            """;
        List<Integer> validSerial = jdbc.query(validSql, (rs, i) -> rs.getInt("SerialNum"), customerId, siteId);
        result.setValidSerialNum(validSerial.isEmpty() ? null : validSerial.get(0));

        // Last alert call
        String callSql = """
            SELECT MAX(DateOfCall) AS LastCall FROM tblAlertCalls
            WHERE CustomerID = ? AND SiteID = ?
            """;
        List<LocalDateTime> lastCall = jdbc.query(callSql, (rs, i) -> {
            Timestamp ts = rs.getTimestamp("LastCall");
            return ts != null ? ts.toLocalDateTime() : null;
        }, customerId, siteId);
        result.setLastCallDate(lastCall.isEmpty() ? null : lastCall.get(0));

        return result;
    }

    // ── Screen 3: Queries ────────────────────────────────────────────────────

    public QueryResult runSimpleView(String viewSql) {
        return runWithWarnings(viewSql);
    }

    public QueryResult runProcedureMonthlyCharge(int customerId) {
        return runWithWarnings("EXEC sp_CalculateMonthlyCharge @CustomerID = " + customerId);
    }

    public QueryResult runProcedurePatrolStats(int minRequest, int month, int year) {
        return runWithWarnings(
            "EXEC sp_PatrolMonthlyStats @minRequest = " + minRequest +
            ", @month = " + month + ", @year = " + year
        );
    }

    // ── SQLWarning capture ───────────────────────────────────────────────────

    /**
     * Executes any SQL and captures both result rows and PRINT/RAISERROR messages.
     */
    public QueryResult runWithWarnings(String sql) {
        QueryResult result = new QueryResult();
        try (Connection conn = Objects.requireNonNull(jdbc.getDataSource()).getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute(sql);

            // Collect PRINT messages (SQLWarning chain)
            collectWarnings(stmt.getWarnings(), result);
            collectWarnings(conn.getWarnings(), result);

            // Try to get result set
            try (ResultSet rs = stmt.getResultSet()) {
                if (rs != null) {
                    ResultSetMetaData meta = rs.getMetaData();
                    int colCount = meta.getColumnCount();
                    List<String> headers = new ArrayList<>();
                    for (int i = 1; i <= colCount; i++) {
                        headers.add(meta.getColumnLabel(i));
                    }
                    result.setHeaders(headers);

                    List<List<String>> rows = new ArrayList<>();
                    while (rs.next()) {
                        List<String> row = new ArrayList<>();
                        for (int i = 1; i <= colCount; i++) {
                            Object val = rs.getObject(i);
                            row.add(val != null ? val.toString() : "");
                        }
                        rows.add(row);
                    }
                    result.setRows(rows);
                }
            }

            // Check for additional result sets
            while (stmt.getMoreResults() || stmt.getUpdateCount() != -1) {
                try (ResultSet rs2 = stmt.getResultSet()) {
                    if (rs2 != null && result.getHeaders().isEmpty()) {
                        ResultSetMetaData meta = rs2.getMetaData();
                        int colCount = meta.getColumnCount();
                        List<String> headers = new ArrayList<>();
                        for (int i = 1; i <= colCount; i++) headers.add(meta.getColumnLabel(i));
                        result.setHeaders(headers);
                        List<List<String>> rows = new ArrayList<>();
                        while (rs2.next()) {
                            List<String> row = new ArrayList<>();
                            for (int i = 1; i <= colCount; i++) {
                                Object val = rs2.getObject(i);
                                row.add(val != null ? val.toString() : "");
                            }
                            rows.add(row);
                        }
                        result.setRows(rows);
                    }
                }
                collectWarnings(stmt.getWarnings(), result);
            }
            collectWarnings(stmt.getWarnings(), result);
            collectWarnings(conn.getWarnings(), result);

        } catch (SQLException e) {
            result.getMessages().add("שגיאה: " + e.getMessage());
        }
        return result;
    }

    private void collectWarnings(SQLWarning w, QueryResult result) {
        while (w != null) {
            String msg = w.getMessage();
            if (msg != null && !msg.isBlank() && !result.getMessages().contains(msg)) {
                result.getMessages().add(msg);
            }
            w = w.getNextWarning();
        }
    }

    // ── Inner DTO ────────────────────────────────────────────────────────────

    public static class QueryResult {
        private List<String> headers = new ArrayList<>();
        private List<List<String>> rows = new ArrayList<>();
        private List<String> messages = new ArrayList<>();

        public List<String> getHeaders() { return headers; }
        public void setHeaders(List<String> h) { this.headers = h; }
        public List<List<String>> getRows() { return rows; }
        public void setRows(List<List<String>> r) { this.rows = r; }
        public List<String> getMessages() { return messages; }
        public boolean hasData() { return !headers.isEmpty(); }
        public boolean hasMessages() { return !messages.isEmpty(); }
    }
}
