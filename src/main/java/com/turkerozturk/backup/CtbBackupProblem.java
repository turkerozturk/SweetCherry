package com.turkerozturk.backup;

/** A bounded message key, never a JDBC URL, password, or database exception text. */
public final class CtbBackupProblem extends RuntimeException {
    private final String key;
    public CtbBackupProblem(String key) { super(key); this.key = "ctbBackup.error." + key; }
    public String key() { return key; }
}
