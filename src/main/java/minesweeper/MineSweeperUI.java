package minesweeper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Swing-based GUI for the Minesweeper game.
 * Supports Beginner (9×9, 10 mines), Intermediate (16×16, 40 mines),
 * and Expert (16×30, 99 mines) difficulty levels.
 */
public class MineSweeperUI extends JFrame {

    // Color constants for number labels
    private static final Color[] NUMBER_COLORS = {
        null,
        new Color(0, 0, 255),    // 1 – blue
        new Color(0, 128, 0),    // 2 – green
        new Color(255, 0, 0),    // 3 – red
        new Color(0, 0, 128),    // 4 – dark blue
        new Color(128, 0, 0),    // 5 – dark red
        new Color(0, 128, 128),  // 6 – teal
        new Color(0, 0, 0),      // 7 – black
        new Color(128, 128, 128) // 8 – grey
    };

    private static final int CELL_SIZE = 32;

    private Board board;
    private JButton[][] buttons;
    private JLabel mineCountLabel;
    private JLabel timerLabel;
    private JButton resetButton;
    private Timer swingTimer;
    private int elapsedSeconds;
    private int chordRow = -1;
    private int chordCol = -1;

    // Current difficulty settings
    private int rows;
    private int cols;
    private int mines;

    public MineSweeperUI() {
        setTitle("MineSweeper");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        buildMenuBar();
        startGame(9, 9, 10);  // Default: Beginner
    }

    // -------------------------------------------------------------------------
    // Menu
    // -------------------------------------------------------------------------

    private void buildMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu gameMenu = new JMenu("Game");

        JMenuItem beginnerItem = new JMenuItem("Beginner (9×9, 10 mines)");
        beginnerItem.addActionListener(e -> startGame(9, 9, 10));

        JMenuItem intermediateItem = new JMenuItem("Intermediate (16×16, 40 mines)");
        intermediateItem.addActionListener(e -> startGame(16, 16, 40));

        JMenuItem expertItem = new JMenuItem("Expert (16×30, 99 mines)");
        expertItem.addActionListener(e -> startGame(16, 30, 99));

