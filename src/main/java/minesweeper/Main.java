package minesweeper;

import javax.swing.SwingUtilities;

/**
 * Entry point for the Minesweeper application.
 */
public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MineSweeperUI ui = new MineSweeperUI();
            ui.setVisible(true);
        });
    }
}
