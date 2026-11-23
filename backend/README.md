# LibraryHub Spring Boot API

Backend for LibraryHub using Java 21, Spring Boot, Spring Security and MongoDB Atlas.

## Included

- MongoDB Atlas persistence (no local MongoDB server required)
- Member registration and BCrypt password hashing
- Email OTP for registration and every login
- OTP expiry and attempt limits
- Password reset by OTP
- Secure HttpOnly authentication cookie
- Admin, Librarian and Member roles
- Protected Book, Member and Loan APIs
- Admin-only staff account creation
- Book links for permitted online/PDF reading
- Book reservations with waiting and ready states
- Reviews, star ratings and category-based recommendations
- Per-user reading progress
- Physical-copy counts, barcode values and availability tracking
- Automatic overdue fine calculation
- Validation and structured API errors

## Required software

- Java 21
- Maven 3.9+
- A MongoDB Atlas cluster
- An SMTP email account for real OTP delivery

## Email delivery

Registration, sign-in, password reset, due-date reminders and ready-reservation notices use SMTP. The default configuration does not send real email. Set `MAIL_ENABLED=true`, `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME` (also used as the sender if `MAIL_FROM` is blank), optional `MAIL_FROM`, `MAIL_PASSWORD`, `MAIL_SMTP_AUTH=true`, `MAIL_STARTTLS=true`, and `SHOW_DEV_OTP=false` in the **backend environment**. Obtain these values from an SMTP provider. Do not put SMTP credentials in the frontend or commit them.

A successful SMTP submission means the server accepted the message; actual inbox delivery can still depend on provider verification, spam filtering, and DNS authentication. Test registration and login with a real mailbox. If SMTP fails during registration, the API reports a delivery error rather than claiming the message was sent. With mail disabled, registration and login require `SHOW_DEV_OTP=true` for local testing.

## Configuration

Set these in PowerShell before starting the backend:

```powershell
$env:MONGODB_URI="mongodb+srv://USERNAME:PASSWORD@YOUR-CLUSTER.mongodb.net/libraryhub?retryWrites=true&w=majority"
$env:JWT_SECRET="replace-this-with-a-long-random-secret"
$env:FRONTEND_URL="http://localhost:5173"
$env:ADMIN_NAME="LibraryHub Administrator"
$env:ADMIN_EMAIL="admin@example.com"
$env:ADMIN_PASSWORD="ChangeMe123!"
```

The first administrator is created only when the users collection does not already contain that email. Members cannot create Admin accounts from the registration page.

### Real email OTP

```powershell
$env:MAIL_ENABLED="true"
$env:MAIL_HOST="smtp.your-provider.com"
$env:MAIL_PORT="587"
$env:MAIL_USERNAME="your-smtp-username"
$env:MAIL_PASSWORD="your-smtp-password"
$env:MAIL_FROM="no-reply@your-domain.com"
$env:MAIL_SMTP_AUTH="true"
$env:MAIL_STARTTLS="true"
```

For development without an email provider:

```powershell
$env:MAIL_ENABLED="false"
$env:SHOW_DEV_OTP="true"
```

Development OTPs will then be returned to the frontend. Never enable `SHOW_DEV_OTP` in production.

## Run

```powershell
mvn spring-boot:run
```

The API runs at `http://localhost:8080`.

## Main endpoints

| Method | Endpoint | Access |
| --- | --- | --- |
| POST | `/api/auth/register` | Public |
| POST | `/api/auth/login` | Public |
| POST | `/api/auth/verify-otp` | Public |
| POST | `/api/auth/resend-otp` | Public |
| POST | `/api/auth/forgot-password` | Public |
| POST | `/api/auth/reset-password` | Public |
| GET | `/api/auth/me` | Logged-in users |
| POST | `/api/auth/logout` | Logged-in users |
| GET | `/api/books` | Logged-in users |
| POST/PUT | `/api/books` | Admin or Librarian |
| DELETE | `/api/books/{id}` | Admin |
| Any | `/api/members/**` | Admin or Librarian |
| Any | `/api/loans/**` | Admin or Librarian |
| Any | `/api/admin/**` | Admin only |
| Any | `/api/my-library/reservations/**` | Logged-in users |
| Any | `/api/my-library/reviews/**` | Logged-in users |
| Any | `/api/my-library/progress/**` | Logged-in users |
| GET | `/api/my-library/recommendations` | Logged-in users |
| GET | `/api/my-library/loans` | Member profile |

## Atlas checklist

1. Create an Atlas database user.
2. Add the backend computer/server IP to Atlas Network Access.
3. Copy the Atlas Java connection string.
4. Replace its username and password.
5. Store it in `MONGODB_URI`.
6. Never put the Atlas URI in the React project.
