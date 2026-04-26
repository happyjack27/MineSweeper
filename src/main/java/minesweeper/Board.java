package minesweeper;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Random;

/**
 * Manages the Minesweeper game board: mine placement, reveal logic, win/loss detection.
 */
public class Board {

    private static final int INITIAL_MISSILES = 3;
    private static final int MINELAYER_RESPAWN_TICKS = 6;
    private static final int BOAT_WIDTH = 2;
    private static final int BOAT_HEIGHT = 1;

    public enum VoyageStage {
        OUTBOUND,
        RETURNING
    }

    public static final class Snapshot {
        private final boolean[][] mines;
        private final int[][] adjacentMines;
        private final Cell.State[][] states;
        private final GameState gameState;
        private final int revealedCount;
        private final int flaggedCount;
        private final int placedMines;
        private final int triggeredRow;
        private final int triggeredCol;
        private final int boatRow;
        private final int boatCol;
        private final boolean boatDocked;
        private final VoyageStage voyageStage;
        private final List<MineLayerPosition> mineLayerPositions;
        private final int missilesRemaining;
        private final List<Integer> mineLayerRespawnTimers;

        private Snapshot(
            boolean[][] mines,
            int[][] adjacentMines,
            Cell.State[][] states,
            GameState gameState,
            int revealedCount,
            int flaggedCount,
            int placedMines,
            int triggeredRow,
            int triggeredCol,
            int boatRow,
            int boatCol,
            boolean boatDocked,
            VoyageStage voyageStage,
            List<MineLayerPosition> mineLayerPositions,
            int missilesRemaining,
            List<Integer> mineLayerRespawnTimers) {
            this.mines = mines;
            this.adjacentMines = adjacentMines;
            this.states = states;
            this.gameState = gameState;
            this.revealedCount = revealedCount;
            this.flaggedCount = flaggedCount;
            this.placedMines = placedMines;
            this.triggeredRow = triggeredRow;
            this.triggeredCol = triggeredCol;
            this.boatRow = boatRow;
            this.boatCol = boatCol;
            this.boatDocked = boatDocked;
            this.voyageStage = voyageStage;
            this.mineLayerPositions = mineLayerPositions;
            this.missilesRemaining = missilesRemaining;
            this.mineLayerRespawnTimers = mineLayerRespawnTimers;
        }
    }

    public static final class MineLayerPosition {
        private final int row;
        private final int col;

        private MineLayerPosition(int row, int col) {
            this.row = row;
            this.col = col;
        }

        public int getRow() { return row; }
        public int getCol() { return col; }
    }

    private static final class MineLayer {
        private int row;
        private int col;

        private MineLayer(int row, int col) {
            this.row = row;
            this.col = col;
        }
    }

    public enum GameState {
        WAITING,   // Before first click
        PLAYING,
        WON,
        LOST
    }

    private final int rows;
    private final int cols;
    private final int totalMines;
    private final int mineLayerCount;
    private final Cell[][] cells;
    private final Random random;
    private final List<MineLayer> mineLayers;
    private final List<Integer> mineLayerRespawnTimers;
    private GameState gameState;
    private int revealedCount;
    private int flaggedCount;
    private int placedMines;  // actual mines placed (may be < totalMines on tiny boards)
    private int triggeredRow = -1;
    private int triggeredCol = -1;
    private int boatRow = -1;
    private int boatCol = -1;
    private boolean boatDocked = true;
    private VoyageStage voyageStage = VoyageStage.OUTBOUND;
    private int missilesRemaining = INITIAL_MISSILES;
    private int safePassageRow = -1;

    public Board(int rows, int cols, int totalMines) {
        this(rows, cols, totalMines, 0);
    }

    public Board(int rows, int cols, int totalMines, int mineLayerCount) {
        this(rows, cols, totalMines, mineLayerCount, new Random());
    }

    Board(int rows, int cols, int totalMines, int mineLayerCount, Random random) {
        this.rows = rows;
        this.cols = cols;
        this.totalMines = totalMines;
        this.mineLayerCount = Math.max(0, mineLayerCount);
        this.cells = new Cell[rows][cols];
        this.random = random;
        this.mineLayers = new ArrayList<>();
        this.mineLayerRespawnTimers = new ArrayList<>();
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
        this(mineGrid, 0, new Random(0L));
    }

