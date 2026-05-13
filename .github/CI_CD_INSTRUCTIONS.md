# Pakpay — Maven + GitHub Actions (CI/CD)

This repository’s Git root is **`pakpay/`** (parent folder). The Spring Boot app and `pom.xml` live in **`pakpay/pakpay/`** (child folder). Workflows use `working-directory: pakpay` and `cache-dependency-path: pakpay/pom.xml` so paths stay correct.

## What the workflow does

| Stage | When it runs | What happens |
|--------|----------------|----------------|
| **CI — Build and test** | Every push/PR to `main`, `master`, or `develop`, and on **Run workflow** (manual) | Checkout → JDK **17** (Temurin) → Maven cache → **`./mvnw -B -ntp verify`** → upload **`target/*.jar`** as artifact `pakpay-jar`. |
| **CD — GitHub Release** | Push a **Git tag** matching `v*.*.*` (e.g. `v1.0.0`) | After the same build/tests succeed, attaches the JAR to a **GitHub Release** for that tag (auto release notes). |

**Why MySQL in CI?** Your `application.properties` points at MySQL and `@SpringBootTest` loads the full context, so CI starts a **MySQL 8** service container and sets `SPRING_DATASOURCE_*` env vars so tests do not need code changes.

## One-time GitHub setup

1. Push this repo to GitHub (if it is not already remote-connected).
2. Confirm default branch name (`main` vs `master`) matches the `branches:` lists in `.github/workflows/maven-ci-cd.yml` (edit if your default branch differs).
3. No extra secrets are required for the default flow: **`GITHUB_TOKEN`** is used for releases.

## Daily developer flow

1. Create a branch, commit, open a **pull request** → Actions runs **build** (verify + artifact).
2. Merge to your default branch → same pipeline runs on push.

## Release flow (CD)

1. Update version in `pakpay/pom.xml` if you publish non-SNAPSHOT artifacts (optional; you can still tag while on `0.0.1-SNAPSHOT`).
2. Commit and push.
3. Create and push an annotated or lightweight tag, for example:
   - `git tag v1.0.0`
   - `git push origin v1.0.0`
4. Open **Releases** on GitHub: the workflow creates/updates the release and uploads the runnable JAR.

## Local parity with CI

- **Java:** JDK **17** (matches `<java.version>17</java.version>` in `pom.xml`).
- **Maven:** Prefer **`pakpay/mvnw`** so you use the same Maven version as CI (wrapper uses **3.9.15** per `.mvn/wrapper/maven-wrapper.properties`).
- **MySQL:** Run MySQL 8 locally with database `pakpay_db`, or adjust `application.properties` / env vars the same way as in the workflow (`SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`).

Example local verify (from repo root, with MySQL already running and credentials matching your config):

```bash
cd pakpay
chmod +x mvnw   # on Linux/macOS
./mvnw -B verify
```

On Windows PowerShell from `pakpay`:

```powershell
.\mvnw.cmd -B verify
```

## Optional next steps (not in repo by default)

- **Deploy to a server:** Add a job after `build` (or after `release`) that SSH/rsyncs the artifact or runs `java -jar` behind systemd, using **GitHub Environments** and **secrets** (host, key, etc.).
- **Docker:** Add a `Dockerfile` in `pakpay/` and a second workflow that builds/pushes to GHCR on tag or `main`.
- **Dependabot:** Add `.github/dependabot.yml` for Maven and GitHub Actions version bumps.

## Troubleshooting

- **Tests fail on DB connection:** Ensure MySQL is up before tests; in Actions, the job waits for the service health check. Locally, match URL/user/password.
- **Workflow not triggered:** Check that pushes target a branch listed under `on.push.branches` or that tags match `v*.*.*`.
- **Release job skipped:** Tags must look like `v1.2.3` (leading `v` + semver-style); adjust the `if:` in the workflow if you prefer another pattern.
