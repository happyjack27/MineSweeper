package minesweeper;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Board} game logic.
 */
class BoardTest {

    @Test
    void initialStateIsWaiting() {
        Board board = new Board(9, 9, 10);
        assertEquals(Board.GameState.WAITING, board.getGameState());
        assertTrue(board.isBoatDocked());
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
        for (int i = 0; i < 50; i++) {
            Board board = new Board(9, 9, 10);
            board.reveal(4, 4);
            assertFalse(board.getCell(4, 4).isMine());
        }
    }

    @Test
    void firstClickNeighboursAreNeverMines() {
        for (int i = 0; i < 30; i++) {
            Board board = new Board(9, 9, 10);
            board.reveal(4, 4);
            for (int dr = -1; dr <= 1; dr++) {
                for (int dc = -1; dc <= 1; dc++) {
                    assertFalse(board.getCell(4 + dr, 4 + dc).isMine());
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
        boolean[][] grid = {
            {true, false, false},
            {false, false, false},
            {false, false, false}
        };
        Board board = new Board(grid);

        board.reveal(1, 1);
        board.toggleFlag(0, 2);
        assertEquals(Cell.State.FLAGGED, board.getCell(0, 2).getState());

        board.toggleFlag(0, 2);
        assertEquals(Cell.State.QUESTION, board.getCell(0, 2).getState());

        board.toggleFlag(0, 2);
        assertEquals(Cell.State.HIDDEN, board.getCell(0, 2).getState());
    }

    @Test
    void flaggedCellCannotBeRevealed() {
        boolean[][] grid = {
            {true, false, false},
            {false, false, false},
            {false, false, false}
        };
        Board board = new Board(grid);
        board.reveal(1, 1);
        board.toggleFlag(0, 0);
        board.reveal(0, 0);

        assertEquals(Cell.State.FLAGGED, board.getCell(0, 0).getState());
    }

    @Test
    void chordRevealsAllUnmarkedNeighborsWhenFlagsMatch() {
        boolean[][] grid = {
            {true, false, false},
            {false, false, false},
            {false, false, false}
        };
        Board board = new Board(grid);

        board.reveal(1, 1);
        board.toggleFlag(0, 0);

        assertEquals(Cell.State.HIDDEN, board.getCell(0, 1).getState());
        assertEquals(Cell.State.HIDDEN, board.getCell(0, 2).getState());

        board.chord(1, 1);

        assertEquals(Cell.State.REVEALED, board.getCell(0, 1).getState());
        assertEquals(Cell.State.REVEALED, board.getCell(0, 2).getState());
        assertEquals(Cell.State.FLAGGED, board.getCell(0, 0).getState());
    }
    @Test
    void remainingMinesCountUpdatesWithFlags() {
        boolean[][] grid = {
            {true, false, true},
            {false, false, false},
            {false, false, false}
        };
        Board board = new Board(grid);
        assertEquals(2, board.getRemainingMines());

        board.reveal(1, 1);
        board.toggleFlag(0, 0);

        assertEquals(1, board.getRemainingMines());
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
    void firstClickSeedsFirstColumnWithFlagsAndClears() {
        Board board = new Board(9, 9, 10);
        board.reveal(4, 4);

        for (int r = 0; r < 9; r++) {
            Cell cell = board.getCell(r, 0);
            if (cell.isMine()) {
                assertEquals(Cell.State.FLAGGED, cell.getState());
            } else {
                assertEquals(Cell.State.REVEALED, cell.getState());
            }
        }
    }

    @Test
    void configuredMineLayerCountSpawnsAfterFirstReveal() {
        Board board = new Board(8, 18, 24, 3, new Random(0L));

        board.reveal(4, 4);

        assertEquals(3, board.getMineLayerPositions().size());
        assertEquals(3, board.getMineLayerCount());
    }

    @Test
    void mineLayerMovementHidesVisitedCells() {
        boolean[][] grid = {
            {false, false}
        };
        Board board = new Board(grid, 1, new Random(0L));

        List<Board.MineLayerPosition> initialPositions = board.getMineLayerPositions();
        assertEquals(1, initialPositions.size());
        assertEquals(1, initialPositions.get(0).getCol());

        board.getCell(0, 0).setState(Cell.State.REVEALED);

        assertTrue(board.advanceMineLayers());
        assertEquals(Cell.State.HIDDEN, board.getCell(0, 0).getState());
        assertEquals(0, board.getMineLayerPositions().get(0).getCol());
    }

    @Test
    void missileDestroysMineLayerAndConsumesOneMissile() {
        boolean[][] grid = {
            {false, false, false},
            {false, false, false},
            {false, false, false}
        };
        Board board = new Board(grid, 1, new Random(0L));
        Board.MineLayerPosition target = board.getMineLayerPositions().get(0);

        assertEquals(3, board.getMissilesRemaining());
        assertTrue(board.fireMissileAt(target.getRow(), target.getCol()));

        assertEquals(2, board.getMissilesRemaining());
        assertFalse(board.hasMineLayerAt(target.getRow(), target.getCol()));
        assertTrue(board.getMineLayerPositions().isEmpty());
    }

    @Test
    void missileClearsMinesInThreeByThreeBlastRadius() {
        boolean[][] grid = {
            {true, true, false, false},
            {true, true, false, false},
            {false, false, false, false},
            {false, false, false, true}
        };
        Board board = new Board(grid);

        assertTrue(board.fireMissileAt(1, 1));

        for (int row = 0; row <= 2; row++) {
            for (int col = 0; col <= 2; col++) {
                assertFalse(board.getCell(row, col).isMine());
            }
        }
        assertTrue(board.getCell(3, 3).isMine());
        assertEquals(2, board.getMissilesRemaining());
    }

    @Test
    void missileRevealsEntireThreeByThreeBlastRadius() {
        boolean[][] grid = {
            {true, true, false, false},
            {true, false, false, false},
            {false, false, false, false},
            {false, false, false, false}
        };
        Board board = new Board(grid);
        board.toggleFlag(0, 0);

        assertTrue(board.fireMissileAt(1, 1));

        for (int row = 0; row <= 2; row++) {
            for (int col = 0; col <= 2; col++) {
                assertEquals(Cell.State.REVEALED, board.getCell(row, col).getState());
            }
        }
        assertEquals(2, board.getMissilesRemaining());
    }

    @Test
    void missileDestroysMineLayersAnywhereInThreeByThreeBlastRadius() {
        boolean[][] grid = {
            {false, false, false},
            {false, false, false},
            {false, false, false}
        };
        Board board = new Board(grid, 1, new Random(0L));
        Board.MineLayerPosition target = board.getMineLayerPositions().get(0);

        assertTrue(Math.abs(target.getRow() - 1) <= 1 && Math.abs(target.getCol() - 1) <= 1);
        assertTrue(board.fireMissileAt(1, 1));

        assertFalse(board.hasMineLayerAt(target.getRow(), target.getCol()));
        assertTrue(board.getMineLayerPositions().isEmpty());
        assertEquals(2, board.getMissilesRemaining());
    }

    @Test
    void destroyedMineLayerRespawnsOnPerimeterAfterDelay() {
        boolean[][] grid = {
            {false, false, false},
            {false, false, false},
            {false, false, false}
        };
        Board board = new Board(grid, 1, new Random(0L));
        Board.MineLayerPosition target = board.getMineLayerPositions().get(0);

        assertTrue(board.fireMissileAt(target.getRow(), target.getCol()));
        assertTrue(board.getMineLayerPositions().isEmpty());

        for (int i = 0; i < 5; i++) {
            board.advanceMineLayers();
            assertTrue(board.getMineLayerPositions().isEmpty());
        }

        board.advanceMineLayers();

        assertEquals(1, board.getMineLayerPositions().size());
        Board.MineLayerPosition respawned = board.getMineLayerPositions().get(0);
        assertTrue(respawned.getRow() == 0 || respawned.getRow() == 2 || respawned.getCol() == 0 || respawned.getCol() == 2);
    }

    @Test
    void launchingBoatPushesNearbyMineLayerOutsideTwoCellBuffer() {
        boolean[][] grid = {
            {false, false, false, false, false},
            {false, false, false, false, false},
            {false, false, false, false, false}
        };
        Board board = new Board(grid, 1, new Random(0L));

        for (int col = 0; col < 5; col++) {
            board.reveal(1, col);
        }

        assertTrue(board.moveBoatTo(1, 0));
        for (Board.MineLayerPosition position : board.getMineLayerPositions()) {
            assertTrue(isAtLeastTwoCellsFromBoat(board, position));
        }
    }

    @Test
    void mineLayerRespawnStaysAtLeastTwoCellsFromBoat() {
        boolean[][] grid = {
            {false, false, false, false, false},
            {false, false, false, false, false},
            {false, false, false, false, false}
        };
        Board board = new Board(grid, 1, new Random(0L));

        for (int col = 0; col < 5; col++) {
            board.reveal(1, col);
        }

        assertTrue(board.moveBoatTo(1, 0));
        Board.MineLayerPosition target = board.getMineLayerPositions().get(0);
        assertTrue(board.fireMissileAt(target.getRow(), target.getCol()));

        for (int i = 0; i < 6; i++) {
            board.advanceMineLayers();
        }

        Board.MineLayerPosition respawned = board.getMineLayerPositions().get(0);
        assertTrue(isAtLeastTwoCellsFromBoat(board, respawned));
    }

    @Test
    void movingMineLayerKeepsTwoCellBufferFromBoat() {
        boolean[][] grid = {
            {false, false, false, false, false},
            {false, false, false, false, false},
            {false, false, false, false, false}
        };
        Board board = new Board(grid, 1, new Random(0L));

        for (int col = 0; col < 5; col++) {
            board.reveal(1, col);
        }

        assertTrue(board.moveBoatTo(1, 0));
        for (int i = 0; i < 5; i++) {
            board.advanceMineLayers();
            for (Board.MineLayerPosition position : board.getMineLayerPositions()) {
                assertTrue(isAtLeastTwoCellsFromBoat(board, position));
            }
        }
    }

    @Test
    void restoringSnapshotRevertsBoardState() {
        boolean[][] grid = {
            {true, false, false},
            {false, false, false},
            {false, false, false}
        };
        Board board = new Board(grid);

        Board.Snapshot snapshot = board.createSnapshot();
        board.reveal(1, 1);
        board.toggleFlag(0, 0);
        board.moveBoatTo(1, 0);

        board.restoreSnapshot(snapshot);

        assertEquals(Board.GameState.PLAYING, board.getGameState());
        assertTrue(board.isBoatDocked());
    assertEquals(Board.VoyageStage.OUTBOUND, board.getVoyageStage());
        assertEquals(-1, board.getBoatRow());
        assertEquals(-1, board.getBoatCol());
        assertEquals(Cell.State.HIDDEN, board.getCell(1, 1).getState());
        assertEquals(Cell.State.HIDDEN, board.getCell(0, 0).getState());
    }

    @Test
    void boatStartsDockedUntilPlayerLaunchesIt() {
        boolean[][] grid = {
            {true, false, false},
            {false, false, false},
            {false, false, false}
        };
        Board board = new Board(grid);

        board.reveal(1, 0);

        assertTrue(board.isBoatDocked());
        assertEquals(-1, board.getBoatRow());
        assertEquals(-1, board.getBoatCol());
    }

    @Test
    void boatCanLaunchOnlyFromRevealedWestEdge() {
        boolean[][] grid = {
            {false, false, false, false},
            {false, true, true, false},
            {false, false, false, false}
        };
        Board board = new Board(grid);

        board.reveal(2, 0);
        board.reveal(2, 1);

        assertFalse(board.moveBoatTo(2, 1));
        assertTrue(board.moveBoatTo(2, 0));
        assertFalse(board.isBoatDocked());
        assertEquals(2, board.getBoatRow());
        assertEquals(0, board.getBoatCol());
    }

    @Test
    void boatRequiresTwoClearHorizontalCells() {
        boolean[][] grid = {
            {false, true, false},
            {false, false, false}
        };
        Board board = new Board(grid);

        board.reveal(0, 0);
        board.reveal(1, 0);
        board.reveal(1, 1);

        assertFalse(board.moveBoatTo(0, 0));
        assertTrue(board.moveBoatTo(1, 0));
        assertTrue(board.isBoatOccupying(1, 0));
        assertTrue(board.isBoatOccupying(1, 1));
    }

    @Test
    void boatMovesAlongStraightRevealedChannel() {
        boolean[][] grid = {
            {false, false, false, false, false},
            {false, true, true, true, false},
            {false, false, false, false, false}
        };
        Board board = new Board(grid);

        board.reveal(2, 0);
        board.reveal(2, 1);
        board.reveal(2, 2);
        board.reveal(2, 3);
        board.reveal(2, 4);
        assertTrue(board.moveBoatTo(2, 0));

        assertTrue(board.moveBoatTo(2, 3));
        assertFalse(board.moveBoatTo(1, 1));
        assertEquals(2, board.getBoatRow());
        assertEquals(3, board.getBoatCol());
    }

    @Test
    void boatCannotSkipOverUnrevealedCells() {
        boolean[][] grid = {
            {false, false, false, false},
            {false, true, true, false},
            {false, false, false, false}
        };
        Board board = new Board(grid);

        board.reveal(2, 0);
        board.reveal(2, 1);
        board.reveal(2, 2);
        assertTrue(board.moveBoatTo(2, 0));

        assertFalse(board.moveBoatTo(2, 2));
        assertEquals(0, board.getBoatCol());
    }

    @Test
    void directionalMoveAdvancesOneSquare() {
        boolean[][] grid = {
            {false, false, false, false, false},
            {false, true, true, true, false},
            {false, false, false, false, false}
        };
        Board board = new Board(grid);

        board.reveal(2, 0);
        board.reveal(2, 1);
        board.reveal(2, 2);
        board.reveal(2, 3);
        assertTrue(board.moveBoatTo(2, 0));

        assertTrue(board.moveBoatBy(0, 1));
        assertEquals(1, board.getBoatCol());
        assertTrue(board.moveBoatBy(0, -1));
        assertEquals(0, board.getBoatCol());
    }

    @Test
    void reachingHarborStartsReturnVoyage() {
        boolean[][] grid = {
            {false, false, false, false, false},
            {true, true, true, true, true},
            {false, false, false, false, false}
        };
        Board board = new Board(grid);

        board.reveal(0, 0);
        board.reveal(0, 1);
        board.reveal(0, 2);
        board.reveal(0, 3);
        board.reveal(0, 4);

        assertEquals(Board.GameState.PLAYING, board.getGameState());
        assertTrue(board.moveBoatTo(0, 0));
        assertTrue(board.moveBoatTo(0, 1));
        assertTrue(board.moveBoatTo(0, 2));
        assertEquals(Board.GameState.PLAYING, board.getGameState());

        assertTrue(board.moveBoatTo(0, 3));
        assertEquals(Board.GameState.PLAYING, board.getGameState());
        assertEquals(Board.VoyageStage.RETURNING, board.getVoyageStage());
    }

    @Test
    void winRequiresReturningToWestAfterRefuel() {
        boolean[][] grid = {
            {false, false, false, false, false},
            {true, true, true, true, true},
            {false, false, false, false, false}
        };
        Board board = new Board(grid);

        board.reveal(0, 0);
        board.reveal(0, 1);
        board.reveal(0, 2);
        board.reveal(0, 3);
        board.reveal(0, 4);

        assertTrue(board.moveBoatTo(0, 0));
        assertTrue(board.moveBoatTo(0, 1));
        assertTrue(board.moveBoatTo(0, 2));
        assertTrue(board.moveBoatTo(0, 3));
        assertEquals(Board.VoyageStage.RETURNING, board.getVoyageStage());

        assertTrue(board.moveBoatTo(0, 2));
        assertTrue(board.moveBoatTo(0, 1));
        assertEquals(Board.GameState.PLAYING, board.getGameState());

        assertTrue(board.moveBoatTo(0, 0));
        assertEquals(Board.GameState.WON, board.getGameState());
    }

    @Test
    void generatedBoardsAlwaysLeaveAtLeastOneFullSafeCrossingLane() {
        Board board = new Board(8, 18, 24, 0, new Random(0L));

        board.reveal(4, 4);

        boolean foundSafeLane = false;
        for (int row = 0; row < board.getRows(); row++) {
            boolean safeLane = true;
            for (int col = 0; col < board.getCols(); col++) {
                if (board.getCell(row, col).isMine()) {
                    safeLane = false;
                    break;
                }
            }
            if (safeLane) {
                foundSafeLane = true;
                break;
            }
        }

        assertTrue(foundSafeLane);
    }

    @Test
    void clearingRevealedRouteWithoutMovingBoatDoesNotWin() {
        boolean[][] grid = {
            {false, false, false, false},
            {true, true, true, true},
            {false, false, false, false}
        };
        Board board = new Board(grid);

        board.reveal(0, 0);
        board.reveal(0, 1);
        board.reveal(0, 2);
        board.reveal(0, 3);

        assertEquals(Board.GameState.PLAYING, board.getGameState());
        assertTrue(board.isBoatDocked());
    }

    @Test
    void gameOverWhenMineRevealed() {
        boolean[][] grid = {
            {true, false, false},
            {false, false, false},
            {false, false, false}
        };
        Board board = new Board(grid);

        board.reveal(0, 0);

        assertEquals(Board.GameState.LOST, board.getGameState());
    }

    @Test
    void noActionsAfterGameOver() {
        boolean[][] grid = {
            {true, false, false},
            {false, false, false},
            {false, false, false}
        };
        Board board = new Board(grid);

        board.reveal(0, 0);
        Cell.State stateBefore = board.getCell(1, 1).getState();
        board.toggleFlag(1, 1);

        assertEquals(stateBefore, board.getCell(1, 1).getState());
        assertFalse(board.moveBoatTo(1, 1));
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
                            int nr = r + dr;
                            int nc = c + dc;
                            if (nr >= 0 && nr < 9 && nc >= 0 && nc < 9 && board.getCell(nr, nc).isMine()) {
                                expected++;
                            }
                        }
                    }
                    assertEquals(expected, board.getCell(r, c).getAdjacentMines());
                }
            }
        }
    }

    @Test
    void triggeredMineCoordsRecordedOnLoss() {
        boolean[][] grid = {
            {true, false, false},
            {false, false, false},
            {false, false, false}
        };
        Board board = new Board(grid);

        board.reveal(0, 0);

        assertEquals(Board.GameState.LOST, board.getGameState());
        assertEquals(0, board.getTriggeredRow());
        assertEquals(0, board.getTriggeredCol());
    }

    @Test
    void triggeredMineCoordsDefaultToMinusOneBeforeGameStart() {
        Board board = new Board(9, 9, 10);
        assertEquals(-1, board.getTriggeredRow());
        assertEquals(-1, board.getTriggeredCol());
    }

    @Test
    void incorrectlyFlaggedCellBecomesWrongFlagOnLoss() {
        boolean[][] grid = {
            {true, false, false},
            {false, false, false},
            {false, false, false}
        };
        Board board = new Board(grid);

        board.toggleFlag(1, 1);
        board.reveal(0, 0);

        assertEquals(Board.GameState.LOST, board.getGameState());
        assertEquals(Cell.State.WRONG_FLAG, board.getCell(1, 1).getState());
    }

    @Test
    void correctFlagRemainsFlaggedAfterLoss() {
        boolean[][] grid = {
            {true, true, false},
            {false, false, false},
            {false, false, false}
        };
        Board board = new Board(grid);

        board.toggleFlag(0, 0);
        board.reveal(0, 1);

        assertEquals(Board.GameState.LOST, board.getGameState());
        assertEquals(Cell.State.FLAGGED, board.getCell(0, 0).getState());
    }

    private static boolean isAtLeastTwoCellsFromBoat(Board board, Board.MineLayerPosition position) {
        int boatRow = board.getBoatRow();
        int boatLeft = board.getBoatCol();
        int boatRight = boatLeft + 1;
        int closestCol = Math.max(boatLeft, Math.min(position.getCol(), boatRight));
        int rowDistance = Math.abs(position.getRow() - boatRow);
        int colDistance = Math.abs(position.getCol() - closestCol);
        return Math.max(rowDistance, colDistance) >= 2;
    }
}
