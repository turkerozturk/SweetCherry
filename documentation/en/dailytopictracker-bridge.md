# DailyTopicTracker bridge and topic imports

DailyTopicTracker (DTT) is a separately installed application. Use a compatible
DTT version that provides the `/api/bridge/v1` topic catalog, dated-entry and
topic-details endpoints. SweetCherry packages do not include a DTT server.
The bridge is optional: ordinary notes continue to work without it.

## Connect a tenant

Configure the bridge on the DTT side and supply its matching token to the
SweetCherry process through an environment variable before launching it.
Tokens must satisfy the bridge's 32–256 character URL-safe token format.
In the tenant wizard, set the DTT base URL and token environment-variable name,
then use the separate database and DTT connection-test buttons. These checks
report connection results; they do not import topics or edit DTT entries.

For example, a tenant file can contain:

```properties
custom.dtt.baseUrl=http://127.0.0.1:8081/
custom.dtt.tokenEnv=DTT_BRIDGE_TOKEN
```

The URL is an example, not a fixed DTT port. Keep the actual token in the process
environment, not in a note, tenant file, screenshot or release asset. Wizard-managed
settings accept loopback endpoints directly. For a remote peer, explicitly
approve the exact base URL using `DTT_BRIDGE_URL` in the environment; named pairs
such as `DTT_BRIDGE_TOKEN_WORK` and `DTT_BRIDGE_URL_WORK` are supported. Restart
the launcher/process after changing its environment and reload the tenant after
changing its configuration. Browser requests do not receive the bearer token.

## Link a note to a topic

A linked note has the `dttid` tag and contains the positive numeric DTT topic ID
as its text. The importer creates this structure automatically. A configured
linked node shows the DTT panel, including the selected day's entry, topic
Markdown description, category/group links, recurrence rule, latest completion
and predicted dates when supplied by the DTT server.

Select a date to load that day's status and note. The panel starts with today's
entry. ADMIN users can save or delete the selected entry; other users can read
it. Changing the date asks for confirmation when the form has unsaved edits.
Revision/binding checks reject stale writes, and unavailable peers produce panel
feedback. DTT entry changes are sent to DTT; the CTB node retains its topic link.

## Import topics

Use **Import DTT Topics** with an ADMIN account and a writable target CTB.
Review the preview before importing. By default, only topics that do not already
have a valid `dttid` node are selected. The full-catalog option is also available.
Each nonempty import creates a new timestamped top-level branch with group,
category and topic nodes; name collisions receive a suffix. Existing note content
is preserved, and an empty missing-topic result creates no branch. Incremental
import adds missing topics; it does not continuously synchronize imported names
or reorganize previously imported branches.

The optional import control can be disabled with
`myapp.dtt.topic-import.enabled=false`. DTT backup and bridge configuration remain
separate from SweetCherry's CTB backups.
