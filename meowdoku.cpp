// ============================================================================
// MeowDoku Console Edition - C++ Implementation
// MeowdoCrew | CS208 - Programming Languages
// ============================================================================
// A lightweight, single-player logic puzzle game played in the terminal.
// Place exactly one cat (C) per row, column, and region.
// No two cats may be adjacent (including diagonally).
// Use elimination marks (X) to help deduce the solution.
// ============================================================================

#include <iostream>
#include <vector>
#include <string>
#include <sstream>
#include <algorithm>
#include <cstdlib>
#include <cctype>
#include <ctime>

// ---------------------------------------------------------------------------
// Constants
// ---------------------------------------------------------------------------
const char EMPTY = '.';
const char CAT   = 'C';
const char MARK  = 'X';

// Region labels used for display (letters A-Z for up to 26 regions)
const std::string REGION_LABELS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

// ---------------------------------------------------------------------------
// Puzzle definition: each puzzle has a size and a region map
// ---------------------------------------------------------------------------
struct Puzzle {
    int size;
    std::string name;
    std::vector<std::vector<int>> regionMap; // regionMap[row][col] = region id
};

// ---------------------------------------------------------------------------
// Pre-built puzzles
// ---------------------------------------------------------------------------
std::vector<Puzzle> createPuzzles() {
    std::vector<Puzzle> puzzles;

    // Puzzle 1: 5x5 Easy
    {
        Puzzle p;
        p.size = 5;
        p.name = "Kitten's First Steps (5x5 Easy)";
        p.regionMap = {
            {0, 0, 1, 1, 1},
            {0, 0, 0, 1, 1},
            {2, 2, 0, 3, 3},
            {2, 2, 4, 4, 3},
            {2, 4, 4, 4, 3}
        };
        puzzles.push_back(p);
    }

    // Puzzle 2: 6x6 Medium
    {
        Puzzle p;
        p.size = 6;
        p.name = "Cat Nap Challenge (6x6 Medium)";
        p.regionMap = {
            {0, 0, 1, 1, 2, 2},
            {0, 0, 1, 1, 2, 2},
            {3, 3, 3, 4, 4, 2},
            {3, 5, 5, 4, 4, 4},
            {3, 5, 5, 5, 4, 4},
            {3, 5, 5, 5, 4, 4}
        };
        puzzles.push_back(p);
    }

    // Puzzle 3: 7x7 Hard
    {
        Puzzle p;
        p.size = 7;
        p.name = "Meow Master (7x7 Hard)";
        p.regionMap = {
            {0, 0, 0, 1, 1, 2, 2},
            {0, 3, 0, 1, 1, 2, 2},
            {3, 3, 3, 1, 4, 4, 2},
            {3, 3, 5, 5, 4, 4, 4},
            {6, 3, 5, 5, 5, 4, 4},
            {6, 6, 6, 5, 5, 5, 4},
            {6, 6, 6, 6, 5, 5, 5}
        };
        puzzles.push_back(p);
    }

    return puzzles;
}

// ---------------------------------------------------------------------------
// Game Board class
// ---------------------------------------------------------------------------
class MeowdokuBoard {
private:
    int size;
    std::vector<std::vector<int>> regionMap;
    std::vector<std::vector<char>> board;    // player's board
    int numRegions;

public:
    MeowdokuBoard(int sz, const std::vector<std::vector<int>>& regions)
        : size(sz), regionMap(regions)
    {
        board.assign(size, std::vector<char>(size, EMPTY));
        numRegions = 0;
        for (int r = 0; r < size; r++)
            for (int c = 0; c < size; c++)
                if (regionMap[r][c] + 1 > numRegions)
                    numRegions = regionMap[r][c] + 1;
    }

    int getSize() const { return size; }

