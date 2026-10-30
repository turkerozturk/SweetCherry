package com.turkerozturk.login;

import java.io.PrintStream;

/** Local process output only: never a web response, URL, or application-logger message. */
final class InitialLoginHint {
    static void print(PrintStream output, boolean created, boolean enabled, boolean externalPassword,
                      String username, String password) {
        if (!created || !enabled || externalPassword) return;
        output.println("\nSweetCherry — first local start / ilk yerel başlatma");
        output.println("Admin username / kullanıcı adı: " + username);
        output.println("Initial password / ilk şifre: " + password);
        output.println("Saved in login-credentials.properties. Keep this output private; change the password there and restart.");
        output.println("Şifre login-credentials.properties dosyasında. Bu çıktıyı paylaşmayın; dosyada değiştirip yeniden başlatın.\n");
    }
    private InitialLoginHint() { }
}
