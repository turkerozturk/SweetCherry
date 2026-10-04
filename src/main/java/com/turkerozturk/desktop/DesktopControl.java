package com.turkerozturk.desktop;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;

/** Optional in-process desktop controls; server/script launches never create this UI. */
public final class DesktopControl {
    private final boolean turkish = java.util.Locale.getDefault().getLanguage().equals("tr");
    private final AtomicBoolean stopping = new AtomicBoolean();
    private JFrame frame;
    private JLabel status;
    private JLabel address;
    private JProgressBar progress;
    private JButton open;
    private JButton stop;
    private TrayIcon tray;
    private volatile String url;
    private volatile Runnable shutdown;
    private volatile boolean failed;

    /** Reads the launcher opt-in before Spring initializes AWT's headless setting. */
    public static boolean requested(String[] args) {
        boolean enabled = Boolean.getBoolean("myapp.desktop.enabled");
        for (String arg : args) {
            if (arg.startsWith("--myapp.desktop.enabled=")) enabled = Boolean.parseBoolean(arg.substring(arg.indexOf('=') + 1));
        }
        return enabled;
    }

    private String text(String tr, String en) { return turkish ? tr : en; }

    /** Shows startup feedback synchronously, before the Spring context starts. */
    public static DesktopControl show() throws Exception {
        DesktopControl control = new DesktopControl();
        SwingUtilities.invokeAndWait(control::create);
        return control;
    }

