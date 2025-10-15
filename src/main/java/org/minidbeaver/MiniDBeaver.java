package org.minidbeaver;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

public class MiniDBeaver extends JFrame {
    private static final Preferences PREF = Preferences.userRoot().node("MiniDBeaver");
    private static final String PREF_LAST_TEXT = "lastEditorText";
    private static final String PREF_CATALOG = "catalog";

    private final JTextArea editor = new JTextArea();
    private final JButton btnExecLine = createButtonWithIcon("execute-line.png");
    private final JButton btnExecSel = createButtonWithIcon("execute-selection.png");
    private final JButton btnExecAll = createButtonWithIcon("execute-all.png");
    private final JButton btnClear = createButtonWithIcon("clear.png");
    private final JButton btnAdminInfo = createButtonWithIcon("star.png");
    private final JButton btnDatabaseSett = createButtonWithIcon("database.png");
    private final JLabel lblStatus = new JLabel("Desconectado");
    private final JTabbedPane resultsTabs = new JTabbedPane();

    private Connection conn;
    private String currentCatalog;

    public MiniDBeaver() {
        super("Mini DBeaver - By Jose Manuel Castillo Queh");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setSize(screenSize.width, screenSize.height);
        setLocation(0, 0);

        setLayout(new BorderLayout());
        add(buildCenterPanel(), BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);

        wireActions();
        restoreState();
    }

    private JButton createButtonWithIcon(String imagePath) {
        JButton button = new JButton();
        ImageIcon icon = new ImageIcon(getClass().getResource("/icons/" + imagePath));
        Image img = icon.getImage().getScaledInstance(35, 35, Image.SCALE_SMOOTH);
        button.setIcon(new ImageIcon(img));
        button.setBorder(BorderFactory.createEmptyBorder());
        button.setFocusable(false);
        return button;
    }