    Board(boolean[][] mineGrid, int mineLayerCount, Random random) {
        this.rows = mineGrid.length;
        this.cols = mineGrid[0].length;
        this.mineLayerCount = Math.max(0, mineLayerCount);
        this.cells = new Cell[rows][cols];
        this.random = random;
        this.mineLayers = new ArrayList<>();
        this.mineLayerRespawnTimers = new ArrayList<>();
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
        initMineLayers();
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
        safePassageRow = safeRow;
        List<Integer> positions = new ArrayList<>();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (r == safePassageRow) {
                    continue;
                }
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

    private void initMineLayers() {
        mineLayers.clear();
        if (mineLayerCount == 0) {
            return;
        }

        List<int[]> candidates = new ArrayList<>();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (cols > 1 && c == 0) {
                    continue;
                }
                candidates.add(new int[]{r, c});
            }
        }

        Collections.shuffle(candidates, random);
        int count = Math.min(mineLayerCount, candidates.size());
        for (int i = 0; i < count; i++) {
            int[] pos = candidates.get(i);
            mineLayers.add(new MineLayer(pos[0], pos[1]));
        }
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

        if (gameState == GameState.WAITING) {
            gameState = GameState.PLAYING;
            placeMines(row, col);
            seedFirstColumn();
            initMineLayers();
        }

        Cell cell = cells[row][col];
        if (cell.isRevealed() || cell.isFlagged()) {
            checkWin();
            return;
        }

        if (cell.isMine()) {
            cell.setState(Cell.State.REVEALED);
            triggeredRow = row;
            triggeredCol = col;
            boatRow = -1;
            boatCol = -1;
            boatDocked = true;
            gameState = GameState.LOST;
            revealAllMines(row, col);
            return;
        }