        JMenuItem customItem = new JMenuItem("Custom…");
        customItem.addActionListener(e -> showCustomDialog());

        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> System.exit(0));

        gameMenu.add(beginnerItem);
        gameMenu.add(intermediateItem);
        gameMenu.add(expertItem);
        gameMenu.addSeparator();
        gameMenu.add(customItem);
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
        panel.add(new JLabel("Cols (5–50):"));
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
                c = Math.max(5, Math.min(50, c));
                m = Math.max(1, Math.min(r * c - 9, m));
                startGame(r, c, m);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Please enter valid numbers.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Game initialisation
    // -------------------------------------------------------------------------

    private void startGame(int r, int c, int m) {
        this.rows = r;
        this.cols = c;
        this.mines = m;

        if (swingTimer != null) {
            swingTimer.stop();
        }
        elapsedSeconds = 0;

        board = new Board(rows, cols, mines);
        getContentPane().removeAll();
        buildUI();
        pack();
        setLocationRelativeTo(null);
        revalidate();
        repaint();
    }

    private void buildUI() {
        setLayout(new BorderLayout(0, 4));

        // ── Top toolbar ──────────────────────────────────────────────────────
        JPanel toolbar = new JPanel(new BorderLayout(10, 0));
        toolbar.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));

        mineCountLabel = new JLabel(formatMineCount(board.getRemainingMines()));
        mineCountLabel.setFont(new Font(Font.MONOSPACED, Font.BOLD, 22));
        mineCountLabel.setForeground(Color.RED);
        mineCountLabel.setBackground(Color.BLACK);
        mineCountLabel.setOpaque(true);
        mineCountLabel.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));

        resetButton = new JButton("\uD83D\uDE0A");  // 😊
        resetButton.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 20));
        resetButton.setFocusPainted(false);
        resetButton.addActionListener(e -> startGame(rows, cols, mines));

        timerLabel = new JLabel("000");
        timerLabel.setFont(new Font(Font.MONOSPACED, Font.BOLD, 22));
        timerLabel.setForeground(Color.RED);
        timerLabel.setBackground(Color.BLACK);
        timerLabel.setOpaque(true);
        timerLabel.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));

        toolbar.add(mineCountLabel, BorderLayout.WEST);
        toolbar.add(resetButton, BorderLayout.CENTER);
        toolbar.add(timerLabel, BorderLayout.EAST);
        toolbar.setBackground(new Color(192, 192, 192));

        add(toolbar, BorderLayout.NORTH);

        // ── Grid panel ───────────────────────────────────────────────────────
        JPanel gridPanel = new JPanel(new GridLayout(rows, cols, 1, 1));
        gridPanel.setBackground(Color.GRAY);
        gridPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));

        buttons = new JButton[rows][cols];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                JButton btn = createCellButton(r, c);
                buttons[r][c] = btn;
                gridPanel.add(btn);
            }
        }
        add(gridPanel, BorderLayout.CENTER);

        // ── Timer ────────────────────────────────────────────────────────────
        swingTimer = new Timer(1000, e -> {
            if (board.getGameState() == Board.GameState.PLAYING) {
                elapsedSeconds = Math.min(999, elapsedSeconds + 1);
                timerLabel.setText(String.format("%03d", elapsedSeconds));
            }
        });
        swingTimer.start();
    }

    private JButton createCellButton(int row, int col) {
        JButton btn = new JButton();
        btn.setPreferredSize(new Dimension(CELL_SIZE, CELL_SIZE));
        btn.setFocusPainted(false);
        btn.setMargin(new Insets(0, 0, 0, 0));
        btn.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        styleHiddenButton(btn);

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (board.getGameState() == Board.GameState.WON
                    || board.getGameState() == Board.GameState.LOST) {
                    return;
                }
                int modifiers = e.getModifiersEx();
                if (SwingUtilities.isMiddleMouseButton(e)
                    || ((modifiers & InputEvent.BUTTON1_DOWN_MASK) != 0
                    && (modifiers & InputEvent.BUTTON3_DOWN_MASK) != 0)) {
                    chordRow = row;
                    chordCol = col;
                    return;
                }
                if (SwingUtilities.isLeftMouseButton(e)) {
                    resetButton.setText("\uD83D\uDE2E"); // 😮
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (board.getGameState() == Board.GameState.WON
                    || board.getGameState() == Board.GameState.LOST) {
                    return;
                }
                if ((row == chordRow && col == chordCol)
                    && (SwingUtilities.isMiddleMouseButton(e)
                    || SwingUtilities.isLeftMouseButton(e)
                    || SwingUtilities.isRightMouseButton(e))) {
                    board.chord(row, col);
                    chordRow = -1;
                    chordCol = -1;
                } else if (SwingUtilities.isLeftMouseButton(e)) {
                    board.reveal(row, col);
                } else if (SwingUtilities.isRightMouseButton(e)) {
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

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                renderCell(r, c);
            }
        }

        mineCountLabel.setText(formatMineCount(board.getRemainingMines()));

        switch (state) {
            case WON:
                resetButton.setText("\uD83D\uDE0E");  // 😎
                swingTimer.stop();
                break;
            case LOST:
                resetButton.setText("\uD83D\uDE35");  // 😵
                swingTimer.stop();
                break;
            default:
                resetButton.setText("\uD83D\uDE0A");  // 😊
        }
    }

    private void renderCell(int row, int col) {
        JButton btn = buttons[row][col];
        Cell cell = board.getCell(row, col);

        switch (cell.getState()) {
            case HIDDEN:
                styleHiddenButton(btn);
                btn.setText("");
                break;
            case FLAGGED:
                styleHiddenButton(btn);
                btn.setText("\uD83D\uDEA9");  // 🚩
                btn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));
                break;
            case QUESTION:
                styleHiddenButton(btn);
                btn.setText("?");
                btn.setForeground(Color.MAGENTA);
                btn.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
                break;
            case REVEALED:
                styleRevealedButton(btn, cell, row, col);
                break;
        }
    }

    private void styleHiddenButton(JButton btn) {
        btn.setBackground(new Color(192, 192, 192));
        btn.setBorder(BorderFactory.createRaisedBevelBorder());
        btn.setEnabled(true);
    }

    private void styleRevealedButton(JButton btn, Cell cell, int row, int col) {
        btn.setBorder(BorderFactory.createLineBorder(new Color(128, 128, 128), 1));
        btn.setBackground(new Color(210, 210, 210));

        if (cell.isMine()) {
            btn.setText("\uD83D\uDCA3");  // 💣
            btn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));
        } else {
            int adj = cell.getAdjacentMines();
            if (adj == 0) {
                btn.setText("");
            } else {
                btn.setText(String.valueOf(adj));
                btn.setForeground(NUMBER_COLORS[adj]);
                btn.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
            }
        }
        btn.setEnabled(false);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private String formatMineCount(int count) {
        return String.format("%03d", Math.max(-99, Math.min(999, count)));
    }
}
