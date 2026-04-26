package minesweeper;

/**
 * Represents a single cell on the Minesweeper board.
 */
public class Cell {

    public enum State {
        HIDDEN,
        REVEALED,
        FLAGGED,
        QUESTION
    }

    private boolean mine;
    private int adjacentMines;
    private State state;

    public Cell() {
        this.mine = false;
        this.adjacentMines = 0;
        this.state = State.HIDDEN;
    }

    public boolean isMine() {
        return mine;
    }

    public void setMine(boolean mine) {
        this.mine = mine;
    }

    public int getAdjacentMines() {
        return adjacentMines;
    }

    public void setAdjacentMines(int adjacentMines) {
        this.adjacentMines = adjacentMines;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public boolean isRevealed() {
        return state == State.REVEALED;
    }

    public boolean isFlagged() {
        return state == State.FLAGGED;
    }

    public boolean isHidden() {
        return state == State.HIDDEN;
    }
}
