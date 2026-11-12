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

## Follow-up on 2e4047c — remaining alerts (2026-11-12)

This follow-up uses the complete alert list supplied after the first patch. It does not suppress alerts, change alert severities, or claim that a local test run is a CodeQL rescan.

### Dependency maintenance

| Dependabot alert numbers | Component | Managed version before | Selected version |
| --- | --- | --- | --- |
| #1, #2, #10–#17, #20 | Jackson 2 core/databind/modules | BOM 2.21.4 | BOM 2.21.7 |
| #3, #4 | HttpCore / HttpCore HTTP/2 | 5.3.6 | 5.4.3 |
| #6 | HttpClient | 5.5.2 | 5.6.3 |
| #5 | Log4j API and bridge family | 2.24.3 | 2.25.5 |
| #21 | OGNL (via Thymeleaf) | Resolved transitive dependency | 3.4.13 |
| #18, #19 | Spring Framework | 6.2.19 | Unchanged; Boot 4 migration remains necessary |

The Jackson BOM aligns core, databind, datatypes and dataformats on the patched maintenance release. `jackson-annotations` resolves to 2.21 as specified by that BOM; it does not use the same micro-version scheme. HttpClient 5.6.3's own parent specifies HttpCore 5.4.3. OGNL is overridden in dependency management, not added as a new application-facing expression engine. Compatibility checks cover Java time/record JSON round trips and standalone Thymeleaf/OGNL expression evaluation, in addition to the application's existing tests.

These upgrades target the 16 non-Spring dependency alerts in the supplied list. Their closure must be confirmed after GitHub refreshes its dependency graph; newly reported advisories may change the result.

### Application changes

- **#1, clipboard HTML:** a bundled, offline DOMPurify 3.4.17 sanitizes HTML into a detached fragment before the existing CTB text/attribute/table conversion. The raw clipboard string is no longer assigned to `innerHTML` by application code. Only the supported tags and attributes are retained. Missing/unsupported sanitizer fails closed. Node IDs, protected-object IDs and arbitrary image bytes cannot be introduced through clipboard markup. Trusted image-map tokens from the existing server import remain supported. The upstream distribution, license and source/hash reference are included.
- **#6, preference cookie:** `viewMode` now has HttpOnly and SameSite=Lax, and Secure when the servlet request is secure. Native HTTPS and the existing trusted-proxy valve determine this flag; arbitrary forwarded headers are not trusted here. Plain local HTTP still needs a non-Secure preference cookie. This cookie contains only `tree`/`reader`, not authentication data. If CodeQL flags the intentional HTTP branch, evaluate that scope separately from session-cookie policy.
- **#8–#19, export paths:** writers, listing, download and deletion share a single output-folder boundary. It checks normalized containment, single-segment names and no symlink/non-regular destinations. Collection export rejects an existing symlink before opening it as SQLite. Backup names use the same resolver. Downloads use a filesystem resource, not a URL resource. The check is defense against remote path selection and pre-existing symlinks, not an atomic sandbox against an administrator concurrently replacing filesystem entries.
- **#20, CTB picker:** control characters and oversized inputs are rejected before constructing a path. The existing real-path, `.ctb` extension, regular-file and SQLite magic-header checks remain. ADMIN's ability to deliberately select an existing CTB outside `CTBDATA` is required functionality; it is not converted to an arbitrary-file download endpoint or restricted to one fixed path.
- **#21, search query:** filter values remain JDBC parameters. ORDER BY column and direction are now selected from literal switch branches; raw form values are not concatenated into SQL grammar. Tests cover all supported sort choices and malicious direction strings.
- **#22/#23, SSRF:** no general private-IP block is added to tenant configuration, because authorized private-network database connections are intended. The first-pass driver/URL policy, ADMIN authorization and DNS-pinned web-import boundary remain relevant. The alert locations moved into shared helpers; this alone is not proof that the controls failed or that the alerts are false positives. Review the full current dataflow and deployment trust boundary before any dismissal. No CodeQL sanitizer models or broad exclusions are introduced.

### Verification and limits

- Full Java suite and packaging are run on Linux; Windows execution and real MariaDB/MySQL/PostgreSQL endpoints require separate checks. Symlink tests use an explicit assumption only when the filesystem/permissions cannot create symlinks (for example an unprivileged Windows setup).
- The JavaScript suite includes fail-closed sanitizer and import-token checks.
- A real Chromium check exercised malicious script/event/SVG/MathML/template markup, remote image/style URLs, headings, bold text, links, tables and trusted image tokens. It observed no script execution, no HTML attached to the live document and no resource requests during parsing for these fixtures. These observations are not a guarantee for every possible browser input.
- The actual Maven dependency tree is checked for all selected versions; a Spring Boot context test and application test suite still run on Boot 3.5.16.
- Before further migration, manually try rich-text paste and webpage import, CTB export/download/delete, database selection/creation, and view switching over the transport modes you use.

### Additional primary references

- https://github.com/FasterXML/jackson-databind/security/advisories/GHSA-cxp5-3px4-pw24
- https://github.com/FasterXML/jackson-databind/security/advisories/GHSA-wv8q-qhhj-9h54
- https://github.com/FasterXML/jackson-core/security/advisories/GHSA-7hhh-6rmp-j9qf
- https://github.com/advisories/GHSA-v3jc-474w-2wm6
- https://github.com/advisories/GHSA-hf6x-8p5f-cgmf
- https://github.com/advisories/GHSA-hjcp-jmpx-g3qm
- https://github.com/advisories/GHSA-qv9r-c865-cp47
- https://github.com/orphan-oss/ognl/releases/tag/OGNL_3_4_13
- https://github.com/cure53/DOMPurify/releases/tag/3.4.17