    // Display the board with region labels and player marks
    void display() const {
        // Column header
        std::cout << "\n     ";
        for (int c = 0; c < size; c++)
            std::cout << " " << (c + 1) << "  ";
        std::cout << "\n";

        // Top border
        std::cout << "   +";
        for (int c = 0; c < size; c++)
            std::cout << "---+";
        std::cout << "\n";

        for (int r = 0; r < size; r++) {
            // Row label
            std::cout << " " << (char)('A' + r) << " |";
            for (int c = 0; c < size; c++) {
                char cellChar = board[r][c];
                char regionLabel = REGION_LABELS[regionMap[r][c]];
                if (cellChar == CAT) {
                    std::cout << " C |";
                } else if (cellChar == MARK) {
                    std::cout << " X |";
                } else {
                    std::cout << " " << regionLabel << " |";
                }
            }
            std::cout << "\n";

            // Row border
            std::cout << "   +";
            for (int c = 0; c < size; c++)
                std::cout << "---+";
            std::cout << "\n";
        }

        // Region legend
        std::cout << "\n   Regions: ";
        for (int i = 0; i < numRegions; i++) {
            if (i > 0) std::cout << ", ";
            std::cout << REGION_LABELS[i];
        }
        std::cout << "\n";
    }

    // Place a cat at (row, col) - returns true if placed, false if invalid
    bool placeCat(int row, int col) {
        if (!inBounds(row, col)) {
            std::cout << "   [!] Position out of bounds.\n";
            return false;
        }
        if (board[row][col] == CAT) {
            std::cout << "   [!] A cat is already here! Removing it.\n";
            board[row][col] = EMPTY;
            return true;
        }
        board[row][col] = CAT;
        return true;
    }

    // Place an elimination mark at (row, col)
    bool placeMark(int row, int col) {
        if (!inBounds(row, col)) {
            std::cout << "   [!] Position out of bounds.\n";
            return false;
        }
        if (board[row][col] == CAT) {
            std::cout << "   [!] Remove the cat first before marking.\n";
            return false;
        }
        if (board[row][col] == MARK) {
            std::cout << "   [!] Mark removed.\n";
            board[row][col] = EMPTY;
            return true;
        }
        board[row][col] = MARK;
        return true;
    }

    // Clear a cell
    bool clearCell(int row, int col) {
        if (!inBounds(row, col)) {
            std::cout << "   [!] Position out of bounds.\n";
            return false;
        }
        board[row][col] = EMPTY;
        return true;
    }

    // Reset the entire board
    void resetBoard() {
        for (int r = 0; r < size; r++)
            for (int c = 0; c < size; c++)
                board[r][c] = EMPTY;
        std::cout << "   Board has been reset.\n";
    }

    // Validate the current board state and check for a win
    // Returns true if the puzzle is solved correctly
    bool validate(bool verbose) const {
        bool valid = true;
        int totalCats = 0;

        // Count cats per row
        for (int r = 0; r < size; r++) {
            int count = 0;
            for (int c = 0; c < size; c++)
                if (board[r][c] == CAT) count++;
            totalCats += count;
            if (count > 1) {
                if (verbose)
                    std::cout << "   [!] Row " << (char)('A' + r) << " has more than one cat!\n";
                valid = false;
            }
        }

        // Count cats per column
        for (int c = 0; c < size; c++) {
            int count = 0;
            for (int r = 0; r < size; r++)
                if (board[r][c] == CAT) count++;
            if (count > 1) {
                if (verbose)
                    std::cout << "   [!] Column " << (c + 1) << " has more than one cat!\n";
                valid = false;
            }
        }

        // Count cats per region
        std::vector<int> regionCount(numRegions, 0);
        for (int r = 0; r < size; r++)
            for (int c = 0; c < size; c++)
                if (board[r][c] == CAT)
                    regionCount[regionMap[r][c]]++;
        for (int i = 0; i < numRegions; i++) {
            if (regionCount[i] > 1) {
                if (verbose)
                    std::cout << "   [!] Region " << REGION_LABELS[i] << " has more than one cat!\n";
                valid = false;
            }
        }

        // Check adjacency (no two cats touching, including diagonals)
        int dr[] = {-1, -1, -1, 0, 0, 1, 1, 1};
        int dc[] = {-1, 0, 1, -1, 1, -1, 0, 1};
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                if (board[r][c] == CAT) {
                    for (int d = 0; d < 8; d++) {
                        int nr = r + dr[d];
                        int nc = c + dc[d];
                        if (inBounds(nr, nc) && board[nr][nc] == CAT) {
                            if (verbose)
                                std::cout << "   [!] Cats at " << (char)('A' + r) << (c + 1)
                                          << " and " << (char)('A' + nr) << (nc + 1)
                                          << " are adjacent!\n";
                            valid = false;
                        }
                    }
                }
            }
        }

        // Check win: exactly one cat per row/col/region and no adjacency issues
        if (valid && totalCats == size) {
            return true;
        }

        if (valid && totalCats < size && verbose) {
            std::cout << "   [i] No rule violations so far. Place "
                      << (size - totalCats) << " more cat(s) to win!\n";
        }

        return false;
    }