    private Component buildCenterPanel() {
        editor.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 18));
        editor.setLineWrap(false);
        JScrollPane editorScroll = new JScrollPane(editor);

        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setBorder(new EmptyBorder(8, 8, 8, 8));
        leftPanel.add(btnDatabaseSett);
        leftPanel.add(Box.createVerticalStrut(6));
        leftPanel.add(btnExecLine);
        leftPanel.add(Box.createVerticalStrut(6));
        leftPanel.add(btnExecSel);
        leftPanel.add(Box.createVerticalStrut(6));
        leftPanel.add(btnExecAll);
        leftPanel.add(Box.createVerticalStrut(6));
        leftPanel.add(btnClear);
        leftPanel.add(Box.createVerticalStrut(6));
        leftPanel.add(btnAdminInfo);
        leftPanel.add(Box.createVerticalGlue());

        // Fijar ancho del panel
        int fixedWidth = 55;
        leftPanel.setPreferredSize(new Dimension(fixedWidth, 0));
        leftPanel.setMinimumSize(new Dimension(fixedWidth, 0));
        leftPanel.setMaximumSize(new Dimension(fixedWidth, Integer.MAX_VALUE));

        // Panel contenedor para leftPanel y editor sin JSplitPane
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(leftPanel, BorderLayout.WEST);
        topPanel.add(editorScroll, BorderLayout.CENTER);

        resultsTabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

        JSplitPane center = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topPanel, resultsTabs);
        center.setResizeWeight(0.6);

        return center;
    }

    private JPanel buildStatusBar() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(new EmptyBorder(4, 8, 4, 8));
        p.add(lblStatus, BorderLayout.WEST);
        return p;
    }

    private void openCreatorInfo() {
        JDialog dialog = new JDialog(this, "Información del creador", true);
        dialog.setLayout(new BorderLayout(10, 10));

        // Crear un JLabel con la información del creador
        String creatorInfo = "<html><div style='text-align: center;'>" +
                "<h1>José Manuel Castillo Queh</h1>" +
                "<h2>Desarrollador de MiniDBeaver</h2>" +
                "<p>GitHub: CastilloDevX</p>" +
                "</div></html>";

        JLabel infoLabel = new JLabel(creatorInfo);
        infoLabel.setHorizontalAlignment(SwingConstants.CENTER); // Centrar el texto

        // Agregar el JLabel al centro del JDialog
        dialog.add(infoLabel, BorderLayout.CENTER);

        dialog.setSize(350, 225);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void openDatabaseSettings() {
        JDialog dbConfigDialog = new JDialog(this, "Configuración de Base de Datos", true);
        dbConfigDialog.setLayout(new BorderLayout(10, 10));

        // Panel principal con los campos
        JPanel fieldsPanel = new JPanel(new GridBagLayout());
        fieldsPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // Tipo de Base de Datos
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.weightx = 0.3;
        fieldsPanel.add(new JLabel("Tipo de Base de Datos:"), gbc);

        gbc.gridx = 1; gbc.weightx = 0.7;
        JComboBox<String> dbTypeComboBox = new JComboBox<>(new String[]{"MariaDB", "MySQL"});
        fieldsPanel.add(dbTypeComboBox, gbc);

        // Usuario
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.3;
        fieldsPanel.add(new JLabel("Usuario:"), gbc);

        gbc.gridx = 1; gbc.weightx = 0.7;
        JTextField userField = new JTextField();
        fieldsPanel.add(userField, gbc);

        // Contraseña
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.3;
        fieldsPanel.add(new JLabel("Contraseña:"), gbc);

        gbc.gridx = 1; gbc.weightx = 0.7;
        JPasswordField passwordField = new JPasswordField();
        fieldsPanel.add(passwordField, gbc);

        // JDBC URL
        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0.3;
        fieldsPanel.add(new JLabel("JDBC URL:"), gbc);

        gbc.gridx = 1; gbc.weightx = 0.7;
        JTextField jdbcUrlField = new JTextField("jdbc:mariadb://localhost:3306/");
        fieldsPanel.add(jdbcUrlField, gbc);

        // Selector de Base de Datos
        gbc.gridx = 0; gbc.gridy = 4; gbc.weightx = 0.3;
        fieldsPanel.add(new JLabel("Base de Datos:"), gbc);

        gbc.gridx = 1; gbc.weightx = 0.7;
        JComboBox<String> databaseComboBox = new JComboBox<>();
        databaseComboBox.addItem("(Cargando...)");
        databaseComboBox.setEnabled(false);
        fieldsPanel.add(databaseComboBox, gbc);

        // Listener automático para cargar bases de datos cuando se completen los campos
        DocumentListener autoLoadListener = new DocumentListener() {
            private void tryAutoLoad() {
                String user = userField.getText().trim();
                String password = new String(passwordField.getPassword());
                String jdbcUrl = jdbcUrlField.getText().trim();

                // Solo cargar si todos los campos necesarios están llenos
                if (!user.isEmpty() && !password.isEmpty() && !jdbcUrl.isEmpty()) {
                    SwingUtilities.invokeLater(() -> {
                        String dbType = (String) dbTypeComboBox.getSelectedItem();
                        loadDatabases(dbType, jdbcUrl, user, password, databaseComboBox);
                    });
                }
            }

            public void insertUpdate(DocumentEvent e) { tryAutoLoad(); }
            public void removeUpdate(DocumentEvent e) { tryAutoLoad(); }
            public void changedUpdate(DocumentEvent e) { tryAutoLoad(); }
        };

        userField.getDocument().addDocumentListener(autoLoadListener);
        passwordField.getDocument().addDocumentListener(autoLoadListener);
        jdbcUrlField.getDocument().addDocumentListener(autoLoadListener);

        // También agregar listener al cambio de tipo de BD
        dbTypeComboBox.addActionListener(e -> {
            String type = (String) dbTypeComboBox.getSelectedItem();
            if (type.equals("MariaDB")) {
                jdbcUrlField.setText("jdbc:mariadb://localhost:3306/");
            } else if (type.equals("MySQL")) {
                jdbcUrlField.setText("jdbc:mysql://localhost:3306/");
            }
        });

        // Panel del botón Aceptar (centrado)
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        JButton btnAccept = new JButton("Aceptar");
        btnAccept.setPreferredSize(new Dimension(120, 35));
        btnAccept.addActionListener(e -> {
            String dbType = (String) dbTypeComboBox.getSelectedItem();
            String jdbcUrl = jdbcUrlField.getText().trim();
            String selectedDb = (String) databaseComboBox.getSelectedItem();

            // Si seleccionó una base de datos específica, agregarla a la URL
            if (selectedDb != null && !selectedDb.startsWith("(")) {
                if (!jdbcUrl.endsWith("/")) jdbcUrl += "/";
                jdbcUrl += selectedDb;
            }

            String user = userField.getText().trim();
            String password = new String(passwordField.getPassword());

            boolean success = connectToDatabase(dbType, jdbcUrl, user, password);

            if (success) {
                JOptionPane.showMessageDialog(dbConfigDialog,
                        "¡Conexión exitosa!",
                        "Éxito",
                        JOptionPane.INFORMATION_MESSAGE);
                dbConfigDialog.dispose();
            } else {
                int option = JOptionPane.showConfirmDialog(dbConfigDialog,
                        "No se pudo conectar a la base de datos.\n¿Desea intentar de nuevo?",
                        "Error de Conexión",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.ERROR_MESSAGE);

                if (option == JOptionPane.NO_OPTION) {
                    dbConfigDialog.dispose();
                }
            }
        });
        buttonPanel.add(btnAccept);

        dbConfigDialog.add(fieldsPanel, BorderLayout.CENTER);
        dbConfigDialog.add(buttonPanel, BorderLayout.SOUTH);

        dbConfigDialog.setSize(500, 320);
        dbConfigDialog.setLocationRelativeTo(this);
        dbConfigDialog.setVisible(true);
    }

    private void loadDatabases(String dbType, String jdbcUrl, String user, String password, JComboBox<String> comboBox) {
        // Ejecutar en un hilo separado para no bloquear la UI
        new Thread(() -> {
            try {
                if (dbType.equals("MariaDB")) {
                    Class.forName("org.mariadb.jdbc.Driver");
                } else if (dbType.equals("MySQL")) {
                    Class.forName("com.mysql.cj.jdbc.Driver");
                }

                Connection tempConn = DriverManager.getConnection(jdbcUrl, user, password);
                Statement stmt = tempConn.createStatement();
                ResultSet rs = stmt.executeQuery("SHOW DATABASES");

                List<String> databases = new ArrayList<>();
                databases.add("(Ninguna - Seleccionar después)");

                while (rs.next()) {
                    String dbName = rs.getString(1);
                    databases.add(dbName);
                }

                rs.close();
                stmt.close();
                tempConn.close();

                // Actualizar el combo box en el hilo de la UI
                SwingUtilities.invokeLater(() -> {
                    comboBox.removeAllItems();
                    for (String db : databases) {
                        comboBox.addItem(db);
                    }
                    comboBox.setEnabled(true);
                });

            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    comboBox.removeAllItems();
                    comboBox.addItem("(Error al cargar - Verifica credenciales)");
                    comboBox.setEnabled(false);
                });
            }
        }).start();
    }

    private boolean connectToDatabase(String dbType, String jdbcUrl, String user, String password) {
        try {
            if (dbType.equals("MariaDB")) {
                Class.forName("org.mariadb.jdbc.Driver");
            } else if (dbType.equals("MySQL")) {
                Class.forName("com.mysql.cj.jdbc.Driver");
            }

            conn = DriverManager.getConnection(jdbcUrl, user, password);
            if (conn != null) {
                lblStatus.setText("Conectado a " + dbType);

                if (currentCatalog != null && !currentCatalog.isEmpty()) {
                    try {
                        conn.setCatalog(currentCatalog);
                        lblStatus.setText(lblStatus.getText() + " | catálogo=" + currentCatalog);
                    } catch (SQLException ex) {
                        try (Statement s = conn.createStatement()) {
                            s.execute("USE " + quoteIdent(currentCatalog) + ";");
                            lblStatus.setText(lblStatus.getText() + " | USE=" + currentCatalog);
                        } catch (SQLException ignored) {}
                    }
                }
                return true;
            }
        } catch (Exception ex) {
            lblStatus.setText("Error al conectar: " + ex.getMessage());
            ex.printStackTrace();
        }
        return false;
    }

    private void wireActions() {
        btnExecLine.addActionListener(e -> executeCurrentLine());
        btnExecSel.addActionListener(e -> executeSelection());
        btnExecAll.addActionListener(e -> executeAll());
        btnDatabaseSett.addActionListener(e -> openDatabaseSettings());
        btnClear.addActionListener(e -> resultsTabs.removeAll());
        btnAdminInfo.addActionListener(e -> openCreatorInfo());

        editor.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_ENTER) {
                    if (editor.getSelectedText() != null && !editor.getSelectedText().isEmpty()) {
                        executeSql(editor.getSelectedText());
                    } else {
                        executeCurrentLine();
                    }
                    e.consume();
                }
            }
        });

        editor.getDocument().addDocumentListener(new DocumentListener() {
            private long last = 0;

            private void maybeSave() {
                long now = System.currentTimeMillis();
                if (now - last > 800) {
                    PREF.put(PREF_LAST_TEXT, editor.getText());
                    last = now;
                }
            }

            public void insertUpdate(DocumentEvent e) {
                maybeSave();
            }

            public void removeUpdate(DocumentEvent e) {
                maybeSave();
            }

            public void changedUpdate(DocumentEvent e) {
                maybeSave();
            }
        });
    }

    private void restoreState() {
        editor.setText(PREF.get(PREF_LAST_TEXT, defaultEditorText()));
        currentCatalog = PREF.get(PREF_CATALOG, null);
        if (currentCatalog != null && !currentCatalog.isEmpty()) {
            lblStatus.setText("Catálogo guardado: " + currentCatalog);
        }
    }

    private String defaultEditorText() {
        return "-- Escribe SQL aquí. Usa Ctrl+Enter para ejecutar selección/línea.\n" +
                "-- Ejemplos:\n" +
                "-- USE mi_base;\n" +
                "-- SELECT 1;\n";
    }

    private void executeCurrentLine() {
        int caret = editor.getCaretPosition();
        try {
            int line = editor.getLineOfOffset(caret);
            int start = editor.getLineStartOffset(line);
            int end = editor.getLineEndOffset(line);
            String sql = editor.getText().substring(start, end).trim();
            if (!sql.isEmpty()) executeSql(sql);
        } catch (Exception ex) {
            showError("No se pudo obtener la línea actual: " + ex.getMessage(), ex);
        }
    }

    private void executeSelection() {
        String sel = editor.getSelectedText();
        if (sel != null && !sel.isBlank()) executeSql(sel);
    }

    private void executeAll() {
        executeSql(editor.getText());
    }

    private void executeSql(String sqlText) {
        if (conn == null) {
            showError("No hay conexión activa.", null);
            return;
        }
        List<String> statements = splitStatements(sqlText);
        for (String stmt : statements) {
            String trimmed = stmt.trim();
            if (trimmed.isEmpty()) continue;

            if (trimmed.toUpperCase().startsWith("USE ")) {
                String cat = trimmed.substring(4).trim();
                cat = stripSemicolon(cat);
                applyUse(cat);
                continue;
            }

            try (Statement s = conn.createStatement()) {
                boolean hasResult = s.execute(trimmed);
                if (hasResult) {
                    try (ResultSet rs = s.getResultSet()) {
                        JTable table = buildTable(rs);
                        addClosableResultTab(new JScrollPane(table), previewTitle(trimmed));
                    }
                } else {
                    int count = s.getUpdateCount();
                    JTextArea log = new JTextArea("OK, filas afectadas: " + count + "\n\n" + previewBody(trimmed));
                    log.setEditable(false);
                    log.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
                    addClosableResultTab(new JScrollPane(log), previewTitle(trimmed));
                }
            } catch (SQLException ex) {
                JTextArea err = new JTextArea("ERROR: " + ex.getMessage());
                err.setForeground(Color.RED);
                err.setEditable(false);
                addClosableResultTab(new JScrollPane(err), "Error");
            }
        }
    }

    private void applyUse(String catRaw) {
        String cat = unquote(catRaw);
        try {
            try {
                conn.setCatalog(cat);
            } catch (SQLException e) {
                try (Statement s = conn.createStatement()) {
                    s.execute("USE " + quoteIdent(cat) + ";");
                }
            }
            currentCatalog = cat;
            PREF.put(PREF_CATALOG, currentCatalog);
            lblStatus.setText("Conectado | catálogo=" + currentCatalog);
            JOptionPane.showMessageDialog(this, "Catálogo activo: " + cat);
        } catch (SQLException ex) {
            showError("No se pudo aplicar USE '" + cat + "': " + ex.getMessage(), ex);
        }
    }

    private JTable buildTable(ResultSet rs) throws SQLException {
        ResultSetMetaData md = rs.getMetaData();
        int cols = md.getColumnCount();
        String[] headers = new String[cols];
        for (int i = 1; i <= cols; i++) headers[i - 1] = md.getColumnLabel(i);

        DefaultTableModel model = new DefaultTableModel(headers, 0);
        int maxRows = 10_000;
        int r = 0;
        while (rs.next() && r++ < maxRows) {
            Object[] row = new Object[cols];
            for (int i = 1; i <= cols; i++) row[i - 1] = rs.getObject(i);
            model.addRow(row);
        }
        JTable table = new JTable(model);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        return table;
    }

    private void addClosableResultTab(JScrollPane sp, String title) {
        resultsTabs.addTab(title, sp);
        int idx = resultsTabs.indexOfComponent(sp);
        resultsTabs.setTabComponentAt(idx, makeTabHeader(title, sp));
        resultsTabs.setSelectedIndex(idx);
    }

    private Component makeTabHeader(String title, Component comp) {
        JPanel tab = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        tab.setOpaque(false);

        JLabel lbl = new JLabel(title + "  ");
        JButton btnClose = new JButton("✕");
        btnClose.setMargin(new Insets(0, 4, 0, 4));
        btnClose.setBorder(BorderFactory.createEmptyBorder());
        btnClose.setFocusable(false);
        btnClose.setOpaque(false);
        btnClose.addActionListener(e -> {
            int i = resultsTabs.indexOfComponent(comp);
            if (i != -1) resultsTabs.removeTabAt(i);
        });

        tab.add(lbl);
        tab.add(btnClose);
        return tab;
    }

    private String previewTitle(String sql) {
        String s = sql.trim().replaceAll("\\n+", " ");
        if (s.length() > 40) s = s.substring(0, 40) + "...";
        return s;
    }

    private String previewBody(String sql) {
        String s = sql.trim();
        if (s.length() > 2000) s = s.substring(0, 2000) + "...";
        return s;
    }

    private void showError(String msg, Exception ex) {
        lblStatus.setText(msg);
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
        if (ex != null) ex.printStackTrace();
    }

    private static String stripSemicolon(String s) {
        s = s.trim();
        if (s.endsWith(";")) s = s.substring(0, s.length() - 1).trim();
        return s;
    }

    private static String unquote(String s) {
        s = stripSemicolon(s);
        if ((s.startsWith("`") && s.endsWith("`")) ||
                (s.startsWith("\"") && s.endsWith("\"")) ||
                (s.startsWith("'") && s.endsWith("'"))) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }

    private static String quoteIdent(String ident) {
        if (ident.matches("[A-Za-z0-9_]+")) return ident;
        return "`" + ident.replace("`", "``") + "`";
    }

    private static List<String> splitStatements(String sql) {
        List<String> out = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inS = false, inD = false;
        for (int i = 0; i < sql.length(); i++) {
            char ch = sql.charAt(i);
            if (ch == '\'' && !inD) {
                inS = !inS;
                sb.append(ch);
            } else if (ch == '\"' && !inS) {
                inD = !inD;
                sb.append(ch);
            } else if (ch == ';' && !inS && !inD) {
                out.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(ch);
            }
        }
        if (sb.length() > 0) out.add(sb.toString());
        return out;
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }
        SwingUtilities.invokeLater(
                () -> {
                    MiniDBeaver miniDBeaver = new MiniDBeaver();
                    miniDBeaver.setVisible(true);
                    miniDBeaver.openDatabaseSettings();
                }
        );

    }
}