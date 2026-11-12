# Security alert review — first hardening pass

Reviewed against `996332e` on 2026-11-12. This is a targeted hardening patch, not a declaration that all CodeQL or Dependabot alerts have been resolved. No alerts are suppressed or dismissed by this patch.

## Changes and verification

| Area | Change | Verification |
| --- | --- | --- |
| Application YAML settings | Explicit SnakeYAML `SafeConstructor`, denial of custom global/local tags, and a nesting limit of 50. Composition still edits scalar ranges in the original text, preserving comments and unrelated settings. | Malicious/class tags, aliases, duplicate keys, multiple documents, excessive nesting, normal scalar types and stale-file checks. |
| URL content import | Isolated the checked, immutable DNS pin from the single-hop HTTP transport. Every redirect is revalidated and resolved again; socket resolution uses only that hop's checked addresses. Existing scheme, port, response size, time and request-budget limits remain. | Mixed public/private DNS answers, IPv4/IPv6 restrictions, mutation of resolver arrays, DNS rebinding, same-host redirects, private redirects and redirect limit. Tests use fake DNS/transport; no requests to third-party services or internal endpoints. |
| Tenant JDBC registration | Restrict driver classes to the four supported SQLite/PostgreSQL/MariaDB/MySQL drivers and require the matching JDBC prefix. Recheck before closing an old pool or persisting a replacement, including configuration reloads. Reject SQLite's remote `:resource:` form. | Invalid driver/URL pairs fail before replacing a registration; localhost, private-network and IPv6 database endpoints remain supported. |
| Empty CTB and CTB export | A shared helper accepts a `Path` and invokes the SQLite driver directly. Paths containing `?`, `#` or control characters cannot become JDBC options. Export filenames replace `#` in tenant names with `_`. | Real SQLite file creation, reopening, Unicode paths, no-create mode and invalid-path rejection. Existing wizard/export tests remain applicable. |
| Embedded Tomcat | Override the Boot-managed version from 10.1.55 to 10.1.61 for all managed Tomcat components. | Verify the resolved dependency tree and run the application tests and packaging. |

Connection configuration is an ADMIN capability. Deliberate connections to a database on localhost or a private network are supported behavior, not the same policy as fetching an arbitrary webpage. Legacy configurations using unsupported JDBC drivers or SQLite `:resource:` URLs must be replaced with supported CTB/database connections; skipped configurations must be reviewed in the application log. Existing SQLite file URIs and options in trusted tenant configurations remain supported; generated CTB/export connections use only plain file paths.

## Alert disposition

- CodeQL #7: explicit safe construction and tag policy harden the existing composition-only YAML path. Rerun CodeQL before deciding the alert's status.
- CodeQL #5: custom SSRF controls may not be recognized by static analysis. Evaluate the full source-to-sink trace against the DNS pin, redirect checks and tests; do not add blanket suppression just to clear the alert.
- CodeQL #2/#3: generated connections select SQLite explicitly and use validated file paths. If an alert remains, retain evidence of the fixed driver and local-file boundary when reviewing it.
- CodeQL #4: configured database endpoints require ADMIN and may intentionally be private. Review the trust boundary as well as the allowed driver/URL pairing; do not block all private database addresses to silence this alert.
- Dependabot: resubmit the dependency graph after updating `pom.xml`. This patch addresses the Tomcat version, not every reported dependency.

## Remaining work: supported Spring generation

Spring Boot 3.5.16 manages Spring Framework 6.2.19. Boot 3.5.16 is the final OSS release of its generation. The Spring advisories for XsltView (CVE-2026-47884) and SSE fragments (CVE-2026-47890) list Framework 6.2.20 as Enterprise-only and 7.0.9 as an OSS fixed version. Updating Tomcat does not fix these Framework advisories.

The reviewed application has no XsltView/XsltViewResolver or SSE implementation in `src/main`. This is an initial applicability observation, not a guarantee covering future changes or other Spring advisories. Plan a separately tested Boot 4 migration; do not combine Boot 3 with arbitrary Spring 7 module overrides. Boot 4 supports Java 17, but its Spring, Security, Hibernate, Servlet and test integration changes need their own compatibility checks.

## After applying the patch

1. Run `mvnw.cmd clean package` and `node --test src/test/js/*.test.cjs` (Node.js required for the latter).
2. Run `mvnw.cmd -Preports compile`. In `target/dependency-tree.txt`, verify `tomcat-embed-core`, `tomcat-embed-el` and `tomcat-embed-websocket` resolve to 10.1.61.
3. Smoke-test settings save, opening/creating a CTB, CTB export, and importing a public webpage. Verify invalid imports fail without changing the note.
4. Where used, test real PostgreSQL and MariaDB/MySQL connections. SQLite tests and URL-policy tests do not verify remote server compatibility.
5. Push the changes, wait for CodeQL and dependency submission, then review remaining alerts. Do not mark unresolved Spring alerts as fixed by this patch.

## Primary references

- https://tomcat.apache.org/security-10.html
- https://spring.io/blog/2026/06/25/spring-boot-3-5-16-available-now/
- https://spring.io/security/cve-2026-47884/
- https://spring.io/security/cve-2026-47890/
- https://docs.spring.io/spring-boot/4.0/system-requirements.html
- https://codeql.github.com/codeql-query-help/java/java-unsafe-deserialization/
- https://codeql.github.com/codeql-query-help/java/java-ssrf/