private:
    bool inBounds(int row, int col) const {
        return row >= 0 && row < size && col >= 0 && col < size;
    }
};

// ---------------------------------------------------------------------------
// Parse user coordinate input like "A1", "B3", etc.
// ---------------------------------------------------------------------------
bool parseCoord(const std::string& input, int size, int& row, int& col) {
    if (input.length() < 2) return false;
    char rowChar = std::toupper(input[0]);
    if (rowChar < 'A' || rowChar >= 'A' + size) return false;
    row = rowChar - 'A';

    std::string colStr = input.substr(1);
    for (char ch : colStr)
        if (!std::isdigit(ch)) return false;

    col = std::stoi(colStr) - 1;
    if (col < 0 || col >= size) return false;
    return true;
}

// ---------------------------------------------------------------------------
// Display title screen
// ---------------------------------------------------------------------------
void displayTitle() {
    std::cout << "\n";
    std::cout << "  ╔══════════════════════════════════════════════════╗\n";
    std::cout << "  ║                                                  ║\n";
    std::cout << "  ║      /\\_/\\    M E O W D O K U                    ║\n";
    std::cout << "  ║     ( o.o )   Console Edition                    ║\n";
    std::cout << "  ║      > ^ <                                       ║\n";
    std::cout << "  ║     /|   |\\   A Cat Placement Puzzle             ║\n";
    std::cout << "  ║                                                  ║\n";
    std::cout << "  ║   MeowdoCrew | CS208 - Programming Languages    ║\n";
    std::cout << "  ║                                                  ║\n";
    std::cout << "  ╚══════════════════════════════════════════════════╝\n";
    std::cout << "\n";
}

// ---------------------------------------------------------------------------
// Display rules
// ---------------------------------------------------------------------------
void displayRules() {
    std::cout << "\n  ===== HOW TO PLAY =====\n\n";
    std::cout << "  Goal: Place exactly ONE cat (C) in each row, column,\n";
    std::cout << "        and region of the grid.\n\n";
    std::cout << "  Rules:\n";
    std::cout << "    1. Each ROW must contain exactly one cat.\n";
    std::cout << "    2. Each COLUMN must contain exactly one cat.\n";
    std::cout << "    3. Each REGION (marked by letters A, B, C...) must\n";
    std::cout << "       contain exactly one cat.\n";
    std::cout << "    4. No two cats can be ADJACENT to each other\n";
    std::cout << "       (horizontally, vertically, or diagonally).\n\n";
    std::cout << "  Commands:\n";
    std::cout << "    c <pos>   - Place/remove a cat (e.g., c A1)\n";
    std::cout << "    x <pos>   - Place/remove an elimination mark (e.g., x B3)\n";
    std::cout << "    e <pos>   - Clear a cell (e.g., e C2)\n";
    std::cout << "    check     - Validate your current board\n";
    std::cout << "    reset     - Clear the entire board\n";
    std::cout << "    rules     - Show these rules again\n";
    std::cout << "    quit      - Exit the game\n\n";
}

