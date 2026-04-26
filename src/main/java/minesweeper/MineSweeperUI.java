package minesweeper;

import javax.swing.*;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.QuadCurve2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
/**
 * Swing-based GUI for Minesweeper: Hormuz Edition.
 * Wide boards, a west-to-east crossing objective, and a visible convoy marker.
 */
public class MineSweeperUI extends JFrame {

    private static final String TITLE = "Minesweeper: Hormuz Edition";
    private static final String LEVEL_ESCORT = "Escort Run";
    private static final String LEVEL_TANKER = "Tanker Lane";
    private static final String LEVEL_BREAKER = "Blockade Breaker";

    // Color constants for number labels
    private static final Color[] NUMBER_COLORS = {
        null,
        new Color(0, 54, 179),   // 1 – strong blue
        new Color(0, 102, 0),    // 2 – dark green
        new Color(179, 0, 0),    // 3 – dark red
        new Color(36, 38, 130),  // 4 – indigo
        new Color(110, 25, 25),  // 5 – maroon
        new Color(0, 96, 96),    // 6 – dark teal
        new Color(24, 24, 24),   // 7 – near black
        new Color(74, 74, 74)    // 8 – dark grey
    };

    private static final Color TRIGGERED_MINE_BG = new Color(255, 80, 80);
    private static final Color HIDDEN_CELL_BG = new Color(95, 148, 181);
    private static final Color REVEALED_CELL_BG = new Color(188, 228, 239);
    private static final Color REVEALED_BORDER = new Color(53, 101, 128);
    private static final Color QUESTION_MARK_COLOR = new Color(84, 0, 140);
    private static final Color BOAT_CELL_BG = new Color(155, 205, 232);
    private static final Color STATUS_BG = new Color(212, 229, 220);
    private static final Color STATUS_FG = new Color(29, 63, 82);
    private static final Color DOCK_BG = new Color(171, 139, 102);
    private static final Color HARBOR_BG = new Color(192, 213, 176);
    private static final Color SIDE_PANEL_BG = new Color(196, 220, 214);
    private static final Color MINELAYER_HULL = new Color(153, 56, 44);
    private static final Color MINELAYER_DECK = new Color(236, 211, 151);
    private static final Color MINELAYER_WAKE = new Color(118, 179, 216, 170);
    private static final Color TOOLBAR_BG = new Color(58, 104, 132);
    private static final Color GRID_WATER_BG = new Color(66, 116, 146);
    private static final int CELL_SIZE = 32;
    private static final int BOAT_ANIMATION_DELAY_MS = 16;
    private static final float BOAT_ANIMATION_STEP = 0.18f;
    private static final int HARBOR_SEQUENCE_DELAY_MS = 40;
    private static final float HARBOR_DOCKING_STEP = 0.1f;
    private static final float HARBOR_MOORING_STEP = 0.14f;
    private static final float HARBOR_REFUEL_STEP = 0.06f;
    private static final float WEST_DOCK_CELEBRATION_STEP = 0.09f;
    private static final int MISSILE_ANIMATION_DELAY_MS = 16;
    private static final float MISSILE_FLIGHT_STEP = 0.12f;
    private static final float MISSILE_EXPLOSION_STEP = 0.14f;
    private static final int AMBIENT_ANIMATION_DELAY_MS = 33;
    private static final int MINELAYER_STEP_DELAY_MS = 1350;
    private static final int DEFAULT_MINELAYER_COUNT = 2;
    private static final int TANKER_MINELAYER_COUNT = 2;
    private static final int BREAKER_MINELAYER_COUNT = 3;
    private static final int MAX_MINELAYER_COUNT = 6;

    private Board board;
    private JButton[][] buttons;
    private JLabel mineCountLabel;
    private JLabel timerLabel;
    private JLabel statusLabel;
    private JLabel westDockIconLabel;
    private JLabel westDockSubtitleLabel;
    private JButton resetButton;
    private JToggleButton missileButton;
    private JCheckBoxMenuItem missileMenuItem;
    private JCheckBoxMenuItem soundMenuItem;
    private JCheckBoxMenuItem showTutorialOnStartupMenuItem;
    private JPanel gridPanel;
    private JLayeredPane boardLayer;
    private BoatOverlay boatOverlay;
    private MissileOverlay missileOverlay;
    private MineLayerOverlay mineLayerOverlay;
    private Timer swingTimer;
    private Timer ambientAnimationTimer;
    private Timer boatAnimationTimer;
    private Timer harborSequenceTimer;
    private Timer westDockSequenceTimer;
    private Timer missileAnimationTimer;
    private Timer mineLayerTimer;
    private Board.Snapshot lastMoveSnapshot;
    private int lastMoveElapsedSeconds;
    private int elapsedSeconds;
    private int chordRow = -1;
    private int chordCol = -1;
    private boolean instructionsShown;
    private boolean missileModeArmed;
    private boolean soundEnabled = true;
    private boolean showTutorialOnStartup = true;
    private boolean winDialogShown;
    private Board.GameState lastGameState = Board.GameState.WAITING;
    private Board.VoyageStage lastVoyageStage = Board.VoyageStage.OUTBOUND;
    private HarborSequence harborSequence;
    private WestDockSequence westDockSequence;
    private MissileAnimation missileAnimation;
    private long animationEpochNanos = System.nanoTime();

    // Current difficulty settings
    private int rows;
    private int cols;
    private int mines;
    private int mineLayerCount = DEFAULT_MINELAYER_COUNT;
    private String difficultyKey = "Custom";
    private final Icon toolbarBoatIcon = createBoatIcon(28, 22);
    private final Icon dockIcon = createDockIcon(26, 20);
    private final Icon harborIcon = createHarborIcon(26, 20);
    private final Icon waveIcon = createWaveIcon(28, 20);
    private final Icon blastIcon = createBlastIcon(26, 22);
    private final Icon successIcon = createHarborIcon(28, 22);
    private final Icon harborDialogIcon = createHarborIcon(64, 46);
    private final Icon homecomingDialogIcon = createDockIcon(64, 46);

    // In-memory best times (seconds) keyed by difficulty label
    private final Map<String, Integer> bestTimes = new HashMap<>();

    public MineSweeperUI() {
        setTitle(TITLE);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        buildMenuBar();
        startGame(10, 24, 44, TANKER_MINELAYER_COUNT, LEVEL_TANKER);
        SwingUtilities.invokeLater(() -> showOpeningInstructions(false));
    }

    // -------------------------------------------------------------------------
    // Menu
    // -------------------------------------------------------------------------

    private void buildMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu gameMenu = new JMenu("Game");

