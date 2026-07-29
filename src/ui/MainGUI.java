package ui;

import committees.CentralCommittee;
import committees.Committee;
import committees.DistrictCommittee;
import committees.DivisionalCommittee;
import exceptions.DuplicateMemberException;
import exceptions.InvalidDonationException;
import java.awt.BorderLayout;
import java.awt.BasicStroke;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.text.DecimalFormat;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableRowSorter;
import javax.swing.plaf.basic.BasicButtonUI;
import model.Address;
import model.CommitteeLevel;
import model.District;
import model.Division;
import model.DonationRecord;
import model.Member;
import model.Role;
import service.Election;
import service.PartySystem;
import service.SystemStats;

public final class MainGUI {
    private static final Color BACKGROUND = new Color(244, 247, 245);
    private static final Color SURFACE = Color.WHITE;
    private static final Color SIDEBAR = new Color(26, 36, 31);
    private static final Color PRIMARY = new Color(35, 122, 87);
    private static final Color PRIMARY_DARK = new Color(25, 91, 64);
    private static final Color ACCENT = new Color(190, 47, 112);
    private static final Color TEXT = new Color(31, 40, 35);
    private static final Color MUTED = new Color(100, 113, 106);
    private static final Color BORDER = new Color(219, 226, 222);
    private static final Color SUCCESS_BG = new Color(224, 243, 234);
    private static final Color WARNING = new Color(186, 112, 22);
    private static final Color DANGER = new Color(183, 55, 55);

    private static final Font FONT = new Font("Segoe UI", Font.PLAIN, 14);
    private static final Font FONT_MEDIUM = new Font("Segoe UI", Font.BOLD, 14);
    private static final Font TITLE_FONT = new Font("Segoe UI", Font.BOLD, 28);
    private static final DecimalFormat MONEY = new DecimalFormat("#,##0.00");
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    private final PartySystem system;
    private final CardLayout rootLayout = new CardLayout();
    private final JPanel rootCards = new JPanel(rootLayout);

    private JFrame frame;
    private Member currentUser;
    private WelcomePanel welcomePanel;
    private ApplicationPanel applicationPanel;
    private LoginPanel loginPanel;
    private WorkspacePanel workspacePanel;

    public MainGUI() {
        this(new PartySystem());
    }

    MainGUI(PartySystem system) {
        this.system = Objects.requireNonNull(system, "system");
    }

    public static void main(String[] args) {
        configureLookAndFeel();
        SwingUtilities.invokeLater(() -> new MainGUI().init());
    }

