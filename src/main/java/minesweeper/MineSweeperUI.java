package minesweeper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
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
    private static final Color BOAT_CELL_BG = new Color(73, 140, 181);
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
    private static final float HARBOR_DOCKING_STEP = 0.16f;
    private static final float HARBOR_REFUEL_STEP = 0.08f;
    private static final int MISSILE_ANIMATION_DELAY_MS = 16;
    private static final float MISSILE_FLIGHT_STEP = 0.12f;
    private static final float MISSILE_EXPLOSION_STEP = 0.14f;
    private static final int MINELAYER_STEP_DELAY_MS = 1350;
    private static final int DEFAULT_MINELAYER_COUNT = 2;
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
    private JPanel gridPanel;
    private JLayeredPane boardLayer;
    private BoatOverlay boatOverlay;
    private MissileOverlay missileOverlay;
    private MineLayerOverlay mineLayerOverlay;
    private Timer swingTimer;
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
    private boolean winDialogShown;
    private Board.GameState lastGameState = Board.GameState.WAITING;
    private Board.VoyageStage lastVoyageStage = Board.VoyageStage.OUTBOUND;
    private HarborSequence harborSequence;
    private WestDockSequence westDockSequence;
    private MissileAnimation missileAnimation;

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
        startGame(10, 24, 44, mineLayerCount, LEVEL_TANKER);
        SwingUtilities.invokeLater(this::showOpeningInstructions);
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

        JMenuItem beginnerItem = new JMenuItem(LEVEL_ESCORT + " (7×14, 10 mines, 0 ships)");
        beginnerItem.addActionListener(e -> startGame(7, 14, 10, 0, LEVEL_ESCORT));

        JMenuItem intermediateItem = new JMenuItem(LEVEL_TANKER + " (10×24, 44 mines)");
        intermediateItem.addActionListener(e -> startGame(10, 24, 44, mineLayerCount, LEVEL_TANKER));

        JMenuItem expertItem = new JMenuItem(LEVEL_BREAKER + " (12×30, 76 mines)");
        expertItem.addActionListener(e -> startGame(12, 30, 76, mineLayerCount, LEVEL_BREAKER));

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
        gameMenu.addSeparator();
        gameMenu.add(bestTimesItem);
        gameMenu.add(mineLayerMenu);
        gameMenu.addSeparator();
        gameMenu.add(exitItem);

        menuBar.add(gameMenu);
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

    private void showOpeningInstructions() {
        if (instructionsShown) {
            return;
        }
        instructionsShown = true;

        String message =
            "Objective:\n"
                + "Escort the ship to the right side, refuel there, then bring it back to the left side.\n\n"
                + "Ship movement:\n"
                + "- Launch from a revealed safe cell in the first column.\n"
                + "- Click any straight revealed channel to move the ship.\n"
                + "- You can also use the arrow keys to sail.\n\n"
                + "Voyage:\n"
                + "- First leg: reach the east harbor.\n"
                + "- Refuel happens automatically there.\n"
                + "- Second leg: sail the ship back to the west dock to win.\n\n"
                + "Minesweeper controls:\n"
                + "- Left click reveals.\n"
                + "- Right click marks mines.\n"
                + "- Double-click or chord a revealed number to open surrounding unmarked cells when the marked mines match.";

        JOptionPane.showMessageDialog(this, message, "How To Play", JOptionPane.INFORMATION_MESSAGE);
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
        JButton btn = new JButton();
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
                } else if (SwingUtilities.isLeftMouseButton(e) && missileModeArmed) {
                    rememberUndoState();
                    startMissileStrike(row, col);
                } else if (SwingUtilities.isLeftMouseButton(e)
                    && e.getClickCount() >= 2
                    && cell.isRevealed()
                    && cell.getAdjacentMines() > 0) {
                    rememberUndoState();
                    board.chord(row, col);
                } else if (SwingUtilities.isLeftMouseButton(e)) {
                    rememberUndoState();
                    if (!board.moveBoatTo(row, col)) {
                        board.reveal(row, col);
                    }
                } else if (SwingUtilities.isRightMouseButton(e)) {
                    rememberUndoState();
                    board.toggleFlag(row, col);
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
                if (board != null && board.moveBoatBy(rowDelta, colDelta)) {
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
            } else {
                westDockSequence.celebrationProgress = Math.min(1f, westDockSequence.celebrationProgress + HARBOR_REFUEL_STEP);
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
        ensureHarborSequenceTimer();
        harborSequenceTimer.start();
        if (boatOverlay != null) {
            boatOverlay.repaint();
        }
    }

    private void startWestDockSequence() {
        westDockSequence = new WestDockSequence();
        setMissileModeArmed(false);
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
        ensureMissileAnimationTimer();
        missileAnimationTimer.start();
        updateMissileOverlay();
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
        int waterline = Math.max(14, height - 20);
        g.setColor(new Color(78, 141, 181, 120));
        g.fillRoundRect(3, waterline + 6, width - 6, Math.max(7, height - waterline - 8), 12, 12);

        Path2D hull = new Path2D.Double();
        hull.moveTo(6, waterline + 2);
        hull.lineTo(width - 16, waterline + 2);
        hull.lineTo(width - 8, waterline + 8);
        hull.lineTo(width - 4, height - 9);
        hull.lineTo(14, height - 9);
        hull.lineTo(6, waterline + 2);
        g.setColor(new Color(34, 49, 62));
        g.fill(hull);

        g.setColor(new Color(116, 132, 143));
        g.fillRoundRect(10, waterline + 1, width - 22, 4, 3, 3);

        g.setColor(new Color(199, 180, 136));
        g.fillRoundRect(12, waterline - 6, width - 30, 7, 4, 4);
        g.setColor(new Color(89, 82, 70));
        g.fillRect(16, waterline - 3, width - 38, 1);
        g.fillRect(16, waterline - 1, width - 38, 1);

        g.setColor(new Color(228, 234, 239));
        g.fillRoundRect(width - 24, waterline - 21, 15, 13, 5, 5);
        g.fillRoundRect(width - 20, waterline - 29, 9, 9, 4, 4);
        g.fillRoundRect(width - 28, waterline - 15, 8, 7, 3, 3);

        List<Shape> windows = new ArrayList<>();
        windows.add(new RoundRectangle2D.Double(width - 21, waterline - 17, 4, 3, 2, 2));
        windows.add(new RoundRectangle2D.Double(width - 15, waterline - 17, 4, 3, 2, 2));
        windows.add(new RoundRectangle2D.Double(width - 18, waterline - 25, 4, 3, 2, 2));
        g.setColor(new Color(127, 187, 216));
        for (Shape window : windows) {
            g.fill(window);
        }

        g.setColor(new Color(78, 87, 92));
        g.fillRect(width - 17, waterline - 36, 3, 8);
        g.fillRect(width - 11, waterline - 34, 3, 6);

        g.setColor(new Color(165, 213, 235));
        g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawArc(8, height - 13, 24, 10, 0, 180);
        g.drawArc(width - 34, height - 13, 24, 10, 0, 180);
    }

    private void paintMineLayer(Graphics2D g) {
        int width = CELL_SIZE;
        int height = CELL_SIZE;
        int waterline = height - 11;

        g.setColor(MINELAYER_WAKE);
        g.fillArc(2, height - 10, 10, 8, 0, 180);
        g.fillArc(width - 12, height - 10, 10, 8, 0, 180);

        Path2D hull = new Path2D.Double();
        hull.moveTo(6, waterline - 1);
        hull.lineTo(width - 7, waterline - 1);
        hull.lineTo(width - 11, height - 6);
        hull.lineTo(10, height - 6);
        hull.closePath();
        g.setColor(MINELAYER_HULL);
        g.fill(hull);

        g.setColor(MINELAYER_DECK);
        g.fillRoundRect(width / 2 - 7, waterline - 12, 12, 7, 4, 4);
        g.fillRoundRect(width / 2 - 3, waterline - 17, 6, 6, 3, 3);

        g.setColor(new Color(52, 63, 74));
        g.fillRect(width / 2 + 1, waterline - 18, 2, 11);
        Path2D flag = new Path2D.Double();
        flag.moveTo(width / 2 + 3, waterline - 17);
        flag.lineTo(width / 2 + 9, waterline - 14);
        flag.lineTo(width / 2 + 3, waterline - 11);
        flag.closePath();
        g.fill(flag);
    }

    private static final class Point2D {
        private final float x;
        private final float y;

        private Point2D(float x, float y) {
            this.x = x;
            this.y = y;
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

    private static final class HarborSequence {
        private float dockingProgress;
        private float refuelProgress;
    }

    private static final class WestDockSequence {
        private float dockingProgress;
        private float celebrationProgress;
    }

    private final class BoatOverlay extends JComponent {
        private float currentX;
        private float currentY;
        private float targetX;
        private float targetY;
        private boolean hasPosition;

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
                if (harborSequence != null) {
                    dockOffset = harborSequence.dockingProgress * 6f;
                } else if (westDockSequence != null) {
                    dockOffset = -westDockSequence.dockingProgress * 6f;
                }
                g.translate(currentX + dockOffset, currentY);
                g.setColor(new Color(255, 255, 255, 56));
                g.fill(new RoundRectangle2D.Double(1, 1, CELL_SIZE * 2 - 2, CELL_SIZE - 2, 16, 16));
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
            float clampDocking = Math.max(0f, Math.min(1f, sequence.dockingProgress));
            float clampRefuel = Math.max(0f, Math.min(1f, sequence.refuelProgress));

            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.45f + 0.35f * clampDocking));
            g.setColor(new Color(231, 241, 245));
            g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(CELL_SIZE * 2 - 10, 7, CELL_SIZE * 2 + 10, 2);
            g.drawLine(CELL_SIZE * 2 - 10, CELL_SIZE - 8, CELL_SIZE * 2 + 10, CELL_SIZE - 3);

            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f));
            g.setColor(new Color(61, 70, 76));
            g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(CELL_SIZE * 2 - 17, 6, CELL_SIZE * 2 - 9, 10);

            if (clampDocking >= 1f) {
                g.setColor(new Color(34, 42, 46));
                g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.drawLine(CELL_SIZE * 2 - 14, 6, CELL_SIZE * 2 - 28, 8);

                g.setColor(new Color(191, 138, 47, 210));
                float fillWidth = Math.max(0f, (CELL_SIZE * 2 - 28f) * clampRefuel);
                g.fill(new RoundRectangle2D.Float(14, CELL_SIZE - 13, fillWidth, 5f, 4f, 4f));

                g.setColor(new Color(255, 219, 123, 190));
                g.fill(new Ellipse2D.Float(CELL_SIZE * 2 - 19, 4f, 10f, 10f));
            }
        }

        private void paintWestDockArrivalEffect(Graphics2D g, WestDockSequence sequence) {
            float clampDocking = Math.max(0f, Math.min(1f, sequence.dockingProgress));
            float clampCelebration = Math.max(0f, Math.min(1f, sequence.celebrationProgress));

            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.45f + 0.25f * clampDocking));
            g.setColor(new Color(222, 213, 194));
            g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(2, 6, -10, 2);
            g.drawLine(2, CELL_SIZE - 7, -10, CELL_SIZE - 3);

            if (clampDocking >= 1f) {
                g.setColor(new Color(208, 180, 103, 210));
                g.fill(new Ellipse2D.Float(5f, 4f, 10f, 10f));

                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f + 0.45f * clampCelebration));
                g.setColor(new Color(255, 230, 157));
                g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.drawArc(6, -2, 20, 12, 15, 150);
                g.drawArc(18, -4, 18, 10, 5, 150);
            }
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

                    g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g.setColor(new Color(255, 214, 95, 190));
                    g.drawLine(Math.round(missileAnimation.startX), Math.round(missileAnimation.startY), Math.round(currentX), Math.round(currentY));

                    g.setColor(new Color(244, 103, 52));
                    g.fill(new Ellipse2D.Float(currentX - 5f, currentY - 5f, 10f, 10f));
                } else {
                    float radius = CELL_SIZE * 1.6f * missileAnimation.explosionProgress;
                    float alpha = Math.max(0.15f, 1f - missileAnimation.explosionProgress);

                    g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                    g.setColor(new Color(255, 214, 95));
                    g.fill(new Ellipse2D.Float(missileAnimation.targetX - radius / 2f, missileAnimation.targetY - radius / 2f, radius, radius));

                    g.setColor(new Color(229, 86, 47));
                    g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g.draw(new Ellipse2D.Float(missileAnimation.targetX - radius / 2f, missileAnimation.targetY - radius / 2f, radius, radius));
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
                for (Board.MineLayerPosition position : board.getMineLayerPositions()) {
                    Graphics2D layerGraphics = (Graphics2D) g.create();
                    try {
                        layerGraphics.translate(position.getCol() * CELL_SIZE, position.getRow() * CELL_SIZE);
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
