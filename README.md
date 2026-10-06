# 🎓 Education Consultancy Management System

A full-stack web application for managing an education consultancy — course catalog, student
subscriptions, and payment tracking — built with **Spring Boot** on the backend and
**Thymeleaf + HTML/CSS/JavaScript** on the frontend (no separate frontend framework required).

This is a redesigned, frontend-added version of the original REST-only backend, kept to a
tech stack of pure Java + HTML/CSS/JS.

---

## 🚀 Features

### Admin
- Log in to an admin dashboard showing total courses, subscriptions, and revenue
- Add new courses (title, description, duration, price)
- View and delete existing courses

### Student
- Register and log in
- Browse available courses
- Subscribe to a course
- Pay for a subscription with Razorpay Checkout in test mode
- View full transaction history

### Razorpay test mode

The app creates Razorpay orders on the server, verifies Checkout signatures, and waits for
signed `payment.captured` / `payment.failed` webhooks to update payment state. Webhook event IDs
are stored so Razorpay retries are processed idempotently. Card details are entered only in
Razorpay Checkout and are never sent to this application.

Configure Razorpay **test-mode** credentials as environment variables before starting Spring Boot:

```powershell
$env:RAZORPAY_KEY_ID = "rzp_test_..."
$env:RAZORPAY_KEY_SECRET = "..."
$env:RAZORPAY_WEBHOOK_SECRET = "..."
mvn spring-boot:run
```

In the Razorpay Dashboard's test-mode webhook settings, configure the URL
`https://<your-public-test-tunnel>/webhooks/razorpay`, subscribe to `payment.captured` and
`payment.failed`, and use the same webhook secret in `RAZORPAY_WEBHOOK_SECRET`. For local
testing, expose port 8081 through a development tunnel such as ngrok and use its HTTPS URL.
Do not commit API or webhook secrets. Without all three settings, the payment page reports
that Razorpay is not configured and does not offer a simulated payment fallback.

---

## ⚙️ Tech Stack

| Layer          | Technology                                   |
|----------------|-----------------------------------------------|
| Language       | Java 17                                       |
| Backend        | Spring Boot 3, Spring MVC                     |
| Frontend       | Thymeleaf, HTML5, CSS3                        |
| Data           | Spring Data JPA + Hibernate                   |
| Database       | H2 in-memory by default; optional MySQL for persistent storage |
| Security       | Spring Security, BCrypt password hashing      |
| Build tool     | Maven                                         |

Everything is written in languages you already know: **Java** for the backend and logic,
**HTML/CSS** for the pages (rendered server-side by Thymeleaf, so no JavaScript framework
or build tooling is required).

---

## 🏗️ Architecture

```
Controller → Service → Repository → Database
```

- **Controller layer** – handles HTTP requests, returns Thymeleaf view names
- **Service layer** – business logic (registration, subscriptions, payments)
- **Repository layer** – Spring Data JPA interfaces
- **Entity layer** – JPA-mapped Java classes (User, Course, Subscription, Payment)

---

## ▶️ How to Run

**Prerequisites:** JDK 17+ and Maven (or use your IDE's built-in Maven support).

1. Open the project folder in IntelliJ IDEA / Eclipse / VS Code as a Maven project
2. Let Maven download dependencies (needs internet access on first run)
3. Run `EducationConsultancyApplication.java`, or from a terminal:
   ```bash
   mvn spring-boot:run
   ```
4. Visit **http://localhost:8081**

The application uses an in-memory H2 database by default, so no database installation or server is
required. Data is kept while the application is running and is recreated when it restarts; demo data is
seeded at startup. The test suite also uses an isolated in-memory H2 database.

For persistent local storage, you can switch to MySQL by editing `src/main/resources/application.properties`:
comment out the active H2 datasource settings and uncomment the labeled MySQL settings. Install and start
MySQL first, then set the MySQL username and password to match your local database account.

Demo data is seeded on startup:

- **Admin login:** `admin@educonsult.com` / `admin123`
- **3 sample courses** are pre-loaded
- Register your own account to try the student flow

No database setup is needed for the default H2 configuration. If you switch to MySQL, keep the server
running before launching the app. Configure Razorpay test-mode variables as described in the Razorpay
section above before attempting checkout.

---

## 🔐 Security

- Passwords are hashed with **BCrypt** before being stored
- Role-based access control: `/admin/**` routes require the `ADMIN` role,
  `/student/**` routes require the `STUDENT` role
- Session-based authentication via Spring Security's form login

---

## 📈 Resume Points

- Built a full-stack education consultancy platform using Spring Boot and Thymeleaf, following a layered architecture (Controller → Service → Repository)
- Designed role-based authentication and authorization (Admin/Student) using Spring Security with BCrypt password hashing
- Modeled relational data (users, courses, subscriptions, payments) using Spring Data JPA and Hibernate
- Implemented core consultancy workflows end-to-end: course management, subscription enrollment, and payment tracking, across both backend logic and a server-rendered frontend
- Delivered a responsive, styled UI using HTML5, CSS3, and Thymeleaf templates without relying on a separate frontend framework

---

## 🔭 Future Enhancements 

- Email notifications on subscription and payment
- Admin ability to edit courses and view enrolled students per course
- REST API layer alongside the web UI for mobile/third-party integration

---

**Author:** Pavishini K
