package com.smartformatter;

import java.sql.Connection;
import java.sql.DriverManager;

public class DBConnection {
    private static final String URL = "jdbc:mysql://localhost:3306/smartformatter_db";
    private static final String USER = "root";   // your XAMPP user
    private static final String PASS = "";       // default empty, or your password

    public static Connection getConnection() throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");
        return DriverManager.getConnection(URL, USER, PASS);
    }
}
