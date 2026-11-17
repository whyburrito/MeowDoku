import java.io.BufferedReader;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
public class MeowDoku {
    private static final String RESET = "\u001B[0m";
    private static final String BOLD = "\u001B[1m";
    private static final String DIM = "\u001B[2m";
    private static final String WHITE = "\u001B[97m";
    private static final String CYAN = "\u001B[96m";
    private static final String BLUE = "\u001B[94m";
    private static final String GREEN = "\u001B[92m";
    private static final String YELLOW = "\u001B[93m";
    private static final String RED = "\u001B[91m";
    private static final String MAGENTA = "\u001B[95m";
    private static final String GRAY = "\u001B[90m";
    private static final String SELECTED_BG = "\u001B[48;5;57m";
    private static final String BORDER_COLOR = "\u001B[38;5;39m";
    private static final String[] REGION_COLORS = {
        "\u001B[38;5;45m",
        "\u001B[38;5;213m",
        "\u001B[38;5;220m",
        "\u001B[38;5;114m",
        "\u001B[38;5;141m",
        "\u001B[38;5;208m",
        "\u001B[38;5;81m"
    };
    private static final char EMPTY = '.';
    private static final char CAT = 'C';
    private static final char MARK = 'X';
    private static final String CAT_EMOJI = "\uD83D\uDC31";
    private static final String REGION_LABELS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String GAME_KEYS = "WASDCXEVRHQ";
    private static final BufferedReader CONSOLE_READER =
            new BufferedReader(new InputStreamReader(System.in));
    private static final boolean IS_WINDOWS =
            System.getProperty("os.name", "").toLowerCase().contains("win");
    private static final String[] PUZZLE_NAMES = {
        "Kitten's First Steps (5x5 Easy)",
        "Cat Nap Challenge (6x6 Medium)",
        "Meow Master (7x7 Hard)"
    };
    private static final int[][][] PUZZLE_REGIONS = {
        {
            {0, 0, 1, 1, 1},
            {0, 0, 0, 1, 1},
            {2, 2, 0, 3, 3},
            {2, 2, 4, 4, 3},
            {2, 4, 4, 4, 3}
        },
        {
            {0, 0, 1, 1, 2, 2},
            {0, 0, 1, 1, 2, 2},
            {3, 3, 3, 4, 4, 2},
            {3, 5, 5, 4, 4, 4},
            {3, 5, 5, 5, 4, 4},
            {3, 5, 5, 5, 4, 4}
        },
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
    private final int size;
    private final int[][] regionMap;
    private final char[][] board;
    private final int numRegions;
    private int cursorRow = 0;
    private int cursorCol = 0;
    private boolean running = true;
    private String statusMessage = "Welcome! Use W/A/S/D to move, then C to place a cat.";
    private String statusColor = CYAN;
    private MeowDoku(int puzzleIndex) {
        if (puzzleIndex < 0 || puzzleIndex >= PUZZLE_REGIONS.length) {
            throw new IllegalArgumentException("Invalid puzzle index.");
        }
        regionMap = PUZZLE_REGIONS[puzzleIndex];
        size = regionMap.length;
        board = new char[size][size];
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                board[row][col] = EMPTY;
            }
        }
        int highestRegion = -1;
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                highestRegion = Math.max(highestRegion, regionMap[row][col]);
            }
        }
        numRegions = highestRegion + 1;
    }
    private String getCellDisplay(int row, int col) {
        if (board[row][col] == CAT) {
            return CAT_EMOJI;
        }
        if (board[row][col] == MARK) {
            return "X";
        }
        return String.valueOf(REGION_LABELS.charAt(regionMap[row][col]));
    }
    private String formatCell(String symbol) {
        if (CAT_EMOJI.equals(symbol)) {
            return " " + symbol + "  ";
        }
        return "  " + symbol + "  ";
    }
    private static void clearScreen() {
        System.out.print("\u001B[2J\u001B[H");
        System.out.flush();
    }
    private static void displayTitle() {
        System.out.println(CYAN + BOLD + "  +------------------------------------------------------+" + RESET);
        System.out.println(CYAN + BOLD + "  |" + RESET + "                                                      " + CYAN + BOLD + "|" + RESET);
        System.out.println(CYAN + BOLD + "  |" + RESET + GREEN + "       /\\_/\\       M E O W D O K U                 " + CYAN + BOLD + "|" + RESET);
        System.out.println(CYAN + BOLD + "  |" + RESET + GREEN + "      ( o.o )      " + MAGENTA + BOLD + "COLOR TERMINAL EDITION" + RESET + "   " + CYAN + BOLD + "|" + RESET);
        System.out.println(CYAN + BOLD + "  |" + RESET + GREEN + "       > ^ <       " + WHITE + "A Cat Placement Puzzle" + RESET + "      " + CYAN + BOLD + "|" + RESET);
        System.out.println(CYAN + BOLD + "  |" + RESET + "                                                      " + CYAN + BOLD + "|" + RESET);
        System.out.println(CYAN + BOLD + "  +------------------------------------------------------+" + RESET);
        System.out.println();
    }
    private void displayBoard() {
        System.out.println(CYAN + BOLD + "  MEOWDOKU" + RESET + WHITE + "  |  " + size + "x" + size + " PUZZLE" + RESET);
        System.out.println(DIM + "  Move the purple selection with W/A/S/D, then choose an action." + RESET);
        System.out.println();
        System.out.print("       ");
        for (int col = 0; col < size; col++) {
            System.out.printf(BLUE + BOLD + " %2d  " + RESET, col + 1);
        }
        System.out.println();
        printBorder();
        for (int row = 0; row < size; row++) {
            System.out.print("  " + MAGENTA + BOLD + (char) ('A' + row) + RESET + "  " + BORDER_COLOR + "|" + RESET);
            for (int col = 0; col < size; col++) {
                String symbol = getCellDisplay(row, col);
                String symbolColor = getSymbolColor(row, col);
                if (row == cursorRow && col == cursorCol) {
                    System.out.print(SELECTED_BG + WHITE + BOLD + formatCell(symbol) + RESET);
                } else {
                    System.out.print(symbolColor + BOLD + formatCell(symbol) + RESET);
                }
                System.out.print(BORDER_COLOR + "|" + RESET);
            }
            System.out.println();
            printBorder();
        }
        System.out.print("\n  " + WHITE + BOLD + "REGIONS  " + RESET);
        for (int i = 0; i < numRegions; i++) {
            if (i > 0) System.out.print(GRAY + "  " + RESET);
            System.out.print(REGION_COLORS[i % REGION_COLORS.length] + BOLD
                    + REGION_LABELS.charAt(i) + RESET);
        }
        System.out.println();
        System.out.println("\n" + CYAN + BOLD + "  CONTROLS" + RESET);
        System.out.println("  " + YELLOW + "W / A / S / D" + RESET + "  Move selection (up / left / down / right)");
        System.out.println("  " + GREEN + "C" + RESET + "              Place or remove a " + CAT_EMOJI + " cat");
        System.out.println("  " + YELLOW + "X" + RESET + "              Place or remove an elimination mark");
        System.out.println("  " + RED + "E" + RESET + "              Clear the selected cell");
        System.out.println("  " + CYAN + "V" + RESET + "              Check the puzzle");
        System.out.println("  " + MAGENTA + "R" + RESET + "              Reset the board");
        System.out.println("  " + BLUE + "H" + RESET + "              Show the rules");
        System.out.println("  " + GRAY + "Q" + RESET + "              Quit the game");
        System.out.println();
        System.out.println("  " + WHITE + BOLD + "STATUS  " + RESET + statusColor + statusMessage + RESET);
        System.out.print("\n  " + DIM + "Press a key: " + RESET);
        System.out.flush();
    }
    private void printBorder() {
        System.out.print("     " + BORDER_COLOR + "+" + RESET);
        for (int col = 0; col < size; col++) {
            System.out.print(BORDER_COLOR + "-----+" + RESET);
        }
        System.out.println();
    }
    private String getSymbolColor(int row, int col) {
        if (board[row][col] == CAT) return GREEN;
        if (board[row][col] == MARK) return YELLOW;
        return REGION_COLORS[regionMap[row][col] % REGION_COLORS.length];
    }
    private void setStatus(String message, String color) {
        statusMessage = message;
        statusColor = color;
    }
    private void moveCursor(int rowChange, int colChange) {
        int oldRow = cursorRow;
        int oldCol = cursorCol;
        cursorRow = Math.max(0, Math.min(size - 1, cursorRow + rowChange));
        cursorCol = Math.max(0, Math.min(size - 1, cursorCol + colChange));
        if (oldRow == cursorRow && oldCol == cursorCol) {
            setStatus("You are at the edge of the board.", YELLOW);
        } else {
            setStatus("Selected cell: " + coordinate(cursorRow, cursorCol), CYAN);
        }
    }
    private void placeCat(int row, int col) {
        if (board[row][col] == CAT) {
            board[row][col] = EMPTY;
            setStatus("Cat removed from " + coordinate(row, col) + ".", YELLOW);
        } else {
            board[row][col] = CAT;
            setStatus("Cat placed at " + coordinate(row, col) + ".", GREEN);
        }
    }
    private void placeMark(int row, int col) {
        if (board[row][col] == CAT) {
            setStatus("Remove the cat before placing a mark.", RED);
            return;
        }
        if (board[row][col] == MARK) {
            board[row][col] = EMPTY;
            setStatus("Mark removed from " + coordinate(row, col) + ".", YELLOW);
        } else {
            board[row][col] = MARK;
            setStatus("Elimination mark placed at " + coordinate(row, col) + ".", YELLOW);
        }
    }
    private void clearCell(int row, int col) {
        board[row][col] = EMPTY;
        setStatus("Cell " + coordinate(row, col) + " cleared.", CYAN);
    }
    private void resetBoard() {
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                board[row][col] = EMPTY;
            }
        }
        cursorRow = 0;
        cursorCol = 0;
        setStatus("Board has been reset.", GREEN);
    }
    private String coordinate(int row, int col) {
        return "" + (char) ('A' + row) + (col + 1);
    }
    private boolean validate(boolean verbose) {
        boolean valid = true;
        int totalCats = 0;
        for (int row = 0; row < size; row++) {
            int count = 0;
            for (int col = 0; col < size; col++) {
                if (board[row][col] == CAT) count++;
            }
            totalCats += count;
            if (count > 1) {
                if (verbose) System.out.println(RED + "  [!] Row " + (char) ('A' + row)
                        + " has more than one cat." + RESET);
                valid = false;
            }
        }
        for (int col = 0; col < size; col++) {
            int count = 0;
            for (int row = 0; row < size; row++) {
                if (board[row][col] == CAT) count++;
            }
            if (count > 1) {
                if (verbose) System.out.println(RED + "  [!] Column " + (col + 1)
                        + " has more than one cat." + RESET);
                valid = false;
            }
        }
        int[] regionCount = new int[numRegions];
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                if (board[row][col] == CAT) regionCount[regionMap[row][col]]++;
            }
        }
        for (int region = 0; region < numRegions; region++) {
            if (regionCount[region] > 1) {
                if (verbose) System.out.println(RED + "  [!] Region "
                        + REGION_LABELS.charAt(region) + " has more than one cat." + RESET);
                valid = false;
            }
        }
        int[][] directions = {{0, 1}, {1, -1}, {1, 0}, {1, 1}};
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                if (board[row][col] != CAT) continue;
                for (int[] direction : directions) {
                    int nextRow = row + direction[0];
                    int nextCol = col + direction[1];
                    if (nextRow >= 0 && nextRow < size
                            && nextCol >= 0 && nextCol < size
                            && board[nextRow][nextCol] == CAT) {
                        if (verbose) System.out.println(RED + "  [!] Cats at "
                                + coordinate(row, col) + " and "
                                + coordinate(nextRow, nextCol) + " are adjacent." + RESET);
                        valid = false;
                    }
                }
            }
        }
        if (valid && totalCats == size) return true;
        if (verbose) {
            if (valid) {
                System.out.println(GREEN + "  [i] No rule violations. Place "
                        + (size - totalCats) + " more cat(s) to win." + RESET);
            } else {
                System.out.println(YELLOW + "  [i] Fix the rule violations and check again." + RESET);
            }
        }
        return false;
    }
    private void displayRules() {
        clearScreen();
        displayTitle();
        System.out.println(CYAN + BOLD + "  HOW TO PLAY" + RESET);
        System.out.println("  1. Place exactly " + GREEN + "ONE " + CAT_EMOJI + " cat" + RESET + " in each row.");
        System.out.println("  2. Place exactly " + GREEN + "ONE " + CAT_EMOJI + " cat" + RESET + " in each column.");
        System.out.println("  3. Place exactly " + GREEN + "ONE " + CAT_EMOJI + " cat" + RESET + " in each region.");
        System.out.println("  4. No two cats may touch, including diagonally.");
        System.out.println();
        System.out.println("  Empty cells show their region letter.");
        System.out.println("  The selected cell has a purple background.");
        System.out.println();
        System.out.println(DIM + "  Press any game-control key to return to the puzzle." + RESET);
        readKey(GAME_KEYS);
    }
    private void handleWin() {
        clearScreen();
        displayTitle();
        displayBoardWithoutControls();
        System.out.println();
        System.out.println(GREEN + BOLD + "  +----------------------------------------------+");
        System.out.println("  |             CONGRATULATIONS!                 |");
        System.out.println("  |          You solved the puzzle!              |");
        System.out.println("  |           All cats are happy!                |");
        System.out.println("  +----------------------------------------------+" + RESET);
        System.out.println();
        System.out.print(WHITE + BOLD + "  Play this puzzle again? (Y/N): " + RESET);
        System.out.flush();
        char answer = readKey("YN");
        if (answer == 'Y') {
            resetBoard();
        } else {
            running = false;
        }
    }
    private void displayBoardWithoutControls() {
        System.out.println(CYAN + BOLD + "  SOLVED PUZZLE" + RESET);
        System.out.print("       ");
        for (int col = 0; col < size; col++) {
            System.out.printf(BLUE + BOLD + " %2d  " + RESET, col + 1);
        }
        System.out.println();
        printBorder();
        for (int row = 0; row < size; row++) {
            System.out.print("  " + MAGENTA + BOLD + (char) ('A' + row) + RESET + "  " + BORDER_COLOR + "|" + RESET);
            for (int col = 0; col < size; col++) {
                String symbol = getCellDisplay(row, col);
                System.out.print(getSymbolColor(row, col) + BOLD + formatCell(symbol) + RESET);
                System.out.print(BORDER_COLOR + "|" + RESET);
            }
            System.out.println();
            printBorder();
        }
    }
    private void play() {
        running = true;
        while (running) {
            clearScreen();
            displayTitle();
            displayBoard();
            char key = readKey(GAME_KEYS);
            switch (key) {
                case 'W': moveCursor(-1, 0); break;
                case 'A': moveCursor(0, -1); break;
                case 'S': moveCursor(1, 0); break;
                case 'D': moveCursor(0, 1); break;
                case 'C':
                    placeCat(cursorRow, cursorCol);
                    if (validate(false)) handleWin();
                    break;
                case 'X': placeMark(cursorRow, cursorCol); break;
                case 'E': clearCell(cursorRow, cursorCol); break;
                case 'V':
                    if (validate(false)) {
                        handleWin();
                    } else {
                        showCheckResults();
                    }
                    break;
                case 'R': resetBoard(); break;
                case 'H':
                    displayRules();
                    setStatus("Returned to the puzzle.", CYAN);
                    break;
                case 'Q': running = false; break;
                default: setStatus("Unknown key.", RED); break;
            }
        }
    }
    private void showCheckResults() {
        clearScreen();
        displayTitle();
        displayBoardWithoutControls();
        System.out.println("\n" + CYAN + BOLD + "  CHECK RESULTS" + RESET);
        validate(true);
        System.out.println();
        System.out.print(DIM + "  Press any game-control key to continue..." + RESET);
        System.out.flush();
        readKey(GAME_KEYS);
    }
    private static char readKey(String allowedKeys) {
        if (IS_WINDOWS) {
            try {
                ProcessBuilder builder = new ProcessBuilder(
                        "cmd.exe", "/c", "choice", "/n", "/c", allowedKeys
                );
                builder.redirectInput(ProcessBuilder.Redirect.INHERIT);
                builder.redirectOutput(ProcessBuilder.Redirect.DISCARD);
                builder.redirectError(ProcessBuilder.Redirect.DISCARD);
                int exitCode = builder.start().waitFor();
                if (exitCode >= 1 && exitCode <= allowedKeys.length()) {
                    return allowedKeys.charAt(exitCode - 1);
                }
            } catch (IOException exception) {
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return '\0';
            }
        }
        try {
            String line = CONSOLE_READER.readLine();
            if (line == null || line.trim().isEmpty()) return '\0';
            char key = Character.toUpperCase(line.trim().charAt(0));
            return allowedKeys.indexOf(key) >= 0 ? key : '\0';
        } catch (IOException exception) {
            return '\0';
        }
    }
    private static int selectPuzzle() {
        while (true) {
            clearScreen();
            displayTitle();
            System.out.println(CYAN + BOLD + "  SELECT A PUZZLE" + RESET + "\n");
            for (int i = 0; i < PUZZLE_NAMES.length; i++) {
                String difficultyColor = i == 0 ? GREEN : (i == 1 ? YELLOW : RED);
                System.out.println("  " + WHITE + (i + 1) + ". " + RESET
                        + difficultyColor + PUZZLE_NAMES[i] + RESET);
            }
            System.out.println("  " + GRAY + "Q. Quit" + RESET);
            System.out.println();
            System.out.print("  " + WHITE + BOLD + "Press 1, 2, 3, or Q: " + RESET);
            System.out.flush();
            char choice = readKey("123Q");
            if (choice == 'Q') return -1;
            if (choice >= '1' && choice <= '3') return choice - '1';
        }
    }
    private static void configureWindowsUtf8CodePage() {
        if (!IS_WINDOWS) {
            return;
        }
        try {
            Process process = new ProcessBuilder(
                    "cmd.exe", "/c", "chcp 65001 > nul")
                    .inheritIO()
                    .start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                System.err.println("Warning: could not set Windows code page to UTF-8.");
            }
        } catch (IOException exception) {
            System.err.println("Warning: could not run chcp to enable UTF-8.");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            System.err.println("Warning: UTF-8 console setup was interrupted.");
        }
    }
    public static void main(String[] args) {
        configureWindowsUtf8CodePage();
        try {
            System.setOut(new PrintStream(
                    new FileOutputStream(FileDescriptor.out), true, "UTF-8"));
            System.setErr(new PrintStream(
                    new FileOutputStream(FileDescriptor.err), true, "UTF-8"));
        } catch (UnsupportedEncodingException exception) {
            System.err.println("UTF-8 is not available on this Java installation.");
        }
        int puzzleIndex = selectPuzzle();
        if (puzzleIndex == -1) {
            System.out.println(GREEN + "Thanks for playing MeowDoku. Goodbye! =^.^=" + RESET);
            return;
        }
        MeowDoku game = new MeowDoku(puzzleIndex);
        game.play();
        clearScreen();
        displayTitle();
        System.out.println(GREEN + BOLD + "  Thanks for playing MeowDoku! Goodbye! =^.^=" + RESET);
    }
}
