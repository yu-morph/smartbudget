# SmartBudget Agent Guide

## Project scope

SmartBudget is an Android personal-finance app built with Kotlin and Jetpack Compose. It combines manual entry, receipt OCR, and financial-data import with monthly budgets and AI-assisted spending reports. Keep the client usable on its own while backend features are still being implemented.

## Repository map

- `app/src/main/java/com/yu_morph/smartbudget`: application and Compose UI code.
- `app/src/test`: local JVM tests.
- `app/src/androidTest`: device and emulator tests.
- `knowledges/design/proposal.docx`: product scope, user flows, architecture, and technology choices.
- `knowledges/design/ui-mockups`: screen and interaction references. Match the intended flow as well as the static appearance.
- `knowledges/api/openapi.json`: target backend contract and the source of truth for paths and schemas.
- `knowledges/api/authentication.md`: authentication, validation, token, and account-management details.
- `knowledges/api/ocr.md`: receipt OCR contract and implementation decisions.
- `knowledges/api/azure-deployment-design.md`: development server, deployment, and version-check information.
- `knowledges/mock-api`: temporary fixed-transaction API documentation. Do not treat this API as the production contract.

## Working rules

- Read the relevant documents under `knowledges` before changing a screen, domain model, or API integration.
- When documents conflict, follow `knowledges/api/openapi.json` for the target API contract. Treat the running server's `/openapi.json` and `/docs` as the authority for what is implemented today.
- Keep target-contract support separate from temporary compatibility code. Do not silently replace target `/api/v1` endpoints with the mock API.
- Use Kotlin and Jetpack Compose for UI. Follow unidirectional data flow: composables render immutable state and emit events; networking and persistence stay outside composables.
- Prefer small, feature-oriented packages and interfaces at external boundaries. Keep DTOs, domain models, and UI models distinct when their shapes or lifecycles differ.
- Put user-visible text in Android string resources. Preserve accessibility semantics, touch-target sizes, loading states, empty states, and actionable error states.
- Never commit secrets, tokens, API keys, local database paths, `.env` files, or production financial data. Do not log passwords, JWTs, receipt images, or full API payloads containing personal data.
- Add or update tests with behavior changes. Include failure paths and ownership/authentication cases for API-facing code.
- Before finishing, run the repository verification harness described below and resolve failures caused by the change.

## Backend API integration reference

### Contract and environment

- Target API prefix: `/api/v1`.
- Development server documented in `knowledges/api/azure-deployment-design.md`: `https://smartbudget-dev-kc-260925.koreacentral.cloudapp.azure.com`.
- Check `GET /api/v1/version` before diagnosing an apparent contract mismatch. The root path returning 404 is not a health-check failure.
- Do not hardcode the base URL in feature code. Supply it through build configuration or dependency injection so local, development, and test servers can be selected independently.
- API implementation is incremental. Authentication and version endpoints are documented as implemented; transaction, monthly-budget, report, and OCR endpoints may still be stubs. Confirm the running server contract before relying on them.

### HTTP and response handling

- Send JSON for `POST`, `PUT`, and `PATCH` unless an endpoint explicitly specifies multipart data.
- The standard API envelope contains `status`, stable English `code`, English `message`, and `data`. Branch application behavior on `code`, not `message`.
- Model error bodies as normal API responses even when the HTTP status is non-2xx. Preserve the status and stable code for UI mapping and diagnostics.
- Expect `400 INVALID_REQUEST`, authentication-related `401` codes, `409 USERNAME_CONFLICT`, `429 SIGN_IN_RATE_LIMITED`, and `503 SERVICE_UNAVAILABLE` where documented.
- Honor `Retry-After` on sign-in rate limits. Do not automatically retry authentication, mutation, OCR, or report-generation requests unless the operation is proven idempotent.
- Use ISO 8601 `YYYY-MM-DD` for dates, `HH:mm:ss` for optional transaction times, and an offset-bearing timestamp such as `YYYY-MM-DDTHH:mm:ss+09:00` for instants.

### Authentication and ownership

- Send access tokens only as `Authorization: Bearer <JWT>` for protected endpoints. The refresh endpoint is the exception: it accepts the token in its JSON body and rejects an Authorization header.
- Store JWTs in secure platform storage. Clear local credentials on logout or terminal authentication failure; remember that local deletion does not revoke a server token.
- A password change increments the server token version and invalidates existing JWTs. Return the user to sign-in after a successful password change.
- Token refresh is recommended only when the token is at least one day old and has no more than six hours remaining. An old token remains valid until its original expiry unless otherwise invalidated.
- Never accept or send another user's identifier for account-scoped operations. Transaction, budget, report, and account data must always remain scoped to the authenticated user.

### Feature endpoints

- Authentication: `/api/v1/auth/sign-up`, `/sign-in`, `/refresh`, and `/account` as defined in `authentication.md`.
- Transactions: list/create at `/api/v1/transaction`; detail/update/delete at `/api/v1/transaction/{id}`.
- Monthly budgets: get or replace `/api/v1/monthly-budget/{month}`.
- Reports: list/request at `/api/v1/report`; detail/delete at `/api/v1/report/{id}`.
- Receipt OCR: proxy through `/api/v1/proxy/ocr`. Validate image type and size, let the user review and correct extracted values, and only then create a transaction.
- Never send identifying fields to an LLM from the client. The backend must calculate statistics and remove identifying data before analysis.

## Build and verification

Run Gradle with the checked-in wrapper from the repository root.

```powershell
.\gradlew.bat test
.\gradlew.bat ktlintCheck detekt
.\gradlew.bat ktlintFormat
```

Use the repository's final verification harness before handing off work. Formatting may change files, so review the resulting diff and repeat checks after formatting when necessary.

