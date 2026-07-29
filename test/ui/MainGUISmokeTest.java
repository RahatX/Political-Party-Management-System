package ui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import model.Member;
import service.PartySystem;

public final class MainGUISmokeTest {
    private final Path previewDirectory = Path.of("out", "ui-preview");

    public static void main(String[] args) throws Exception {
        new MainGUISmokeTest().run();
    }

    private void run() throws Exception {
        Files.createDirectories(previewDirectory);
        PartySystem system = new PartySystem(Files.createTempDirectory("ppm-ui-test-"));
        MainGUI gui = new MainGUI(system);
        SwingUtilities.invokeAndWait(gui::init);
        try {
            settle();
            capture(gui.getFrame(), "01-welcome.png");
            SwingUtilities.invokeAndWait(() -> gui.showScreen("APPLICATION"));
            settle();
            capture(gui.getFrame(), "02-application.png");
            SwingUtilities.invokeAndWait(() -> gui.showScreen("LOGIN"));
            settle();
            capture(gui.getFrame(), "03-login.png");

            Member admin = system.login("admin@party.org", "admin123");
            check(admin != null, "administrator account is available");
            SwingUtilities.invokeAndWait(() -> gui.startSession(admin));
            settle();
            capture(gui.getFrame(), "04-workspace.png");
            captureWorkspaceView(gui, "DIRECTORY", "05-directory.png");
            captureWorkspaceView(gui, "APPLICATIONS", "06-applications.png");
            captureWorkspaceView(gui, "DONATIONS", "07-donations.png");
            captureWorkspaceView(gui, "ELECTIONS", "08-elections.png");
            captureWorkspaceView(gui, "PROFILE", "09-profile.png");
        } finally {
            SwingUtilities.invokeAndWait(gui.getFrame()::dispose);
        }
        System.out.println("MainGUISmokeTest passed: 9 nonblank views rendered to "
                + previewDirectory.toAbsolutePath());
    }

    private void captureWorkspaceView(MainGUI gui, String viewName, String fileName)
            throws Exception {
        SwingUtilities.invokeAndWait(() -> gui.showWorkspaceView(viewName));
        settle();
        capture(gui.getFrame(), fileName);
    }

    private void settle() throws InterruptedException {
        Thread.sleep(150);
    }

    private void capture(JFrame frame, String fileName) throws Exception {
        BufferedImage image = new BufferedImage(
                frame.getWidth(),
                frame.getHeight(),
                BufferedImage.TYPE_INT_RGB);
        SwingUtilities.invokeAndWait(() -> {
            Graphics2D graphics = image.createGraphics();
            frame.paint(graphics);
            graphics.dispose();
        });
        check(hasVisualVariety(image), fileName + " should not be blank");
        ImageIO.write(image, "png", previewDirectory.resolve(fileName).toFile());
    }

    private boolean hasVisualVariety(BufferedImage image) {
        Set<Integer> sampledColors = new HashSet<>();
        int horizontalStep = Math.max(1, image.getWidth() / 50);
        int verticalStep = Math.max(1, image.getHeight() / 30);
        for (int y = 0; y < image.getHeight(); y += verticalStep) {
            for (int x = 0; x < image.getWidth(); x += horizontalStep) {
                Color color = new Color(image.getRGB(x, y));
                sampledColors.add(color.getRGB());
            }
        }
        return sampledColors.size() > 8;
    }

    private void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