    private void create() {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) { }
        frame = new JFrame("SweetCherry");
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { chooseClose(); }
        });
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        JLabel title = new JLabel("SweetCherry"); title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
        status = new JLabel(text("SweetCherry başlatılıyor…", "Starting SweetCherry…"));
        address = new JLabel(" ");
        progress = new JProgressBar(); progress.setIndeterminate(true);
        open = new JButton(text("Tarayıcıda Aç", "Open in Browser")); open.setEnabled(false);
        open.addActionListener(e -> openBrowser());
        stop = new JButton(text("SweetCherry’yi Durdur", "Stop SweetCherry")); stop.setEnabled(false);
        stop.addActionListener(e -> confirmStop());
        JButton log = new JButton(text("Logu Aç", "Open Log"));
        log.addActionListener(e -> background(() -> {
            File file = new File("myapp.log");
            if (!file.isFile()) throw new IllegalStateException(text("Log henüz oluşmadı.", "The log file does not exist yet."));
            openTextFile(file);
        }));
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        buttons.add(open); buttons.add(stop); buttons.add(log);
        JButton folder = new JButton(text("Uygulama Klasörü", "Application Folder"));
        folder.addActionListener(e -> background(() -> Desktop.getDesktop().open(new File(".").getCanonicalFile())));
        JPanel files = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0)); files.add(folder);
        JLabel loginHelp = new JLabel(text(
                "<html>Varsayılan kullanıcı adları: <b>admin</b> ve <b>user</b>.<br>Şifreler uygulama klasöründeki <b>login-credentials.properties</b> dosyasındadır.<br><b>admin.password</b> bir ayar anahtarıdır; kullanıcı adı değildir.<br>Şifreyi dosyada değiştirip SweetCherry’yi yeniden başlatın.<br>application.yml içinde özel giriş ayarları varsa onlar kullanılır.</html>",
                "<html>Default usernames: <b>admin</b> and <b>user</b>.<br>Passwords are in <b>login-credentials.properties</b> in the application folder.<br><b>admin.password</b> is a setting key, not a username.<br>Change the password in the file, then restart SweetCherry.<br>Custom login settings in application.yml take precedence.</html>"));
        panel.add(title); panel.add(Box.createVerticalStrut(12)); panel.add(status);
        panel.add(Box.createVerticalStrut(8)); panel.add(address); panel.add(Box.createVerticalStrut(8));
        panel.add(progress); panel.add(Box.createVerticalStrut(16)); panel.add(buttons);
        panel.add(Box.createVerticalStrut(12)); panel.add(loginHelp); panel.add(Box.createVerticalStrut(8)); panel.add(files);
        for (Component component : panel.getComponents()) {
            if (component instanceof JComponent item) item.setAlignmentX(Component.LEFT_ALIGNMENT);
        }
        frame.setContentPane(panel); frame.pack(); frame.setMinimumSize(frame.getSize());
        frame.setLocationRelativeTo(null); frame.setVisible(true);
        Image image = Toolkit.getDefaultToolkit().getImage(DesktopControl.class.getResource("/static/img/logo.jpg"));
        frame.setIconImage(image);
        if (SystemTray.isSupported()) {
            PopupMenu menu = new PopupMenu();
            MenuItem window = new MenuItem(text("Kontrol Penceresi", "Control Window"));
            window.addActionListener(e -> SwingUtilities.invokeLater(this::reveal)); menu.add(window);
            MenuItem browser = new MenuItem(text("Tarayıcıda Aç", "Open in Browser"));
            browser.addActionListener(e -> { if (url != null) openBrowser(); }); menu.add(browser);
            MenuItem exit = new MenuItem(text("Durdur ve Çık", "Stop and Exit"));
            exit.addActionListener(e -> SwingUtilities.invokeLater(this::confirmStop)); menu.add(exit);
            tray = new TrayIcon(image, "SweetCherry", menu); tray.setImageAutoSize(true);
            tray.addActionListener(e -> SwingUtilities.invokeLater(this::reveal));
            try { SystemTray.getSystemTray().add(tray); } catch (AWTException e) { tray = null; }
        }
    }

    private void reveal() { frame.setVisible(true); frame.setState(Frame.NORMAL); frame.toFront(); }

    /** Enables controls only after the server reports ApplicationReadyEvent. */
    public void ready(String address, Runnable shutdown, boolean browse) {
        this.url = address; this.shutdown = shutdown;
        SwingUtilities.invokeLater(() -> {
            status.setText(text("SweetCherry çalışıyor.", "SweetCherry is running."));
            this.address.setText(address); progress.setIndeterminate(false); progress.setValue(100);
            open.setEnabled(true); stop.setEnabled(true);
            if (browse) openBrowser();
        });
    }

    /** Keeps startup failures visible and points to the existing application log. */
    public void failed(Throwable error) {
        failed = true;
        boolean port = false;
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof java.net.BindException || cause.getClass().getSimpleName().equals("PortInUseException")) port = true;
        }
        final boolean portConflict = port;
        SwingUtilities.invokeLater(() -> {
            status.setText(portConflict ? text("Port kullanımda. Diğer uygulamayı veya port ayarını kontrol edin.", "Port in use. Check the other application or port settings.")
                    : text("Başlatılamadı. Ayrıntılar için logu açın.", "Startup failed. Open the log for details."));
            progress.setIndeterminate(false); stop.setText(text("Çık", "Exit")); stop.setEnabled(true);
            reveal(); frame.pack();
        });
    }

    private void chooseClose() {
        if (failed) { closed(); return; }
        if (shutdown == null) {
            JOptionPane.showMessageDialog(frame, text("Başlatma tamamlanana kadar bekleyin.", "Please wait until startup completes.")); return;
        }
        Object[] choices = {text("Arka Planda Çalış", "Keep Running"), text("Durdur ve Çık", "Stop and Exit"), text("Vazgeç", "Cancel")};
        int choice = JOptionPane.showOptionDialog(frame, text("SweetCherry arka planda çalışmaya devam etsin mi?", "Keep SweetCherry running in the background?"), "SweetCherry", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, choices, choices[0]);
        if (choice == 0) { if (tray != null) frame.setVisible(false); else frame.setState(Frame.ICONIFIED); }
        else if (choice == 1) stop();
    }

    private void confirmStop() {
        if (failed) { closed(); return; }
        if (shutdown != null && JOptionPane.showConfirmDialog(frame, text("SweetCherry durdurulsun mu?", "Stop SweetCherry?"), "SweetCherry", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) stop();
    }

    /** Closes Spring off the EDT so graceful shutdown cannot freeze the controls. */
    private void stop() {
        if (!stopping.compareAndSet(false, true)) return;
        status.setText(text("SweetCherry kapatılıyor…", "Stopping SweetCherry…"));
        open.setEnabled(false); stop.setEnabled(false); progress.setIndeterminate(true);
        new Thread(shutdown, "sweetcherry-desktop-shutdown").start();
    }

    /** Removes desktop resources when shutdown also originates from the web interface. */
    public void closed() {
        SwingUtilities.invokeLater(() -> {
            if (tray != null) SystemTray.getSystemTray().remove(tray);
            frame.dispose();
        });
    }

    /** Opens Windows logs with Notepad instead of a potentially broken .log file association. */
    private static void openTextFile(File file) throws Exception {
        if (System.getProperty("os.name").toLowerCase(java.util.Locale.ROOT).contains("win")) {
            String windows = System.getenv("SystemRoot");
            File notepad = new File(windows == null ? "C:\\Windows" : windows, "System32/notepad.exe");
            new ProcessBuilder(notepad.getAbsolutePath(), file.getCanonicalPath()).start();
        } else {
            Desktop.getDesktop().open(file);
        }
    }

    private void openBrowser() { if (url != null) background(() -> Desktop.getDesktop().browse(URI.create(url))); }
    private void background(Task task) {
        Thread thread = new Thread(() -> {
            try { task.run(); } catch (Exception error) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(frame, error.getMessage(), "SweetCherry", JOptionPane.WARNING_MESSAGE));
            }
        }, "sweetcherry-desktop-action");
        thread.setDaemon(true); thread.start();
    }
    @FunctionalInterface private interface Task { void run() throws Exception; }
}