// ---------------------------------------------------------------------------
// Main game loop
// ---------------------------------------------------------------------------
int main() {
    displayTitle();

    std::vector<Puzzle> puzzles = createPuzzles();

    // Puzzle selection
    std::cout << "  Select a puzzle:\n\n";
    for (size_t i = 0; i < puzzles.size(); i++) {
        std::cout << "    " << (i + 1) << ". " << puzzles[i].name << "\n";
    }
    std::cout << "\n  Enter choice (1-" << puzzles.size() << "): ";

    int choice = 0;
    std::string line;
    std::getline(std::cin, line);
    std::istringstream iss(line);
    iss >> choice;

    if (choice < 1 || choice > (int)puzzles.size()) {
        std::cout << "  Invalid choice. Defaulting to puzzle 1.\n";
        choice = 1;
    }

    Puzzle& selectedPuzzle = puzzles[choice - 1];
    MeowdokuBoard board(selectedPuzzle.size, selectedPuzzle.regionMap);

    std::cout << "\n  Loading: " << selectedPuzzle.name << "\n";
    displayRules();

    // Game loop
    bool running = true;
    while (running) {
        board.display();

        std::cout << "\n  >> ";
        if (!std::getline(std::cin, line)) break;

        // Trim whitespace
        size_t start = line.find_first_not_of(" \t");
        if (start == std::string::npos) continue;
        line = line.substr(start);

        // Parse command
        if (line.empty()) continue;

        // Convert first part to lowercase for command matching
        std::string cmdLower = line;
        std::transform(cmdLower.begin(), cmdLower.end(), cmdLower.begin(), ::tolower);

        if (cmdLower == "quit" || cmdLower == "exit" || cmdLower == "q") {
            std::cout << "\n  Thanks for playing MeowDoku! Goodbye! =^.^=\n\n";
            running = false;
        }
        else if (cmdLower == "rules" || cmdLower == "help" || cmdLower == "h") {
            displayRules();
        }
        else if (cmdLower == "reset") {
            board.resetBoard();
        }
        else if (cmdLower == "check" || cmdLower == "validate") {
            std::cout << "\n";
            if (board.validate(true)) {
                std::cout << "\n";
                std::cout << "  ╔══════════════════════════════════════════╗\n";
                std::cout << "  ║                                          ║\n";
                std::cout << "  ║    /\\_/\\   CONGRATULATIONS!               ║\n";
                std::cout << "  ║   ( ^.^ )  You solved the puzzle!        ║\n";
                std::cout << "  ║    > ^ <   All cats are happy! =^.^=     ║\n";
                std::cout << "  ║                                          ║\n";
                std::cout << "  ╚══════════════════════════════════════════╝\n";
                std::cout << "\n  Play again? (y/n): ";
                std::getline(std::cin, line);
                if (line.empty() || std::tolower(line[0]) != 'y') {
                    running = false;
                } else {
                    board.resetBoard();
                }
            }
        }
        else if (cmdLower.length() >= 2 && (cmdLower[0] == 'c' || cmdLower[0] == 'x' || cmdLower[0] == 'e') && cmdLower[1] == ' ') {
            char cmd = cmdLower[0];
            std::string coord = line.substr(2);
            // Trim coord
            size_t cs = coord.find_first_not_of(" \t");
            if (cs != std::string::npos) coord = coord.substr(cs);
            size_t ce = coord.find_last_not_of(" \t");
            if (ce != std::string::npos) coord = coord.substr(0, ce + 1);

            int row, col;
            if (parseCoord(coord, board.getSize(), row, col)) {
                bool success = false;
                if (cmd == 'c') {
                    success = board.placeCat(row, col);
                } else if (cmd == 'x') {
                    success = board.placeMark(row, col);
                } else if (cmd == 'e') {
                    success = board.clearCell(row, col);
                }

                // Auto-check for win after placing a cat
                if (success && cmd == 'c') {
                    if (board.validate(false)) {
                        board.display();
                        std::cout << "\n";
                        std::cout << "  ╔══════════════════════════════════════════╗\n";
                        std::cout << "  ║                                          ║\n";
                        std::cout << "  ║    /\\_/\\   CONGRATULATIONS!               ║\n";
                        std::cout << "  ║   ( ^.^ )  You solved the puzzle!        ║\n";
                        std::cout << "  ║    > ^ <   All cats are happy! =^.^=     ║\n";
                        std::cout << "  ║                                          ║\n";
                        std::cout << "  ╚══════════════════════════════════════════╝\n";
                        std::cout << "\n  Play again? (y/n): ";
                        std::getline(std::cin, line);
                        if (line.empty() || std::tolower(line[0]) != 'y') {
                            running = false;
                        } else {
                            board.resetBoard();
                        }
                    }
                }
            } else {
                std::cout << "   [!] Invalid position. Use format like A1, B3, etc.\n";
            }
        }
        else {
            std::cout << "   [!] Unknown command. Type 'rules' for help.\n";
        }
    }

    return 0;
}
