# 🧠 SmartFormatter Pro

**SmartFormatter Pro** is a professional desktop Java application that automatically generates **IEEE-style research paper PDFs**.  
It supports double-column formatting, auto page numbering, tables, equations, and secure user login via **MySQL (XAMPP)**.

---

## 🚀 Features

✅ **IEEE-Style PDF Generation**  
- Single or double column layout  
- Proper title, author, abstract, and keywords  
- Automatic page numbering  
- Smart table & image placement  
- Perfect margins and spacing  

✅ **Tables & Figures**  
- Automatically splits wide tables after 5 columns  
- Maintains spacing before and after each table  
- Inline images with automatic scaling & captions  

✅ **Equations**  
- Add complex equations with `$E = mc^2$` syntax  
- Large, centered, and bold formatting  

✅ **User Authentication**  
- Secure registration & login  
- Password hashing (SHA-256)  
- MySQL-based credential storage  

✅ **Modern UI**  
- Minimal notepad-style writing area  
- Buttons for image, table, and PDF export  
- Clean login & registration screens  

---

## 🧰 Tech Stack

| Component | Technology |
|------------|-------------|
| Language | Java 17+ |
| UI | Java Swing |
| PDF Engine | OpenPDF (`com.lowagie.text.*`) |
| Database | MySQL (via XAMPP) |
| JDBC Driver | MySQL Connector/J 8.3+ |
| IDE | NetBeans / IntelliJ IDEA |

---

## ⚙️ Setup Instructions

### 1️⃣ Install Requirements
- **Java JDK 17+**  
- **XAMPP** (Apache + MySQL)  
- **NetBeans IDE** or **IntelliJ**  
- **MySQL Connector/J 8.3+** → [Download Here](https://dev.mysql.com/downloads/connector/j/)  
- **OpenPDF 1.3.x** JAR

---

### 2️⃣ Create the Database
Open [phpMyAdmin](http://localhost/phpmyadmin) and run:

```sql
CREATE DATABASE IF NOT EXISTS smartformatter_db;
USE smartformatter_db;

CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(256) NOT NULL,
    email VARCHAR(100),
    full_name VARCHAR(150),
    institution VARCHAR(200),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
