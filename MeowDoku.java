// ============================================================================
// MeowDoku Console Edition - Java Implementation
// MeowdoCrew | CS208 - Programming Languages
// ============================================================================
// A lightweight, single-player logic puzzle game played in the terminal.
// Place exactly one cat (C) per row, column, and region.
// No two cats may be adjacent (including diagonally).
// Use elimination marks (X) to help deduce the solution.
// ============================================================================

import java.util.Scanner;

public class MeowDoku {

    // -----------------------------------------------------------------------
    // Constants
    // -----------------------------------------------------------------------
    static final char EMPTY = '.';
    static final char CAT   = 'C';
    static final char MARK  = 'X';
    static final String REGION_LABELS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    // -----------------------------------------------------------------------
    // Puzzle data
    // -----------------------------------------------------------------------
    static final String[] PUZZLE_NAMES = {
        "Kitten's First Steps (5x5 Easy)",
        "Cat Nap Challenge (6x6 Medium)",
        "Meow Master (7x7 Hard)"
    };

    static final int[][][] PUZZLE_REGIONS = {
        // Puzzle 1: 5x5 Easy
        {
            {0, 0, 1, 1, 1},
            {0, 0, 0, 1, 1},
            {2, 2, 0, 3, 3},
            {2, 2, 4, 4, 3},
            {2, 4, 4, 4, 3}
        },
        // Puzzle 2: 6x6 Medium
        {
            {0, 0, 1, 1, 2, 2},
            {0, 0, 1, 1, 2, 2},
            {3, 3, 3, 4, 4, 2},
            {3, 5, 5, 4, 4, 4},
            {3, 5, 5, 5, 4, 4},
            {3, 5, 5, 5, 4, 4}
        },
        // Puzzle 3: 7x7 Hard
        {
            {0, 0, 0, 1, 1, 2, 2},
            {0, 3, 0, 1, 1, 2, 2},
            {3, 3, 3, 1, 4, 4, 2},
            {3, 3, 5, 5, 4, 4, 4},
            {6, 3, 5, 5, 5, 4, 4},
            {6, 6, 6, 5, 5, 5, 4},
            {6, 6, 6, 6, 5, 5, 5}
        }
    };

    // -----------------------------------------------------------------------
    // Game state
    // -----------------------------------------------------------------------
    private int size;
    private int[][] regionMap;
    private char[][] board;
    private int numRegions;

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------
    public MeowDoku(int puzzleIndex) {
        this.regionMap = PUZZLE_REGIONS[puzzleIndex];
        this.size = regionMap.length;
        this.board = new char[size][size];

        // Initialize board with empty cells
        for (int r = 0; r < size; r++)
            for (int c = 0; c < size; c++)
                board[r][c] = EMPTY;

        // Count regions
        numRegions = 0;
        for (int r = 0; r < size; r++)
            for (int c = 0; c < size; c++)
                if (regionMap[r][c] + 1 > numRegions)
                    numRegions = regionMap[r][c] + 1;
    }

    // -----------------------------------------------------------------------
    // Display the board
    // -----------------------------------------------------------------------
    public void display() {
        System.out.println();

        // Column header
        System.out.print("     ");
        for (int c = 0; c < size; c++)
            System.out.print(" " + (c + 1) + "  ");
        System.out.println();

        // Top border
        System.out.print("   +");
        for (int c = 0; c < size; c++)
            System.out.print("---+");
        System.out.println();

        for (int r = 0; r < size; r++) {
            // Row label
            System.out.print(" " + (char)('A' + r) + " |");
            for (int c = 0; c < size; c++) {
                char cellChar = board[r][c];
                if (cellChar == CAT) {
                    System.out.print(" C |");
                } else if (cellChar == MARK) {
                    System.out.print(" X |");
                } else {
                    System.out.print(" " + REGION_LABELS.charAt(regionMap[r][c]) + " |");
                }
            }
            System.out.println();

            // Row border
            System.out.print("   +");
            for (int c = 0; c < size; c++)
                System.out.print("---+");
            System.out.println();
        }

        // Region legend
        System.out.print("\n   Regions: ");
        for (int i = 0; i < numRegions; i++) {
            if (i > 0) System.out.print(", ");
            System.out.print(REGION_LABELS.charAt(i));
        }
        System.out.println();
    }

    // -----------------------------------------------------------------------
    // Bounds check
    // -----------------------------------------------------------------------
    private boolean inBounds(int row, int col) {
        return row >= 0 && row < size && col >= 0 && col < size;
    }

    // -----------------------------------------------------------------------
    // Place a cat (toggle)
    // -----------------------------------------------------------------------
    public boolean placeCat(int row, int col) {
        if (!inBounds(row, col)) {
            System.out.println("   [!] Position out of bounds.");
            return false;
        }
        if (board[row][col] == CAT) {
            System.out.println("   [!] A cat is already here! Removing it.");
            board[row][col] = EMPTY;
            return true;
        }
        board[row][col] = CAT;
        return true;
    }