    private static void configureLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }
        UIManager.put("Label.font", FONT);
        UIManager.put("Button.font", FONT_MEDIUM);
        UIManager.put("TextField.font", FONT);
        UIManager.put("PasswordField.font", FONT);
        UIManager.put("ComboBox.font", FONT);
        UIManager.put("Table.font", FONT);
        UIManager.put("TableHeader.font", FONT_MEDIUM);
        UIManager.put("OptionPane.messageFont", FONT);
        UIManager.put("OptionPane.buttonFont", FONT_MEDIUM);
    }

    void init() {
        frame = new JFrame("Political Party Management System");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setSize(1280, 760);
        frame.setMinimumSize(new Dimension(1024, 640));
        frame.setLocationRelativeTo(null);

        ImageIcon icon = loadLogo(64);
        if (icon != null) {
            frame.setIconImage(icon.getImage());
        }

        welcomePanel = new WelcomePanel();
        applicationPanel = new ApplicationPanel();
        loginPanel = new LoginPanel();
        workspacePanel = new WorkspacePanel();

        rootCards.add(welcomePanel, "WELCOME");
        rootCards.add(applicationPanel, "APPLICATION");
        rootCards.add(loginPanel, "LOGIN");
        rootCards.add(workspacePanel, "WORKSPACE");
        frame.setContentPane(rootCards);
        showScreen("WELCOME");
        frame.setVisible(true);

        Runtime.getRuntime().addShutdownHook(new Thread(system::saveToFiles));
    }

    void showScreen(String screen) {
        switch (screen) {
            case "WELCOME" -> welcomePanel.refresh();
            case "APPLICATION" -> applicationPanel.prepare();
            case "LOGIN" -> loginPanel.prepare();
            case "WORKSPACE" -> workspacePanel.rebuild();
            default -> {
            }
        }
        rootLayout.show(rootCards, screen);
        rootCards.revalidate();
        if (frame != null) frame.validate();
        rootCards.repaint();
    }

    void startSession(Member member) {
        currentUser = Objects.requireNonNull(member, "member");
        showScreen("WORKSPACE");
    }

    JFrame getFrame() {
        return frame;
    }

    void showWorkspaceView(String viewName) {
        switch (viewName) {
            case "DIRECTORY" -> workspacePanel.openView(
                    viewName, "Member directory", workspacePanel.directoryView);
            case "APPLICATIONS" -> workspacePanel.openView(
                    viewName, "Applications", workspacePanel.applicationsView);
            case "DONATIONS" -> workspacePanel.openView(
                    viewName, "Donations", workspacePanel.donationsView);
            case "ELECTIONS" -> workspacePanel.openView(
                    viewName, "Elections", workspacePanel.electionsView);
            case "PROFILE" -> workspacePanel.openView(
                    viewName, "My profile", workspacePanel.profileView);
            default -> workspacePanel.openView(
                    "OVERVIEW", "Overview", workspacePanel.overviewView);
        }
    }

    private ImageIcon loadLogo(int size) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setColor(new Color(43, 8, 97));
        graphics.fillRoundRect(0, 0, size, size, size / 12, size / 12);

        graphics.setStroke(new BasicStroke(
                Math.max(3, size / 15),
                BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND));
        int centerY = size * 31 / 100;
        int arm = size * 13 / 100;
        graphics.setColor(new Color(235, 52, 145));
        graphics.drawLine(size * 31 / 100, centerY - arm, size * 20 / 100, centerY);
        graphics.drawLine(size * 20 / 100, centerY, size * 31 / 100, centerY + arm);
        graphics.setColor(new Color(124, 50, 238));
        graphics.drawLine(size * 69 / 100, centerY - arm, size * 80 / 100, centerY);
        graphics.drawLine(size * 80 / 100, centerY, size * 69 / 100, centerY + arm);

        Font logoFont = new Font("Segoe UI", Font.BOLD, Math.max(10, size * 22 / 100));
        graphics.setFont(logoFont);
        graphics.setColor(Color.WHITE);
        FontMetrics metrics = graphics.getFontMetrics();
        String mark = "PPMS";
        int textX = (size - metrics.stringWidth(mark)) / 2;
        int textY = size * 72 / 100;
        graphics.drawString(mark, textX, textY);
        graphics.dispose();
        return new ImageIcon(image);
    }

    private final class WelcomePanel extends JPanel implements Refreshable {
        private static final long serialVersionUID = 1L;
        private final JLabel membersValue = metricValueLabel();
        private final JLabel pendingValue = metricValueLabel();
        private final JLabel electionsValue = metricValueLabel();
        private final JLabel donationsValue = metricValueLabel();

        WelcomePanel() {
            setLayout(new BorderLayout());
            setBackground(BACKGROUND);

            JPanel topBar = new JPanel(new BorderLayout());
            topBar.setBackground(SURFACE);
            topBar.setBorder(BorderFactory.createEmptyBorder(14, 28, 14, 28));
            JLabel brand = new JLabel("PPMS");
            brand.setFont(new Font("Segoe UI", Font.BOLD, 20));
            brand.setForeground(TEXT);
            topBar.add(brand, BorderLayout.WEST);
            JLabel course = new JLabel("CSE215 Object Oriented Programming");
            course.setForeground(MUTED);
            topBar.add(course, BorderLayout.EAST);
            add(topBar, BorderLayout.NORTH);

            JPanel hero = new JPanel(new GridBagLayout());
            hero.setBackground(SIDEBAR);
            hero.setBorder(BorderFactory.createEmptyBorder(40, 54, 40, 54));
            GridBagConstraints gc = new GridBagConstraints();
            gc.gridx = 0;
            gc.gridy = 0;
            gc.anchor = GridBagConstraints.WEST;
            ImageIcon logo = loadLogo(150);
            if (logo != null) {
                hero.add(new JLabel(logo), gc);
                gc.gridy++;
                gc.insets = new Insets(24, 0, 0, 0);
            }
            JLabel heading = new JLabel("<html>Political Party<br>Management System</html>");
            heading.setFont(new Font("Segoe UI", Font.BOLD, 40));
            heading.setForeground(Color.WHITE);
            hero.add(heading, gc);
            gc.gridy++;
            gc.insets = new Insets(18, 0, 0, 0);
            JLabel description = new JLabel(
                    "<html>Membership, committees, donations, and elections<br>"
                            + "organized in one role-based desktop application.</html>");
            description.setFont(new Font("Segoe UI", Font.PLAIN, 16));
            description.setForeground(new Color(198, 209, 203));
            hero.add(description, gc);
            gc.gridy++;
            gc.weighty = 1;
            hero.add(Box.createVerticalGlue(), gc);

            JPanel access = new JPanel();
            access.setLayout(new BoxLayout(access, BoxLayout.Y_AXIS));
            access.setBackground(BACKGROUND);
            access.setBorder(BorderFactory.createEmptyBorder(58, 50, 44, 50));

            JLabel accessTitle = new JLabel("Access the system");
            accessTitle.setFont(TITLE_FONT);
            accessTitle.setForeground(TEXT);
            accessTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
            access.add(accessTitle);
            access.add(Box.createVerticalStrut(8));
            JLabel accessCopy = new JLabel("Choose an action to continue.");
            accessCopy.setForeground(MUTED);
            accessCopy.setAlignmentX(Component.LEFT_ALIGNMENT);
            access.add(accessCopy);
            access.add(Box.createVerticalStrut(28));

            JButton login = primaryButton("Sign in");
            login.setAlignmentX(Component.LEFT_ALIGNMENT);
            login.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
            login.addActionListener(e -> showScreen("LOGIN"));
            access.add(login);
            access.add(Box.createVerticalStrut(10));

            JButton apply = secondaryButton("Apply for membership");
            apply.setAlignmentX(Component.LEFT_ALIGNMENT);
            apply.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
            apply.addActionListener(e -> showScreen("APPLICATION"));
            access.add(apply);
            access.add(Box.createVerticalStrut(10));

            JButton donate = textButton("Make a guest donation");
            donate.setAlignmentX(Component.LEFT_ALIGNMENT);
            donate.addActionListener(e -> guestDonate());
            access.add(donate);
            access.add(Box.createVerticalStrut(38));

            JLabel snapshot = new JLabel("System snapshot");
            snapshot.setFont(new Font("Segoe UI", Font.BOLD, 17));
            snapshot.setForeground(TEXT);
            snapshot.setAlignmentX(Component.LEFT_ALIGNMENT);
            access.add(snapshot);
            access.add(Box.createVerticalStrut(14));

            JPanel metrics = new JPanel(new GridLayout(2, 2, 12, 12));
            metrics.setOpaque(false);
            metrics.setAlignmentX(Component.LEFT_ALIGNMENT);
            metrics.setMaximumSize(new Dimension(Integer.MAX_VALUE, 170));
            metrics.add(smallMetric("Approved members", membersValue));
            metrics.add(smallMetric("Pending", pendingValue));
            metrics.add(smallMetric("Active elections", electionsValue));
            metrics.add(smallMetric("Donations (BDT)", donationsValue));
            access.add(metrics);
            access.add(Box.createVerticalGlue());

            JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, hero, access);
            split.setBorder(null);
            split.setDividerSize(0);
            split.setResizeWeight(0.57);
            split.setEnabled(false);
            add(split, BorderLayout.CENTER);
        }

        @Override
        public void refresh() {
            SystemStats stats = system.getStats();
            membersValue.setText(String.valueOf(stats.getApprovedMembers()));
            pendingValue.setText(String.valueOf(stats.getPendingApplications()));
            electionsValue.setText(String.valueOf(stats.getActiveElections()));
            donationsValue.setText(compactMoney(stats.getTotalDonations()));
        }
    }

    private final class ApplicationPanel extends JPanel {
        private static final long serialVersionUID = 1L;
        private final JTextField nationalIdField = inputField();
        private final JTextField nameField = inputField();
        private final JTextField emailField = inputField();
        private final JTextField phoneField = inputField();
        private final JPasswordField passwordField = passwordField();
        private final JPasswordField confirmPasswordField = passwordField();
        private final JTextField professionField = inputField();
        private final JTextField incomeField = inputField();
        private final JComboBox<Division> divisionBox = new JComboBox<>(Division.values());
        private final JComboBox<District> districtBox = new JComboBox<>();
        private final JLabel status = statusLabel();

        ApplicationPanel() {
            setLayout(new BorderLayout());
            setBackground(BACKGROUND);
            add(pageHeader("Membership application", "Submit your information for review", "Back",
                    e -> showScreen("WELCOME")), BorderLayout.NORTH);

            styleCombo(divisionBox);
            styleCombo(districtBox);
            divisionBox.addActionListener(e -> reloadDistricts());

            JPanel form = surfacePanel();
            form.setLayout(new GridBagLayout());
            form.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER),
                    BorderFactory.createEmptyBorder(28, 32, 28, 32)));
            form.setPreferredSize(new Dimension(880, 510));

            GridBagConstraints gc = new GridBagConstraints();
            gc.fill = GridBagConstraints.HORIZONTAL;
            gc.weightx = 1;
            gc.insets = new Insets(0, 0, 17, 18);

            addFormField(form, gc, 0, 0, "National ID *", nationalIdField);
            addFormField(form, gc, 2, 0, "Full name *", nameField);
            addFormField(form, gc, 0, 2, "Email address *", emailField);
            addFormField(form, gc, 2, 2, "Phone number *", phoneField);
            addFormField(form, gc, 0, 4, "Password *", passwordField);
            addFormField(form, gc, 2, 4, "Confirm password *", confirmPasswordField);
            addFormField(form, gc, 0, 6, "Profession *", professionField);
            addFormField(form, gc, 2, 6, "Yearly income (BDT) *", incomeField);
            addFormField(form, gc, 0, 8, "Division *", divisionBox);
            addFormField(form, gc, 2, 8, "District *", districtBox);

            gc.gridx = 0;
            gc.gridy = 10;
            gc.gridwidth = 4;
            gc.insets = new Insets(4, 0, 14, 0);
            form.add(status, gc);

            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
            buttons.setOpaque(false);
            JButton clear = secondaryButton("Clear");
            clear.addActionListener(e -> clearForm());
            JButton submit = primaryButton("Submit application");
            submit.addActionListener(e -> submit());
            buttons.add(clear);
            buttons.add(submit);
            gc.gridy = 11;
            gc.insets = new Insets(0, 0, 8, 0);
            form.add(buttons, gc);

            JPanel formWrapper = new JPanel(new GridBagLayout());
            formWrapper.setBackground(BACKGROUND);
            formWrapper.setBorder(BorderFactory.createEmptyBorder(28, 28, 28, 28));
            formWrapper.add(form);
            JScrollPane scroll = new JScrollPane(formWrapper);
            scroll.setBorder(null);
            scroll.getVerticalScrollBar().setUnitIncrement(18);
            add(scroll, BorderLayout.CENTER);
            reloadDistricts();
        }

        void prepare() {
            setStatus(status, "Applications are reviewed by party leadership.", MUTED);
            nationalIdField.requestFocusInWindow();
        }

        private void reloadDistricts() {
            District selected = (District) districtBox.getSelectedItem();
            districtBox.removeAllItems();
            Division division = (Division) divisionBox.getSelectedItem();
            for (District district : District.values()) {
                if (district.getDivision() == division) {
                    districtBox.addItem(district);
                }
            }
            if (selected != null && selected.getDivision() == division) {
                districtBox.setSelectedItem(selected);
            }
        }

        private void submit() {
            String nationalId = nationalIdField.getText().trim();
            String name = nameField.getText().trim();
            String email = emailField.getText().trim();
            String phone = phoneField.getText().trim();
            String password = new String(passwordField.getPassword());
            String confirmation = new String(confirmPasswordField.getPassword());
            String profession = professionField.getText().trim();
            double income;

            if (nationalId.isBlank() || name.length() < 2 || profession.isBlank()) {
                setStatus(status, "Enter your National ID, full name, and profession.", DANGER);
                return;
            }
            if (!EMAIL_PATTERN.matcher(email).matches()) {
                setStatus(status, "Enter a valid email address.", DANGER);
                return;
            }
            if (phone.length() < 10) {
                setStatus(status, "Enter a valid phone number.", DANGER);
                return;
            }
            if (password.length() < 6) {
                setStatus(status, "Password must contain at least 6 characters.", DANGER);
                return;
            }
            if (!password.equals(confirmation)) {
                setStatus(status, "Passwords do not match.", DANGER);
                return;
            }
            try {
                income = Double.parseDouble(incomeField.getText().trim());
                if (income <= 0) throw new NumberFormatException();
            } catch (NumberFormatException exception) {
                setStatus(status, "Yearly income must be a positive number.", DANGER);
                return;
            }

            District district = (District) districtBox.getSelectedItem();
            try {
                Member member = new Member(
                        nationalId,
                        name,
                        email,
                        phone,
                        password,
                        profession,
                        income,
                        false,
                        false,
                        new Address(district),
                        Role.MEMBER,
                        CommitteeLevel.DISTRICT);
                system.applyForMembership(member);
                system.saveToFiles();
                clearForm();
                JOptionPane.showMessageDialog(
                        frame,
                        "Application submitted successfully.\nYou can sign in after approval.",
                        "Application received",
                        JOptionPane.INFORMATION_MESSAGE);
                showScreen("LOGIN");
            } catch (DuplicateMemberException exception) {
                setStatus(status, exception.getMessage(), DANGER);
            }
        }

        private void clearForm() {
            nationalIdField.setText("");
            nameField.setText("");
            emailField.setText("");
            phoneField.setText("");
            passwordField.setText("");
            confirmPasswordField.setText("");
            professionField.setText("");
            incomeField.setText("");
            divisionBox.setSelectedIndex(0);
            reloadDistricts();
            setStatus(status, "Required fields are marked with *.", MUTED);
        }
    }

    private final class LoginPanel extends JPanel {
        private static final long serialVersionUID = 1L;
        private final JTextField emailField = inputField();
        private final JPasswordField passwordField = passwordField();
        private final JLabel status = statusLabel();

        LoginPanel() {
            setLayout(new BorderLayout());
            setBackground(BACKGROUND);
            add(pageHeader("Sign in", "Access your role-based workspace", "Back",
                    e -> showScreen("WELCOME")), BorderLayout.NORTH);

            JPanel brandPanel = new JPanel(new GridBagLayout());
            brandPanel.setBackground(new Color(238, 242, 239));
            GridBagConstraints brandGc = new GridBagConstraints();
            brandGc.gridx = 0;
            brandGc.gridy = 0;
            ImageIcon logo = loadLogo(180);
            if (logo != null) brandPanel.add(new JLabel(logo), brandGc);
            brandGc.gridy++;
            brandGc.insets = new Insets(22, 0, 0, 0);
            JLabel systemName = new JLabel("PPMS");
            systemName.setFont(new Font("Segoe UI", Font.BOLD, 32));
            systemName.setForeground(TEXT);
            brandPanel.add(systemName, brandGc);

            JPanel form = surfacePanel();
            form.setLayout(new GridBagLayout());
            form.setBorder(BorderFactory.createEmptyBorder(44, 58, 44, 58));
            GridBagConstraints gc = new GridBagConstraints();
            gc.gridx = 0;
            gc.gridy = 0;
            gc.fill = GridBagConstraints.HORIZONTAL;
            gc.weightx = 1;

            JLabel title = new JLabel("Welcome back");
            title.setFont(TITLE_FONT);
            title.setForeground(TEXT);
            form.add(title, gc);
            gc.gridy++;
            gc.insets = new Insets(8, 0, 28, 0);
            JLabel copy = new JLabel("Enter your account credentials.");
            copy.setForeground(MUTED);
            form.add(copy, gc);

            gc.gridy++;
            gc.insets = new Insets(0, 0, 7, 0);
            form.add(fieldLabel("Email address"), gc);
            gc.gridy++;
            gc.insets = new Insets(0, 0, 18, 0);
            emailField.setPreferredSize(new Dimension(360, 42));
            form.add(emailField, gc);
            gc.gridy++;
            gc.insets = new Insets(0, 0, 7, 0);
            form.add(fieldLabel("Password"), gc);
            gc.gridy++;
            gc.insets = new Insets(0, 0, 8, 0);
            form.add(passwordField, gc);
            gc.gridy++;

            JCheckBox showPassword = new JCheckBox("Show password");
            showPassword.setOpaque(false);
            showPassword.setForeground(MUTED);
            char hiddenCharacter = passwordField.getEchoChar();
            showPassword.addActionListener(e -> passwordField.setEchoChar(
                    showPassword.isSelected() ? (char) 0 : hiddenCharacter));
            gc.insets = new Insets(0, 0, 18, 0);
            form.add(showPassword, gc);
            gc.gridy++;
            gc.insets = new Insets(0, 0, 16, 0);
            form.add(status, gc);
            gc.gridy++;
            gc.insets = new Insets(0, 0, 0, 0);
            JButton login = primaryButton("Sign in");
            login.addActionListener(this::login);
            form.add(login, gc);
            passwordField.addActionListener(this::login);

            JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, brandPanel, form);
            split.setBorder(BorderFactory.createEmptyBorder(34, 80, 50, 80));
            split.setDividerSize(0);
            split.setResizeWeight(0.46);
            split.setEnabled(false);
            split.setBackground(BACKGROUND);
            add(split, BorderLayout.CENTER);
        }

        void prepare() {
            emailField.setText("");
            passwordField.setText("");
            setStatus(status, "Use the email and password linked to your account.", MUTED);
            emailField.requestFocusInWindow();
        }

        private void login(ActionEvent event) {
            String email = emailField.getText().trim();
            String password = new String(passwordField.getPassword());
            if (email.isBlank() || password.isBlank()) {
                setStatus(status, "Enter both email and password.", DANGER);
                return;
            }
            Member member = system.login(email, password);
            if (member == null) {
                setStatus(status, "Email or password is incorrect.", DANGER);
                return;
            }
            if (!member.isApproved()) {
                setStatus(status, "Your membership application is awaiting approval.", WARNING);
                return;
            }
            startSession(member);
        }
    }

    @SuppressWarnings("serial")
    private final class WorkspacePanel extends JPanel {
        private static final long serialVersionUID = 1L;
        private final JPanel navigation = new JPanel();
        private final JPanel viewCards = new JPanel();
        private final CardLayout viewLayout = new CardLayout();
        private final JLabel pageTitle = new JLabel();
        private final JLabel userSummary = new JLabel();
        private final List<JButton> navigationButtons = new ArrayList<>();

        private final OverviewView overviewView = new OverviewView();
        private final DirectoryView directoryView = new DirectoryView();
        private final ApplicationsView applicationsView = new ApplicationsView();
        private final DonationsView donationsView = new DonationsView();
        private final ElectionsView electionsView = new ElectionsView();
        private final ProfileView profileView = new ProfileView();

        WorkspacePanel() {
            setLayout(new BorderLayout());
            setBackground(BACKGROUND);

            navigation.setLayout(new BorderLayout());
            navigation.setBackground(SIDEBAR);
            navigation.setPreferredSize(new Dimension(230, 0));
            add(navigation, BorderLayout.WEST);

            JPanel main = new JPanel(new BorderLayout());
            main.setBackground(BACKGROUND);
            JPanel toolbar = new JPanel(new BorderLayout());
            toolbar.setBackground(SURFACE);
            toolbar.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER),
                    BorderFactory.createEmptyBorder(14, 26, 14, 28)));
            pageTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
            pageTitle.setForeground(TEXT);
            userSummary.setForeground(MUTED);
            toolbar.add(pageTitle, BorderLayout.WEST);
            toolbar.add(userSummary, BorderLayout.EAST);
            main.add(toolbar, BorderLayout.NORTH);

            viewCards.setLayout(viewLayout);
            viewCards.setBackground(BACKGROUND);
            viewCards.add(overviewView, "OVERVIEW");
            viewCards.add(directoryView, "DIRECTORY");
            viewCards.add(applicationsView, "APPLICATIONS");
            viewCards.add(donationsView, "DONATIONS");
            viewCards.add(electionsView, "ELECTIONS");
            viewCards.add(profileView, "PROFILE");
            main.add(viewCards, BorderLayout.CENTER);
            add(main, BorderLayout.CENTER);
        }

        void rebuild() {
            if (currentUser == null) return;
            navigation.removeAll();
            navigationButtons.clear();

            JPanel brand = new JPanel(new GridBagLayout());
            brand.setOpaque(false);
            brand.setBorder(BorderFactory.createEmptyBorder(22, 20, 20, 20));
            GridBagConstraints brandConstraints = new GridBagConstraints();
            brandConstraints.gridx = 0;
            brandConstraints.gridy = 0;
            brandConstraints.weightx = 1;
            brandConstraints.fill = GridBagConstraints.HORIZONTAL;
            brandConstraints.anchor = GridBagConstraints.WEST;
            JLabel logo = new JLabel("PPMS");
            logo.setFont(new Font("Segoe UI", Font.BOLD, 23));
            logo.setForeground(Color.WHITE);
            brand.add(logo, brandConstraints);
            brandConstraints.gridy++;
            brandConstraints.insets = new Insets(5, 0, 0, 0);
            JLabel role = new JLabel(formatEnum(currentUser.getRole()));
            role.setForeground(new Color(167, 184, 175));
            brand.add(role, brandConstraints);
            navigation.add(brand, BorderLayout.NORTH);

            JPanel links = new JPanel(new GridBagLayout());
            links.setOpaque(false);
            links.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
            addNavigation(links, "Overview", "OVERVIEW", overviewView);
            addNavigation(links, "Member directory", "DIRECTORY", directoryView);
            if (currentUser.getRole() != Role.MEMBER) {
                addNavigation(links, "Applications", "APPLICATIONS", applicationsView);
            }
            addNavigation(links, "Donations", "DONATIONS", donationsView);
            addNavigation(links, "Elections", "ELECTIONS", electionsView);
            addNavigation(links, "My profile", "PROFILE", profileView);
            GridBagConstraints filler = new GridBagConstraints();
            filler.gridx = 0;
            filler.gridy = navigationButtons.size();
            filler.weightx = 1;
            filler.weighty = 1;
            filler.fill = GridBagConstraints.BOTH;
            JPanel navigationFiller = new JPanel();
            navigationFiller.setOpaque(false);
            links.add(navigationFiller, filler);
            navigation.add(links, BorderLayout.CENTER);

            JPanel footer = new JPanel(new BorderLayout());
            footer.setOpaque(false);
            footer.setBorder(BorderFactory.createEmptyBorder(10, 12, 18, 12));
            JButton logout = navButton("Sign out");
            logout.addActionListener(e -> {
                system.saveToFiles();
                currentUser = null;
                showScreen("WELCOME");
            });
            footer.add(logout);
            navigation.add(footer, BorderLayout.SOUTH);

            userSummary.setText(currentUser.getName() + "  |  "
                    + formatEnum(currentUser.getCommitteeLevel()));
            navigation.revalidate();
            navigation.repaint();
            openView("OVERVIEW", "Overview", overviewView);
            WorkspacePanel.this.revalidate();
            WorkspacePanel.this.repaint();
        }

        private void addNavigation(
                JPanel parent,
                String label,
                String viewName,
                Refreshable view) {
            JButton button = navButton(label);
            button.putClientProperty("viewName", viewName);
            button.setMinimumSize(new Dimension(206, 42));
            button.setPreferredSize(new Dimension(206, 42));
            button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
            button.addActionListener(e -> openView(viewName, label, view));
            GridBagConstraints constraints = new GridBagConstraints();
            constraints.gridx = 0;
            constraints.gridy = navigationButtons.size();
            constraints.weightx = 1;
            constraints.fill = GridBagConstraints.HORIZONTAL;
            constraints.anchor = GridBagConstraints.NORTHWEST;
            constraints.insets = new Insets(0, 0, 5, 0);
            parent.add(button, constraints);
            navigationButtons.add(button);
        }

        private void openView(String name, String title, Refreshable view) {
            pageTitle.setText(title);
            view.refresh();
            viewLayout.show(viewCards, name);
            viewCards.revalidate();
            viewCards.repaint();
            for (JButton button : navigationButtons) {
                boolean active = name.equals(button.getClientProperty("viewName"));
                button.setBackground(active ? PRIMARY : SIDEBAR);
                button.setForeground(Color.WHITE);
                button.repaint();
            }
            navigation.repaint();
        }

        void refreshCurrentViews() {
            overviewView.refresh();
            directoryView.refresh();
            applicationsView.refresh();
            donationsView.refresh();
            electionsView.refresh();
            profileView.refresh();
        }
    }

    private final class OverviewView extends JPanel implements Refreshable {
        private static final long serialVersionUID = 1L;
        private final JLabel greeting = new JLabel();
        private final MetricCard members = new MetricCard("Approved members");
        private final MetricCard pending = new MetricCard("Pending applications");
        private final MetricCard leaders = new MetricCard("Active leaders");
        private final MetricCard elections = new MetricCard("Active elections");
        private final MetricCard donations = new MetricCard("Donations (BDT)");
        private final DefaultTableModel activityModel =
                tableModel("Date", "Donor", "Amount (BDT)");

        OverviewView() {
            setLayout(new BorderLayout());
            setBackground(BACKGROUND);
            JPanel content = new JPanel();
            content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
            content.setBackground(BACKGROUND);
            content.setBorder(BorderFactory.createEmptyBorder(28, 28, 28, 28));

            greeting.setFont(TITLE_FONT);
            greeting.setForeground(TEXT);
            greeting.setAlignmentX(Component.LEFT_ALIGNMENT);
            content.add(greeting);
            content.add(Box.createVerticalStrut(6));
            JLabel subtitle = new JLabel("Here is the latest organization snapshot.");
            subtitle.setForeground(MUTED);
            subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
            content.add(subtitle);
            content.add(Box.createVerticalStrut(24));

            JPanel metrics = new JPanel(new GridLayout(1, 5, 12, 0));
            metrics.setOpaque(false);
            metrics.setMaximumSize(new Dimension(Integer.MAX_VALUE, 112));
            metrics.setAlignmentX(Component.LEFT_ALIGNMENT);
            metrics.add(members);
            metrics.add(pending);
            metrics.add(leaders);
            metrics.add(elections);
            metrics.add(donations);
            content.add(metrics);
            content.add(Box.createVerticalStrut(24));

            JPanel quickActions = surfacePanel();
            quickActions.setLayout(new BorderLayout());
            quickActions.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER),
                    BorderFactory.createEmptyBorder(18, 20, 18, 20)));
            quickActions.setMaximumSize(new Dimension(Integer.MAX_VALUE, 82));
            quickActions.setAlignmentX(Component.LEFT_ALIGNMENT);
            JLabel quickTitle = new JLabel("Quick actions");
            quickTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
            quickTitle.setForeground(TEXT);
            quickActions.add(quickTitle, BorderLayout.WEST);
            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 9, 0));
            buttons.setOpaque(false);
            JButton donate = primaryButton("Record donation");
            donate.addActionListener(e -> workspacePanel.openView(
                    "DONATIONS", "Donations", workspacePanel.donationsView));
            JButton election = secondaryButton("Open election center");
            election.addActionListener(e -> workspacePanel.openView(
                    "ELECTIONS", "Elections", workspacePanel.electionsView));
            buttons.add(election);
            buttons.add(donate);
            quickActions.add(buttons, BorderLayout.EAST);
            content.add(quickActions);
            content.add(Box.createVerticalStrut(24));

            JPanel activity = sectionPanel("Recent donations");
            JTable table = createTable(activityModel);
            activity.add(new JScrollPane(table), BorderLayout.CENTER);
            activity.setPreferredSize(new Dimension(700, 245));
            activity.setMaximumSize(new Dimension(Integer.MAX_VALUE, 245));
            activity.setAlignmentX(Component.LEFT_ALIGNMENT);
            content.add(activity);

            JScrollPane scroll = new JScrollPane(content);
            scroll.setBorder(null);
            scroll.getVerticalScrollBar().setUnitIncrement(18);
            add(scroll, BorderLayout.CENTER);
        }

        @Override
        public void refresh() {
            if (currentUser == null) return;
            greeting.setText("Welcome, " + firstName(currentUser.getName()));
            SystemStats stats = system.getStats();
            members.setValue(String.valueOf(stats.getApprovedMembers()));
            pending.setValue(String.valueOf(stats.getPendingApplications()));
            leaders.setValue(String.valueOf(stats.getLeaders()));
            elections.setValue(String.valueOf(stats.getActiveElections()));
            donations.setValue(compactMoney(stats.getTotalDonations()));
            activityModel.setRowCount(0);
            List<DonationRecord> records = system.getDonationHistory();
            for (int index = 0; index < Math.min(5, records.size()); index++) {
                DonationRecord record = records.get(index);
                activityModel.addRow(new Object[] {
                    record.getFormattedTimestamp(),
                    record.getDonor(),
                    MONEY.format(record.getAmount())
                });
            }
        }
    }

    @SuppressWarnings("serial")
    private final class DirectoryView extends JPanel implements Refreshable {
        private static final long serialVersionUID = 1L;
        private final DefaultTableModel model = tableModel(
                "Name", "Role", "Committee", "Division", "District", "Email", "Donation (BDT)");
        private final JTable table = createTable(model);
        private final TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        private final JTextField search = inputField();
        private final JComboBox<String> roleFilter = new JComboBox<>();
        private final JComboBox<String> divisionFilter = new JComboBox<>();
        private final JPanel managementActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        private List<Member> visibleMembers = new ArrayList<>();

        DirectoryView() {
            setLayout(new BorderLayout(0, 18));
            setBackground(BACKGROUND);
            setBorder(BorderFactory.createEmptyBorder(24, 26, 26, 26));

            JPanel controls = new JPanel(new BorderLayout(12, 0));
            controls.setOpaque(false);
            search.setPreferredSize(new Dimension(290, 38));
            search.putClientProperty("JTextField.placeholderText", "Search members");
            JPanel searchControl = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            searchControl.setOpaque(false);
            searchControl.add(fieldLabel("Search"));
            searchControl.add(search);
            controls.add(searchControl, BorderLayout.WEST);

            JPanel filters = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            filters.setOpaque(false);
            roleFilter.addItem("All roles");
            for (Role role : Role.values()) {
                if (role != Role.ADMIN) roleFilter.addItem(formatEnum(role));
            }
            divisionFilter.addItem("All divisions");
            for (Division division : Division.values()) {
                divisionFilter.addItem(division.name());
            }
            styleCombo(roleFilter);
            styleCombo(divisionFilter);
            filters.add(roleFilter);
            filters.add(divisionFilter);
            controls.add(filters, BorderLayout.EAST);

            table.setRowSorter(sorter);
            JPanel tablePanel = sectionPanel("Organization directory");
            tablePanel.add(new JScrollPane(table), BorderLayout.CENTER);
            add(controls, BorderLayout.NORTH);
            add(tablePanel, BorderLayout.CENTER);

            JPanel actions = new JPanel(new BorderLayout());
            actions.setOpaque(false);
            JButton details = secondaryButton("View details");
            details.addActionListener(e -> showSelectedMember());
            actions.add(details, BorderLayout.WEST);
            managementActions.setOpaque(false);
            actions.add(managementActions, BorderLayout.EAST);
            add(actions, BorderLayout.SOUTH);

            DocumentListener listener = new SimpleDocumentListener(this::applyFilters);
            search.getDocument().addDocumentListener(listener);
            roleFilter.addActionListener(e -> applyFilters());
            divisionFilter.addActionListener(e -> applyFilters());
        }

        @Override
        public void refresh() {
            visibleMembers = system.getAllApprovedMembers();
            model.setRowCount(0);
            for (Member member : visibleMembers) {
                model.addRow(new Object[] {
                    member.getName(),
                    formatEnum(member.getRole()),
                    formatEnum(member.getCommitteeLevel()),
                    member.getAddress().getDivision(),
                    member.getAddress().getDistrict(),
                    member.getEmail(),
                    MONEY.format(member.getDonation())
                });
            }
            rebuildManagementActions();
            applyFilters();
        }

        private void rebuildManagementActions() {
            managementActions.removeAll();
            if (canManageMembers()) {
                JButton promote = primaryButton("Promote");
                promote.addActionListener(e -> promoteSelected());
                JButton demote = secondaryButton("Demote");
                demote.addActionListener(e -> demoteSelected());
                JButton terminate = dangerButton("Terminate");
                terminate.addActionListener(e -> terminateSelected());
                managementActions.add(promote);
                managementActions.add(demote);
                managementActions.add(terminate);
            }
            managementActions.revalidate();
            managementActions.repaint();
        }

        private void applyFilters() {
            String query = search.getText().trim().toLowerCase(Locale.ROOT);
            String selectedRole = (String) roleFilter.getSelectedItem();
            String selectedDivision = (String) divisionFilter.getSelectedItem();
            sorter.setRowFilter(new RowFilter<>() {
                @Override
                public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                    boolean matchesQuery = query.isBlank();
                    for (int column = 0; column < entry.getValueCount() && !matchesQuery; column++) {
                        String value = String.valueOf(entry.getValue(column)).toLowerCase(Locale.ROOT);
                        matchesQuery = value.contains(query);
                    }
                    boolean matchesRole = "All roles".equals(selectedRole)
                            || selectedRole.equals(entry.getStringValue(1));
                    boolean matchesDivision = "All divisions".equals(selectedDivision)
                            || selectedDivision.equals(entry.getStringValue(3));
                    return matchesQuery && matchesRole && matchesDivision;
                }
            });
        }

        private Member selectedMember() {
            int viewRow = table.getSelectedRow();
            if (viewRow < 0) {
                JOptionPane.showMessageDialog(frame, "Select a member first.");
                return null;
            }
            int modelRow = table.convertRowIndexToModel(viewRow);
            return visibleMembers.get(modelRow);
        }

        private void showSelectedMember() {
            Member member = selectedMember();
            if (member != null) {
                JOptionPane.showMessageDialog(
                        frame,
                        profileText(member),
                        "Member details",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        }

        private void promoteSelected() {
            Member member = selectedMember();
            if (member == null) return;
            Role role = chooseLeadershipRole();
            if (role == null) return;
            CommitteeLevel level = chooseCommitteeLevel();
            if (level == null) return;
            Division division = member.getAddress().getDivision();
            District district = null;
            if (level == CommitteeLevel.DISTRICT) {
                division = chooseDivision();
                if (division == null) return;
                district = chooseDistrict(division);
                if (district == null) return;
            }
            boolean success = system.promoteToLeader(
                    member.getNationalId(), level, role, division, district);
            afterMutation(success ? "Member promoted successfully." : "Unable to promote member.");
        }

        private void demoteSelected() {
            Member member = selectedMember();
            if (member == null) return;
            boolean success = system.demoteLeader(member.getEmail());
            afterMutation(success ? "Leader demoted to member." : "Selected person is not a leader.");
        }

        private void terminateSelected() {
            Member member = selectedMember();
            if (member == null) return;
            int choice = JOptionPane.showConfirmDialog(
                    frame,
                    "Terminate membership for " + member.getName() + "?",
                    "Confirm termination",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (choice != JOptionPane.YES_OPTION) return;
            boolean success = system.terminateMembershipID(member.getNationalId());
            afterMutation(success ? "Membership terminated." : "Unable to terminate membership.");
        }

        private void afterMutation(String message) {
            system.saveToFiles();
            workspacePanel.refreshCurrentViews();
            JOptionPane.showMessageDialog(frame, message);
        }
    }

    @SuppressWarnings("serial")
    private final class ApplicationsView extends JPanel implements Refreshable {
        private static final long serialVersionUID = 1L;
        private final DefaultTableModel model = tableModel(
                "Applicant", "National ID", "Email", "Profession", "Division", "District");
        private final JTable table = createTable(model);
        private final JLabel count = new JLabel();
        private List<Member> applications = new ArrayList<>();

        ApplicationsView() {
            setLayout(new BorderLayout(0, 18));
            setBackground(BACKGROUND);
            setBorder(BorderFactory.createEmptyBorder(24, 26, 26, 26));

            JPanel summary = new JPanel(new BorderLayout());
            summary.setOpaque(false);
            JLabel title = new JLabel("Pending membership applications");
            title.setFont(new Font("Segoe UI", Font.BOLD, 18));
            title.setForeground(TEXT);
            summary.add(title, BorderLayout.WEST);
            count.setForeground(MUTED);
            summary.add(count, BorderLayout.EAST);
            add(summary, BorderLayout.NORTH);

            JPanel tablePanel = sectionPanel("Review queue");
            tablePanel.add(new JScrollPane(table), BorderLayout.CENTER);
            add(tablePanel, BorderLayout.CENTER);

            JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 9, 0));
            actions.setOpaque(false);
            JButton reject = dangerButton("Reject");
            reject.addActionListener(e -> process(false));
            JButton approve = primaryButton("Approve");
            approve.addActionListener(e -> process(true));
            actions.add(reject);
            actions.add(approve);
            add(actions, BorderLayout.SOUTH);
        }

        @Override
        public void refresh() {
            applications = system.getAllPendingApplications();
            model.setRowCount(0);
            for (Member member : applications) {
                model.addRow(new Object[] {
                    member.getName(),
                    member.getNationalId(),
                    member.getEmail(),
                    member.getProfession(),
                    member.getAddress().getDivision(),
                    member.getAddress().getDistrict()
                });
            }
            count.setText(applications.size() + " waiting");
        }

        private void process(boolean approve) {
            int row = table.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(frame, "Select an application first.");
                return;
            }
            Member member = applications.get(table.convertRowIndexToModel(row));
            boolean success = approve
                    ? system.approveApplication(member.getNationalId())
                    : system.rejectApplication(member.getNationalId());
            if (success) {
                system.saveToFiles();
                workspacePanel.refreshCurrentViews();
            }
            JOptionPane.showMessageDialog(
                    frame,
                    success
                            ? member.getName() + (approve ? " was approved." : " was rejected.")
                            : "The application could not be processed.");
        }
    }

    private final class DonationsView extends JPanel implements Refreshable {
        private static final long serialVersionUID = 1L;
        private final JLabel totalValue = new JLabel();
        private final JLabel personalValue = new JLabel();
        private final JTextField amountField = inputField();
        private final JLabel status = statusLabel();
        private final DefaultTableModel model = tableModel("Date", "Donor", "Amount (BDT)");

        DonationsView() {
            setLayout(new BorderLayout(18, 18));
            setBackground(BACKGROUND);
            setBorder(BorderFactory.createEmptyBorder(24, 26, 26, 26));

            JPanel summary = new JPanel(new GridLayout(1, 2, 14, 0));
            summary.setOpaque(false);
            summary.add(valuePanel("Organization total", totalValue));
            summary.add(valuePanel("Your contribution", personalValue));
            add(summary, BorderLayout.NORTH);

            JPanel donationForm = surfacePanel();
            donationForm.setLayout(new BoxLayout(donationForm, BoxLayout.Y_AXIS));
            donationForm.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER),
                    BorderFactory.createEmptyBorder(24, 24, 24, 24)));
            donationForm.setPreferredSize(new Dimension(330, 0));
            JLabel title = new JLabel("Record a donation");
            title.setFont(new Font("Segoe UI", Font.BOLD, 18));
            title.setForeground(TEXT);
            title.setAlignmentX(Component.LEFT_ALIGNMENT);
            donationForm.add(title);
            donationForm.add(Box.createVerticalStrut(22));
            JLabel amountLabel = fieldLabel("Amount (BDT)");
            amountLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            donationForm.add(amountLabel);
            donationForm.add(Box.createVerticalStrut(7));
            amountField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
            amountField.setAlignmentX(Component.LEFT_ALIGNMENT);
            donationForm.add(amountField);
            donationForm.add(Box.createVerticalStrut(10));
            JButton suggested = textButton("Use suggested 5% amount");
            suggested.setAlignmentX(Component.LEFT_ALIGNMENT);
            suggested.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
            suggested.addActionListener(e -> amountField.setText(
                    MONEY.format(Member.calculateDonation(currentUser.getYearlyIncome()))
                            .replace(",", "")));
            donationForm.add(suggested);
            donationForm.add(Box.createVerticalStrut(24));
            JButton record = primaryButton("Record donation");
            record.setAlignmentX(Component.LEFT_ALIGNMENT);
            record.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
            record.addActionListener(e -> recordDonation());
            donationForm.add(record);
            donationForm.add(Box.createVerticalStrut(14));
            status.setAlignmentX(Component.LEFT_ALIGNMENT);
            donationForm.add(status);

            JPanel history = sectionPanel("Donation history");
            history.add(new JScrollPane(createTable(model)), BorderLayout.CENTER);
            add(donationForm, BorderLayout.WEST);
            add(history, BorderLayout.CENTER);
        }

        @Override
        public void refresh() {
            if (currentUser == null) return;
            totalValue.setText("BDT " + MONEY.format(system.getDonations()));
            personalValue.setText("BDT " + MONEY.format(currentUser.getDonation()));
            setStatus(status, "Donations are saved to the organization ledger.", MUTED);
            model.setRowCount(0);
            for (DonationRecord record : system.getDonationHistory()) {
                model.addRow(new Object[] {
                    record.getFormattedTimestamp(),
                    record.getDonor(),
                    MONEY.format(record.getAmount())
                });
            }
        }

        private void recordDonation() {
            try {
                double amount = Double.parseDouble(amountField.getText().trim().replace(",", ""));
                system.recordDonation(currentUser, amount);
                system.saveToFiles();
                amountField.setText("");
                workspacePanel.refreshCurrentViews();
                setStatus(status, "Donation recorded successfully.", PRIMARY);
            } catch (NumberFormatException exception) {
                setStatus(status, "Enter a valid donation amount.", DANGER);
            } catch (InvalidDonationException exception) {
                setStatus(status, exception.getMessage(), DANGER);
            }
        }
    }

    @SuppressWarnings("serial")
    private final class ElectionsView extends JPanel implements Refreshable {
        private static final long serialVersionUID = 1L;
        private final JComboBox<CommitteeOption> committeeBox = new JComboBox<>();
        private final JLabel status = new JLabel();
        private final DefaultTableModel model =
                tableModel("Role", "Candidate", "District", "Votes");
        private final JTable table = createTable(model);
        private final JComboBox<Role> candidateRoleBox =
                new JComboBox<>(leadershipRoles().toArray(new Role[0]));
        private final JPanel managementActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        private final JButton registerButton = secondaryButton("Register as candidate");
        private final JButton voteButton = primaryButton("Vote for selected");
        private List<CandidateEntry> candidates = new ArrayList<>();

        ElectionsView() {
            setLayout(new BorderLayout(0, 18));
            setBackground(BACKGROUND);
            setBorder(BorderFactory.createEmptyBorder(24, 26, 26, 26));
            styleCombo(committeeBox);
            styleCombo(candidateRoleBox);

            JPanel selector = surfacePanel();
            selector.setLayout(new BorderLayout(14, 0));
            selector.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER),
                    BorderFactory.createEmptyBorder(15, 18, 15, 18)));
            JPanel selection = new JPanel(new FlowLayout(FlowLayout.LEFT, 9, 0));
            selection.setOpaque(false);
            selection.add(fieldLabel("Committee"));
            committeeBox.setPreferredSize(new Dimension(310, 38));
            selection.add(committeeBox);
            status.setFont(FONT_MEDIUM);
            status.setBorder(BorderFactory.createEmptyBorder(9, 10, 9, 10));
            selection.add(status);
            selector.add(selection, BorderLayout.WEST);
            managementActions.setOpaque(false);
            selector.add(managementActions, BorderLayout.EAST);
            add(selector, BorderLayout.NORTH);

            JPanel tablePanel = sectionPanel("Candidates and live vote count");
            tablePanel.add(new JScrollPane(table), BorderLayout.CENTER);
            add(tablePanel, BorderLayout.CENTER);

            JPanel actions = new JPanel(new BorderLayout());
            actions.setOpaque(false);
            JPanel registration = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            registration.setOpaque(false);
            registration.add(candidateRoleBox);
            registerButton.addActionListener(e -> registerCandidate());
            registration.add(registerButton);
            actions.add(registration, BorderLayout.WEST);
            voteButton.addActionListener(e -> vote());
            actions.add(voteButton, BorderLayout.EAST);
            add(actions, BorderLayout.SOUTH);

            committeeBox.addActionListener(e -> refreshElection());
        }

        @Override
        public void refresh() {
            CommitteeOption previous = (CommitteeOption) committeeBox.getSelectedItem();
            committeeBox.removeAllItems();
            committeeBox.addItem(new CommitteeOption(
                    "National Central Committee", system.getCentralCommittee()));
            if (currentUser != null && currentUser.getRole() == Role.ADMIN) {
                for (Division division : Division.values()) {
                    committeeBox.addItem(new CommitteeOption(
                            division + " Divisional Committee",
                            system.getDivisionalCommittee(division)));
                }
            } else if (currentUser != null) {
                Division division = currentUser.getAddress().getDivision();
                committeeBox.addItem(new CommitteeOption(
                        division + " Divisional Committee",
                        system.getDivisionalCommittee(division)));
            }
            if (currentUser != null) {
                District district = currentUser.getAddress().getDistrict();
                committeeBox.addItem(new CommitteeOption(
                        district + " District Committee",
                        system.getDistrictCommittee(district)));
            }
            if (previous != null) {
                selectCommittee(previous.committee);
            }
            rebuildElectionManagement();
            refreshElection();
        }

        private void rebuildElectionManagement() {
            managementActions.removeAll();
            if (canManageElections()) {
                JButton declare = primaryButton("Declare election");
                declare.addActionListener(e -> declareElection());
                JButton close = dangerButton("Close election");
                close.addActionListener(e -> closeElection());
                managementActions.add(declare);
                managementActions.add(close);
            }
            managementActions.revalidate();
            managementActions.repaint();
        }

        private void refreshElection() {
            CommitteeOption option = (CommitteeOption) committeeBox.getSelectedItem();
            model.setRowCount(0);
            candidates = new ArrayList<>();
            if (option == null) return;
            Election election = electionFor(option.committee);
            boolean declared = election.isDeclared();
            status.setText(declared ? "OPEN" : "CLOSED");
            status.setForeground(declared ? PRIMARY_DARK : MUTED);
            status.setBackground(declared ? SUCCESS_BG : new Color(235, 238, 236));
            status.setOpaque(true);
            registerButton.setEnabled(declared && currentUser.getRole() != Role.ADMIN);
            voteButton.setEnabled(declared);
            for (Map.Entry<Role, List<Member>> entry : election.getAllCandidates().entrySet()) {
                for (Member candidate : entry.getValue()) {
                    CandidateEntry candidateEntry = new CandidateEntry(entry.getKey(), candidate);
                    candidates.add(candidateEntry);
                    model.addRow(new Object[] {
                        formatEnum(entry.getKey()),
                        candidate.getName(),
                        candidate.getAddress().getDistrict(),
                        election.getVoteCount(entry.getKey(), candidate)
                    });
                }
            }
        }

        private void declareElection() {
            CommitteeOption option = (CommitteeOption) committeeBox.getSelectedItem();
            if (option == null) return;
            boolean success = system.declareElection(option.committee, currentUser);
            workspacePanel.refreshCurrentViews();
            JOptionPane.showMessageDialog(
                    frame,
                    success ? "Election declared." : "You cannot declare this election.");
        }

        private void closeElection() {
            CommitteeOption option = (CommitteeOption) committeeBox.getSelectedItem();
            if (option == null) return;
            boolean success = system.closeElection(option.committee, currentUser);
            system.saveToFiles();
            workspacePanel.refreshCurrentViews();
            JOptionPane.showMessageDialog(
                    frame,
                    success ? "Election closed and winners assigned." : "Election could not be closed.");
        }

        private void registerCandidate() {
            CommitteeOption option = (CommitteeOption) committeeBox.getSelectedItem();
            Role role = (Role) candidateRoleBox.getSelectedItem();
            if (option == null || role == null) return;
            CommitteeLevel level = committeeLevel(option.committee);
            boolean success = system.applyForLeadership(currentUser, role, level);
            refreshElection();
            JOptionPane.showMessageDialog(
                    frame,
                    success ? "Candidate registration completed." : "Unable to register for this election.");
        }

        private void vote() {
            int row = table.getSelectedRow();
            CommitteeOption option = (CommitteeOption) committeeBox.getSelectedItem();
            if (row < 0 || option == null) {
                JOptionPane.showMessageDialog(frame, "Select a candidate first.");
                return;
            }
            CandidateEntry selected = candidates.get(table.convertRowIndexToModel(row));
            boolean success = system.vote(
                    option.committee, selected.role, selected.member, currentUser);
            refreshElection();
            JOptionPane.showMessageDialog(
                    frame,
                    success ? "Your vote was recorded." : "Vote could not be recorded.");
        }

        private void selectCommittee(Committee committee) {
            for (int index = 0; index < committeeBox.getItemCount(); index++) {
                CommitteeOption option = committeeBox.getItemAt(index);
                if (option.committee == committee) {
                    committeeBox.setSelectedIndex(index);
                    return;
                }
            }
        }
    }

    private final class ProfileView extends JPanel implements Refreshable {
        private static final long serialVersionUID = 1L;
        private final JLabel nameValue = new JLabel();
        private final JLabel roleValue = new JLabel();
        private final JLabel locationValue = new JLabel();
        private final JTextField emailField = inputField();
        private final JTextField phoneField = inputField();
        private final JTextField professionField = inputField();
        private final JPasswordField passwordField = passwordField();
        private final JLabel status = statusLabel();

        ProfileView() {
            setLayout(new BorderLayout());
            setBackground(BACKGROUND);
            setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

            JPanel profile = surfacePanel();
            profile.setLayout(new BorderLayout());
            profile.setBorder(BorderFactory.createLineBorder(BORDER));

            JPanel identity = new JPanel();
            identity.setLayout(new BoxLayout(identity, BoxLayout.Y_AXIS));
            identity.setBackground(new Color(238, 243, 240));
            identity.setBorder(BorderFactory.createEmptyBorder(36, 32, 36, 32));
            identity.setPreferredSize(new Dimension(320, 0));
            JLabel avatar = new JLabel("", SwingConstants.CENTER);
            avatar.setFont(new Font("Segoe UI", Font.BOLD, 34));
            avatar.setForeground(Color.WHITE);
            avatar.setBackground(PRIMARY);
            avatar.setOpaque(true);
            avatar.setPreferredSize(new Dimension(76, 76));
            avatar.setMaximumSize(new Dimension(76, 76));
            avatar.setAlignmentX(Component.LEFT_ALIGNMENT);
            identity.add(avatar);
            identity.add(Box.createVerticalStrut(20));
            nameValue.setFont(new Font("Segoe UI", Font.BOLD, 23));
            nameValue.setForeground(TEXT);
            nameValue.setAlignmentX(Component.LEFT_ALIGNMENT);
            identity.add(nameValue);
            identity.add(Box.createVerticalStrut(8));
            roleValue.setForeground(PRIMARY_DARK);
            roleValue.setFont(FONT_MEDIUM);
            roleValue.setAlignmentX(Component.LEFT_ALIGNMENT);
            identity.add(roleValue);
            identity.add(Box.createVerticalStrut(8));
            locationValue.setForeground(MUTED);
            locationValue.setAlignmentX(Component.LEFT_ALIGNMENT);
            identity.add(locationValue);
            identity.putClientProperty("avatar", avatar);
            profile.add(identity, BorderLayout.WEST);

            JPanel form = new JPanel(new GridBagLayout());
            form.setBackground(SURFACE);
            form.setBorder(BorderFactory.createEmptyBorder(34, 42, 34, 42));
            GridBagConstraints gc = new GridBagConstraints();
            gc.fill = GridBagConstraints.HORIZONTAL;
            gc.weightx = 1;
            addSingleFormField(form, gc, 0, "Email address", emailField);
            addSingleFormField(form, gc, 2, "Phone number", phoneField);
            addSingleFormField(form, gc, 4, "Profession", professionField);
            addSingleFormField(form, gc, 6, "New password", passwordField);
            gc.gridx = 0;
            gc.gridy = 8;
            gc.insets = new Insets(4, 0, 15, 0);
            form.add(status, gc);
            gc.gridy = 9;
            gc.anchor = GridBagConstraints.EAST;
            gc.fill = GridBagConstraints.NONE;
            JButton save = primaryButton("Save profile");
            save.addActionListener(e -> saveProfile());
            form.add(save, gc);
            profile.add(form, BorderLayout.CENTER);
            add(profile, BorderLayout.CENTER);
        }

        @Override
        public void refresh() {
            if (currentUser == null) return;
            nameValue.setText(currentUser.getName());
            roleValue.setText(formatEnum(currentUser.getRole()) + "  |  "
                    + formatEnum(currentUser.getCommitteeLevel()));
            locationValue.setText(currentUser.getAddress().getDistrict() + ", "
                    + currentUser.getAddress().getDivision());
            emailField.setText(currentUser.getEmail());
            phoneField.setText(currentUser.getPhone());
            professionField.setText(currentUser.getProfession());
            passwordField.setText("");
            setStatus(status, "Leave the password blank to keep it unchanged.", MUTED);
            Component west = ((BorderLayout) ((JPanel) getComponent(0)).getLayout())
                    .getLayoutComponent(BorderLayout.WEST);
            if (west instanceof JPanel) {
                Object avatarObject = ((JPanel) west).getClientProperty("avatar");
                if (avatarObject instanceof JLabel) {
                    ((JLabel) avatarObject).setText(initials(currentUser.getName()));
                }
            }
        }

        private void saveProfile() {
            String email = emailField.getText().trim();
            String phone = phoneField.getText().trim();
            String profession = professionField.getText().trim();
            String password = new String(passwordField.getPassword());
            if (!EMAIL_PATTERN.matcher(email).matches()) {
                setStatus(status, "Enter a valid email address.", DANGER);
                return;
            }
            Member existing = system.findByEmail(email);
            if (existing != null && existing != currentUser) {
                setStatus(status, "That email address is already in use.", DANGER);
                return;
            }
            if (phone.length() < 10 || profession.isBlank()) {
                setStatus(status, "Enter a valid phone number and profession.", DANGER);
                return;
            }
            if (!password.isBlank() && password.length() < 6) {
                setStatus(status, "New password must contain at least 6 characters.", DANGER);
                return;
            }
            currentUser.setEmail(email);
            currentUser.setPhone(phone);
            currentUser.setProfession(profession);
            if (!password.isBlank()) currentUser.setPassword(password);
            system.saveToFiles();
            passwordField.setText("");
            workspacePanel.userSummary.setText(currentUser.getName() + "  |  "
                    + formatEnum(currentUser.getCommitteeLevel()));
            setStatus(status, "Profile saved.", PRIMARY);
        }
    }

    private void guestDonate() {
        JTextField email = inputField();
        JTextField amount = inputField();
        JPanel form = new JPanel(new GridLayout(0, 1, 0, 7));
        form.add(fieldLabel("Member email (optional)"));
        form.add(email);
        form.add(fieldLabel("Donation amount (BDT)"));
        form.add(amount);
        int choice = JOptionPane.showConfirmDialog(
                frame,
                form,
                "Guest donation",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) return;
        Member member = email.getText().isBlank()
                ? null
                : system.findByEmail(email.getText().trim());
        if (member != null && !member.isApproved()) member = null;
        try {
            double donationAmount = Double.parseDouble(amount.getText().trim().replace(",", ""));
            system.recordDonation(member, donationAmount);
            system.saveToFiles();
            welcomePanel.refresh();
            JOptionPane.showMessageDialog(frame, "Donation recorded. Thank you.");
        } catch (NumberFormatException exception) {
            JOptionPane.showMessageDialog(frame, "Enter a valid donation amount.");
        } catch (InvalidDonationException exception) {
            JOptionPane.showMessageDialog(frame, exception.getMessage());
        }
    }

    private boolean canManageMembers() {
        return currentUser != null
                && (currentUser.getRole() == Role.ADMIN
                        || (currentUser.getRole() == Role.PRESIDENT
                                && currentUser.getCommitteeLevel() == CommitteeLevel.CENTRAL));
    }

    private boolean canManageElections() {
        return currentUser != null
                && (currentUser.getRole() == Role.ADMIN
                        || currentUser.getRole() == Role.PRESIDENT);
    }

    private Role chooseLeadershipRole() {
        return (Role) JOptionPane.showInputDialog(
                frame,
                "Leadership role",
                "Promote member",
                JOptionPane.PLAIN_MESSAGE,
                null,
                leadershipRoles().toArray(),
                null);
    }

    private CommitteeLevel chooseCommitteeLevel() {
        return (CommitteeLevel) JOptionPane.showInputDialog(
                frame,
                "Committee level",
                "Promote member",
                JOptionPane.PLAIN_MESSAGE,
                null,
                CommitteeLevel.values(),
                CommitteeLevel.DISTRICT);
    }

    private Division chooseDivision() {
        return (Division) JOptionPane.showInputDialog(
                frame,
                "Division",
                "Select division",
                JOptionPane.PLAIN_MESSAGE,
                null,
                Division.values(),
                Division.Dhaka);
    }

    private District chooseDistrict(Division division) {
        List<District> districts = new ArrayList<>();
        for (District district : District.values()) {
            if (district.getDivision() == division) districts.add(district);
        }
        return (District) JOptionPane.showInputDialog(
                frame,
                "District",
                "Select district",
                JOptionPane.PLAIN_MESSAGE,
                null,
                districts.toArray(),
                null);
    }

    private List<Role> leadershipRoles() {
        List<Role> roles = new ArrayList<>();
        for (Role role : Role.values()) {
            if (role != Role.ADMIN && role != Role.MEMBER) roles.add(role);
        }
        return roles;
    }

    private Election electionFor(Committee committee) {
        return committee.getElection();
    }

    private CommitteeLevel committeeLevel(Committee committee) {
        if (committee instanceof CentralCommittee) return CommitteeLevel.CENTRAL;
        if (committee instanceof DivisionalCommittee) return CommitteeLevel.DIVISIONAL;
        return CommitteeLevel.DISTRICT;
    }

    private String profileText(Member member) {
        return "Name: " + member.getName() + "\n"
                + "Role: " + formatEnum(member.getRole()) + "\n"
                + "Committee: " + formatEnum(member.getCommitteeLevel()) + "\n"
                + "National ID: " + member.getNationalId() + "\n"
                + "Email: " + member.getEmail() + "\n"
                + "Phone: " + member.getPhone() + "\n"
                + "Profession: " + member.getProfession() + "\n"
                + "Location: " + member.getAddress().getDistrict() + ", "
                + member.getAddress().getDivision() + "\n"
                + "Yearly income: BDT " + MONEY.format(member.getYearlyIncome()) + "\n"
                + "Total donation: BDT " + MONEY.format(member.getDonation());
    }

    private JPanel pageHeader(
            String title,
            String subtitle,
            String actionLabel,
            java.awt.event.ActionListener action) {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(SURFACE);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER),
                BorderFactory.createEmptyBorder(15, 25, 15, 25)));
        JButton back = secondaryButton(actionLabel);
        back.addActionListener(action);
        header.add(back, BorderLayout.WEST);
        JPanel titles = new JPanel();
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        titles.setOpaque(false);
        JLabel heading = new JLabel(title);
        heading.setFont(new Font("Segoe UI", Font.BOLD, 20));
        heading.setForeground(TEXT);
        heading.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel copy = new JLabel(subtitle);
        copy.setForeground(MUTED);
        copy.setAlignmentX(Component.CENTER_ALIGNMENT);
        titles.add(heading);
        titles.add(Box.createVerticalStrut(3));
        titles.add(copy);
        header.add(titles, BorderLayout.CENTER);
        header.add(Box.createHorizontalStrut(back.getPreferredSize().width), BorderLayout.EAST);
        return header;
    }

    private JPanel surfacePanel() {
        JPanel panel = new JPanel();
        panel.setBackground(SURFACE);
        return panel;
    }

    private JPanel sectionPanel(String title) {
        JPanel panel = surfacePanel();
        panel.setLayout(new BorderLayout(0, 12));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(17, 18, 18, 18)));
        JLabel heading = new JLabel(title);
        heading.setFont(new Font("Segoe UI", Font.BOLD, 16));
        heading.setForeground(TEXT);
        panel.add(heading, BorderLayout.NORTH);
        return panel;
    }

    private JPanel smallMetric(String label, JLabel value) {
        JPanel panel = surfacePanel();
        panel.setLayout(new BorderLayout(0, 5));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(13, 14, 13, 14)));
        JLabel name = new JLabel(label);
        name.setForeground(MUTED);
        name.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        panel.add(name, BorderLayout.NORTH);
        panel.add(value, BorderLayout.CENTER);
        return panel;
    }

    private JPanel valuePanel(String label, JLabel value) {
        JPanel panel = surfacePanel();
        panel.setLayout(new BorderLayout(0, 8));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(18, 22, 18, 22)));
        JLabel name = new JLabel(label);
        name.setForeground(MUTED);
        value.setFont(new Font("Segoe UI", Font.BOLD, 24));
        value.setForeground(TEXT);
        panel.add(name, BorderLayout.NORTH);
        panel.add(value, BorderLayout.CENTER);
        return panel;
    }

    private static JLabel metricValueLabel() {
        JLabel label = new JLabel("0");
        label.setFont(new Font("Segoe UI", Font.BOLD, 22));
        label.setForeground(TEXT);
        return label;
    }

    private JTextField inputField() {
        JTextField field = new JTextField();
        styleInput(field);
        return field;
    }

    private JPasswordField passwordField() {
        JPasswordField field = new JPasswordField();
        styleInput(field);
        return field;
    }

    private void styleInput(JTextField field) {
        field.setFont(FONT);
        field.setForeground(TEXT);
        field.setBackground(SURFACE);
        field.setCaretColor(PRIMARY);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        field.setPreferredSize(new Dimension(220, 40));
    }

    private void styleCombo(JComboBox<?> comboBox) {
        comboBox.setFont(FONT);
        comboBox.setBackground(SURFACE);
        comboBox.setForeground(TEXT);
        comboBox.setBorder(BorderFactory.createLineBorder(BORDER));
        comboBox.setPreferredSize(new Dimension(190, 40));
    }

    private JLabel fieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(FONT_MEDIUM);
        label.setForeground(TEXT);
        return label;
    }

    private JLabel statusLabel() {
        JLabel label = new JLabel();
        label.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        return label;
    }

    private void setStatus(JLabel label, String message, Color color) {
        label.setText(message);
        label.setForeground(color);
    }

    private JButton primaryButton(String text) {
        return styledButton(text, PRIMARY, Color.WHITE, PRIMARY);
    }

    private JButton secondaryButton(String text) {
        return styledButton(text, SURFACE, TEXT, BORDER);
    }

    private JButton dangerButton(String text) {
        return styledButton(text, DANGER, Color.WHITE, DANGER);
    }

    private JButton textButton(String text) {
        JButton button = styledButton(text, BACKGROUND, PRIMARY_DARK, BACKGROUND);
        button.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
        button.setHorizontalAlignment(SwingConstants.LEFT);
        return button;
    }

    private JButton styledButton(String text, Color background, Color foreground, Color border) {
        JButton button = new JButton(text);
        button.setFont(FONT_MEDIUM);
        button.setForeground(foreground);
        button.setBackground(background);
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(border),
                BorderFactory.createEmptyBorder(9, 16, 9, 16)));
        return button;
    }

    private JButton navButton(String text) {
        JButton button = new JButton(text);
        button.setUI(new BasicButtonUI());
        button.setFont(FONT_MEDIUM);
        button.setForeground(new Color(213, 223, 217));
        button.setBackground(SIDEBAR);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createEmptyBorder(11, 13, 11, 13));
        button.setMinimumSize(new Dimension(206, 42));
        button.setPreferredSize(new Dimension(206, 42));
        return button;
    }

    private void addFormField(
            JPanel form,
            GridBagConstraints constraints,
            int x,
            int y,
            String label,
            JComponent field) {
        constraints.gridx = x;
        constraints.gridy = y;
        constraints.gridwidth = 1;
        constraints.insets = new Insets(0, 0, 7, x == 0 ? 18 : 0);
        form.add(fieldLabel(label), constraints);
        constraints.gridy = y + 1;
        constraints.insets = new Insets(0, 0, 17, x == 0 ? 18 : 0);
        form.add(field, constraints);
    }

    private void addSingleFormField(
            JPanel form,
            GridBagConstraints constraints,
            int y,
            String label,
            JComponent field) {
        constraints.gridx = 0;
        constraints.gridy = y;
        constraints.gridwidth = 1;
        constraints.insets = new Insets(0, 0, 7, 0);
        form.add(fieldLabel(label), constraints);
        constraints.gridy = y + 1;
        constraints.insets = new Insets(0, 0, 17, 0);
        form.add(field, constraints);
    }

    private DefaultTableModel tableModel(String... columns) {
        return new DefaultTableModel(columns, 0) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private JTable createTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setFont(FONT);
        table.setForeground(TEXT);
        table.setBackground(SURFACE);
        table.setGridColor(new Color(235, 239, 237));
        table.setShowVerticalLines(false);
        table.setRowHeight(34);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setSelectionBackground(new Color(220, 239, 230));
        table.setSelectionForeground(TEXT);
        table.setFillsViewportHeight(true);
        JTableHeader header = table.getTableHeader();
        header.setFont(FONT_MEDIUM);
        header.setForeground(TEXT);
        header.setBackground(new Color(238, 242, 239));
        header.setPreferredSize(new Dimension(0, 38));
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
        renderer.setBorder(BorderFactory.createEmptyBorder(0, 9, 0, 9));
        for (int column = 0; column < table.getColumnCount(); column++) {
            table.getColumnModel().getColumn(column).setCellRenderer(renderer);
        }
        return table;
    }

    private String formatEnum(Enum<?> value) {
        if (value == null) return "Not assigned";
        String[] words = value.name().toLowerCase(Locale.ROOT).split("_");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (result.length() > 0) result.append(' ');
            result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return result.toString();
    }

    private String compactMoney(double amount) {
        double absolute = Math.abs(amount);
        if (absolute >= 1_000_000_000) return MONEY.format(amount / 1_000_000_000) + "B";
        if (absolute >= 1_000_000) return MONEY.format(amount / 1_000_000) + "M";
        if (absolute >= 1_000) return MONEY.format(amount / 1_000) + "K";
        return MONEY.format(amount);
    }

    private String firstName(String name) {
        if (name == null || name.isBlank()) return "Member";
        return name.trim().split("\\s+")[0];
    }

    private String initials(String name) {
        if (name == null || name.isBlank()) return "M";
        String[] words = name.trim().split("\\s+");
        String first = words[0].substring(0, 1);
        String second = words.length > 1 ? words[words.length - 1].substring(0, 1) : "";
        return (first + second).toUpperCase(Locale.ROOT);
    }

    private interface Refreshable {
        void refresh();
    }

    private static final class MetricCard extends JPanel {
        private static final long serialVersionUID = 1L;
        private final JLabel value = new JLabel("0");

        MetricCard(String title) {
            setLayout(new BorderLayout(0, 8));
            setBackground(SURFACE);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER),
                    BorderFactory.createEmptyBorder(16, 17, 16, 17)));
            JLabel label = new JLabel(title);
            label.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            label.setForeground(MUTED);
            value.setFont(new Font("Segoe UI", Font.BOLD, 23));
            value.setForeground(TEXT);
            add(label, BorderLayout.NORTH);
            add(value, BorderLayout.CENTER);
        }

        void setValue(String text) {
            value.setText(text);
        }
    }

    private static final class CommitteeOption {
        private final String label;
        private final Committee committee;

        CommitteeOption(String label, Committee committee) {
            this.label = label;
            this.committee = committee;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private static final class CandidateEntry {
        private final Role role;
        private final Member member;

        CandidateEntry(Role role, Member member) {
            this.role = role;
            this.member = member;
        }
    }

    private static final class SimpleDocumentListener implements DocumentListener {
        private final Runnable action;

        SimpleDocumentListener(Runnable action) {
            this.action = action;
        }

        @Override
        public void insertUpdate(DocumentEvent event) {
            action.run();
        }

        @Override
        public void removeUpdate(DocumentEvent event) {
            action.run();
        }

        @Override
        public void changedUpdate(DocumentEvent event) {
            action.run();
        }
    }
}
