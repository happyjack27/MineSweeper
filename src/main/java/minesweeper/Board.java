package minesweeper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Manages the Minesweeper game board: mine placement, reveal logic, win/loss detection.
 */
public class Board {

    public enum GameState {
        WAITING,   // Before first click
        PLAYING,
        WON,
        LOST
    }

    private final int rows;
    private final int cols;
    private final int totalMines;
    private final Cell[][] cells;
    private GameState gameState;
    private int revealedCount;
    private int flaggedCount;
    private int placedMines;  // actual mines placed (may be < totalMines on tiny boards)

    public Board(int rows, int cols, int totalMines) {
        this.rows = rows;
        this.cols = cols;
        this.totalMines = totalMines;
        this.cells = new Cell[rows][cols];
        this.gameState = GameState.WAITING;
        this.revealedCount = 0;
        this.flaggedCount = 0;
        this.placedMines = 0;
        initCells();
    }

    /**
     * Package-private constructor for testing: accepts an explicit mine grid so
     * tests can create deterministic board configurations.
     * The game starts in PLAYING state (mines are already placed).
     */
    Board(boolean[][] mineGrid) {
        this.rows = mineGrid.length;
        this.cols = mineGrid[0].length;
        this.cells = new Cell[rows][cols];
        this.gameState = GameState.PLAYING;
        this.revealedCount = 0;
        this.flaggedCount = 0;
        initCells();
        int count = 0;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (mineGrid[r][c]) {
                    cells[r][c].setMine(true);
                    count++;
                }
            }
        }
        this.placedMines = count;
        this.totalMines = count;
        calculateAdjacent();
    }

    private void initCells() {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                cells[r][c] = new Cell();
            }
        }
    }

    /**
     * Places mines randomly, avoiding the first-clicked cell and its neighbours.
     */
    private void placeMines(int safeRow, int safeCol) {
        List<Integer> positions = new ArrayList<>();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (Math.abs(r - safeRow) > 1 || Math.abs(c - safeCol) > 1) {
                    positions.add(r * cols + c);
                }
            }
        }
        Collections.shuffle(positions);
        int count = Math.min(totalMines, positions.size());
        for (int i = 0; i < count; i++) {
            int pos = positions.get(i);
            cells[pos / cols][pos % cols].setMine(true);
        }
        placedMines = count;
        calculateAdjacent();
    }

    private void calculateAdjacent() {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (!cells[r][c].isMine()) {
                    cells[r][c].setAdjacentMines(countAdjacentMines(r, c));
                }
            }
        }
    }

    private int countAdjacentMines(int row, int col) {
        int count = 0;
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                int nr = row + dr;
                int nc = col + dc;
                if (nr >= 0 && nr < rows && nc >= 0 && nc < cols && cells[nr][nc].isMine()) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Handles a left-click reveal action at (row, col).
     */
    public void reveal(int row, int col) {
        if (gameState == GameState.WON || gameState == GameState.LOST) {
            return;
        }
        Cell cell = cells[row][col];
        if (cell.isRevealed() || cell.isFlagged()) {
            return;
        }

        if (gameState == GameState.WAITING) {
            gameState = GameState.PLAYING;
            placeMines(row, col);
        }

        if (cell.isMine()) {
            cell.setState(Cell.State.REVEALED);
            gameState = GameState.LOST;
            revealAllMines(row, col);
            return;
        }

        floodReveal(row, col);
        checkWin();
    }

    /**
     * Flood-fill reveal: reveals cell and, if it has 0 adjacent mines, recursively reveals neighbours.
     */
    private void floodReveal(int row, int col) {
        if (row < 0 || row >= rows || col < 0 || col >= cols) {
            return;
        }
        Cell cell = cells[row][col];
        if (cell.isRevealed() || cell.isFlagged() || cell.isMine()) {
            return;
        }
        cell.setState(Cell.State.REVEALED);
        revealedCount++;
        if (cell.getAdjacentMines() == 0) {
            for (int dr = -1; dr <= 1; dr++) {
                for (int dc = -1; dc <= 1; dc++) {
                    if (dr != 0 || dc != 0) {
                        floodReveal(row + dr, col + dc);
                    }
                }
            }
        }
    }

    /**
     * Handles a right-click flag/unflag action at (row, col).
     */
    public void toggleFlag(int row, int col) {
        if (gameState == GameState.WON || gameState == GameState.LOST) {
            return;
        }
        Cell cell = cells[row][col];
        if (cell.isRevealed()) {
            return;
        }
        if (cell.isFlagged()) {
            cell.setState(Cell.State.QUESTION);
            flaggedCount--;
        } else if (cell.getState() == Cell.State.QUESTION) {
            cell.setState(Cell.State.HIDDEN);
        } else {
            cell.setState(Cell.State.FLAGGED);
            flaggedCount++;
        }
    }

    /**
     * Chord-click: if a revealed numbered cell has exactly the right number of flags around it,
     * reveal all non-flagged neighbours.
     */
    public void chord(int row, int col) {
        if (gameState == GameState.WON || gameState == GameState.LOST) {
            return;
        }
        Cell cell = cells[row][col];
        if (!cell.isRevealed() || cell.getAdjacentMines() == 0) {
            return;
        }
        int flags = countAdjacentFlags(row, col);
        if (flags == cell.getAdjacentMines()) {
            for (int dr = -1; dr <= 1; dr++) {
                for (int dc = -1; dc <= 1; dc++) {
                    int nr = row + dr;
                    int nc = col + dc;
                    if (nr >= 0 && nr < rows && nc >= 0 && nc < cols) {
                        Cell neighbour = cells[nr][nc];
                        if (!neighbour.isRevealed() && !neighbour.isFlagged()) {
                            reveal(nr, nc);
                        }
                    }
                }
            }
        }
    }

    private int countAdjacentFlags(int row, int col) {
        int count = 0;
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                int nr = row + dr;
                int nc = col + dc;
                if (nr >= 0 && nr < rows && nc >= 0 && nc < cols && cells[nr][nc].isFlagged()) {
                    count++;
                }
            }
        }
        return count;
    }

    private void revealAllMines(int triggeredRow, int triggeredCol) {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                Cell cell = cells[r][c];
                if (cell.isMine() && !cell.isRevealed()) {
                    if (!(r == triggeredRow && c == triggeredCol)) {
                        cell.setState(Cell.State.REVEALED);
                    }
                }
                // Mark incorrectly placed flags
                if (cell.isFlagged() && !cell.isMine()) {
                    cell.setState(Cell.State.REVEALED);
                }
            }
        }
    }

    private void checkWin() {
        int nonMineCells = rows * cols - placedMines;
        if (revealedCount == nonMineCells) {
            gameState = GameState.WON;
            // Auto-flag remaining mines
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    if (cells[r][c].isMine() && !cells[r][c].isFlagged()) {
                        cells[r][c].setState(Cell.State.FLAGGED);
                        flaggedCount++;
                    }
                }
            }
        }
    }

    // Getters
    public int getRows() { return rows; }
    public int getCols() { return cols; }
    public int getTotalMines() { return totalMines; }
    public Cell getCell(int row, int col) { return cells[row][col]; }
    public GameState getGameState() { return gameState; }
    public int getRemainingMines() {
        int base = (gameState == GameState.WAITING) ? totalMines : placedMines;
        return base - flaggedCount;
    }
}
