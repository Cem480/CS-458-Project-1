# ⚔ Project Ares — Setup Guide

> CS458 Software Verification & Validation — Spring 2025-2026  
> Bilkent University

---

## Table of Contents

1. [Architecture Overview](#1-architecture-overview)
2. [Prerequisites](#2-prerequisites)
3. [Environment Variables — Create These Files](#3-environment-variables--create-these-files)
4. [Running the Backend](#4-running-the-backend)
5. [Running the Frontend](#5-running-the-frontend)
6. [OAuth Configuration (Google & Facebook)](#6-oauth-configuration-google--facebook)
7. [Test User Accounts](#7-test-user-accounts)
8. [Running the Selenium Tests](#8-running-the-selenium-tests)
9. [Troubleshooting](#9-troubleshooting)
10. [Quick Start Checklist](#10-quick-start-checklist)

---

## 1. Architecture Overview

Three components must run simultaneously:

| Component | Technology       | Port | Start Command        |
|-----------|-----------------|------|----------------------|
| Frontend  | React 18         | 3000 | `npm start`          |
| Backend   | Spring Boot 3.2  | 8080 | `mvn spring-boot:run`|
| Database  | MongoDB Atlas    | cloud | *(no local start)*  |

The database is hosted on **MongoDB Atlas** (cloud). No local MongoDB installation is needed — just the connection string in the backend config.

---

## 2. Prerequisites

Install everything below before running the project.

| Tool          | Required Version        | Check Command          | Download                          |
|---------------|------------------------|------------------------|-----------------------------------|
| Java JDK      | **17 or 21**           | `java -version`        | https://adoptium.net              |
| Apache Maven  | **3.8+**               | `mvn -version`         | https://maven.apache.org          |
| Node.js       | **18 or 20 LTS**       | `node --version`       | https://nodejs.org                |
| npm           | **9+** (comes w/ Node) | `npm --version`        | bundled with Node.js              |
| Git           | any recent             | `git --version`        | https://git-scm.com               |
| Chrome        | latest                 | —                      | https://www.google.com/chrome     |
| ChromeDriver  | **must match Chrome**  | `chromedriver --version` | https://chromedriver.chromium.org |

> ⚠️ **ChromeDriver must exactly match your Chrome version.**  
> Check your Chrome version at `chrome://settings/help`, then download the matching ChromeDriver.

---

## 3. Environment Variables — Create These Files

The `.env` files have been removed from the repository. You must create them yourself before the project will run.

---

### 3A. Frontend — `ares-frontend/.env`

Create a file called `.env` inside the `ares-frontend/` folder with this content:

```env
REACT_APP_GOOGLE_CLIENT_ID=your_google_client_id_here
REACT_APP_FACEBOOK_APP_ID=1014667905062807
REACT_APP_API_URL=http://localhost:8080
```

**Where to get `REACT_APP_GOOGLE_CLIENT_ID`:**

1. Go to https://console.cloud.google.com
2. APIs & Services → Credentials
3. Click the OAuth 2.0 Client ID for this project
4. Copy the **Client ID** — it looks like `123456789-abc...apps.googleusercontent.com`
5. Paste it as the value for `REACT_APP_GOOGLE_CLIENT_ID`

> ℹ️ `REACT_APP_FACEBOOK_APP_ID` is already filled in above — that is the App ID for this project.

---

### 3B. Backend — `ares-backend/src/main/resources/application.properties`

Open (or create) `application.properties` and fill in the values below.  
Lines starting with `#` are comments — do not change them.

```properties
# ── Server ────────────────────────────────────────────────────────
server.port=8080

# ── MongoDB Atlas ─────────────────────────────────────────────────
# Get this from: MongoDB Atlas → your cluster → Connect → Drivers
# Format: mongodb+srv://<user>:<password>@<cluster>.mongodb.net/<dbname>
spring.data.mongodb.uri=mongodb+srv://YOUR_USER:YOUR_PASSWORD@YOUR_CLUSTER.mongodb.net/aresdb

# ── JWT ───────────────────────────────────────────────────────────
# Any long random string (min 32 characters). Example below is safe to use.
jwt.secret=AresProjectSecretKey2026BilkentCS458SuperSecureRandomString
jwt.expiration=86400000

# ── Claude API (Anthropic) ────────────────────────────────────────
# Get this from: https://console.anthropic.com → API Keys → Create Key
anthropic.api.key=sk-ant-YOUR_KEY_HERE
anthropic.model=claude-haiku-4-5-20251001

# ── Google OAuth ──────────────────────────────────────────────────
# Same Client ID as the frontend .env above
google.client.id=your_google_client_id_here

# ── Facebook OAuth ────────────────────────────────────────────────
# App ID and Secret from: https://developers.facebook.com/apps → your app → App Settings → Basic
facebook.app.id=1014667905062807
facebook.app.secret=YOUR_FACEBOOK_APP_SECRET_HERE

# ── Risk Scoring Threshold ────────────────────────────────────────
risk.score.llm.threshold=70
```

**Where to get each value:**

| Variable | Where to find it |
|----------|-----------------|
| `spring.data.mongodb.uri` | MongoDB Atlas → your cluster → Connect → Connect your application → copy the URI, replace `<password>` with the actual password |
| `jwt.secret` | Any random string you choose — the one above works fine |
| `anthropic.api.key` | https://console.anthropic.com → API Keys → Create new key |
| `google.client.id` | Google Cloud Console → APIs & Services → Credentials → your OAuth 2.0 Client ID |
| `facebook.app.id` | Already filled in: `1014667905062807` |
| `facebook.app.secret` | https://developers.facebook.com/apps → select app → App Settings → Basic → App Secret (click Show) |

---

### 3C. Backend — `ares-backend/src/main/resources/application.properties` (test override)

If there is a separate `application-test.properties` file, it may need the same MongoDB URI. Copy the same values there if the file exists.

---

## 4. Running the Backend

```bash
# 1. Navigate to the backend folder (where pom.xml is)
cd ares-backend

# 2. Download dependencies and compile (first run only — takes ~2 min)
mvn clean install -DskipTests

# 3. Start the server
mvn spring-boot:run
```

Wait until you see a line like:
```
Started AresApplication in 4.2 seconds
```

**Verify it works:**

```bash
curl http://localhost:8080/api/auth/health
```

Expected response:
```json
{ "status": "UP", "message": "Ares Auth Service is running" }
```

Or open `http://localhost:8080/api/auth/health` in a browser — you should see the JSON above.

---

## 5. Running the Frontend

Open a **new terminal** (keep the backend running in the first one).

```bash
# 1. Navigate to the frontend folder (where package.json is)
cd ares-frontend

# 2. Install dependencies (first run only)
npm install

# 3. Start the dev server
npm start
```

The browser will open automatically at `http://localhost:3000`.  
You should see the Project Ares login page.

> ⚠️ **Do not change the port from 3000.**  
> Google and Facebook OAuth are configured to allow `localhost:3000` only. Running on a different port will break OAuth login.

---

## 6. OAuth Configuration (Google & Facebook)

This step is required if you see errors like:

- `The given origin is not allowed for the given client ID` (Google)
- Facebook login button does nothing / shows an error

These errors mean the OAuth providers don't know that `localhost:3000` is an allowed origin. This is a **one-time cloud console configuration** — no code change needed.

---

### 6A. Fix Google — Google Cloud Console

1. Go to https://console.cloud.google.com
2. Left sidebar → **APIs & Services** → **Credentials**
3. Click the **OAuth 2.0 Client ID** for this project
4. Under **Authorised JavaScript origins** → click **+ ADD URI** → enter:
   ```
   http://localhost:3000
   ```
5. Under **Authorised redirect URIs** → click **+ ADD URI** → enter:
   ```
   http://localhost:3000
   ```
6. Click **SAVE**
7. Wait ~5 minutes for the change to propagate
8. Hard-refresh the browser: `Ctrl+Shift+R` (Windows/Linux) or `Cmd+Shift+R` (Mac)

---

### 6B. Fix Facebook — Facebook Developer Console

1. Go to https://developers.facebook.com/apps
2. Select the **Ares** app (App ID: `1014667905062807`)
3. Left sidebar → **Facebook Login** → **Settings**
4. In **Valid OAuth Redirect URIs** add:
   ```
   http://localhost:3000
   ```
5. Left sidebar → **App Settings** → **Basic**
6. In **App Domains** add:
   ```
   localhost
   ```
7. Make sure the toggle at the very top of the page shows **"In development"** — not "Live". If it says Live, switch it back to development mode.
8. Click **Save Changes**, then hard-refresh the browser.

> ⚠️ **Clearing the MongoDB database does not fix OAuth errors.**  
> The Facebook button sends an access token to the backend which verifies it directly with Facebook's API — this path does not touch the database at all.

---

## 7. Test User Accounts

These accounts are pre-loaded in MongoDB Atlas. No registration needed.

| Email | Password | Provider | Status | Used In |
|-------|----------|----------|--------|---------|
| `selenium@ares.com` | `selenium123` | LOCAL | ACTIVE | Scenarios 1, 2, 3 |
| `ratetest@ares.com` | `ratetest123` | LOCAL | ACTIVE | Scenario 5 — rate limit |
| `lockrate@ares.com` | `lock123` | LOCAL | LOCKED | Scenario 5 — lockout |
| `googleuser@gmail.com` | *(Google OAuth)* | GOOGLE | ACTIVE | Scenario 4 |
| `fbuser@facebook.com` | *(Facebook OAuth)* | FACEBOOK | ACTIVE | Scenario 4 |
| `nofb@ares.com` | `nofb123` | LOCAL | ACTIVE | Scenario 4 — non-Facebook user |

> ℹ️ If `lockrate@ares.com` is still locked from a previous test run, it auto-unlocks after 30 minutes.  
> Or call: `POST http://localhost:8080/api/auth/admin/unlock` with body `{ "email": "lockrate@ares.com" }`

---

## 8. Running the Selenium Tests

> ⚠️ **Both the frontend (port 3000) and backend (port 8080) must be running before starting tests.**

Open a **third terminal**:

```bash
# Navigate to the backend project (tests live here with Maven)
cd ares-backend

# Run all tests
mvn test

# Or run a specific scenario
mvn test -Dtest=DynamicIdRecoveryTest    # Scenario 1 — Dynamic ID Recovery
mvn test -Dtest=MultimodalFailureTest    # Scenario 2 — Multimodal Failure
mvn test -Dtest=CrossBrowserTest         # Scenario 3 — Cross-Browser
mvn test -Dtest=OAuthHandshakeTest       # Scenario 4 — OAuth Handshake
mvn test -Dtest=RateLimitTest            # Scenario 5 — Rate Limiting
```

**Expected output:**

```
[INFO] Tests run: 24, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

**Test logs:**

- Healing activity is written to `target/healing-report.log`
- Healed selectors are cached in `selector-cache.json` — delete this file between runs to force fresh healing

---

## 9. Troubleshooting

| Error / Symptom | Fix |
|----------------|-----|
| `origin not allowed` in browser console | Follow Section 6 — add `localhost:3000` in Google Cloud Console |
| Facebook button does nothing | App must be in **Development Mode** (Section 6B step 7) |
| Backend crashes on startup | Check `java -version` — must be 17 or 21. Check MongoDB URI in `application.properties` |
| `mvn` not found | Maven is not installed or not on your PATH — see Section 2 |
| `npm install` fails | Run `npm cache clean --force` then `npm install` again. Node.js must be 18+ |
| Selenium: `ChromeDriver` error | ChromeDriver version must match Chrome exactly — check `chrome://settings/help` |
| Selenium: element not found after healing | Delete `selector-cache.json` and run again — stale cache entry |
| Port 8080 already in use | Mac/Linux: `lsof -ti:8080 \| xargs kill` — Windows: `netstat -ano \| findstr 8080` then `taskkill /PID <pid> /F` |
| Port 3000 already in use | Mac/Linux: `lsof -ti:3000 \| xargs kill` — Windows: same pattern with 3000 |
| CORS error in browser | Backend must be on port 8080. Frontend proxy in `package.json` points to `localhost:8080` |
| `lockrate@ares.com` permanently locked | Wait 30 min for auto-unlock, or POST to `/api/auth/admin/unlock` |
| MongoDB connection refused | Check the URI in `application.properties` — password must not contain unescaped special characters (`@`, `#`, etc.) |
| `anthropic.api.key` invalid | Create a new key at https://console.anthropic.com — keys start with `sk-ant-` |

---

## 10. Quick Start Checklist

Use this to get running from scratch:

```
[ ] Java 17+      →  java -version
[ ] Maven 3.8+    →  mvn -version
[ ] Node.js 18+   →  node --version
[ ] ChromeDriver installed and matches Chrome version

[ ] ares-frontend/.env  created with REACT_APP_GOOGLE_CLIENT_ID filled in
[ ] ares-backend/src/main/resources/application.properties  filled in:
      spring.data.mongodb.uri   → your Atlas connection string
      anthropic.api.key         → your Claude API key
      google.client.id          → your Google OAuth client ID
      facebook.app.secret       → your Facebook app secret

[ ] Google Cloud Console: localhost:3000 added to Authorised JavaScript Origins
[ ] Facebook Developer Console: localhost:3000 added to OAuth Redirect URIs
[ ] Facebook app is in Development Mode (not Live)

[ ] Terminal 1:  cd ares-backend  &&  mvn spring-boot:run
[ ] http://localhost:8080/api/auth/health  →  { "status": "UP" }

[ ] Terminal 2:  cd ares-frontend  &&  npm install  &&  npm start
[ ] http://localhost:3000  →  login page appears

[ ] Login with  selenium@ares.com / selenium123  →  dashboard appears
[ ] Google login works without console errors
[ ] Facebook login works without console errors

[ ] Terminal 3 (tests):  cd ares-backend  &&  mvn test
[ ] Result:  Tests run: 24, Failures: 0, Errors: 0  —  BUILD SUCCESS
```

---

*Project Ares — CS458 Spring 2025-2026 — Bilkent University*