        floodReveal(row, col);
        checkWin();
    }

    public boolean moveBoatTo(int row, int col) {
        if (gameState == GameState.WON || gameState == GameState.LOST) {
            return false;
        }
        if (!isBoatPlacementValid(row, col, true)) {
            return false;
        }

        if (boatDocked) {
            if (col != 0) {
                return false;
            }
        } else {
            if (row != boatRow && col != boatCol) {
                return false;
            }
            if (!isStraightRevealedPath(boatRow, boatCol, row, col)) {
                return false;
            }
        }

        boatRow = row;
        boatCol = col;
        boatDocked = false;
        enforceMineLayerDistanceFromBoat();
        checkWin();
        return true;
    }

    public boolean moveBoatBy(int rowDelta, int colDelta) {
        if (boatDocked || gameState == GameState.WON || gameState == GameState.LOST) {
            return false;
        }
        if (Math.abs(rowDelta) + Math.abs(colDelta) != 1) {
            return false;
        }

        int nextRow = boatRow + rowDelta;
        int nextCol = boatCol + colDelta;
        if (nextRow < 0 || nextRow >= rows || nextCol < 0 || nextCol >= cols) {
            return false;
        }
        return moveBoatTo(nextRow, nextCol);
    }

    /**
     * Arrow-key movement that dares unrevealed water.
     * - Blocked by board edges and mine-layer cells (same as normal).
     * - If the new footprint contains a mine, the ship is sunk (LOST).
     * - If the new footprint is safe but unrevealed, those cells are revealed first.
     * Returns true if the board state changed (ship moved or was sunk).
     */
    public boolean moveBoatByBrave(int rowDelta, int colDelta) {
        if (boatDocked || gameState == GameState.WON || gameState == GameState.LOST) {
            return false;
        }
        if (Math.abs(rowDelta) + Math.abs(colDelta) != 1) {
            return false;
        }

        int nextRow = boatRow + rowDelta;
        int nextCol = boatCol + colDelta;
        if (nextRow < 0 || nextRow + BOAT_HEIGHT > rows || nextCol < 0 || nextCol + BOAT_WIDTH > cols) {
            return false;
        }

        // Check for a mine in the target footprint — sink the ship if found
        for (int r = nextRow; r < nextRow + BOAT_HEIGHT; r++) {
            for (int c = nextCol; c < nextCol + BOAT_WIDTH; c++) {
                if (cells[r][c].isMine()) {
                    cells[r][c].setState(Cell.State.REVEALED);
                    triggeredRow = r;
                    triggeredCol = c;
                    boatRow = nextRow;
                    boatCol = nextCol;
                    boatDocked = false;
                    gameState = GameState.LOST;
                    revealAllMines(r, c);
                    return true;
                }
            }
        }

        // Safe — reveal any hidden cells the boat is sailing into
        for (int r = nextRow; r < nextRow + BOAT_HEIGHT; r++) {
            for (int c = nextCol; c < nextCol + BOAT_WIDTH; c++) {
                if (!cells[r][c].isRevealed()) {
                    floodReveal(r, c);
                }
            }
        }

        boatRow = nextRow;
        boatCol = nextCol;
        boatDocked = false;
        enforceMineLayerDistanceFromBoat();
        calculateAdjacent();
        checkWin();
        return true;
    }

    private boolean isStraightRevealedPath(int startRow, int startCol, int endRow, int endCol) {
        int rowStep = Integer.compare(endRow, startRow);
        int colStep = Integer.compare(endCol, startCol);
        int currentRow = startRow;
        int currentCol = startCol;

        while (currentRow != endRow || currentCol != endCol) {
            currentRow += rowStep;
            currentCol += colStep;
            if (!isBoatPlacementValid(currentRow, currentCol, true)) {
                return false;
            }
        }
        return true;
    }

    public boolean advanceMineLayers() {
        if (gameState != GameState.PLAYING) {
            return false;
        }

        boolean changed = false;
        for (MineLayer mineLayer : mineLayers) {
            changed |= moveMineLayer(mineLayer);
        }

        changed |= advanceMineLayerRespawns();

        if (changed) {
            calculateAdjacent();
        }
        checkWin();
        return changed;
    }

    public boolean fireMissileAt(int row, int col) {
        if (gameState == GameState.WON || gameState == GameState.LOST || missilesRemaining <= 0) {
            return false;
        }

        boolean destroyedMine = false;
        int removedMineLayers = 0;

        for (int targetRow = Math.max(0, row - 1); targetRow <= Math.min(rows - 1, row + 1); targetRow++) {
            for (int targetCol = Math.max(0, col - 1); targetCol <= Math.min(cols - 1, col + 1); targetCol++) {
                Cell cell = cells[targetRow][targetCol];
                if (cell.isMine()) {
                    cell.setMine(false);
                    destroyedMine = true;
                    if (cell.isFlagged()) {
                        cell.setState(Cell.State.HIDDEN);
                        flaggedCount--;
                    }
                }
            }
        }

        for (int targetRow = Math.max(0, row - 1); targetRow <= Math.min(rows - 1, row + 1); targetRow++) {
            for (int targetCol = Math.max(0, col - 1); targetCol <= Math.min(cols - 1, col + 1); targetCol++) {
                revealBlastCell(targetRow, targetCol);
            }
        }

        for (int index = mineLayers.size() - 1; index >= 0; index--) {
            MineLayer mineLayer = mineLayers.get(index);
            if (Math.abs(mineLayer.row - row) <= 1 && Math.abs(mineLayer.col - col) <= 1) {
                mineLayers.remove(index);
                removedMineLayers++;
            }
        }

        for (int index = 0; index < removedMineLayers; index++) {
            mineLayerRespawnTimers.add(MINELAYER_RESPAWN_TICKS);
        }

        if (!destroyedMine && removedMineLayers == 0) {
            return false;
        }

        missilesRemaining--;
        calculateAdjacent();
        checkWin();
        return true;
    }

    private void revealBlastCell(int row, int col) {
        Cell cell = cells[row][col];
        if (cell.isFlagged()) {
            flaggedCount--;
        }
        if (!cell.isRevealed()) {
            revealedCount++;
        }
        cell.setState(Cell.State.REVEALED);
    }

    private boolean advanceMineLayerRespawns() {
        if (mineLayerRespawnTimers.isEmpty()) {
            return false;
        }

        boolean changed = false;
        for (int index = 0; index < mineLayerRespawnTimers.size(); ) {
            int nextTimer = mineLayerRespawnTimers.get(index) - 1;
            if (nextTimer <= 0) {
                if (spawnMineLayerAtPerimeter()) {
                    mineLayerRespawnTimers.remove(index);
                    changed = true;
                    continue;
                }
                nextTimer = 1;
            }
            mineLayerRespawnTimers.set(index, nextTimer);
            index++;
        }
        return changed;
    }

    private boolean spawnMineLayerAtPerimeter() {
        List<int[]> perimeterCells = new ArrayList<>();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (r != 0 && r != rows - 1 && c != 0 && c != cols - 1) {
                    continue;
                }
                if (isBoatCell(r, c) || isMineLayerOccupied(r, c, null) || isMineLayerTooCloseToBoat(r, c)) {
                    continue;
                }
                perimeterCells.add(new int[]{r, c});
            }
        }

        if (perimeterCells.isEmpty()) {
            return false;
        }

        int[] spawn = perimeterCells.get(random.nextInt(perimeterCells.size()));
        mineLayers.add(new MineLayer(spawn[0], spawn[1]));
        applyMineLayerEffect(spawn[0], spawn[1]);
        return true;
    }

    private boolean moveMineLayer(MineLayer mineLayer) {
        List<int[]> destinations = new ArrayList<>();
        int[][] directions = {
            {-1, 0},
            {1, 0},
            {0, -1},
            {0, 1}
        };

        for (int[] direction : directions) {
            int nextRow = mineLayer.row + direction[0];
            int nextCol = mineLayer.col + direction[1];
            if (nextRow < 0 || nextRow >= rows || nextCol < 0 || nextCol >= cols) {
                continue;
            }
            if (isBoatCell(nextRow, nextCol)
                || isMineLayerOccupied(nextRow, nextCol, mineLayer)
                || isMineLayerTooCloseToBoat(nextRow, nextCol)) {
                continue;
            }
            destinations.add(new int[]{nextRow, nextCol});
        }

        if (destinations.isEmpty()) {
            return false;
        }

        int[] next = destinations.get(random.nextInt(destinations.size()));
        mineLayer.row = next[0];
        mineLayer.col = next[1];
        applyMineLayerEffect(next[0], next[1]);
        return true;
    }

    private boolean applyMineLayerEffect(int row, int col) {
        Cell cell = cells[row][col];
        boolean changed = resetCellForMineLayer(cell);

        int currentMineCount = countMines();
        if (cell.isMine()) {
            if (currentMineCount > placedMines || random.nextBoolean()) {
                cell.setMine(false);
                changed = true;
            }
        } else if (currentMineCount < placedMines || random.nextBoolean()) {
            cell.setMine(true);
            changed = true;
        }

        return changed;
    }

    private boolean resetCellForMineLayer(Cell cell) {
        boolean changed = false;
        if (cell.isFlagged()) {
            flaggedCount--;
            changed = true;
        }
        if (cell.getState() != Cell.State.HIDDEN) {
            cell.setState(Cell.State.HIDDEN);
            changed = true;
        }
        return changed;
    }

    private int countMines() {
        int mineCount = 0;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (cells[r][c].isMine()) {
                    mineCount++;
                }
            }
        }
        return mineCount;
    }

    private boolean isBoatCell(int row, int col) {
        return !boatDocked
            && row >= boatRow
            && row < boatRow + BOAT_HEIGHT
            && col >= boatCol
            && col < boatCol + BOAT_WIDTH;
    }

    private boolean isBoatPlacementValid(int row, int col, boolean requireRevealed) {
        if (row < 0 || row + BOAT_HEIGHT > rows || col < 0 || col + BOAT_WIDTH > cols) {
            return false;
        }

        for (int occupiedRow = row; occupiedRow < row + BOAT_HEIGHT; occupiedRow++) {
            for (int occupiedCol = col; occupiedCol < col + BOAT_WIDTH; occupiedCol++) {
                Cell cell = cells[occupiedRow][occupiedCol];
                if (cell.isMine()) {
                    return false;
                }
                if (requireRevealed && !cell.isRevealed()) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isMineLayerOccupied(int row, int col, MineLayer currentMineLayer) {
        for (MineLayer mineLayer : mineLayers) {
            if (mineLayer == currentMineLayer) {
                continue;
            }
            if (mineLayer.row == row && mineLayer.col == col) {
                return true;
            }
        }
        return false;
    }

    private boolean isMineLayerTooCloseToBoat(int row, int col) {
        if (boatDocked) {
            return false;
        }

        int boatLeft = boatCol;
        int boatRight = boatCol + BOAT_WIDTH - 1;
        int closestCol = Math.max(boatLeft, Math.min(col, boatRight));
        int rowDistance = Math.abs(row - boatRow);
        int colDistance = Math.abs(col - closestCol);
        return Math.max(rowDistance, colDistance) < 2;
    }

    private void enforceMineLayerDistanceFromBoat() {
        if (boatDocked) {
            return;
        }

        boolean changed = false;
        for (MineLayer mineLayer : mineLayers) {
            if (!isMineLayerTooCloseToBoat(mineLayer.row, mineLayer.col)) {
                continue;
            }

            int[] retreatCell = findMineLayerRetreatCell(mineLayer);
            if (retreatCell == null) {
                continue;  // hemmed in — stay put until space opens
            }

            mineLayer.row = retreatCell[0];
            mineLayer.col = retreatCell[1];
            changed |= applyMineLayerEffect(retreatCell[0], retreatCell[1]);
        }

        if (changed) {
            calculateAdjacent();
        }
    }

    private int[] findMineLayerRetreatCell(MineLayer movingMineLayer) {
        int[] bestCell = null;
        int bestDist = Integer.MIN_VALUE;

        for (int nr = 0; nr < rows; nr++) {
            for (int nc = 0; nc < cols; nc++) {
                if (isMineLayerOccupied(nr, nc, movingMineLayer) || isMineLayerTooCloseToBoat(nr, nc)) {
                    continue;
                }

                int boatLeft = boatCol;
                int boatRight = boatCol + BOAT_WIDTH - 1;
                int closestCol = Math.max(boatLeft, Math.min(nc, boatRight));
                int dist = Math.max(Math.abs(nr - boatRow), Math.abs(nc - closestCol));
                if (dist > bestDist) {
                    bestDist = dist;
                    bestCell = new int[]{nr, nc};
                }
            }
        }
        return bestCell;
    }

    public Snapshot createSnapshot() {
        boolean[][] mines = new boolean[rows][cols];
        int[][] adjacentMines = new int[rows][cols];
        Cell.State[][] states = new Cell.State[rows][cols];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                Cell cell = cells[r][c];
                mines[r][c] = cell.isMine();
                adjacentMines[r][c] = cell.getAdjacentMines();
                states[r][c] = cell.getState();
            }
        }

        List<MineLayerPosition> positions = new ArrayList<>();
        for (MineLayer mineLayer : mineLayers) {
            positions.add(new MineLayerPosition(mineLayer.row, mineLayer.col));
        }

        return new Snapshot(
            mines,
            adjacentMines,
            states,
            gameState,
            revealedCount,
            flaggedCount,
            placedMines,
            triggeredRow,
            triggeredCol,
            boatRow,
            boatCol,
            boatDocked,
            voyageStage,
                positions,
                missilesRemaining,
                new ArrayList<>(mineLayerRespawnTimers));
    }

    public void restoreSnapshot(Snapshot snapshot) {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                Cell cell = cells[r][c];
                cell.setMine(snapshot.mines[r][c]);
                cell.setAdjacentMines(snapshot.adjacentMines[r][c]);
                cell.setState(snapshot.states[r][c]);
            }
        }

        gameState = snapshot.gameState;
        revealedCount = snapshot.revealedCount;
        flaggedCount = snapshot.flaggedCount;
        placedMines = snapshot.placedMines;
        triggeredRow = snapshot.triggeredRow;
        triggeredCol = snapshot.triggeredCol;
        boatRow = snapshot.boatRow;
        boatCol = snapshot.boatCol;
        boatDocked = snapshot.boatDocked;
        voyageStage = snapshot.voyageStage;
        missilesRemaining = snapshot.missilesRemaining;

        mineLayers.clear();
        for (MineLayerPosition position : snapshot.mineLayerPositions) {
            mineLayers.add(new MineLayer(position.getRow(), position.getCol()));
        }
        mineLayerRespawnTimers.clear();
        mineLayerRespawnTimers.addAll(snapshot.mineLayerRespawnTimers);
    }

    private void seedFirstColumn() {
        for (int row = 0; row < rows; row++) {
            Cell cell = cells[row][0];
            if (cell.isMine()) {
                cell.setState(Cell.State.FLAGGED);
                flaggedCount++;
            } else if (!cell.isRevealed()) {
                floodReveal(row, 0);
            }
        }
    }

    /**
     * Iterative flood-fill reveal: reveals cell and, if it has 0 adjacent mines,
     * enqueues all neighbours for the same treatment.  Using an iterative approach
     * avoids stack-overflow on large open boards.
     */
    private void floodReveal(int startRow, int startCol) {
        Deque<int[]> queue = new ArrayDeque<>();
        queue.add(new int[]{startRow, startCol});
        while (!queue.isEmpty()) {
            int[] pos = queue.poll();
            int row = pos[0];
            int col = pos[1];
            if (row < 0 || row >= rows || col < 0 || col >= cols) {
                continue;
            }
            Cell cell = cells[row][col];
            if (cell.isRevealed() || cell.isFlagged() || cell.isMine()) {
                continue;
            }
            cell.setState(Cell.State.REVEALED);
            revealedCount++;
            if (cell.getAdjacentMines() == 0) {
                for (int dr = -1; dr <= 1; dr++) {
                    for (int dc = -1; dc <= 1; dc++) {
                        if (dr != 0 || dc != 0) {
                            queue.add(new int[]{row + dr, col + dc});
                        }
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
            return;
        }

        int hiddenUnflagged = countAdjacentHiddenUnflagged(row, col);
        if (flags + hiddenUnflagged == cell.getAdjacentMines()) {
            for (int dr = -1; dr <= 1; dr++) {
                for (int dc = -1; dc <= 1; dc++) {
                    int nr = row + dr;
                    int nc = col + dc;
                    if (nr >= 0 && nr < rows && nc >= 0 && nc < cols) {
                        Cell neighbour = cells[nr][nc];
                        if (!neighbour.isRevealed() && !neighbour.isFlagged()) {
                            neighbour.setState(Cell.State.FLAGGED);
                            flaggedCount++;
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

    private int countAdjacentHiddenUnflagged(int row, int col) {
        int count = 0;
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                int nr = row + dr;
                int nc = col + dc;
                if (nr >= 0 && nr < rows && nc >= 0 && nc < cols) {
                    Cell cell = cells[nr][nc];
                    if (!cell.isRevealed() && !cell.isFlagged()) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    private void revealAllMines(int triggeredRow, int triggeredCol) {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                Cell cell = cells[r][c];
                // Reveal un-flagged, un-revealed mines (skip the triggered cell – already REVEALED)
                if (cell.isMine() && !cell.isRevealed() && !cell.isFlagged()) {
                    if (!(r == triggeredRow && c == triggeredCol)) {
                        cell.setState(Cell.State.REVEALED);
                    }
                }
                // Mark incorrectly placed flags so the UI can render them distinctly
                if (cell.isFlagged() && !cell.isMine()) {
                    cell.setState(Cell.State.WRONG_FLAG);
                }
            }
        }
    }

    private void checkWin() {
        if (boatDocked) {
            return;
        }

        if (voyageStage == VoyageStage.OUTBOUND && boatCol == cols - BOAT_WIDTH) {
            voyageStage = VoyageStage.RETURNING;
            return;
        }

        if (voyageStage == VoyageStage.RETURNING && boatCol == 0) {
            gameState = GameState.WON;
            autoFlagRemainingMines();
        }
    }

    private void autoFlagRemainingMines() {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (cells[r][c].isMine() && !cells[r][c].isFlagged()) {
                    cells[r][c].setState(Cell.State.FLAGGED);
                    flaggedCount++;
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
    public int getTriggeredRow() { return triggeredRow; }
    public int getTriggeredCol() { return triggeredCol; }
    public int getBoatRow() { return boatRow; }
    public int getBoatCol() { return boatCol; }
    public boolean isBoatDocked() { return boatDocked; }
    public boolean isBoatOccupying(int row, int col) { return isBoatCell(row, col); }
    public VoyageStage getVoyageStage() { return voyageStage; }
    public int getMineLayerCount() { return mineLayerCount; }
    public int getMissilesRemaining() { return missilesRemaining; }
    public boolean hasMineLayerAt(int row, int col) {
        return findMineLayerIndex(row, col) >= 0;
    }
    public List<MineLayerPosition> getMineLayerPositions() {
        List<MineLayerPosition> positions = new ArrayList<>();
        for (MineLayer mineLayer : mineLayers) {
            positions.add(new MineLayerPosition(mineLayer.row, mineLayer.col));
        }
        return positions;
    }
    public int getRemainingMines() {
        int base = (gameState == GameState.WAITING) ? totalMines : placedMines;
        return base - flaggedCount;
    }

    private int findMineLayerIndex(int row, int col) {
        for (int index = 0; index < mineLayers.size(); index++) {
            MineLayer mineLayer = mineLayers.get(index);
            if (mineLayer.row == row && mineLayer.col == col) {
                return index;
            }
        }
        return -1;
    }
}