        JMenuItem newItem = new JMenuItem("New Game");
        newItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0));
        newItem.addActionListener(e -> startGame(rows, cols, mines, mineLayerCount, difficultyKey));

        JMenuItem beginnerItem = new JMenuItem(LEVEL_ESCORT + " (8×16, 20 mines, 1 ship)");
        beginnerItem.addActionListener(e -> startGame(8, 16, 20, 1, LEVEL_ESCORT));

        JMenuItem intermediateItem = new JMenuItem(LEVEL_TANKER + " (10×24, 44 mines, 2 ships)");
        intermediateItem.addActionListener(e -> startGame(10, 24, 44, TANKER_MINELAYER_COUNT, LEVEL_TANKER));

        JMenuItem expertItem = new JMenuItem(LEVEL_BREAKER + " (12×30, 76 mines, 3 ships)");
        expertItem.addActionListener(e -> startGame(12, 30, 76, BREAKER_MINELAYER_COUNT, LEVEL_BREAKER));

        JMenuItem customItem = new JMenuItem("Custom…");
        customItem.addActionListener(e -> showCustomDialog());

        JMenuItem bestTimesItem = new JMenuItem("Best Times…");
        bestTimesItem.addActionListener(e -> showBestTimesDialog());

        JMenuItem undoItem = new JMenuItem("Undo Last Move");
        undoItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK));
        undoItem.addActionListener(e -> undoLastMove());

        missileMenuItem = new JCheckBoxMenuItem("Arm Missile");
        missileMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M, 0));
        missileMenuItem.addActionListener(e -> {
            setMissileModeArmed(missileMenuItem.isSelected());
            updateBoard();
        });

        soundMenuItem = new JCheckBoxMenuItem("Sound Effects", soundEnabled);
        soundMenuItem.addActionListener(e -> soundEnabled = soundMenuItem.isSelected());

        JMenu mineLayerMenu = new JMenu("Enemy Minelayers");
        ButtonGroup mineLayerGroup = new ButtonGroup();
        for (int count = 0; count <= MAX_MINELAYER_COUNT; count++) {
            String label = count == 1 ? "1 Ship" : count + " Ships";
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(label, count == mineLayerCount);
            int selectedCount = count;
            item.addActionListener(e -> startGame(rows, cols, mines, selectedCount, difficultyKey));
            mineLayerGroup.add(item);
            mineLayerMenu.add(item);
        }

        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> System.exit(0));

        gameMenu.add(newItem);
        gameMenu.addSeparator();
        gameMenu.add(beginnerItem);
        gameMenu.add(intermediateItem);
        gameMenu.add(expertItem);
        gameMenu.addSeparator();
        gameMenu.add(customItem);
        gameMenu.addSeparator();
        gameMenu.add(undoItem);
        gameMenu.add(missileMenuItem);
        gameMenu.add(soundMenuItem);
        gameMenu.addSeparator();
        gameMenu.add(bestTimesItem);
        gameMenu.add(mineLayerMenu);
        gameMenu.addSeparator();
        gameMenu.add(exitItem);

        JMenu helpMenu = new JMenu("Help");
        JMenuItem tutorialItem = new JMenuItem("How To Play…");
        tutorialItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0));
        tutorialItem.addActionListener(e -> showOpeningInstructions(true));
        showTutorialOnStartupMenuItem = new JCheckBoxMenuItem("Show Tutorial on Startup", showTutorialOnStartup);
        showTutorialOnStartupMenuItem.addActionListener(e -> {
            showTutorialOnStartup = showTutorialOnStartupMenuItem.isSelected();
        });
        helpMenu.add(tutorialItem);
        helpMenu.addSeparator();
        helpMenu.add(showTutorialOnStartupMenuItem);

        menuBar.add(gameMenu);
        menuBar.add(helpMenu);
        setJMenuBar(menuBar);
    }

    private void showCustomDialog() {
        JTextField rowsField = new JTextField(String.valueOf(rows), 4);
        JTextField colsField = new JTextField(String.valueOf(cols), 4);
        JTextField minesField = new JTextField(String.valueOf(mines), 4);

        JPanel panel = new JPanel(new GridLayout(3, 2, 5, 5));
        panel.add(new JLabel("Rows (5–30):"));
        panel.add(rowsField);
        panel.add(new JLabel("Cols (6–50, must exceed rows):"));
        panel.add(colsField);
        panel.add(new JLabel("Mines:"));
        panel.add(minesField);

        int result = JOptionPane.showConfirmDialog(
            this, panel, "Custom Game", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            try {
                int r = Integer.parseInt(rowsField.getText().trim());
                int c = Integer.parseInt(colsField.getText().trim());
                int m = Integer.parseInt(minesField.getText().trim());
                r = Math.max(5, Math.min(30, r));
                c = Math.max(r + 1, Math.min(50, c));
                m = Math.max(1, Math.min(r * c - 9, m));
                startGame(r, c, m, mineLayerCount, "Custom");
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Please enter valid numbers.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showBestTimesDialog() {
        String[] levels = {LEVEL_ESCORT, LEVEL_TANKER, LEVEL_BREAKER};
        StringBuilder sb = new StringBuilder("<html><table>");
        for (String level : levels) {
            Integer best = bestTimes.get(level);
            sb.append("<tr><td><b>").append(level).append("</b></td><td>&nbsp;&nbsp;</td><td align='right'>")
              .append(best != null ? best + "s" : "–")
              .append("</td></tr>");
        }
        sb.append("</table></html>");

        Object[] options = {"Reset Times", "OK"};
        int choice = JOptionPane.showOptionDialog(
            this, sb.toString(), "Best Times",
            JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE,
            null, options, "OK");
        if (choice == 0) {
            bestTimes.clear();
        }
    }

    private void showOpeningInstructions(boolean manual) {
        if (!manual) {
            if (instructionsShown || !showTutorialOnStartup) {
                return;
            }
        }
        instructionsShown = true;

        final String[] pageTitles = {
            "Welcome to Hormuz Edition",
            "Minesweeper Controls",
            "Navigating the Ship",
            "Firing Missiles",
            "Victory Conditions"
        };

        final String[] pageBodies = {
            "<html><div style='width:280px'>Escort a tanker through the Strait of Hormuz!<br><br>"
                + "Clear a safe channel through the minefield, then sail your ship from the "
                + "<b>west dock</b> all the way to the <b>east harbor</b> \u2014 "
                + "and bring it safely back to win.</div></html>",

            "<html><div style='width:280px'>"
                + "<b>Left-click</b> a hidden cell to reveal it.<br>"
                + "<b>Right-click</b> a hidden cell to flag a suspected mine.<br><br>"
                + "<b>Double-click</b> (or chord) a revealed number to uncover adjacent "
                + "cells when the matching mines are already flagged.</div></html>",

            "<html><div style='width:280px'>"
                + "Click a <b>revealed safe cell in column 1</b> to launch the ship.<br><br>"
                + "Then click any <b>straight revealed channel</b> or use the "
                + "<b>arrow keys</b> to sail. The ship travels only through clear, mine-free water."
                + "</div></html>",

            "<html><div style='width:280px'>"
                + "Click the <b>missile button</b> (or press <b>M</b>) to arm a strike.<br><br>"
                + "Then click any <b>hidden cell</b> to destroy it. Each game provides a limited "
                + "supply \u2014 use them to blast stubborn obstacles off your route."
                + "</div></html>",

            "<html><div style='width:280px'>"
                + "<b>Leg 1:</b> Sail to the east harbor to refuel automatically.<br><br>"
                + "<b>Leg 2:</b> Return the ship to the west dock to win!<br><br>"
                + "Enemy minelayers patrol the strait and can seal your channel \u2014 "
                + "clear your path before they do!</div></html>"
        };

        final int pageCount = pageTitles.length;
        final int[] currentPage = {0};

        JDialog dialog = new JDialog(this, "How To Play", true);
        dialog.setResizable(false);

        JPanel graphicPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                paintTutorialGraphic((Graphics2D) g, currentPage[0], getWidth(), getHeight());
            }
        };
        graphicPanel.setPreferredSize(new Dimension(384, 155));
        graphicPanel.setBackground(GRID_WATER_BG);

        JLabel titleLabel = new JLabel(pageTitles[0]);
        titleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        titleLabel.setForeground(STATUS_FG.darker());
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 14, 4, 14));

        JLabel bodyLabel = new JLabel(pageBodies[0]);
        bodyLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        bodyLabel.setForeground(STATUS_FG);
        bodyLabel.setBorder(BorderFactory.createEmptyBorder(0, 14, 10, 14));

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setBackground(STATUS_BG);
        textPanel.add(titleLabel);
        textPanel.add(bodyLabel);

        JPanel dotsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 4));
        dotsPanel.setBackground(STATUS_BG);
        JLabel[] dots = new JLabel[pageCount];
        for (int i = 0; i < pageCount; i++) {
            dots[i] = new JLabel("\u25cf");
            dots[i].setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
            dotsPanel.add(dots[i]);
        }

        JButton prevButton = new JButton("\u25c4 Back");
        JButton nextButton = new JButton("Next \u25ba");
        JButton closeButton = new JButton("Close");

        JCheckBox startupCheckBox = new JCheckBox("Show this tutorial on startup", showTutorialOnStartup);
        startupCheckBox.setBackground(STATUS_BG);
        startupCheckBox.setForeground(STATUS_FG);
        startupCheckBox.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        startupCheckBox.addActionListener(e -> {
            showTutorialOnStartup = startupCheckBox.isSelected();
            showTutorialOnStartupMenuItem.setSelected(showTutorialOnStartup);
        });

        Runnable refresh = () -> {
            int p = currentPage[0];
            titleLabel.setText(pageTitles[p]);
            bodyLabel.setText(pageBodies[p]);
            prevButton.setEnabled(p > 0);
            nextButton.setEnabled(p < pageCount - 1);
            for (int i = 0; i < pageCount; i++) {
                dots[i].setForeground(i == p ? STATUS_FG.darker() : new Color(170, 190, 200));
            }
            graphicPanel.repaint();
        };
        refresh.run();

        prevButton.addActionListener(e -> {
            if (currentPage[0] > 0) { currentPage[0]--; refresh.run(); }
        });
        nextButton.addActionListener(e -> {
            if (currentPage[0] < pageCount - 1) { currentPage[0]++; refresh.run(); }
        });
        closeButton.addActionListener(e -> dialog.dispose());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 6));
        buttonPanel.setBackground(STATUS_BG);
        buttonPanel.add(prevButton);
        buttonPanel.add(closeButton);
        buttonPanel.add(nextButton);

        JPanel startupPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 2));
        startupPanel.setBackground(STATUS_BG);
        startupPanel.add(startupCheckBox);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(STATUS_BG);
        bottomPanel.add(dotsPanel, BorderLayout.NORTH);
        bottomPanel.add(buttonPanel, BorderLayout.CENTER);
        bottomPanel.add(startupPanel, BorderLayout.SOUTH);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(graphicPanel, BorderLayout.NORTH);
        mainPanel.add(textPanel, BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);
        mainPanel.setBorder(BorderFactory.createLineBorder(REVEALED_BORDER, 2));

        dialog.setContentPane(mainPanel);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void paintTutorialGraphic(Graphics2D g, int page, int w, int h) {
        enableQuality(g);
        g.setColor(GRID_WATER_BG);
        g.fillRect(0, 0, w, h);
        switch (page) {
            case 0: paintTutorialObjective(g, w, h); break;
            case 1: paintTutorialControls(g, w, h); break;
            case 2: paintTutorialShipMove(g, w, h); break;
            case 3: paintTutorialMissile(g, w, h); break;
            case 4: paintTutorialVictory(g, w, h); break;
            default: break;
        }
    }

    private void paintTutorialObjective(Graphics2D g, int w, int h) {
        int cs = 22;
        int gridRows = 5;
        int gridCols = 9;
        int dockW = 30;
        int harborW = 30;
        int totalW = dockW + gridCols * cs + harborW;
        int startX = (w - totalW) / 2;
        int startY = (h - gridRows * cs) / 2;
        int gridX = startX + dockW;

        for (int r = 0; r < gridRows; r++) {
            for (int c = 0; c < gridCols; c++) {
                int x = gridX + c * cs;
                int y = startY + r * cs;
                g.setColor(r == 2 ? REVEALED_CELL_BG : HIDDEN_CELL_BG);
                g.fillRect(x + 1, y + 1, cs - 2, cs - 2);
                g.setColor(GRID_WATER_BG);
                g.drawRect(x, y, cs, cs);
            }
        }

        // West dock
        g.setColor(DOCK_BG);
        g.fillRect(startX, startY, dockW, gridRows * cs);
        g.setColor(new Color(114, 83, 52));
        g.fillRect(startX + 4, startY + 2 * cs + cs / 2 - 2, dockW - 4, 4);
        g.setColor(new Color(193, 58, 45));
        g.fillRect(startX + dockW / 2 - 1, startY + 6, 2, 10);
        g.fillOval(startX + dockW / 2 - 4, startY + 4, 8, 6);

        // East harbor
        g.setColor(HARBOR_BG);
        g.fillRect(gridX + gridCols * cs, startY, harborW, gridRows * cs);
        g.setColor(new Color(89, 120, 74));
        g.fillRoundRect(gridX + gridCols * cs + 3, startY + 3, harborW - 6, gridRows * cs - 6, 5, 5);
        g.setColor(new Color(227, 207, 92));
        g.fillOval(gridX + gridCols * cs + harborW - 13, startY + 4, 8, 8);

        // Labels
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 8));
        g.setColor(new Color(80, 50, 20));
        g.drawString("WEST", startX + 3, startY + gridRows * cs - 5);
        g.setColor(new Color(40, 80, 30));
        g.drawString("EAST", gridX + gridCols * cs + 3, startY + gridRows * cs - 5);

        // Ship in mid-channel
        int shipX = gridX + 4 * cs;
        int shipY = startY + 2 * cs;
        Graphics2D gc = (Graphics2D) g.create();
        gc.translate(shipX, shipY);
        paintBoat(gc, cs * 2, cs);
        gc.dispose();

        // Arrow pointing east
        g.setColor(new Color(255, 255, 255, 200));
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int arrowY = startY + 2 * cs + cs / 2;
        int arrowSX = gridX + 6 * cs + 4;
        int arrowEX = gridX + gridCols * cs - 4;
        g.drawLine(arrowSX, arrowY, arrowEX, arrowY);
        g.fillPolygon(new int[]{arrowEX, arrowEX - 7, arrowEX - 7},
                      new int[]{arrowY, arrowY - 4, arrowY + 4}, 3);
    }

    private void paintTutorialControls(Graphics2D g, int w, int h) {
        int cs = 26;
        int gridCols = 5;
        int gridRows = 4;
        int startX = (w - gridCols * cs) / 2;
        int startY = (h - gridRows * cs) / 2 + 4;

        // 0=hidden, 1=revealed+number, 2=revealed blank, 3=flagged
        int[][] cellType = {
            {0, 0, 0, 3, 0},
            {2, 1, 0, 0, 0},
            {2, 2, 0, 3, 0},
            {2, 1, 0, 0, 0}
        };
        int[][] cellNumber = {
            {0, 0, 0, 0, 0},
            {0, 1, 0, 0, 0},
            {0, 2, 0, 0, 0},
            {0, 1, 0, 0, 0}
        };

        for (int r = 0; r < gridRows; r++) {
            for (int c = 0; c < gridCols; c++) {
                int x = startX + c * cs;
                int y = startY + r * cs;
                int type = cellType[r][c];
                g.setColor((type == 1 || type == 2) ? REVEALED_CELL_BG : HIDDEN_CELL_BG);
                g.fillRect(x + 1, y + 1, cs - 2, cs - 2);
                g.setColor(GRID_WATER_BG);
                g.drawRect(x, y, cs, cs);

                if (type == 1) {
                    int n = cellNumber[r][c];
                    Color nc = (n > 0 && n < NUMBER_COLORS.length) ? NUMBER_COLORS[n] : Color.BLACK;
                    g.setColor(nc != null ? nc : Color.BLACK);
                    g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
                    g.drawString(String.valueOf(n), x + cs / 2 - 4, y + cs / 2 + 5);
                } else if (type == 3) {
                    g.setColor(new Color(193, 58, 45));
                    g.fillRect(x + cs / 2 - 1, y + 5, 2, cs - 11);
                    Path2D flag = new Path2D.Double();
                    flag.moveTo(x + cs / 2 + 1, y + 6);
                    flag.lineTo(x + cs / 2 + 10, y + 10);
                    flag.lineTo(x + cs / 2 + 1, y + 14);
                    flag.closePath();
                    g.fill(flag);
                }
            }
        }

        g.setStroke(new BasicStroke(1.5f));
        // Left-click annotation — top-left hidden cell (col 0, row 0)
        int lx = startX + cs / 2;
        int ly = startY;
        g.setColor(new Color(40, 120, 200));
        g.drawLine(lx, ly - 2, lx, ly - 14);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 9));
        g.drawString("L-click = reveal", lx - 30, ly - 16);

        // Right-click annotation — flagged cell (col 3, row 0)
        int rx = startX + 3 * cs + cs / 2;
        int ry = startY;
        g.setColor(new Color(200, 60, 30));
        g.drawLine(rx, ry - 2, rx, ry - 14);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 9));
        g.drawString("R-click = flag", rx - 28, ry - 16);
    }

    private void paintTutorialShipMove(Graphics2D g, int w, int h) {
        int cs = 22;
        int gridRows = 4;
        int gridCols = 7;
        int dockW = 22;
        int keyClusterW = 72;   // width reserved for arrow-key diagram
        int gap = 10;
        int totalW = dockW + gridCols * cs + gap + keyClusterW;
        int startX = (w - totalW) / 2;
        int startY = (h - gridRows * cs) / 2;
        int gridX = startX + dockW;

        // West dock
        g.setColor(DOCK_BG);
        g.fillRect(startX, startY, dockW, gridRows * cs);

        for (int r = 0; r < gridRows; r++) {
            for (int c = 0; c < gridCols; c++) {
                int x = gridX + c * cs;
                int y = startY + r * cs;
                boolean channel = (r == 2);
                g.setColor(channel ? REVEALED_CELL_BG : HIDDEN_CELL_BG);
                g.fillRect(x + 1, y + 1, cs - 2, cs - 2);
                g.setColor(GRID_WATER_BG);
                g.drawRect(x, y, cs, cs);
                // Highlight launch cell
                if (channel && c == 0) {
                    g.setColor(new Color(80, 180, 240, 90));
                    g.fillRect(x + 1, y + 1, cs - 2, cs - 2);
                    g.setColor(new Color(60, 160, 220));
                    g.setStroke(new BasicStroke(2f));
                    g.drawRect(x + 2, y + 2, cs - 4, cs - 4);
                }
            }
        }

        // Ship at column 1
        int shipX = gridX + 1 * cs;
        int shipY = startY + 2 * cs;
        Graphics2D gc = (Graphics2D) g.create();
        gc.translate(shipX, shipY);
        paintBoat(gc, cs * 2, cs);
        gc.dispose();

        // Movement arrows on grid
        g.setColor(new Color(255, 255, 255, 200));
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int step = 4; step <= 5; step++) {
            int ax = gridX + step * cs + cs / 2;
            int ay = startY + 2 * cs + cs / 2;
            g.drawLine(ax - 7, ay, ax + 1, ay);
            g.fillPolygon(new int[]{ax + 1, ax - 4, ax - 4},
                          new int[]{ay, ay - 3, ay + 3}, 3);
        }

        // "Launch here" label
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 9));
        g.setColor(new Color(60, 160, 220));
        g.drawString("Launch", gridX, startY + 2 * cs - 4);

        // ── Arrow-key cluster ─────────────────────────────────────────────
        // Layout (standard inverted-T):
        //       [  ↑  ]
        //  [  ← ][ ↓ ][ →]
        int ks = 20;  // key size
        int kx = gridX + gridCols * cs + gap;
        int ky = (h - 2 * ks - 2) / 2;  // vertically centered

        // helper: draw one key at (kx+dx, ky+dy) with a label glyph
        // keys: up=(ks,0), left=(0,ks+2), down=(ks,ks+2), right=(2*ks+4,ks+2)
        int[][] keyOffsets = { {ks + 2, 0}, {0, ks + 2}, {ks + 2, ks + 2}, {(ks + 2) * 2, ks + 2} };
        String[] keyGlyphs = { "\u2191", "\u2190", "\u2193", "\u2192" };

        for (int i = 0; i < 4; i++) {
            int kbx = kx + keyOffsets[i][0];
            int kby = ky + keyOffsets[i][1];
            // Key body with bevel effect
            g.setColor(new Color(55, 70, 85));
            g.fillRoundRect(kbx, kby + 3, ks, ks, 5, 5);  // shadow
            g.setColor(new Color(220, 230, 240));
            g.fillRoundRect(kbx, kby, ks, ks, 5, 5);
            g.setColor(new Color(170, 185, 200));
            g.setStroke(new BasicStroke(1f));
            g.drawRoundRect(kbx, kby, ks, ks, 5, 5);
            // Arrow glyph
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
            g.setColor(new Color(40, 60, 90));
            FontMetrics fm = g.getFontMetrics();
            int gx = kbx + (ks - fm.stringWidth(keyGlyphs[i])) / 2;
            int gy = kby + (ks + fm.getAscent() - fm.getDescent()) / 2 - 1;
            g.drawString(keyGlyphs[i], gx, gy);
        }

        // "or arrow keys" label below cluster
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 9));
        g.setColor(new Color(200, 220, 235));
        int labelX = kx;
        int labelY = ky + 2 * ks + 4 + 11;
        g.drawString("or arrow keys", labelX, labelY);
    }

    private void paintTutorialMissile(Graphics2D g, int w, int h) {
        int cs = 24;
        int gridRows = 5;
        int gridCols = 7;
        int startX = (w - gridCols * cs - 44) / 2 + 22;
        int startY = (h - gridRows * cs) / 2;

        for (int r = 0; r < gridRows; r++) {
            for (int c = 0; c < gridCols; c++) {
                int x = startX + c * cs;
                int y = startY + r * cs;
                boolean revealed = (r == 2 && c < 3);
                g.setColor(revealed ? REVEALED_CELL_BG : HIDDEN_CELL_BG);
                g.fillRect(x + 1, y + 1, cs - 2, cs - 2);
                g.setColor(GRID_WATER_BG);
                g.drawRect(x, y, cs, cs);
            }
        }

        // Target cell highlight
        int targetCol = 5;
        int targetRow = 2;
        int tx = startX + targetCol * cs;
        int ty = startY + targetRow * cs;
        g.setColor(new Color(255, 160, 50, 130));
        g.fillRect(tx + 1, ty + 1, cs - 2, cs - 2);
        g.setColor(new Color(220, 80, 20));
        g.setStroke(new BasicStroke(2f));
        g.drawRect(tx + 1, ty + 1, cs - 3, cs - 3);

        // Ship at col 1, row 2
        int shipX = startX + 1 * cs;
        int shipY = startY + 2 * cs;
        Graphics2D gc = (Graphics2D) g.create();
        gc.translate(shipX, shipY);
        paintBoat(gc, cs * 2, cs);
        gc.dispose();

        // Missile arc
        float sx = shipX + cs * 2 - 2;
        float sy = shipY + cs / 2f;
        float ex = tx + cs / 2f;
        float ey = ty + cs / 2f;
        float mx = (sx + ex) / 2f;
        float my = Math.min(sy, ey) - 26f;
        g.setColor(new Color(255, 180, 30));
        g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new QuadCurve2D.Float(sx, sy, mx, my, ex, ey));

        // Explosion at target
        int bx = tx + cs / 2 - 10;
        int by = ty + cs / 2 - 10;
        g.setColor(new Color(229, 86, 47, 210));
        g.fillOval(bx, by, 20, 20);
        g.setColor(new Color(255, 214, 95, 230));
        g.fillOval(bx + 5, by + 5, 10, 10);

        // Missile button on the left
        int btnX = startX - 38;
        int btnY = startY + gridRows * cs / 2 - 13;
        g.setColor(new Color(90, 25, 15));
        g.fillRoundRect(btnX, btnY, 32, 26, 6, 6);
        g.setColor(new Color(255, 140, 30));
        g.setStroke(new BasicStroke(2f));
        g.drawRoundRect(btnX, btnY, 32, 26, 6, 6);
        // Blast star inside button
        int[] bpx = {btnX + 16, btnX + 19, btnX + 29, btnX + 21, btnX + 25, btnX + 18,
                     btnX + 16, btnX + 14, btnX + 7, btnX + 11, btnX + 3, btnX + 13};
        int[] bpy = {btnY + 2, btnY + 8, btnY + 9, btnY + 13, btnY + 22, btnY + 21,
                     btnY + 24, btnY + 21, btnY + 22, btnY + 13, btnY + 9, btnY + 8};
        g.setColor(new Color(255, 200, 60));
        g.fillPolygon(bpx, bpy, bpx.length);
        // Armed label
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 9));
        g.setColor(new Color(255, 140, 30));
        g.drawString("ARMED", btnX, btnY + 38);
    }

    private void paintTutorialVictory(Graphics2D g, int w, int h) {
        int cs = 22;
        int gridRows = 4;
        int gridCols = 5;
        int dockW = 26;
        int harborW = 26;
        int half = w / 2;

        // ---- Left half: Leg 1 — approaching east harbor ----
        int l1GridX = 8 + dockW;
        int l1StartY = (h - gridRows * cs) / 2;

        // Harbor
        g.setColor(HARBOR_BG);
        g.fillRect(l1GridX + gridCols * cs, l1StartY, harborW, gridRows * cs);
        g.setColor(new Color(89, 120, 74));
        g.fillRoundRect(l1GridX + gridCols * cs + 2, l1StartY + 2, harborW - 4, gridRows * cs - 4, 4, 4);
        g.setColor(new Color(227, 207, 92));
        g.fillOval(l1GridX + gridCols * cs + harborW - 12, l1StartY + 4, 8, 8);

        for (int r = 0; r < gridRows; r++) {
            for (int c = 0; c < gridCols; c++) {
                int x = l1GridX + c * cs;
                int y = l1StartY + r * cs;
                g.setColor(r == 2 ? REVEALED_CELL_BG : HIDDEN_CELL_BG);
                g.fillRect(x + 1, y + 1, cs - 2, cs - 2);
                g.setColor(GRID_WATER_BG);
                g.drawRect(x, y, cs, cs);
            }
        }

        // Ship approaching harbor
        int s1X = l1GridX + 3 * cs;
        int s1Y = l1StartY + 2 * cs;
        Graphics2D gc1 = (Graphics2D) g.create();
        gc1.translate(s1X, s1Y);
        paintBoat(gc1, cs * 2, cs);
        gc1.dispose();

        // Arrow into harbor
        g.setColor(new Color(255, 255, 255, 200));
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int a1y = l1StartY + 2 * cs + cs / 2;
        int a1sx = l1GridX + 5 * cs + 2;
        int a1ex = l1GridX + 5 * cs + harborW - 6;
        g.drawLine(a1sx, a1y, a1ex, a1y);
        g.fillPolygon(new int[]{a1ex, a1ex - 6, a1ex - 6},
                      new int[]{a1y, a1y - 4, a1y + 4}, 3);

        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
        g.setColor(new Color(89, 120, 74));
        g.drawString("LEG 1: East", 8, l1StartY - 4);

        // Divider
        g.setColor(new Color(255, 255, 255, 70));
        float[] dash = {4f, 4f};
        g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f, dash, 0f));
        g.drawLine(half, 4, half, h - 4);

        // ---- Right half: Leg 2 — returning to west dock ----
        int l2StartX = half + 6;
        int l2GridX = l2StartX + dockW;
        int l2StartY = l1StartY;

        // West dock
        g.setColor(DOCK_BG);
        g.fillRect(l2StartX, l2StartY, dockW, gridRows * cs);
        g.setColor(new Color(114, 83, 52));
        g.fillRect(l2StartX + 4, l2StartY + 2 * cs + cs / 2 - 2, dockW - 4, 4);
        g.setColor(new Color(193, 58, 45));
        g.fillRect(l2StartX + dockW / 2 - 1, l2StartY + 6, 2, 10);
        g.fillOval(l2StartX + dockW / 2 - 4, l2StartY + 4, 8, 6);

        for (int r = 0; r < gridRows; r++) {
            for (int c = 0; c < gridCols; c++) {
                int x = l2GridX + c * cs;
                int y = l2StartY + r * cs;
                g.setColor(r == 2 ? REVEALED_CELL_BG : HIDDEN_CELL_BG);
                g.fillRect(x + 1, y + 1, cs - 2, cs - 2);
                g.setColor(GRID_WATER_BG);
                g.drawRect(x, y, cs, cs);
            }
        }

        // Ship returning toward dock
        int s2X = l2GridX + 2 * cs;
        int s2Y = l2StartY + 2 * cs;
        Graphics2D gc2 = (Graphics2D) g.create();
        gc2.translate(s2X, s2Y);
        paintBoat(gc2, cs * 2, cs);
        gc2.dispose();

        // Arrow into dock (pointing left)
        g.setColor(new Color(255, 255, 255, 200));
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int a2y = l2StartY + 2 * cs + cs / 2;
        g.drawLine(l2StartX + dockW - 4, a2y, l2StartX + 6, a2y);
        g.fillPolygon(new int[]{l2StartX + 6, l2StartX + 12, l2StartX + 12},
                      new int[]{a2y, a2y - 4, a2y + 4}, 3);

        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
        g.setColor(new Color(193, 58, 45));
        g.drawString("LEG 2: Win!", l2StartX, l2StartY - 4);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
        g.setColor(new Color(193, 58, 45));
        int winLabelX = l2StartX + (dockW + gridCols * cs) / 2 - 22;
        int winLabelY = l2StartY + gridRows * cs + 20;
        if (winLabelY > h - 2) { winLabelY = h - 2; }
        g.drawString("WIN!", winLabelX, winLabelY);
    }

    // -------------------------------------------------------------------------
    // Game initialisation
    // -------------------------------------------------------------------------

    private void startGame(int r, int c, int m, int mineLayerCount, String key) {
        this.rows = r;
        this.cols = c;
        this.mines = m;
        this.mineLayerCount = Math.max(0, mineLayerCount);
        this.difficultyKey = key;

        if (swingTimer != null) {
            swingTimer.stop();
        }
        if (ambientAnimationTimer != null) {
            ambientAnimationTimer.stop();
        }
        if (boatAnimationTimer != null) {
            boatAnimationTimer.stop();
        }
        if (harborSequenceTimer != null) {
            harborSequenceTimer.stop();
        }
        if (westDockSequenceTimer != null) {
            westDockSequenceTimer.stop();
        }
        if (missileAnimationTimer != null) {
            missileAnimationTimer.stop();
        }
        if (mineLayerTimer != null) {
            mineLayerTimer.stop();
        }
        lastMoveSnapshot = null;
        lastMoveElapsedSeconds = 0;
        elapsedSeconds = 0;
        missileModeArmed = false;
        winDialogShown = false;
        harborSequence = null;
        westDockSequence = null;
        animationEpochNanos = System.nanoTime();

        board = new Board(rows, cols, mines, this.mineLayerCount);
        lastGameState = board.getGameState();
        lastVoyageStage = board.getVoyageStage();
        getContentPane().removeAll();
        buildUI();
        pack();
        setLocationRelativeTo(null);
        revalidate();
        repaint();
    }

    private void buildUI() {
        setLayout(new BorderLayout(0, 0));
        installBoatKeyBindings();

        // ── Top toolbar ──────────────────────────────────────────────────────
        JPanel toolbar = new JPanel(new BorderLayout(10, 0));
        toolbar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createRaisedBevelBorder(),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)));

        mineCountLabel = new JLabel(formatMineCount(board.getRemainingMines()));
        mineCountLabel.setFont(new Font(Font.MONOSPACED, Font.BOLD, 22));
        mineCountLabel.setForeground(Color.RED);
        mineCountLabel.setBackground(Color.BLACK);
        mineCountLabel.setOpaque(true);
        mineCountLabel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLoweredBevelBorder(),
            BorderFactory.createEmptyBorder(2, 6, 2, 6)));

        resetButton = new JButton();
        resetButton.setIcon(toolbarBoatIcon);
        resetButton.setFocusPainted(false);
        resetButton.setToolTipText("Restart crossing (F2)");
        resetButton.addActionListener(e -> startGame(rows, cols, mines, mineLayerCount, difficultyKey));

        missileButton = new JToggleButton();
        missileButton.setFocusPainted(false);
        missileButton.setMargin(new Insets(4, 8, 4, 8));
        missileButton.addActionListener(e -> {
            setMissileModeArmed(missileButton.isSelected());
            updateBoard();
        });
        refreshMissileControls();

        timerLabel = new JLabel("000");
        timerLabel.setFont(new Font(Font.MONOSPACED, Font.BOLD, 22));
        timerLabel.setForeground(Color.RED);
        timerLabel.setBackground(Color.BLACK);
        timerLabel.setOpaque(true);
        timerLabel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLoweredBevelBorder(),
            BorderFactory.createEmptyBorder(2, 6, 2, 6)));

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        actionPanel.setOpaque(false);
        actionPanel.add(resetButton);
        actionPanel.add(missileButton);

        toolbar.add(mineCountLabel, BorderLayout.WEST);
        toolbar.add(actionPanel, BorderLayout.CENTER);
        toolbar.add(timerLabel, BorderLayout.EAST);
        toolbar.setBackground(TOOLBAR_BG);

        add(toolbar, BorderLayout.NORTH);

        // ── Grid panel ───────────────────────────────────────────────────────
        gridPanel = new JPanel(new GridLayout(rows, cols, 1, 1));
        gridPanel.setBackground(GRID_WATER_BG);
        gridPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(4, 6, 6, 6),
            BorderFactory.createLineBorder(GRID_WATER_BG.darker(), 1)));

        buttons = new JButton[rows][cols];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                JButton btn = createCellButton(r, c);
                buttons[r][c] = btn;
                gridPanel.add(btn);
            }
        }

        boardLayer = new JLayeredPane();
        Dimension gridSize = gridPanel.getPreferredSize();
        boardLayer.setPreferredSize(gridSize);
        gridPanel.setBounds(0, 0, gridSize.width, gridSize.height);
        boardLayer.add(gridPanel, JLayeredPane.DEFAULT_LAYER);

        boatOverlay = new BoatOverlay();
        boatOverlay.setBounds(0, 0, gridSize.width, gridSize.height);
        boardLayer.add(boatOverlay, JLayeredPane.PALETTE_LAYER);

        missileOverlay = new MissileOverlay();
        missileOverlay.setBounds(0, 0, gridSize.width, gridSize.height);
        boardLayer.add(missileOverlay, JLayeredPane.DRAG_LAYER);

        mineLayerOverlay = new MineLayerOverlay();
        mineLayerOverlay.setBounds(0, 0, gridSize.width, gridSize.height);
        boardLayer.add(mineLayerOverlay, JLayeredPane.MODAL_LAYER);

        JPanel straitPanel = new JPanel(new BorderLayout(6, 0));
        straitPanel.setBackground(SIDE_PANEL_BG);
        straitPanel.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));
        straitPanel.add(createEdgeMarkerPanel("WEST DOCK", "Awaiting Orders", dockIcon, DOCK_BG, true), BorderLayout.WEST);
        straitPanel.add(boardLayer, BorderLayout.CENTER);
        straitPanel.add(createEdgeMarkerPanel("EAST HARBOR", "Landfall", harborIcon, HARBOR_BG, false), BorderLayout.EAST);
        add(straitPanel, BorderLayout.CENTER);

        statusLabel = new JLabel("Reveal a safe western approach, then clear east.");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        statusLabel.setOpaque(true);
        statusLabel.setBackground(STATUS_BG);
        statusLabel.setForeground(STATUS_FG);
        add(statusLabel, BorderLayout.SOUTH);

        // ── Timer ────────────────────────────────────────────────────────────
        swingTimer = new Timer(1000, e -> {
            if (board.getGameState() == Board.GameState.PLAYING) {
                elapsedSeconds = Math.min(999, elapsedSeconds + 1);
                timerLabel.setText(String.format("%03d", elapsedSeconds));
            }
        });
        initializeWestApproach();
        ensureAmbientAnimationTimer();
        ambientAnimationTimer.start();
        swingTimer.start();
        ensureMineLayerTimer();
        if (mineLayerCount > 0) {
            mineLayerTimer.start();
        }
        updateBoard();
    }

    private void initializeWestApproach() {
        if (board == null || board.getGameState() != Board.GameState.WAITING || cols <= 0) {
            return;
        }
        board.reveal(rows / 2, 0);
    }

    private void onGameWon() {
        if (winDialogShown) {
            return;
        }
        winDialogShown = true;

        boolean newBest = false;
        if (!"Custom".equals(difficultyKey)) {
            Integer previous = bestTimes.get(difficultyKey);
            if (previous == null || elapsedSeconds < previous) {
                bestTimes.put(difficultyKey, elapsedSeconds);
                newBest = true;
            }
        }

        String body = newBest
            ? String.format(
                "The tanker is back in the west dock after %d seconds.<br><b>New best time for %s.</b><br>Hormuz is clear again.",
                elapsedSeconds,
                difficultyKey)
            : String.format(
                "The tanker is back in the west dock after %d seconds.<br>Round trip complete. Hormuz is clear again.",
                elapsedSeconds);

        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
            this,
            createVoyageDialogPanel(
                "Home Safe",
                body,
                homecomingDialogIcon,
                blend(STATUS_BG, DOCK_BG, 0.35f),
                DOCK_BG),
            "Convoy Home",
            JOptionPane.PLAIN_MESSAGE));
    }

    private void ensureAmbientAnimationTimer() {
        if (ambientAnimationTimer != null) {
            return;
        }
        ambientAnimationTimer = new Timer(AMBIENT_ANIMATION_DELAY_MS, e -> {
            if (buttons != null) {
                for (JButton[] buttonRow : buttons) {
                    if (buttonRow == null) {
                        continue;
                    }
                    for (JButton button : buttonRow) {
                        if (button != null) {
                            button.repaint();
                        }
                    }
                }
            }
            if (boatOverlay != null && boatOverlay.isVisible()) {
                boatOverlay.repaint();
            }
            if (mineLayerOverlay != null && mineLayerOverlay.isVisible()) {
                mineLayerOverlay.repaint();
            }
            if (missileOverlay != null && missileOverlay.isVisible()) {
                missileOverlay.repaint();
            }
        });
    }

    private void renderCell(int row, int col) {
        JButton btn = buttons[row][col];
        Cell cell = board.getCell(row, col);

        switch (cell.getState()) {
            case HIDDEN:
                styleHiddenButton(btn);
                if (col == 0) {
                    btn.setIcon(dockIcon);
                } else if (col == cols - 1) {
                    btn.setIcon(harborIcon);
                }
                break;
            case FLAGGED:
                styleHiddenButton(btn);
                btn.setText("\uD83D\uDEA9");  // 🚩
                btn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));
                break;
            case QUESTION:
                styleHiddenButton(btn);
                btn.setText("?");
                btn.setForeground(QUESTION_MARK_COLOR);
                btn.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
                break;
            case WRONG_FLAG:
                styleRevealedButton(btn, cell, row, col);
                btn.setText("\u2716");  // ✖ (wrong flag indicator)
                btn.setForeground(Color.RED);
                btn.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
                break;
            case REVEALED:
                styleRevealedButton(btn, cell, row, col);
                break;
        }
    }

    private void styleHiddenButton(JButton btn) {
        btn.setText("");
        btn.setIcon(null);
        btn.setForeground(Color.BLACK);
        btn.setBackground(HIDDEN_CELL_BG);
        btn.setBorder(BorderFactory.createRaisedBevelBorder());
        btn.setEnabled(true);
    }

    private void styleRevealedButton(JButton btn, Cell cell, int row, int col) {
        btn.setBorder(BorderFactory.createLineBorder(REVEALED_BORDER, 1));
        btn.setText("");
        btn.setIcon(null);
        btn.setForeground(Color.BLACK);

        if (board.isBoatOccupying(row, col)) {
            btn.setBackground(BOAT_CELL_BG);
        } else if (cell.isMine() && row == board.getTriggeredRow() && col == board.getTriggeredCol()) {
            btn.setBackground(TRIGGERED_MINE_BG);
        } else if (col == 0) {
            btn.setBackground(blend(REVEALED_CELL_BG, DOCK_BG, 0.45f));
        } else if (col == cols - 1) {
            btn.setBackground(blend(REVEALED_CELL_BG, HARBOR_BG, 0.45f));
        } else {
            btn.setBackground(REVEALED_CELL_BG);
        }

        if (cell.isMine()) {
            btn.setText("\uD83D\uDCA3");
            btn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));
            btn.setEnabled(false);
        } else {
            if (col == 0) {
                btn.setIcon(dockIcon);
            } else if (col == cols - 1) {
                btn.setIcon(harborIcon);
            }
            int adj = cell.getAdjacentMines();
            if (adj == 0) {
                btn.setText("");
            } else {
                btn.setText(String.valueOf(adj));
                btn.setForeground(NUMBER_COLORS[adj]);
                btn.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
            }
            btn.setEnabled(true);
        }
    }

    private JButton createCellButton(int row, int col) {
        JButton btn = new CellButton(row, col);
        btn.setPreferredSize(new Dimension(CELL_SIZE, CELL_SIZE));
        btn.setFocusPainted(false);
        btn.setMargin(new Insets(0, 0, 0, 0));
        btn.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        styleHiddenButton(btn);

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (board.getGameState() == Board.GameState.WON
                    || board.getGameState() == Board.GameState.LOST
                    || isMissileAnimationActive()
                    || isArrivalSequenceActive()) {
                    return;
                }

                int modifiers = e.getModifiersEx();
                Cell cell = board.getCell(row, col);
                boolean dualButtonChord = (modifiers & InputEvent.BUTTON1_DOWN_MASK) != 0
                    && (modifiers & InputEvent.BUTTON3_DOWN_MASK) != 0;

                if ((SwingUtilities.isMiddleMouseButton(e) || dualButtonChord)
                    && cell.isRevealed()
                    && cell.getAdjacentMines() > 0) {
                    chordRow = row;
                    chordCol = col;
                    return;
                }

                if (SwingUtilities.isLeftMouseButton(e)) {
                    resetButton.setIcon(waveIcon);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (board.getGameState() == Board.GameState.WON
                    || board.getGameState() == Board.GameState.LOST
                    || isMissileAnimationActive()
                    || isArrivalSequenceActive()) {
                    return;
                }

                Cell cell = board.getCell(row, col);
                if ((row == chordRow && col == chordCol)
                    && (SwingUtilities.isMiddleMouseButton(e)
                    || SwingUtilities.isLeftMouseButton(e)
                    || SwingUtilities.isRightMouseButton(e))) {
                    rememberUndoState();
                    board.chord(row, col);
                    playSoundEffect(SoundEffect.CHORD);
                } else if (SwingUtilities.isLeftMouseButton(e) && missileModeArmed) {
                    rememberUndoState();
                    startMissileStrike(row, col);
                } else if (SwingUtilities.isLeftMouseButton(e)
                    && e.getClickCount() >= 2
                    && cell.isRevealed()
                    && cell.getAdjacentMines() > 0) {
                    rememberUndoState();
                    board.chord(row, col);
                    playSoundEffect(SoundEffect.CHORD);
                } else if (SwingUtilities.isLeftMouseButton(e)) {
                    rememberUndoState();
                    boolean wasDocked = board.isBoatDocked();
                    if (!board.moveBoatTo(row, col)) {
                        board.reveal(row, col);
                        playSoundEffect(board.getGameState() == Board.GameState.LOST ? SoundEffect.MINE_HIT : SoundEffect.REVEAL);
                    } else {
                        if (wasDocked && boatOverlay != null) {
                            boatOverlay.triggerLaunchWake();
                        }
                        playSoundEffect(SoundEffect.BOAT_MOVE);
                    }
                } else if (SwingUtilities.isRightMouseButton(e)) {
                    rememberUndoState();
                    board.toggleFlag(row, col);
                    playSoundEffect(SoundEffect.FLAG);
                }

                chordRow = -1;
                chordCol = -1;
                updateBoard();
            }
        });
        return btn;
    }

    // -------------------------------------------------------------------------
    // Board rendering
    // -------------------------------------------------------------------------

    private void updateBoard() {
        Board.GameState state = board.getGameState();
        maybeStartHarborSequence(state);
        maybeStartWestDockSequence(state);
        Point2D targetBoatPosition = getBoatRenderPosition();

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                renderCell(r, c);
            }
        }

        updateMineLayerOverlay(state);
        updateBoatOverlay(state, targetBoatPosition);
        refreshDockIndicator();
        updateMissileOverlay();

        mineCountLabel.setText(formatMineCount(board.getRemainingMines()));
        statusLabel.setText(buildStatusText(state));
        refreshMissileControls();

        switch (state) {
            case WON:
                cancelHarborSequence();
                resetButton.setIcon(successIcon);
                swingTimer.stop();
                if (mineLayerTimer != null) {
                    mineLayerTimer.stop();
                }
                if (!isWestDockSequenceActive() && !winDialogShown) {
                    onGameWon();
                }
                break;
            case LOST:
                cancelHarborSequence();
                cancelWestDockSequence();
                resetButton.setIcon(blastIcon);
                swingTimer.stop();
                if (mineLayerTimer != null) {
                    mineLayerTimer.stop();
                }
                break;
            default:
                resetButton.setIcon(toolbarBoatIcon);
        }

        lastGameState = state;
    }

    private String formatMineCount(int count) {
        return String.format("%03d", Math.max(-99, Math.min(999, count)));
    }

    private String buildStatusText(Board.GameState state) {
        String missileSuffix = missileModeArmed
            ? String.format(" Missile armed: click a target cell to blast a 3x3 area. Missiles left: %d.", board.getMissilesRemaining())
            : String.format(" Missiles left: %d.", board.getMissilesRemaining());
        String mineLayerSuffix = mineLayerCount > 0
            ? String.format(" Enemy minelayers active: %d.", mineLayerCount)
            : " Enemy minelayers disabled.";
        if (state == Board.GameState.WON) {
            return "Convoy home safe. Round trip completed." + mineLayerSuffix + missileSuffix;
        }
        if (state == Board.GameState.LOST) {
            return "Route compromised. Restart and search for another channel." + mineLayerSuffix + missileSuffix;
        }
        if (board.isBoatDocked()) {
            return "Ship is in the west dock. Click a revealed west-edge water tile to launch." + mineLayerSuffix + missileSuffix;
        }
        if (board.getVoyageStage() == Board.VoyageStage.RETURNING) {
            return String.format(
                "Refueled at the east harbor. Bring the ship back west. Current column: %d of %d.%s%s",
                board.getBoatCol() + 1,
                cols,
                mineLayerSuffix,
                missileSuffix);
        }
        if (board.getBoatCol() >= 0) {
            return String.format(
                "Outbound leg: reach the east harbor. Ship is at column %d of %d.%s%s",
                board.getBoatCol() + 1,
                cols,
                mineLayerSuffix,
                missileSuffix);
        }
        return "Reveal a safe western approach, then click the dockside channel to launch the ship." + mineLayerSuffix + missileSuffix;
    }

    private void installBoatKeyBindings() {
        JRootPane rootPane = getRootPane();
        InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = rootPane.getActionMap();

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK), "undo-last-move");
        actionMap.put("undo-last-move", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                undoLastMove();
            }
        });
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_M, 0), "toggle-missile-mode");
        actionMap.put("toggle-missile-mode", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setMissileModeArmed(!missileModeArmed);
                playSoundEffect(missileModeArmed ? SoundEffect.ARM_ON : SoundEffect.ARM_OFF);
                updateBoard();
            }
        });

        bindBoatMoveKey(inputMap, actionMap, "UP", "boat-up", -1, 0);
        bindBoatMoveKey(inputMap, actionMap, "DOWN", "boat-down", 1, 0);
        bindBoatMoveKey(inputMap, actionMap, "LEFT", "boat-left", 0, -1);
        bindBoatMoveKey(inputMap, actionMap, "RIGHT", "boat-right", 0, 1);
    }

    private void bindBoatMoveKey(InputMap inputMap, ActionMap actionMap, String keystroke, String actionKey,
                                 int rowDelta, int colDelta) {
        inputMap.put(KeyStroke.getKeyStroke(keystroke), actionKey);
        actionMap.put(actionKey, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (isArrivalSequenceActive() || isMissileAnimationActive()) {
                    return;
                }
                if (board != null) {
                    rememberUndoState();
                }
                if (board != null && board.moveBoatByBrave(rowDelta, colDelta)) {
                    if (board.getGameState() != Board.GameState.LOST) {
                        playSoundEffect(SoundEffect.BOAT_MOVE);
                    }
                    updateBoard();
                }
            }
        });
    }

    private void rememberUndoState() {
        if (board == null) {
            return;
        }
        lastMoveSnapshot = board.createSnapshot();
        lastMoveElapsedSeconds = elapsedSeconds;
    }

    private void setMissileModeArmed(boolean armed) {
        if (board != null && board.getMissilesRemaining() <= 0) {
            missileModeArmed = false;
            if (missileMenuItem != null) {
                missileMenuItem.setSelected(false);
            }
            if (missileButton != null) {
                missileButton.setSelected(false);
            }
            refreshMissileControls();
            return;
        }
        missileModeArmed = armed;
        if (missileMenuItem != null) {
            missileMenuItem.setSelected(missileModeArmed);
        }
        if (missileButton != null) {
            missileButton.setSelected(missileModeArmed);
        }
        setCursor(missileModeArmed ? Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR) : Cursor.getDefaultCursor());
        refreshMissileControls();
    }

    private void refreshMissileControls() {
        if (missileButton == null || board == null) {
            return;
        }

        int missilesRemaining = board.getMissilesRemaining();
        missileButton.setText(missileModeArmed
            ? String.format("Missile Armed (%d)", missilesRemaining)
            : String.format("Arm Missile (%d)", missilesRemaining));
        missileButton.setToolTipText("Arm a missile from the main toolbar (M)");
        missileButton.setEnabled(missilesRemaining > 0 && board.getGameState() == Board.GameState.PLAYING);
        missileButton.setSelected(missileModeArmed);
    }

    private void undoLastMove() {
        if (board == null || lastMoveSnapshot == null) {
            return;
        }

        board.restoreSnapshot(lastMoveSnapshot);
        elapsedSeconds = lastMoveElapsedSeconds;
        timerLabel.setText(String.format("%03d", elapsedSeconds));
        lastMoveSnapshot = null;
        cancelHarborSequence();
        cancelWestDockSequence();
        lastVoyageStage = board.getVoyageStage();
        lastGameState = board.getGameState();
        winDialogShown = false;
        setMissileModeArmed(false);

        if (board.getGameState() == Board.GameState.PLAYING) {
            if (swingTimer != null) {
                swingTimer.start();
            }
            if (mineLayerTimer != null && mineLayerCount > 0) {
                mineLayerTimer.start();
            }
        }

        updateBoard();
    }

    private void updateMineLayerOverlay(Board.GameState state) {
        if (mineLayerOverlay == null) {
            return;
        }
        mineLayerOverlay.setVisible(state != Board.GameState.LOST && !board.getMineLayerPositions().isEmpty());
        mineLayerOverlay.repaint();
    }

    private void updateMissileOverlay() {
        if (missileOverlay == null) {
            return;
        }
        missileOverlay.setVisible(isMissileAnimationActive());
        missileOverlay.repaint();
    }

    private void maybeStartHarborSequence(Board.GameState state) {
        Board.VoyageStage currentVoyageStage = board.getVoyageStage();
        if (state == Board.GameState.PLAYING
            && lastVoyageStage == Board.VoyageStage.OUTBOUND
            && currentVoyageStage == Board.VoyageStage.RETURNING
            && harborSequence == null) {
            startHarborSequence();
        }
        lastVoyageStage = currentVoyageStage;
    }

    private void maybeStartWestDockSequence(Board.GameState state) {
        if (state == Board.GameState.WON
            && lastGameState != Board.GameState.WON
            && westDockSequence == null) {
            startWestDockSequence();
        }
    }

    private void updateBoatOverlay(Board.GameState state, Point2D targetBoatPosition) {
        if (boatOverlay == null) {
            return;
        }

        if (state == Board.GameState.LOST || targetBoatPosition == null) {
            boatOverlay.setVisible(false);
            if (boatAnimationTimer != null) {
                boatAnimationTimer.stop();
            }
            return;
        }

        boatOverlay.setVisible(true);
        if (!boatOverlay.hasPosition()) {
            boatOverlay.snapTo(targetBoatPosition.x, targetBoatPosition.y);
            return;
        }

        boatOverlay.setTarget(targetBoatPosition.x, targetBoatPosition.y);
        if (boatOverlay.isAtTarget()) {
            boatOverlay.repaint();
            return;
        }
        ensureBoatAnimationTimer();
        if (!boatAnimationTimer.isRunning()) {
            boatAnimationTimer.start();
        }
    }

    private void ensureBoatAnimationTimer() {
        if (boatAnimationTimer != null) {
            return;
        }
        boatAnimationTimer = new Timer(BOAT_ANIMATION_DELAY_MS, e -> {
            if (boatOverlay == null || !boatOverlay.isVisible()) {
                ((Timer) e.getSource()).stop();
                return;
            }
            boolean stillMoving = boatOverlay.stepTowardsTarget();
            if (!stillMoving) {
                ((Timer) e.getSource()).stop();
            }
        });
    }

    private void ensureHarborSequenceTimer() {
        if (harborSequenceTimer != null) {
            return;
        }
        harborSequenceTimer = new Timer(HARBOR_SEQUENCE_DELAY_MS, e -> {
            if (harborSequence == null) {
                ((Timer) e.getSource()).stop();
                return;
            }

            if (harborSequence.dockingProgress < 1f) {
                harborSequence.dockingProgress = Math.min(1f, harborSequence.dockingProgress + HARBOR_DOCKING_STEP);
            } else if (harborSequence.mooringProgress < 1f) {
                harborSequence.mooringProgress = Math.min(1f, harborSequence.mooringProgress + HARBOR_MOORING_STEP);
            } else {
                harborSequence.refuelProgress = Math.min(1f, harborSequence.refuelProgress + HARBOR_REFUEL_STEP);
            }

            if (boatOverlay != null) {
                boatOverlay.repaint();
            }

            if (harborSequence.refuelProgress >= 1f) {
                ((Timer) e.getSource()).stop();
                harborSequence = null;
                if (boatOverlay != null) {
                    boatOverlay.repaint();
                }
                playSoundEffect(SoundEffect.HARBOR_COMPLETE);
                SwingUtilities.invokeLater(this::showHarborArrivalDialog);
            }
        });
    }

    private void ensureWestDockSequenceTimer() {
        if (westDockSequenceTimer != null) {
            return;
        }
        westDockSequenceTimer = new Timer(HARBOR_SEQUENCE_DELAY_MS, e -> {
            if (westDockSequence == null) {
                ((Timer) e.getSource()).stop();
                return;
            }

            if (westDockSequence.dockingProgress < 1f) {
                westDockSequence.dockingProgress = Math.min(1f, westDockSequence.dockingProgress + HARBOR_DOCKING_STEP);
            } else if (westDockSequence.mooringProgress < 1f) {
                westDockSequence.mooringProgress = Math.min(1f, westDockSequence.mooringProgress + HARBOR_MOORING_STEP);
            } else {
                westDockSequence.celebrationProgress = Math.min(1f, westDockSequence.celebrationProgress + WEST_DOCK_CELEBRATION_STEP);
            }

            if (boatOverlay != null) {
                boatOverlay.repaint();
            }

            if (westDockSequence.celebrationProgress >= 1f) {
                ((Timer) e.getSource()).stop();
                westDockSequence = null;
                if (boatOverlay != null) {
                    boatOverlay.repaint();
                }
                playSoundEffect(SoundEffect.HOMECOMING);
                onGameWon();
            }
        });
    }

    private void ensureMissileAnimationTimer() {
        if (missileAnimationTimer != null) {
            return;
        }
        missileAnimationTimer = new Timer(MISSILE_ANIMATION_DELAY_MS, e -> {
            if (missileAnimation == null) {
                ((Timer) e.getSource()).stop();
                updateMissileOverlay();
                return;
            }

            if (!missileAnimation.impactApplied) {
                missileAnimation.flightProgress = Math.min(1f, missileAnimation.flightProgress + MISSILE_FLIGHT_STEP);
                if (missileAnimation.flightProgress >= 1f) {
                    missileAnimation.impactApplied = true;
                    playSoundEffect(SoundEffect.MISSILE_IMPACT);
                    if (board.fireMissileAt(missileAnimation.targetRow, missileAnimation.targetCol)) {
                        updateBoard();
                    }
                }
            } else {
                missileAnimation.explosionProgress += MISSILE_EXPLOSION_STEP;
                if (missileAnimation.explosionProgress >= 1f) {
                    missileAnimation = null;
                    ((Timer) e.getSource()).stop();
                    updateBoard();
                    return;
                }
            }

            updateMissileOverlay();
        });
    }

    private void ensureMineLayerTimer() {
        if (mineLayerTimer != null) {
            return;
        }
        mineLayerTimer = new Timer(MINELAYER_STEP_DELAY_MS, e -> {
            if (board == null || board.getGameState() != Board.GameState.PLAYING) {
                return;
            }
            if (board.advanceMineLayers()) {
                updateBoard();
            }
        });
    }

    private Point2D getBoatRenderPosition() {
        if (board.isBoatDocked()) {
            return null;
        }
        int boatRow = board.getBoatRow();
        int boatCol = board.getBoatCol();
        if (boatRow < 0 || boatCol < 0) {
            return null;
        }

        int maxRowAnchor = Math.max(0, rows - 1);
        int maxColAnchor = Math.max(0, cols - 2);
        int anchorRow = Math.min(boatRow, maxRowAnchor);
        int anchorCol = Math.min(boatCol, maxColAnchor);

        int overlayWidth = CELL_SIZE * Math.min(2, cols);
        int overlayHeight = CELL_SIZE;
        float x = anchorCol * CELL_SIZE + (CELL_SIZE * 2 - overlayWidth) / 2f;
        float y = anchorRow * CELL_SIZE;
        return new Point2D(x, y);
    }

    private void startHarborSequence() {
        harborSequence = new HarborSequence();
        setMissileModeArmed(false);
        playSoundEffect(SoundEffect.HARBOR_APPROACH);
        ensureHarborSequenceTimer();
        harborSequenceTimer.start();
        if (boatOverlay != null) {
            boatOverlay.repaint();
        }
    }

    private void startWestDockSequence() {
        westDockSequence = new WestDockSequence();
        setMissileModeArmed(false);
        playSoundEffect(SoundEffect.DOCK_APPROACH);
        ensureWestDockSequenceTimer();
        westDockSequenceTimer.start();
        if (boatOverlay != null) {
            boatOverlay.repaint();
        }
    }

    private void cancelHarborSequence() {
        harborSequence = null;
        if (harborSequenceTimer != null) {
            harborSequenceTimer.stop();
        }
        if (boatOverlay != null) {
            boatOverlay.repaint();
        }
    }

    private void cancelWestDockSequence() {
        westDockSequence = null;
        if (westDockSequenceTimer != null) {
            westDockSequenceTimer.stop();
        }
        if (boatOverlay != null) {
            boatOverlay.repaint();
        }
    }

    private boolean isHarborSequenceActive() {
        return harborSequence != null;
    }

    private boolean isWestDockSequenceActive() {
        return westDockSequence != null;
    }

    private boolean isArrivalSequenceActive() {
        return isHarborSequenceActive() || isWestDockSequenceActive();
    }

    private boolean isMissileAnimationActive() {
        return missileAnimation != null;
    }

    private void startMissileStrike(int targetRow, int targetCol) {
        if (board == null || missileAnimation != null || board.getMissilesRemaining() <= 0) {
            return;
        }

        Point2D start = getMissileLaunchPosition(targetRow);
        Point2D target = new Point2D(
            targetCol * CELL_SIZE + CELL_SIZE / 2f,
            targetRow * CELL_SIZE + CELL_SIZE / 2f);

        missileAnimation = new MissileAnimation(start.x, start.y, target.x, target.y, targetRow, targetCol);
        setMissileModeArmed(false);
        playSoundEffect(SoundEffect.MISSILE_LAUNCH);
        ensureMissileAnimationTimer();
        missileAnimationTimer.start();
        updateMissileOverlay();
    }

    private void playSoundEffect(SoundEffect effect) {
        if (effect == null || !soundEnabled) {
            return;
        }
        Thread thread = new Thread(() -> {
            try {
                playToneSequence(effect.tones);
            } catch (Exception ignored) {
                Toolkit.getDefaultToolkit().beep();
            }
        }, "hormuz-sfx");
        thread.setDaemon(true);
        thread.start();
    }

    private void playToneSequence(Tone[] tones) throws Exception {
        AudioFormat format = new AudioFormat(22050f, 8, 1, true, false);
        try (SourceDataLine line = AudioSystem.getSourceDataLine(format)) {
            line.open(format, 22050);
            line.start();
            for (Tone tone : tones) {
                byte[] buffer = synthesizeTone(tone.frequencyHz, tone.durationMs, tone.volume, tone.descending);
                line.write(buffer, 0, buffer.length);
            }
            line.drain();
        }
    }

    private byte[] synthesizeTone(float frequencyHz, int durationMs, float volume, boolean descending) {
        int sampleRate = 22050;
        int sampleCount = Math.max(1, sampleRate * durationMs / 1000);
        byte[] buffer = new byte[sampleCount];
        for (int i = 0; i < sampleCount; i++) {
            float progress = sampleCount == 1 ? 1f : i / (float) (sampleCount - 1);
            float envelope = (float) Math.sin(Math.PI * progress);
            float frequency = descending ? frequencyHz * (1.15f - 0.25f * progress) : frequencyHz * (0.92f + 0.12f * progress);
            double angle = 2.0 * Math.PI * frequency * i / sampleRate;
            double wave = Math.sin(angle) + 0.25 * Math.sin(angle * 2.0);
            buffer[i] = (byte) Math.round(127f * volume * envelope * wave);
        }
        return buffer;
    }

    private Point2D getMissileLaunchPosition(int targetRow) {
        if (!board.isBoatDocked()) {
            Point2D boatPosition = boatOverlay != null && boatOverlay.hasPosition()
                ? boatOverlay.getCurrentVisualPosition()
                : getBoatRenderPosition();
            if (boatPosition != null) {
                return new Point2D(boatPosition.x + CELL_SIZE * 1.75f, boatPosition.y + CELL_SIZE / 2f);
            }
        }
        return new Point2D(CELL_SIZE / 2f, targetRow * CELL_SIZE + CELL_SIZE / 2f);
    }

    private void refreshDockIndicator() {
        if (westDockIconLabel == null || westDockSubtitleLabel == null) {
            return;
        }
        westDockIconLabel.setIcon(board.isBoatDocked() ? toolbarBoatIcon : dockIcon);
        westDockSubtitleLabel.setText(board.isBoatDocked() ? "Awaiting Orders" : "Launch Clear");
    }

    private void showHarborArrivalDialog() {
        JOptionPane.showMessageDialog(
            this,
            createVoyageDialogPanel(
                "Harbor Reached",
                "The tanker has docked at the east harbor and taken on oil.<br><b>Your job now is to bring it safely back to the west dock.</b>",
                harborDialogIcon,
                blend(STATUS_BG, HARBOR_BG, 0.45f),
                HARBOR_BG),
            "Harbor Reached",
            JOptionPane.PLAIN_MESSAGE);
        updateBoard();
    }

    private JPanel createVoyageDialogPanel(String title, String body, Icon icon, Color background, Color accent) {
        JPanel panel = new JPanel(new BorderLayout(12, 0));
        panel.setBackground(background);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(accent.darker(), 2),
            BorderFactory.createEmptyBorder(14, 14, 14, 14)));

        JLabel iconLabel = new JLabel(icon);
        iconLabel.setVerticalAlignment(SwingConstants.TOP);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        titleLabel.setForeground(STATUS_FG.darker());

        JLabel bodyLabel = new JLabel("<html><div style='width:240px;'>" + body + "</div></html>");
        bodyLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        bodyLabel.setForeground(STATUS_FG);

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);
        textPanel.add(titleLabel);
        textPanel.add(Box.createVerticalStrut(8));
        textPanel.add(bodyLabel);

        panel.add(iconLabel, BorderLayout.WEST);
        panel.add(textPanel, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createEdgeMarkerPanel(String title, String subtitle, Icon icon, Color background, boolean westDock) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(background);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(REVEALED_BORDER, 1),
            BorderFactory.createEmptyBorder(10, 8, 10, 8)));

        JLabel iconLabel = new JLabel(icon);
        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel titleLabel = new JLabel(title);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        titleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        JLabel subtitleLabel = new JLabel(subtitle);
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        subtitleLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));

        if (westDock) {
            westDockIconLabel = iconLabel;
            westDockSubtitleLabel = subtitleLabel;
        }

        panel.add(Box.createVerticalGlue());
        panel.add(iconLabel);
        panel.add(Box.createVerticalStrut(8));
        panel.add(titleLabel);
        panel.add(Box.createVerticalStrut(4));
        panel.add(subtitleLabel);
        panel.add(Box.createVerticalGlue());
        return panel;
    }

    private Icon createBoatIcon(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            enableQuality(g);
            int waterline = height - 7;
            g.setColor(new Color(164, 211, 231));
            g.fillArc(1, height - 6, 9, 5, 0, 180);
            g.fillArc(width - 10, height - 6, 9, 5, 0, 180);

            Path2D hull = new Path2D.Double();
            hull.moveTo(2, waterline - 2);
            hull.lineTo(width - 6, waterline - 2);
            hull.lineTo(width - 2, height - 3);
            hull.lineTo(5, height - 3);
            hull.closePath();
            g.setColor(new Color(35, 52, 64));
            g.fill(hull);

            g.setColor(new Color(116, 132, 143));
            g.fillRoundRect(4, waterline - 3, width - 9, 3, 2, 2);

            g.setColor(new Color(214, 193, 147));
            g.fillRoundRect(5, waterline - 7, width - 13, 4, 2, 2);

            g.setColor(new Color(229, 235, 239));
            g.fillRoundRect(width - 11, waterline - 12, 8, 7, 2, 2);
            g.fillRoundRect(width - 9, waterline - 16, 5, 5, 2, 2);

            g.setColor(new Color(90, 124, 151));
            g.fillRoundRect(width - 9, waterline - 10, 2, 2, 1, 1);
            g.fillRoundRect(width - 6, waterline - 10, 2, 2, 1, 1);

            g.setColor(new Color(77, 87, 93));
            g.fillRect(width - 8, waterline - 19, 2, 4);
            g.fillRect(width - 5, waterline - 18, 2, 3);
            g.fillRect(8, waterline - 10, width - 18, 1);
        } finally {
            g.dispose();
        }
        return new ImageIcon(image);
    }

    private Icon createDockIcon(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            enableQuality(g);
            g.setColor(new Color(114, 83, 52));
            g.fillRect(2, height / 2, width - 4, 4);
            g.fillRect(3, height / 2 - 4, 6, 10);
            g.fillRect(width - 9, height / 2 - 4, 6, 10);
            g.setColor(new Color(154, 198, 218));
            g.fillArc(0, height - 7, 9, 7, 0, 180);
            g.fillArc(7, height - 6, 10, 6, 0, 180);
            g.setColor(new Color(193, 58, 45));
            g.fillRect(width / 2 - 1, 2, 2, height / 2 - 4);
            g.fillOval(width / 2 - 3, 1, 6, 6);
        } finally {
            g.dispose();
        }
        return new ImageIcon(image);
    }

    private Icon createHarborIcon(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            enableQuality(g);
            g.setColor(new Color(89, 120, 74));
            g.fillRoundRect(3, height - 10, width - 6, 7, 4, 4);
            g.setColor(new Color(227, 207, 92));
            g.fillOval(width - 10, 2, 6, 6);
            g.setColor(new Color(177, 60, 39));
            g.fillRect(5, 3, 2, height - 8);
            Path2D beacon = new Path2D.Double();
            beacon.moveTo(7, 5);
            beacon.lineTo(13, 8);
            beacon.lineTo(7, 11);
            beacon.closePath();
            g.fill(beacon);
            g.setColor(new Color(154, 198, 218));
            g.fillArc(width / 2 - 4, height - 6, 10, 6, 0, 180);
        } finally {
            g.dispose();
        }
        return new ImageIcon(image);
    }

    private Icon createWaveIcon(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            enableQuality(g);
            g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(67, 135, 185));
            g.drawArc(2, 8, 10, 8, 0, 180);
            g.drawArc(10, 6, 12, 10, 0, 180);
            g.drawArc(18, 8, 8, 8, 0, 180);
        } finally {
            g.dispose();
        }
        return new ImageIcon(image);
    }

    private Icon createBlastIcon(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            enableQuality(g);
            g.setColor(new Color(229, 86, 47));
            int[] x = {width / 2, width / 2 + 4, width - 2, width / 2 + 6, width - 4, width / 2 + 2, width / 2, width / 2 - 2, 4, width / 2 - 6, 2, width / 2 - 4};
            int[] y = {1, 7, 8, 11, height - 2, height - 3, height - 1, height - 3, height - 2, 11, 8, 7};
            g.fillPolygon(x, y, x.length);
            g.setColor(new Color(255, 214, 95));
            g.fillOval(width / 2 - 4, height / 2 - 4, 8, 8);
        } finally {
            g.dispose();
        }
        return new ImageIcon(image);
    }

    private void enableQuality(Graphics2D graphics) {
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }

    private Color blend(Color first, Color second, float ratio) {
        float clamped = Math.max(0f, Math.min(1f, ratio));
        float inverse = 1f - clamped;
        return new Color(
            Math.round(first.getRed() * inverse + second.getRed() * clamped),
            Math.round(first.getGreen() * inverse + second.getGreen() * clamped),
            Math.round(first.getBlue() * inverse + second.getBlue() * clamped));
    }

    private void paintBoat(Graphics2D g, int width, int height) {
        int hullTop = 7;
        int hullBottom = height - 8;
        int hullMidY = (hullTop + hullBottom) / 2;

        g.setColor(new Color(73, 147, 186, 92));
        g.fill(new Ellipse2D.Float(4f, hullBottom - 1f, width - 8f, 7f));

        Path2D hull = new Path2D.Float();
        hull.moveTo(5, hullMidY);
        hull.lineTo(10, hullTop + 1);
        hull.lineTo(width - 11, hullTop + 1);
        hull.quadTo(width - 5, hullTop + 2, width - 4, hullMidY);
        hull.quadTo(width - 5, hullBottom - 2, width - 11, hullBottom - 1);
        hull.lineTo(10, hullBottom - 1);
        hull.closePath();
        g.setColor(new Color(38, 50, 60));
        g.fill(hull);

        g.setColor(new Color(111, 123, 131));
        g.fill(new RoundRectangle2D.Float(8f, hullTop + 2f, width - 16f, hullBottom - hullTop - 4f, 8f, 8f));

        g.setColor(new Color(205, 188, 147));
        g.fill(new RoundRectangle2D.Float(10f, hullTop + 4f, width - 26f, hullBottom - hullTop - 8f, 6f, 6f));

        g.setColor(new Color(123, 111, 90));
        g.fillRect(13, hullMidY - 1, width - 34, 2);
        g.fillRect(13, hullMidY - 6, width - 34, 1);
        g.fillRect(13, hullMidY + 5, width - 34, 1);

        g.setColor(new Color(89, 95, 101));
        for (int x = 15; x < width - 20; x += 7) {
            g.fillRect(x, hullMidY - 7, 2, 14);
        }

        g.setColor(new Color(156, 140, 105));
        g.fill(new RoundRectangle2D.Float(16f, hullTop + 6f, width - 40f, 4f, 3f, 3f));
        g.fill(new RoundRectangle2D.Float(16f, hullBottom - 10f, width - 40f, 4f, 3f, 3f));

        g.setColor(new Color(231, 236, 240));
        g.fill(new RoundRectangle2D.Float(width - 22f, hullTop + 3f, 12f, 12f, 4f, 4f));
        g.fill(new RoundRectangle2D.Float(width - 19f, hullTop - 2f, 8f, 7f, 3f, 3f));
        g.fill(new RoundRectangle2D.Float(width - 27f, hullTop + 10f, 7f, 7f, 3f, 3f));

        g.setColor(new Color(134, 193, 219));
        g.fill(new RoundRectangle2D.Float(width - 20f, hullTop + 6f, 3f, 2f, 1f, 1f));
        g.fill(new RoundRectangle2D.Float(width - 15f, hullTop + 6f, 3f, 2f, 1f, 1f));
        g.fill(new RoundRectangle2D.Float(width - 17f, hullTop + 1f, 4f, 2f, 1f, 1f));

        g.setColor(new Color(73, 81, 87));
        g.fillRect(width - 16, hullTop - 8, 2, 6);
        g.fillRect(width - 12, hullTop - 6, 2, 4);

        g.setColor(new Color(164, 213, 235));
        g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawArc(8, hullBottom - 1, 20, 8, 0, 180);
        g.drawArc(width - 30, hullBottom - 1, 20, 8, 0, 180);
    }

    private void paintMineLayer(Graphics2D g) {
        int width = CELL_SIZE;
        int height = CELL_SIZE;
        int hullTop = 8;
        int hullBottom = height - 7;
        int hullMidY = (hullTop + hullBottom) / 2;
        float phase = (System.nanoTime() - animationEpochNanos) / 1_000_000_000f * (float) Math.PI * 2f;
        float wakeShift = (float) Math.sin(phase) * 1.6f;

        g.setColor(MINELAYER_WAKE);
        g.fillArc(1, height - 10 + Math.round(wakeShift), 12, 8, 0, 180);
        g.fillArc(width - 13, height - 10 - Math.round(wakeShift), 12, 8, 0, 180);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.28f));
        g.setColor(new Color(199, 241, 255));
        g.fill(new Ellipse2D.Float(-4f, height - 8f, 16f, 6f));
        g.fill(new Ellipse2D.Float(width - 12f, height - 8f, 16f, 6f));
        g.fill(new Ellipse2D.Float(width / 2f - 10f, height - 6f + wakeShift, 20f, 5f));
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));

        Path2D hull = new Path2D.Float();
        hull.moveTo(5, hullMidY);
        hull.lineTo(10, hullTop + 1);
        hull.lineTo(width - 9, hullTop + 2);
        hull.quadTo(width - 4, hullMidY, width - 9, hullBottom - 2);
        hull.lineTo(10, hullBottom - 1);
        hull.closePath();
        g.setColor(MINELAYER_HULL);
        g.fill(hull);

        g.setColor(new Color(111, 120, 124));
        g.fill(new RoundRectangle2D.Float(8f, hullTop + 2f, width - 16f, hullBottom - hullTop - 4f, 5f, 5f));

        g.setColor(MINELAYER_DECK);
        g.fill(new RoundRectangle2D.Float(10f, hullTop + 4f, width - 19f, hullBottom - hullTop - 8f, 4f, 4f));

        g.setColor(new Color(214, 216, 218));
        g.fill(new RoundRectangle2D.Float(width - 14f, hullTop + 4f, 6f, 7f, 2f, 2f));
        g.fill(new RoundRectangle2D.Float(width - 12f, hullTop + 1f, 4f, 4f, 2f, 2f));

        g.setColor(new Color(137, 191, 218));
        g.fill(new RoundRectangle2D.Float(width - 13f, hullTop + 6f, 4f, 2f, 1f, 1f));

        g.setColor(new Color(72, 80, 86));
        g.fillRect(width - 10, hullTop - 4, 1, 6);

        g.setColor(new Color(70, 72, 74));
        g.fillRect(9, hullMidY - 5, width - 20, 2);
        g.fillRect(9, hullMidY + 3, width - 20, 2);

        g.setColor(new Color(48, 53, 57));
        for (int x = 9; x < width - 13; x += 6) {
            g.fill(new Ellipse2D.Float(x, hullMidY - 5.5f, 3.5f, 3.5f));
            g.fill(new Ellipse2D.Float(x, hullMidY + 2.5f, 3.5f, 3.5f));
        }

        g.setColor(new Color(166, 68, 50));
        g.fill(new RoundRectangle2D.Float(6f, hullMidY - 3f, 4f, 7f, 2f, 2f));
    }

    private static final class Point2D {
        private final float x;
        private final float y;

        private Point2D(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }

    private final class CellButton extends JButton {
        private final int row;
        private final int col;

        private CellButton(int row, int col) {
            this.row = row;
            this.col = col;
            setContentAreaFilled(false);
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                enableQuality(g);
                g.setColor(getBackground());
                g.fillRect(0, 0, getWidth(), getHeight());
                paintCellWater(g, getWidth(), getHeight());
            } finally {
                g.dispose();
            }
            super.paintComponent(graphics);
        }

        private void paintCellWater(Graphics2D g, int width, int height) {
            if (board == null) {
                return;
            }

            Cell cell = board.getCell(row, col);
            if (cell == null || board.isBoatOccupying(row, col)) {
                return;
            }
            if (cell.isMine() && cell.isRevealed()) {
                return;
            }

            float phase = (System.nanoTime() - animationEpochNanos) / 1_000_000_000f * (float) Math.PI * 2f;
            float offset = row * 0.55f + col * 0.35f;
            float waveOne = (float) Math.sin(phase * 1.35f + offset);
            float waveTwo = (float) Math.cos(phase * 0.9f + offset * 1.4f);

            Color crest = cell.isRevealed() ? new Color(243, 251, 255, 76) : new Color(220, 245, 255, 58);
            Color trough = cell.isRevealed() ? new Color(88, 149, 182, 44) : new Color(47, 101, 137, 52);

            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.85f));
            g.setColor(crest);
            g.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(new QuadCurve2D.Float(
                2f,
                height * 0.34f + waveOne * 2.2f,
                width * 0.5f,
                height * 0.18f + waveTwo * 2.4f,
                width - 2f,
                height * 0.34f - waveOne * 1.8f));
            g.draw(new QuadCurve2D.Float(
                2f,
                height * 0.66f + waveTwo * 2.1f,
                width * 0.52f,
                height * 0.5f + waveOne * 2.3f,
                width - 2f,
                height * 0.66f - waveTwo * 1.7f));

            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.45f));
            g.setColor(trough);
            g.draw(new QuadCurve2D.Float(
                4f,
                height * 0.5f - waveOne * 1.6f,
                width * 0.45f,
                height * 0.64f - waveTwo * 1.8f,
                width - 4f,
                height * 0.5f + waveOne * 1.2f));
        }
    }

    private static final class MissileAnimation {
        private final float startX;
        private final float startY;
        private final float targetX;
        private final float targetY;
        private final int targetRow;
        private final int targetCol;
        private float flightProgress;
        private float explosionProgress;
        private boolean impactApplied;

        private MissileAnimation(float startX, float startY, float targetX, float targetY, int targetRow, int targetCol) {
            this.startX = startX;
            this.startY = startY;
            this.targetX = targetX;
            this.targetY = targetY;
            this.targetRow = targetRow;
            this.targetCol = targetCol;
        }
    }

    private static final class Tone {
        private final float frequencyHz;
        private final int durationMs;
        private final float volume;
        private final boolean descending;

        private Tone(float frequencyHz, int durationMs, float volume, boolean descending) {
            this.frequencyHz = frequencyHz;
            this.durationMs = durationMs;
            this.volume = volume;
            this.descending = descending;
        }
    }

    private enum SoundEffect {
        REVEAL(new Tone[]{new Tone(660f, 45, 0.22f, false)}),
        FLAG(new Tone[]{new Tone(430f, 50, 0.2f, false), new Tone(515f, 35, 0.15f, false)}),
        CHORD(new Tone[]{new Tone(560f, 40, 0.16f, false), new Tone(720f, 45, 0.16f, false)}),
        BOAT_MOVE(new Tone[]{new Tone(180f, 95, 0.22f, true)}),
        ARM_ON(new Tone[]{new Tone(720f, 35, 0.12f, false), new Tone(880f, 40, 0.12f, false)}),
        ARM_OFF(new Tone[]{new Tone(840f, 35, 0.1f, true)}),
        MISSILE_LAUNCH(new Tone[]{new Tone(360f, 55, 0.18f, false), new Tone(540f, 75, 0.2f, false)}),
        MISSILE_IMPACT(new Tone[]{new Tone(170f, 70, 0.25f, true), new Tone(95f, 90, 0.22f, true)}),
        HARBOR_APPROACH(new Tone[]{new Tone(220f, 110, 0.18f, false), new Tone(330f, 110, 0.16f, false)}),
        HARBOR_COMPLETE(new Tone[]{new Tone(520f, 60, 0.15f, false), new Tone(660f, 90, 0.18f, false)}),
        DOCK_APPROACH(new Tone[]{new Tone(200f, 120, 0.18f, true), new Tone(250f, 90, 0.14f, false)}),
        HOMECOMING(new Tone[]{new Tone(392f, 75, 0.18f, false), new Tone(494f, 75, 0.18f, false), new Tone(588f, 120, 0.2f, false)}),
        MINE_HIT(new Tone[]{new Tone(180f, 60, 0.24f, true), new Tone(120f, 120, 0.2f, true)});

        private final Tone[] tones;

        SoundEffect(Tone[] tones) {
            this.tones = tones;
        }
    }

    private static final class HarborSequence {
        private float dockingProgress;
        private float mooringProgress;
        private float refuelProgress;
    }

    private static final class WestDockSequence {
        private float dockingProgress;
        private float mooringProgress;
        private float celebrationProgress;
    }

    private final class BoatOverlay extends JComponent {
        private float currentX;
        private float currentY;
        private float targetX;
        private float targetY;
        private boolean hasPosition;
        private long launchWakeUntilNanos;

        private BoatOverlay() {
            setOpaque(false);
        }

        private boolean hasPosition() {
            return hasPosition;
        }

        private void snapTo(float x, float y) {
            currentX = x;
            currentY = y;
            targetX = x;
            targetY = y;
            hasPosition = true;
            repaint();
        }

        private void setTarget(float x, float y) {
            targetX = x;
            targetY = y;
        }

        private void triggerLaunchWake() {
            launchWakeUntilNanos = System.nanoTime() + 900_000_000L;
            repaint();
        }

        private Point2D getCurrentVisualPosition() {
            return new Point2D(currentX, currentY);
        }

        private boolean isAtTarget() {
            return Math.abs(currentX - targetX) < 0.5f && Math.abs(currentY - targetY) < 0.5f;
        }

        private boolean stepTowardsTarget() {
            if (!hasPosition) {
                return false;
            }

            currentX += (targetX - currentX) * BOAT_ANIMATION_STEP;
            currentY += (targetY - currentY) * BOAT_ANIMATION_STEP;
            if (isAtTarget()) {
                currentX = targetX;
                currentY = targetY;
                repaint();
                return false;
            }
            repaint();
            return true;
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (!hasPosition) {
                return;
            }

            Graphics2D g = (Graphics2D) graphics.create();
            try {
                enableQuality(g);
                float dockOffset = 0f;
                float verticalBob = 0f;
                float idlePhase = getAnimationPhase(0.9f);
                if (harborSequence != null) {
                    dockOffset = easeOut(harborSequence.dockingProgress) * 11f;
                    verticalBob = (1f - harborSequence.dockingProgress) * 2.4f;
                } else if (westDockSequence != null) {
                    dockOffset = -easeOut(westDockSequence.dockingProgress) * 11f;
                    verticalBob = (1f - westDockSequence.dockingProgress) * 1.8f;
                } else {
                    verticalBob = (float) Math.sin(idlePhase) * 1.2f;
                }
                g.translate(currentX + dockOffset, currentY + verticalBob);
                paintArrivalWater(g);
                paintBoat(g, CELL_SIZE * 2, CELL_SIZE);
                if (harborSequence != null) {
                    paintHarborRefuelEffect(g, harborSequence);
                } else if (westDockSequence != null) {
                    paintWestDockArrivalEffect(g, westDockSequence);
                }
            } finally {
                g.dispose();
            }
        }

        private void paintHarborRefuelEffect(Graphics2D g, HarborSequence sequence) {
            float clampDocking = clamp(sequence.dockingProgress);
            float clampMooring = clamp(sequence.mooringProgress);
            float clampRefuel = clamp(sequence.refuelProgress);

            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f + 0.35f * clampDocking));
            g.setColor(new Color(235, 240, 232));
            g.fill(new RoundRectangle2D.Float(CELL_SIZE * 2 - 4f, 2f, 10f, CELL_SIZE - 4f, 6f, 6f));

            g.setColor(new Color(213, 197, 165));
            g.fill(new RoundRectangle2D.Float(CELL_SIZE * 2 + 3f, 0f, 12f, CELL_SIZE, 6f, 6f));
            g.setColor(new Color(150, 125, 92));
            g.fill(new RoundRectangle2D.Float(CELL_SIZE * 2 + 7f, 4f, 4f, CELL_SIZE - 8f, 3f, 3f));

            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f + 0.45f * clampMooring));
            g.setColor(new Color(59, 66, 72));
            g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(CELL_SIZE * 2 - 14, 7, CELL_SIZE * 2 - 28, 6);
            g.drawLine(CELL_SIZE * 2 - 12, CELL_SIZE - 8, CELL_SIZE * 2 - 28, CELL_SIZE - 6);

            if (clampMooring > 0f) {
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.45f + 0.45f * clampMooring));
                g.setColor(new Color(79, 87, 94));
                g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.drawLine(CELL_SIZE * 2 - 18, 8, CELL_SIZE * 2 - 8, 11);

                float hoseEndX = CELL_SIZE * 2 - 16f + 8f * clampMooring;
                float hoseEndY = 8f + 2f * clampMooring;
                g.setColor(new Color(41, 46, 52));
                g.draw(new java.awt.geom.QuadCurve2D.Float(
                    CELL_SIZE * 2 + 8f,
                    10f,
                    CELL_SIZE * 2 + 1f,
                    4f,
                    hoseEndX,
                    hoseEndY));
            }

            if (clampRefuel > 0f) {
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f));
                g.setColor(new Color(191, 138, 47, 225));
                float fillWidth = Math.max(0f, (CELL_SIZE * 2 - 24f) * clampRefuel);
                g.fill(new RoundRectangle2D.Float(12f, CELL_SIZE - 14f, fillWidth, 6f, 5f, 5f));

                g.setColor(new Color(255, 220, 128, 170));
                float glowRadius = 10f + 10f * clampRefuel;
                g.fill(new Ellipse2D.Float(CELL_SIZE * 2 - glowRadius / 2f - 12f, 3f, glowRadius, glowRadius));

                g.setColor(new Color(245, 201, 90, 190));
                g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.drawArc(CELL_SIZE * 2 - 28, -2, 18, 10, 25, 130);
                g.drawArc(CELL_SIZE * 2 - 18, -4, 16, 9, 20, 120);
            }
        }

        private void paintWestDockArrivalEffect(Graphics2D g, WestDockSequence sequence) {
            float clampDocking = clamp(sequence.dockingProgress);
            float clampMooring = clamp(sequence.mooringProgress);
            float clampCelebration = clamp(sequence.celebrationProgress);

            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f + 0.35f * clampDocking));
            g.setColor(new Color(190, 158, 118));
            g.fill(new RoundRectangle2D.Float(-14f, 1f, 12f, CELL_SIZE - 2f, 6f, 6f));
            g.setColor(new Color(121, 88, 56));
            g.fill(new RoundRectangle2D.Float(-10f, 4f, 4f, CELL_SIZE - 8f, 3f, 3f));

            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f + 0.5f * clampMooring));
            g.setColor(new Color(73, 60, 47));
            g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(6, 7, -6, 5);
            g.drawLine(9, CELL_SIZE - 8, -6, CELL_SIZE - 6);

            if (clampCelebration > 0f) {
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f + 0.45f * clampCelebration));
                g.setColor(new Color(255, 230, 157));
                g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.drawArc(4, -2, 20, 12, 20, 150);
                g.drawArc(15, -5, 18, 10, 8, 145);

                g.setColor(new Color(255, 238, 178, 175));
                g.fill(new Ellipse2D.Float(4f, 3f, 12f, 12f));

                g.setColor(new Color(226, 194, 99, 190));
                float pennantHeight = 10f + 6f * clampCelebration;
                Path2D pennant = new Path2D.Float();
                pennant.moveTo(4, 3);
                pennant.lineTo(4, 3 + pennantHeight);
                pennant.lineTo(12, 6 + pennantHeight * 0.55f);
                pennant.closePath();
                g.fill(pennant);

                g.setColor(new Color(158, 63, 42, 205));
                Path2D pennantTwo = new Path2D.Float();
                pennantTwo.moveTo(11, 5);
                pennantTwo.lineTo(11, 5 + pennantHeight * 0.8f);
                pennantTwo.lineTo(18, 8 + pennantHeight * 0.45f);
                pennantTwo.closePath();
                g.fill(pennantTwo);
            }
        }

        private void paintArrivalWater(Graphics2D g) {
            float wavePhase = getAnimationPhase(1.8f);
            int leftWakeOffset = Math.round((float) Math.sin(wavePhase) * 2f);
            int rightWakeOffset = Math.round((float) Math.cos(wavePhase * 0.9f) * 2f);
            g.setColor(new Color(255, 255, 255, 48));
            g.fill(new RoundRectangle2D.Double(1, 1, CELL_SIZE * 2 - 2, CELL_SIZE - 2, 16, 16));
            g.setColor(new Color(163, 218, 242, 95));
            g.fillArc(2, CELL_SIZE - 10 + leftWakeOffset, 16, 8, 0, 180);
            g.fillArc(CELL_SIZE + 6, CELL_SIZE - 10 + rightWakeOffset, 16, 8, 0, 180);
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.22f));
            g.fill(new Ellipse2D.Float(6f, CELL_SIZE - 9f, CELL_SIZE * 2 - 12f, 6f + (float) Math.sin(wavePhase) * 1.5f));

            float wakeProgress = clamp((launchWakeUntilNanos - System.nanoTime()) / 900_000_000f);
            if (wakeProgress > 0f) {
                float wakeWidth = 20f + (1f - wakeProgress) * 26f;
                float wakeHeight = 7f + (1f - wakeProgress) * 7f;
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.18f + wakeProgress * 0.34f));
                g.setColor(new Color(218, 246, 255));
                g.fill(new Ellipse2D.Float(-8f - (1f - wakeProgress) * 8f, CELL_SIZE - 10f, wakeWidth, wakeHeight));
                g.fill(new Ellipse2D.Float(CELL_SIZE * 2 - 8f, CELL_SIZE - 10f, wakeWidth, wakeHeight));

                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.12f + wakeProgress * 0.2f));
                g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.drawArc(-10, CELL_SIZE - 14, 28 + Math.round((1f - wakeProgress) * 16f), 12, 5, 150);
                g.drawArc(CELL_SIZE * 2 - 18, CELL_SIZE - 14, 28 + Math.round((1f - wakeProgress) * 16f), 12, 25, 150);
            }
        }

        private float clamp(float value) {
            return Math.max(0f, Math.min(1f, value));
        }

        private float easeOut(float value) {
            float clamped = clamp(value);
            float inverse = 1f - clamped;
            return 1f - inverse * inverse;
        }

        private float getAnimationPhase(float speed) {
            return (System.nanoTime() - animationEpochNanos) / 1_000_000_000f * speed * (float) Math.PI * 2f;
        }
    }

    private final class MissileOverlay extends JComponent {
        private MissileOverlay() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (missileAnimation == null) {
                return;
            }

            Graphics2D g = (Graphics2D) graphics.create();
            try {
                enableQuality(g);

                if (!missileAnimation.impactApplied) {
                    float currentX = missileAnimation.startX
                        + (missileAnimation.targetX - missileAnimation.startX) * missileAnimation.flightProgress;
                    float currentY = missileAnimation.startY
                        + (missileAnimation.targetY - missileAnimation.startY) * missileAnimation.flightProgress;

                    for (int i = 1; i <= 4; i++) {
                        float trailProgress = Math.max(0f, missileAnimation.flightProgress - i * 0.06f);
                        float trailX = missileAnimation.startX
                            + (missileAnimation.targetX - missileAnimation.startX) * trailProgress;
                        float trailY = missileAnimation.startY
                            + (missileAnimation.targetY - missileAnimation.startY) * trailProgress;
                        float size = 8f - i;
                        float alpha = Math.max(0.08f, 0.32f - i * 0.05f);
                        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                        g.setColor(new Color(255, 228, 163));
                        g.fill(new Ellipse2D.Float(trailX - size / 2f, trailY - size / 2f, size, size));
                    }

                    g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g.setColor(new Color(255, 214, 95, 190));
                    g.drawLine(Math.round(missileAnimation.startX), Math.round(missileAnimation.startY), Math.round(currentX), Math.round(currentY));

                    g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.55f));
                    g.setColor(new Color(255, 241, 188));
                    g.draw(new QuadCurve2D.Float(
                        missileAnimation.startX,
                        missileAnimation.startY,
                        (missileAnimation.startX + currentX) / 2f,
                        Math.min(missileAnimation.startY, currentY) - 12f,
                        currentX,
                        currentY));

                    g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
                    g.setColor(new Color(244, 103, 52));
                    g.fill(new Ellipse2D.Float(currentX - 5f, currentY - 5f, 10f, 10f));
                } else {
                    float radius = CELL_SIZE * 1.6f * missileAnimation.explosionProgress;
                    float alpha = Math.max(0.15f, 1f - missileAnimation.explosionProgress);

                    g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                    g.setColor(new Color(255, 214, 95));
                    g.fill(new Ellipse2D.Float(missileAnimation.targetX - radius / 2f, missileAnimation.targetY - radius / 2f, radius, radius));

                    g.setColor(new Color(255, 240, 182));
                    g.fill(new Ellipse2D.Float(missileAnimation.targetX - radius * 0.28f, missileAnimation.targetY - radius * 0.28f, radius * 0.56f, radius * 0.56f));

                    g.setColor(new Color(229, 86, 47));
                    g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g.draw(new Ellipse2D.Float(missileAnimation.targetX - radius / 2f, missileAnimation.targetY - radius / 2f, radius, radius));

                    g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0.08f, alpha * 0.9f)));
                    g.setColor(new Color(255, 154, 62));
                    g.draw(new Ellipse2D.Float(missileAnimation.targetX - radius * 0.72f, missileAnimation.targetY - radius * 0.72f, radius * 1.44f, radius * 1.44f));
                }
            } finally {
                g.dispose();
            }
        }
    }

    private final class MineLayerOverlay extends JComponent {
        private MineLayerOverlay() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);

            Graphics2D g = (Graphics2D) graphics.create();
            try {
                enableQuality(g);
                float phase = (System.nanoTime() - animationEpochNanos) / 1_000_000_000f * (float) Math.PI * 2f;
                for (Board.MineLayerPosition position : board.getMineLayerPositions()) {
                    Graphics2D layerGraphics = (Graphics2D) g.create();
                    try {
                        float bob = (float) Math.sin(phase + position.getRow() * 0.7f + position.getCol() * 0.4f) * 1.4f;
                        layerGraphics.translate(position.getCol() * CELL_SIZE, position.getRow() * CELL_SIZE + bob);
                        paintMineLayer(layerGraphics);
                    } finally {
                        layerGraphics.dispose();
                    }
                }
            } finally {
                g.dispose();
            }
        }
    }
}
