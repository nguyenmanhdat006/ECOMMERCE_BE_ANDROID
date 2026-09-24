Postman collection and sample users for User CRUD testing

How to use

1. Start the backend application (make sure env vars for DB and JWT are set). Default base URL: http://localhost:8080

2. Import `User-CRUD-collection.json` into Postman.

3. Use `sample-users.json` for registration payloads. For each user:
   - Send POST /api/auth/register with a sample user body.
   - The server sends an email with a verification code. If you don't have an SMTP configured for local testing, you can:
     - View the verification code by adding a small log in `RegistrationService.createUser` (it already generates a code and calls `emailService.sendMail(user)`); or
     - Directly query the DB `users` table `verification_code` column for the user.

4. Call POST /api/auth/verify with `{ "userName": "<email>", "code": "<code>" }` to verify.

5. Call POST /api/auth/login with `{ "userName": "<email>", "password": "<password>" }`. Copy returned token.

6. Set `{{jwt_token}}` or `{{admin_jwt}}` in Postman environment to the returned token.

Admin CRUD

- Admin endpoints are under `/api/admin/users` and require an authenticated admin token. Create or use an admin account and log in to obtain `admin_jwt`.

Notes

- If you want to avoid email verification for tests, you can temporarily set `user.setEnabled(true)` in `RegistrationService.createUser` after saving, or directly update the DB to set `enabled = true`.