    // -----------------------------------------------------------------------
    // Place an elimination mark (toggle)
    // -----------------------------------------------------------------------
    public boolean placeMark(int row, int col) {
        if (!inBounds(row, col)) {
            System.out.println("   [!] Position out of bounds.");
            return false;
        }
        if (board[row][col] == CAT) {
            System.out.println("   [!] Remove the cat first before marking.");
            return false;
        }
        if (board[row][col] == MARK) {
            System.out.println("   [!] Mark removed.");
            board[row][col] = EMPTY;
            return true;
        }
        board[row][col] = MARK;
        return true;
    }

    // -----------------------------------------------------------------------
    // Clear a cell
    // -----------------------------------------------------------------------
    public boolean clearCell(int row, int col) {
        if (!inBounds(row, col)) {
            System.out.println("   [!] Position out of bounds.");
            return false;
        }
        board[row][col] = EMPTY;
        return true;
    }

    // -----------------------------------------------------------------------
    // Reset the board
    // -----------------------------------------------------------------------
    public void resetBoard() {
        for (int r = 0; r < size; r++)
            for (int c = 0; c < size; c++)
                board[r][c] = EMPTY;
        System.out.println("   Board has been reset.");
    }

    // -----------------------------------------------------------------------
    // Validate the board; returns true if the puzzle is solved
    // -----------------------------------------------------------------------
    public boolean validate(boolean verbose) {
        boolean valid = true;
        int totalCats = 0;

        // Check rows
        for (int r = 0; r < size; r++) {
            int count = 0;
            for (int c = 0; c < size; c++)
                if (board[r][c] == CAT) count++;
            totalCats += count;
            if (count > 1) {
                if (verbose)
                    System.out.println("   [!] Row " + (char)('A' + r) + " has more than one cat!");
                valid = false;
            }
        }

        // Check columns
        for (int c = 0; c < size; c++) {
            int count = 0;
            for (int r = 0; r < size; r++)
                if (board[r][c] == CAT) count++;
            if (count > 1) {
                if (verbose)
                    System.out.println("   [!] Column " + (c + 1) + " has more than one cat!");
                valid = false;
            }
        }

        // Check regions
        int[] regionCount = new int[numRegions];
        for (int r = 0; r < size; r++)
            for (int c = 0; c < size; c++)
                if (board[r][c] == CAT)
                    regionCount[regionMap[r][c]]++;
        for (int i = 0; i < numRegions; i++) {
            if (regionCount[i] > 1) {
                if (verbose)
                    System.out.println("   [!] Region " + REGION_LABELS.charAt(i) + " has more than one cat!");
                valid = false;
            }
        }

        // Check adjacency
        int[] dr = {-1, -1, -1, 0, 0, 1, 1, 1};
        int[] dc = {-1, 0, 1, -1, 1, -1, 0, 1};
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                if (board[r][c] == CAT) {
                    for (int d = 0; d < 8; d++) {
                        int nr = r + dr[d];
                        int nc = c + dc[d];
                        if (inBounds(nr, nc) && board[nr][nc] == CAT) {
                            if (verbose)
                                System.out.println("   [!] Cats at " + (char)('A' + r) + (c + 1)
                                    + " and " + (char)('A' + nr) + (nc + 1) + " are adjacent!");
                            valid = false;
                        }
                    }
                }
            }
        }

        // Win check
        if (valid && totalCats == size) {
            return true;
        }

        if (valid && totalCats < size && verbose) {
            System.out.println("   [i] No rule violations so far. Place "
                + (size - totalCats) + " more cat(s) to win!");
        }

        return false;
    }

    // -----------------------------------------------------------------------
    // Parse coordinate (e.g., "A1" -> row=0, col=0)
    // Returns int[]{row, col} or null on failure
    // -----------------------------------------------------------------------
    public static int[] parseCoord(String input, int size) {
        input = input.trim().toUpperCase();
        if (input.length() < 2) return null;

        char rowChar = input.charAt(0);
        if (rowChar < 'A' || rowChar >= 'A' + size) return null;

        String colStr = input.substring(1);
        try {
            int col = Integer.parseInt(colStr) - 1;
            if (col < 0 || col >= size) return null;
            return new int[]{rowChar - 'A', col};
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // -----------------------------------------------------------------------
    // Display title
    // -----------------------------------------------------------------------
    public static void displayTitle() {
        System.out.println();
        System.out.println("  +====================================================+");
        System.out.println("  |                                                    |");
        System.out.println("  |      /\\_/\\    M E O W D O K U                      |");
        System.out.println("  |     ( o.o )   Console Edition                      |");
        System.out.println("  |      > ^ <                                         |");
        System.out.println("  |     /|   |\\   A Cat Placement Puzzle               |");
        System.out.println("  |                                                    |");
        System.out.println("  |   MeowdoCrew | CS208 - Programming Languages      |");
        System.out.println("  |                                                    |");
        System.out.println("  +====================================================+");
        System.out.println();
    }

    // -----------------------------------------------------------------------
    // Display rules
    // -----------------------------------------------------------------------
    public static void displayRules() {
        System.out.println();
        System.out.println("  ===== HOW TO PLAY =====");
        System.out.println();
        System.out.println("  Goal: Place exactly ONE cat (C) in each row, column,");
        System.out.println("        and region of the grid.");
        System.out.println();
        System.out.println("  Rules:");
        System.out.println("    1. Each ROW must contain exactly one cat.");
        System.out.println("    2. Each COLUMN must contain exactly one cat.");
        System.out.println("    3. Each REGION (marked by letters A, B, C...) must");
        System.out.println("       contain exactly one cat.");
        System.out.println("    4. No two cats can be ADJACENT to each other");
        System.out.println("       (horizontally, vertically, or diagonally).");
        System.out.println();
        System.out.println("  Commands:");
        System.out.println("    c <pos>   - Place/remove a cat (e.g., c A1)");
        System.out.println("    x <pos>   - Place/remove an elimination mark (e.g., x B3)");
        System.out.println("    e <pos>   - Clear a cell (e.g., e C2)");
        System.out.println("    check     - Validate your current board");
        System.out.println("    reset     - Clear the entire board");
        System.out.println("    rules     - Show these rules again");
        System.out.println("    quit      - Exit the game");
        System.out.println();
    }

    // -----------------------------------------------------------------------
    // Display win message
    // -----------------------------------------------------------------------
    public static void displayWin() {
        System.out.println();
        System.out.println("  +============================================+");
        System.out.println("  |                                            |");
        System.out.println("  |    /\\_/\\   CONGRATULATIONS!                |");
        System.out.println("  |   ( ^.^ )  You solved the puzzle!         |");
        System.out.println("  |    > ^ <   All cats are happy! =^.^=      |");
        System.out.println("  |                                            |");
        System.out.println("  +============================================+");
    }

    // -----------------------------------------------------------------------
    // Main
    // -----------------------------------------------------------------------
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        displayTitle();

        // Puzzle selection
        System.out.println("  Select a puzzle:");
        System.out.println();
        for (int i = 0; i < PUZZLE_NAMES.length; i++) {
            System.out.println("    " + (i + 1) + ". " + PUZZLE_NAMES[i]);
        }
        System.out.print("\n  Enter choice (1-" + PUZZLE_NAMES.length + "): ");

        int choice;
        try {
            choice = Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            choice = 0;
        }

        if (choice < 1 || choice > PUZZLE_NAMES.length) {
            System.out.println("  Invalid choice. Defaulting to puzzle 1.");
            choice = 1;
        }

        MeowDoku game = new MeowDoku(choice - 1);
        System.out.println("\n  Loading: " + PUZZLE_NAMES[choice - 1]);
        displayRules();

        // Game loop
        boolean running = true;
        while (running) {
            game.display();
            System.out.print("\n  >> ");

            if (!scanner.hasNextLine()) break;
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) continue;

            String lineLower = line.toLowerCase();

            if (lineLower.equals("quit") || lineLower.equals("exit") || lineLower.equals("q")) {
                System.out.println("\n  Thanks for playing MeowDoku! Goodbye! =^.^=\n");
                running = false;

            } else if (lineLower.equals("rules") || lineLower.equals("help") || lineLower.equals("h")) {
                displayRules();

            } else if (lineLower.equals("reset")) {
                game.resetBoard();

            } else if (lineLower.equals("check") || lineLower.equals("validate")) {
                System.out.println();
                if (game.validate(true)) {
                    displayWin();
                    System.out.print("\n  Play again? (y/n): ");
                    String answer = scanner.hasNextLine() ? scanner.nextLine().trim() : "n";
                    if (answer.isEmpty() || Character.toLowerCase(answer.charAt(0)) != 'y') {
                        running = false;
                    } else {
                        game.resetBoard();
                    }
                }

            } else if (lineLower.length() >= 3 && " ".equals(lineLower.substring(1, 2))
                       && "cxe".indexOf(lineLower.charAt(0)) >= 0) {

                char cmd = lineLower.charAt(0);
                String coordStr = line.substring(2).trim();
                int[] coord = parseCoord(coordStr, game.size);

                if (coord != null) {
                    boolean success = false;
                    switch (cmd) {
                        case 'c': success = game.placeCat(coord[0], coord[1]); break;
                        case 'x': success = game.placeMark(coord[0], coord[1]); break;
                        case 'e': success = game.clearCell(coord[0], coord[1]); break;
                    }

                    // Auto-check for win after placing a cat
                    if (success && cmd == 'c' && game.validate(false)) {
                        game.display();
                        displayWin();
                        System.out.print("\n  Play again? (y/n): ");
                        String answer = scanner.hasNextLine() ? scanner.nextLine().trim() : "n";
                        if (answer.isEmpty() || Character.toLowerCase(answer.charAt(0)) != 'y') {
                            running = false;
                        } else {
                            game.resetBoard();
                        }
                    }
                } else {
                    System.out.println("   [!] Invalid position. Use format like A1, B3, etc.");
                }

            } else {
                System.out.println("   [!] Unknown command. Type 'rules' for help.");
            }
        }

        scanner.close();
    }
}
