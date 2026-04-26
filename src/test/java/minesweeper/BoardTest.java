package minesweeper;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Board} game logic.
 */
class BoardTest {

    @Test
    void initialStateIsWaiting() {
        Board board = new Board(9, 9, 10);
        assertEquals(Board.GameState.WAITING, board.getGameState());
    }

    @Test
    void allCellsHiddenAtStart() {
        Board board = new Board(9, 9, 10);
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                assertEquals(Cell.State.HIDDEN, board.getCell(r, c).getState());
            }
        }
    }

    @Test
    void firstClickTransitionsToPlaying() {
        Board board = new Board(9, 9, 10);
        board.reveal(4, 4);
        assertNotEquals(Board.GameState.WAITING, board.getGameState());
    }

    @Test
    void firstClickCellIsNeverAMine() {
        // Repeat many times to be statistically confident
        for (int i = 0; i < 50; i++) {
            Board board = new Board(9, 9, 10);
            board.reveal(4, 4);
            assertFalse(board.getCell(4, 4).isMine(),
                "The first-clicked cell should never be a mine");
        }
    }

    @Test
    void firstClickNeighboursAreNeverMines() {
        for (int i = 0; i < 30; i++) {
            Board board = new Board(9, 9, 10);
            board.reveal(4, 4);
            for (int dr = -1; dr <= 1; dr++) {
                for (int dc = -1; dc <= 1; dc++) {
                    assertFalse(board.getCell(4 + dr, 4 + dc).isMine(),
                        "Neighbours of first click should never be mines");
                }
            }
        }
    }

    @Test
    void revealedCellStateIsRevealed() {
        Board board = new Board(9, 9, 10);
        board.reveal(4, 4);
        assertEquals(Cell.State.REVEALED, board.getCell(4, 4).getState());
    }

    @Test
    void flagTogglesState() {
        Board board = new Board(9, 9, 10);
        // First click to start the game (so we can flag)
        board.reveal(0, 0);
        // Find a hidden cell to flag
        int fr = -1, fc = -1;
        outer:
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                if (board.getCell(r, c).getState() == Cell.State.HIDDEN) {
                    fr = r; fc = c;
                    break outer;
                }
            }
        }
        assertTrue(fr >= 0, "There should be at least one hidden cell");
        board.toggleFlag(fr, fc);
        assertEquals(Cell.State.FLAGGED, board.getCell(fr, fc).getState());
        board.toggleFlag(fr, fc);
        assertEquals(Cell.State.QUESTION, board.getCell(fr, fc).getState());
        board.toggleFlag(fr, fc);
        assertEquals(Cell.State.HIDDEN, board.getCell(fr, fc).getState());
    }

    @Test
    void flaggedCellCannotBeRevealed() {
        Board board = new Board(9, 9, 10);
        board.reveal(4, 4);
        int fr = -1, fc = -1;
        outer:
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                if (board.getCell(r, c).getState() == Cell.State.HIDDEN) {
                    fr = r; fc = c;
                    break outer;
                }
            }
        }
        assertTrue(fr >= 0);
        board.toggleFlag(fr, fc);
        board.reveal(fr, fc);
        assertEquals(Cell.State.FLAGGED, board.getCell(fr, fc).getState());
    }

    @Test
    void remainingMinesCountUpdatesWithFlags() {
        Board board = new Board(9, 9, 10);
        assertEquals(10, board.getRemainingMines());
        board.reveal(4, 4);
        // Flag any hidden cell
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                if (board.getCell(r, c).getState() == Cell.State.HIDDEN) {
                    board.toggleFlag(r, c);
                    assertEquals(9, board.getRemainingMines());
                    return;
                }
            }
        }
    }

    @Test
    void totalMinesCountIsCorrect() {
        Board board = new Board(9, 9, 10);
        board.reveal(4, 4);
        int mineCount = 0;
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                if (board.getCell(r, c).isMine()) {
                    mineCount++;
                }
            }
        }
        assertEquals(10, mineCount);
    }

    @Test
    void winOnSmallBoardWithOneMine() {
        // 3×3 board, mine at (0,0). Reveal all safe cells to win.
        boolean[][] grid = {
            {true,  false, false},
            {false, false, false},
            {false, false, false}
        };
        Board board = new Board(grid);
        // Reveal all safe cells
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                if (!board.getCell(r, c).isMine()) {
                    board.reveal(r, c);
                }
            }
        }
        assertEquals(Board.GameState.WON, board.getGameState());
    }

    @Test
    void gameOverWhenMineRevealed() {
        // 3×3 board, mine at (0,0). Revealing the mine causes LOST.
        boolean[][] grid = {
            {true,  false, false},
            {false, false, false},
            {false, false, false}
        };
        Board board = new Board(grid);
        board.reveal(0, 0);
        assertEquals(Board.GameState.LOST, board.getGameState());
    }

    @Test
    void noActionsAfterGameOver() {
        // 3×3 board, mine at (0,0). After losing, toggling a flag has no effect.
        boolean[][] grid = {
            {true,  false, false},
            {false, false, false},
            {false, false, false}
        };
        Board board = new Board(grid);
        board.reveal(0, 0);
        assertEquals(Board.GameState.LOST, board.getGameState());
        Cell.State stateBefore = board.getCell(1, 1).getState();
        board.toggleFlag(1, 1);
        assertEquals(stateBefore, board.getCell(1, 1).getState());
    }

    @Test
    void adjacentMineCountsAreCorrect() {
        Board board = new Board(9, 9, 10);
        board.reveal(4, 4);
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                if (!board.getCell(r, c).isMine()) {
                    int expected = 0;
                    for (int dr = -1; dr <= 1; dr++) {
                        for (int dc = -1; dc <= 1; dc++) {
                            int nr = r + dr, nc = c + dc;
                            if (nr >= 0 && nr < 9 && nc >= 0 && nc < 9
                                    && board.getCell(nr, nc).isMine()) {
                                expected++;
                            }
                        }
                    }
                    assertEquals(expected, board.getCell(r, c).getAdjacentMines(),
                        "Adjacent count mismatch at (" + r + "," + c + ")");
                }
            }
        }
    }

    @Test
    void triggeredMineCoordsRecordedOnLoss() {
        boolean[][] grid = {
            {true,  false, false},
            {false, false, false},
            {false, false, false}
        };
        Board board = new Board(grid);
        board.reveal(0, 0);
        assertEquals(Board.GameState.LOST, board.getGameState());
        assertEquals(0, board.getTriggeredRow(), "Triggered row should be 0");
        assertEquals(0, board.getTriggeredCol(), "Triggered col should be 0");
    }

    @Test
    void triggeredMineCoordsDefaultToMinusOneBeforeGameStart() {
        Board board = new Board(9, 9, 10);
        assertEquals(-1, board.getTriggeredRow());
        assertEquals(-1, board.getTriggeredCol());
    }

    @Test
    void incorrectlyFlaggedCellBecomesWrongFlagOnLoss() {
        // Mine at (0,0). Flag a safe cell (1,1) then reveal the mine.
        boolean[][] grid = {
            {true,  false, false},
            {false, false, false},
            {false, false, false}
        };
        Board board = new Board(grid);
        board.toggleFlag(1, 1);
        assertEquals(Cell.State.FLAGGED, board.getCell(1, 1).getState());
        board.reveal(0, 0);
        assertEquals(Board.GameState.LOST, board.getGameState());
        assertEquals(Cell.State.WRONG_FLAG, board.getCell(1, 1).getState(),
            "Incorrectly flagged cell should become WRONG_FLAG after a loss");
    }

    @Test
    void correctFlagRemainsFlaggedAfterLoss() {
        // Mine at (0,0) and (0,1). Flag the mine correctly then reveal the other mine.
        boolean[][] grid = {
            {true,  true,  false},
            {false, false, false},
            {false, false, false}
        };
        Board board = new Board(grid);
        board.toggleFlag(0, 0);  // correct flag on mine
        board.reveal(0, 1);      // reveal the other mine → loss
        assertEquals(Board.GameState.LOST, board.getGameState());
        // A correctly placed flag should stay FLAGGED, not become WRONG_FLAG
        assertEquals(Cell.State.FLAGGED, board.getCell(0, 0).getState(),
            "Correctly placed flag should stay FLAGGED after a loss");
    }
}
