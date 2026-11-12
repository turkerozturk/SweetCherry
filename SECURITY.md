# Security Policy

## Supported Versions

Security maintenance focuses on the latest stable release and the current `main` branch, on a best-effort basis.

| Version | Security maintenance |
| --- | --- |
| Latest stable release | Reports are accepted. Fixes may be delivered in a newer release rather than as a backport. |
| Current `main` branch | Security fixes are developed here before release. This is development code, not a stable release. |
| Older releases | No dedicated security backports. Upgrade to the latest stable release. |

See [Releases](https://github.com/turkerozturk/SweetCherry/releases) for published versions and upgrade notes. Fixes on `main` may not yet be included in downloadable packages. Check the applicable release notes or security advisory to identify a version containing a particular fix.

## Reporting a Vulnerability

Please report potential vulnerabilities privately through GitHub's [Report a vulnerability](https://github.com/turkerozturk/SweetCherry/security/advisories/new) form. You can also find this action under **Security → Advisories** in this repository.

Do not disclose exploit details, credentials, private notes or database contents in a public issue, discussion or pull request. For ordinary bugs and feature requests without security implications, use [Issues](https://github.com/turkerozturk/SweetCherry/issues).

A useful report includes:

- The SweetCherry version and, if available, the exact commit or package build information.
- The operating system, Java version, database type, and relevant configuration.
- Whether the application is local-only, available on a LAN, or exposed through HTTPS or a reverse proxy.
- The access required to reproduce the problem: unauthenticated, USER, ADMIN, or local filesystem access.
- Clear reproduction steps or a minimal proof of concept, the expected behavior, and the observed impact.
- A small, sanitized sample file or request when needed. Use synthetic notes and remove passwords, tokens, personal information and private database content.

An initial report does not need to answer every question above. If you are unsure whether an issue is security-related, use the private reporting channel.

## Response and Disclosure

SweetCherry is maintained on a best-effort basis, without a guaranteed response time, fix deadline or long-term support commitment. If you have not received a response after 14 days, please follow up in the same private report.

The maintainer will review the report and, where possible, explain whether it is accepted, needs more information, duplicates a known issue, or describes intended behavior. For confirmed vulnerabilities, the goal is to coordinate a fix, release guidance and disclosure with the reporter. Reports that are declined should receive a brief explanation when possible.

Please allow a reasonable opportunity to investigate and prepare a fix before publishing exploit details. Reporter credit can be included with permission. Confirmed issues and remediation guidance may be published through [GitHub Security Advisories](https://github.com/turkerozturk/SweetCherry/security/advisories).

## Security Model and Scope

SweetCherry is a web application that accesses CherryTree CTB files and configured database servers with the permissions of the operating-system account running it. Its default deployment is local-only. Remote access requires deliberate network configuration, authentication and appropriate transport security; see the [setup guide](documentation/en/setup-and-run.md).

ADMIN is a trusted operator role. Administrators can intentionally select server-side CTB files, including files outside `CTBDATA`, configure supported database connections to localhost or private networks, and perform privileged write and management operations. These capabilities alone are intended behavior. Do not grant ADMIN access to someone who should not have these capabilities.

Please report ways to cross these boundaries, including:

- Authentication, role, tenant or session-isolation bypasses.
- Unauthorized access to notes, attachments, credentials, configuration or server files.
- Cross-site scripting, request forgery, injection, or unsafe parsing of imported content.
- Web-content import bypasses that reach prohibited network destinations, including through redirects or DNS changes.
- File or archive operations that escape their intended destination or overwrite unauthorized files.

An ADMIN-only action can still have a vulnerability. Authorization does not replace input validation, CSRF protection or other safeguards. Likewise, a dismissed scanning alert is not evidence that a related bypass cannot exist. Reports about third-party dependencies are welcome; include the affected component and version, and any known impact on SweetCherry.

## Testing Safely

Test only on systems and data that you own or have explicit permission to use. Use a local test instance and disposable copies of databases. Avoid destructive testing, service disruption, access to other people's data, and requests against systems you are not authorized to test. Keep sensitive reproduction files within the private report.
