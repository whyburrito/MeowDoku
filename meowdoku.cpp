#include <algorithm>
#include <array>
#include <iostream>
#include <string>
#include <vector>
#ifdef _WIN32
    #include <conio.h>
    #include <windows.h>
#else
    #include <termios.h>
    #include <unistd.h>
#endif
using namespace std;
const string RESET = "\033[0m";
const string BOLD = "\033[1m";
const string DIM = "\033[2m";
const string WHITE = "\033[97m";
const string CYAN = "\033[96m";
const string BLUE = "\033[94m";
const string GREEN = "\033[92m";
const string YELLOW = "\033[93m";
const string RED = "\033[91m";
const string MAGENTA = "\033[95m";
const string GRAY = "\033[90m";
const string SELECTED_BG = "\033[48;5;57m";
const string BORDER_COLOR = "\033[38;5;39m";
const vector<string> REGION_COLORS = {
    "\033[38;5;45m", "\033[38;5;213m", "\033[38;5;220m",
    "\033[38;5;114m", "\033[38;5;141m", "\033[38;5;208m",
    "\033[38;5;81m"
};
const string CAT_ICON = "\xF0\x9F\x90\xB1";
const char EMPTY_CELL = '.';
const char CAT = 'C';
const char MARK = 'X';
const string REGION_LABELS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
const string GAME_KEYS = "WASDCXEVRHQ";
const vector<string> PUZZLE_NAMES = {
    "Kitten's First Steps (5x5 Easy)",
    "Cat Nap Challenge (6x6 Medium)",
    "Meow Master (7x7 Hard)"
};
const vector<vector<vector<int>>> PUZZLE_REGIONS = {
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
void setupTerminal() {
#ifdef _WIN32
    SetConsoleOutputCP(CP_UTF8);
    HANDLE output = GetStdHandle(STD_OUTPUT_HANDLE);
    if (output != INVALID_HANDLE_VALUE) {
        DWORD mode = 0;
        if (GetConsoleMode(output, &mode)) {
            SetConsoleMode(output, mode | ENABLE_VIRTUAL_TERMINAL_PROCESSING);
        }
    }
#endif
}
void clearScreen() {
    cout << "\033[2J\033[H" << flush;
}
char readKey(const string& allowedKeys) {
    while (true) {
        char key = '\0';
#ifdef _WIN32
        int input = _getch();
        if (input == 0 || input == 224) {
            (void)_getch();
            continue;
        }
        key = static_cast<char>(input);
#else
        if (!isatty(STDIN_FILENO)) {
            string line;
            if (!getline(cin, line) || line.empty()) return '\0';
            key = line[0];
        } else {
            termios oldSettings{};
            if (tcgetattr(STDIN_FILENO, &oldSettings) != 0) continue;
            termios newSettings = oldSettings;
            newSettings.c_lflag &= static_cast<unsigned long>(~(ICANON | ECHO));
            newSettings.c_cc[VMIN] = 1;
            newSettings.c_cc[VTIME] = 0;
            if (tcsetattr(STDIN_FILENO, TCSANOW, &newSettings) != 0) continue;
            char input = '\0';
            ssize_t bytesRead = read(STDIN_FILENO, &input, 1);
            tcsetattr(STDIN_FILENO, TCSANOW, &oldSettings);
            if (bytesRead != 1) continue;
            key = input;
        }
#endif
        if (key >= 'a' && key <= 'z') key = static_cast<char>(key - 'a' + 'A');
        if (allowedKeys.find(key) != string::npos) return key;
    }
}
void displayTitle() {
    cout << CYAN << BOLD << "  +------------------------------------------------------+" << RESET << '\n';
    cout << CYAN << BOLD << "  |" << RESET << "                                                      " << CYAN << BOLD << "|" << RESET << '\n';
    cout << CYAN << BOLD << "  |" << RESET << GREEN << "       /\\_/\\       M E O W D O K U                 " << CYAN << BOLD << "|" << RESET << '\n';
    cout << CYAN << BOLD << "  |" << RESET << GREEN << "      ( o.o )      " << MAGENTA << BOLD << "COLOR TERMINAL EDITION" << RESET << "   " << CYAN << BOLD << "|" << RESET << '\n';
    cout << CYAN << BOLD << "  |" << RESET << GREEN << "       > ^ <       " << WHITE << "A Cat Placement Puzzle" << RESET << "      " << CYAN << BOLD << "|" << RESET << '\n';
    cout << CYAN << BOLD << "  |" << RESET << "                                                      " << CYAN << BOLD << "|" << RESET << '\n';
    cout << CYAN << BOLD << "  +------------------------------------------------------+" << RESET << "\n\n";
}
class MeowDoku {
private:
    int size;
    vector<vector<int>> regionMap;
    vector<vector<char>> board;
    int numRegions;
    int cursorRow = 0;
    int cursorCol = 0;
    bool running = true;
    string statusMessage = "Welcome! Use W/A/S/D to move, then C to place a cat.";
    string statusColor = CYAN;
    string coordinate(int row, int col) const {
        return string(1, static_cast<char>('A' + row)) + to_string(col + 1);
    }
    string cellDisplay(int row, int col) const {
        if (board[row][col] == CAT) return CAT_ICON;
        if (board[row][col] == MARK) return "X";
        return string(1, REGION_LABELS[regionMap[row][col]]);
    }
    string formatCell(const string& symbol) const {
        if (symbol == CAT_ICON) return " " + symbol + " ";
        return "  " + symbol + " ";
    }
    string symbolColor(int row, int col) const {
        if (board[row][col] == CAT) return GREEN;
        if (board[row][col] == MARK) return YELLOW;
        return REGION_COLORS[regionMap[row][col] % REGION_COLORS.size()];
    }
    void setStatus(const string& message, const string& color) {
        statusMessage = message;
        statusColor = color;
    }
    void printBorder() const {
        cout << "     " << BORDER_COLOR << "+" << RESET;
        for (int col = 0; col < size; ++col) cout << BORDER_COLOR << "----+" << RESET;
        cout << '\n';
    }
    void displayBoard(bool controls = true) const {
        cout << CYAN << BOLD << (controls ? "  MEOWDOKU" : "  SOLVED PUZZLE") << RESET
             << WHITE << "  |  " << size << "x" << size << " PUZZLE" << RESET << '\n';
        if (controls) cout << DIM << "  Move the purple selection with W/A/S/D, then choose an action." << RESET << '\n';
        cout << '\n' << "       ";
        for (int col = 0; col < size; ++col) {
            cout << BLUE << BOLD << " " << (col + 1 < 10 ? " " : "") << col + 1 << " " << RESET;
        }
        cout << '\n';
        printBorder();
        for (int row = 0; row < size; ++row) {
            cout << "  " << MAGENTA << BOLD << static_cast<char>('A' + row) << RESET
                 << "  " << BORDER_COLOR << "|" << RESET;
            for (int col = 0; col < size; ++col) {
                const string symbol = cellDisplay(row, col);
                if (controls && row == cursorRow && col == cursorCol) {
                    cout << SELECTED_BG << WHITE << BOLD << formatCell(symbol) << RESET;
                } else {
                    cout << symbolColor(row, col) << BOLD << formatCell(symbol) << RESET;
                }
                cout << BORDER_COLOR << "|" << RESET;
            }
            cout << '\n';
            printBorder();
        }
        cout << "\n  " << WHITE << BOLD << "REGIONS  " << RESET;
        for (int region = 0; region < numRegions; ++region) {
            if (region > 0) cout << GRAY << "  " << RESET;
            cout << REGION_COLORS[region % REGION_COLORS.size()] << BOLD
                 << REGION_LABELS[region] << RESET;
        }
        cout << '\n';
        if (controls) {
            cout << "\n" << CYAN << BOLD << "  CONTROLS" << RESET << '\n';
            cout << "  " << YELLOW << "W / A / S / D" << RESET << "  Move selection (up / left / down / right)\n";
            cout << "  " << GREEN << "C" << RESET << "              Place or remove a " << GREEN << CAT_ICON << RESET << " cat\n";
            cout << "  " << YELLOW << "X" << RESET << "              Place or remove an elimination mark\n";
            cout << "  " << RED << "E" << RESET << "              Clear the selected cell\n";
            cout << "  " << CYAN << "V" << RESET << "              Check the puzzle\n";
            cout << "  " << MAGENTA << "R" << RESET << "              Reset the board\n";
            cout << "  " << BLUE << "H" << RESET << "              Show the rules\n";
            cout << "  " << GRAY << "Q" << RESET << "              Quit the game\n";
            cout << "\n  " << WHITE << BOLD << "STATUS  " << RESET << statusColor << statusMessage << RESET << '\n';
            cout << "\n  " << DIM << "Press a key: " << RESET << flush;
        }
    }
    bool validate(bool verbose, vector<string>& messages) const {
        bool valid = true;
        int totalCats = 0;
        for (int row = 0; row < size; ++row) {
            int count = 0;
            for (int col = 0; col < size; ++col) if (board[row][col] == CAT) ++count;
            totalCats += count;
            if (count > 1) {
                valid = false;
                messages.push_back("[!] Row " + string(1, static_cast<char>('A' + row)) + " has more than one cat.");
            }
        }
        for (int col = 0; col < size; ++col) {
            int count = 0;
            for (int row = 0; row < size; ++row) if (board[row][col] == CAT) ++count;
            if (count > 1) {
                valid = false;
                messages.push_back("[!] Column " + to_string(col + 1) + " has more than one cat.");
            }
        }
        vector<int> regionCount(numRegions, 0);
        for (int row = 0; row < size; ++row) {
            for (int col = 0; col < size; ++col) {
                if (board[row][col] == CAT) ++regionCount[regionMap[row][col]];
            }
        }
        for (int region = 0; region < numRegions; ++region) {
            if (regionCount[region] > 1) {
                valid = false;
                messages.push_back("[!] Region " + string(1, REGION_LABELS[region]) + " has more than one cat.");
            }
        }
        const array<pair<int, int>, 4> directions = {{{0, 1}, {1, -1}, {1, 0}, {1, 1}}};
        for (int row = 0; row < size; ++row) {
            for (int col = 0; col < size; ++col) {
                if (board[row][col] != CAT) continue;
                for (const auto& direction : directions) {
                    int nextRow = row + direction.first;
                    int nextCol = col + direction.second;
                    if (nextRow >= 0 && nextRow < size && nextCol >= 0 && nextCol < size
                        && board[nextRow][nextCol] == CAT) {
                        valid = false;
                        messages.push_back("[!] Cats at " + coordinate(row, col) + " and "
                                           + coordinate(nextRow, nextCol) + " are adjacent.");
                    }
                }
            }
        }
        const bool solved = valid && totalCats == size;
        if (valid && !solved) {
            messages.push_back("[i] No rule violations. Place " + to_string(size - totalCats) + " more cat(s) to win.");
        } else if (!valid) {
            messages.push_back("[i] Fix the rule violations and check again.");
        }
        (void)verbose;
        return solved;
    }
    void displayRules() const {
        clearScreen();
        displayTitle();
        cout << CYAN << BOLD << "  HOW TO PLAY" << RESET << '\n';
        cout << "  1. Place exactly " << GREEN << "ONE " << CAT_ICON << " cat" << RESET << " in each row.\n";
        cout << "  2. Place exactly " << GREEN << "ONE " << CAT_ICON << " cat" << RESET << " in each column.\n";
        cout << "  3. Place exactly " << GREEN << "ONE " << CAT_ICON << " cat" << RESET << " in each region.\n";
        cout << "  4. No two cats may touch, including diagonally.\n\n";
        cout << "  Empty cells show their region letter.\n";
        cout << "  The selected cell has a purple background.\n\n";
        cout << DIM << "  Press any game-control key to return to the puzzle." << RESET << flush;
        readKey(GAME_KEYS);
    }
    void resetBoard() {
        for (auto& row : board) fill(row.begin(), row.end(), EMPTY_CELL);
        cursorRow = 0;
        cursorCol = 0;
        setStatus("Board has been reset.", GREEN);
    }
    void handleWin() {
        clearScreen();
        displayTitle();
        displayBoard(false);
        cout << "\n" << GREEN << BOLD;
        cout << "  +----------------------------------------------+\n";
        cout << "  |             CONGRATULATIONS!                 |\n";
        cout << "  |          You solved the puzzle!              |\n";
        cout << "  |           All cats are happy!                |\n";
        cout << "  +----------------------------------------------+" << RESET << "\n\n";
        cout << WHITE << BOLD << "  Play this puzzle again? (Y/N): " << RESET << flush;
        char answer = readKey("YN");
        if (answer == 'Y') resetBoard();
        else running = false;
    }
    void showCheckResults() {
        clearScreen();
        displayTitle();
        displayBoard(false);
        cout << "\n" << CYAN << BOLD << "  CHECK RESULTS" << RESET << '\n';
        vector<string> messages;
        const bool solved = validate(true, messages);
        (void)solved;
        for (const string& message : messages) {
            const string color = message.rfind("[!] ", 0) == 0 ? RED
                : (message.rfind("[i] No", 0) == 0 ? GREEN : YELLOW);
            cout << "  " << color << message << RESET << '\n';
        }
        cout << '\n' << DIM << "  Press any game-control key to continue..." << RESET << flush;
        readKey(GAME_KEYS);
    }
public:
    explicit MeowDoku(int puzzleIndex)
        : size(static_cast<int>(PUZZLE_REGIONS[puzzleIndex].size())),
          regionMap(PUZZLE_REGIONS[puzzleIndex]),
          board(size, vector<char>(size, EMPTY_CELL)),
          numRegions(0) {
        for (const auto& row : regionMap) {
            for (int region : row) numRegions = max(numRegions, region + 1);
        }
    }
    void play() {
        running = true;
        while (running) {
            clearScreen();
            displayTitle();
            displayBoard(true);
            char key = readKey(GAME_KEYS);
            switch (key) {
                case 'W': cursorRow = max(0, cursorRow - 1); setStatus("Selected cell: " + coordinate(cursorRow, cursorCol), CYAN); break;
                case 'A': cursorCol = max(0, cursorCol - 1); setStatus("Selected cell: " + coordinate(cursorRow, cursorCol), CYAN); break;
                case 'S': cursorRow = min(size - 1, cursorRow + 1); setStatus("Selected cell: " + coordinate(cursorRow, cursorCol), CYAN); break;
                case 'D': cursorCol = min(size - 1, cursorCol + 1); setStatus("Selected cell: " + coordinate(cursorRow, cursorCol), CYAN); break;
                case 'C':
                    if (board[cursorRow][cursorCol] == CAT) {
                        board[cursorRow][cursorCol] = EMPTY_CELL;
                        setStatus("Cat removed from " + coordinate(cursorRow, cursorCol) + ".", YELLOW);
                    } else {
                        board[cursorRow][cursorCol] = CAT;
                        setStatus("Cat placed at " + coordinate(cursorRow, cursorCol) + ".", GREEN);
                    }
                    {
                        vector<string> messages;
                        if (validate(false, messages)) handleWin();
                    }
                    break;
                case 'X':
                    if (board[cursorRow][cursorCol] == CAT) {
                        setStatus("Remove the cat before placing a mark.", RED);
                    } else if (board[cursorRow][cursorCol] == MARK) {
                        board[cursorRow][cursorCol] = EMPTY_CELL;
                        setStatus("Mark removed from " + coordinate(cursorRow, cursorCol) + ".", YELLOW);
                    } else {
                        board[cursorRow][cursorCol] = MARK;
                        setStatus("Elimination mark placed at " + coordinate(cursorRow, cursorCol) + ".", YELLOW);
                    }
                    break;
                case 'E':
                    board[cursorRow][cursorCol] = EMPTY_CELL;
                    setStatus("Cell " + coordinate(cursorRow, cursorCol) + " cleared.", CYAN);
                    break;
                case 'V': {
                    vector<string> messages;
                    if (validate(false, messages)) handleWin();
                    else showCheckResults();
                    break;
                }
                case 'R': resetBoard(); break;
                case 'H': displayRules(); setStatus("Returned to the puzzle.", CYAN); break;
                case 'Q': running = false; break;
                default: break;
            }
        }
    }
};
int selectPuzzle() {
    while (true) {
        clearScreen();
        displayTitle();
        cout << CYAN << BOLD << "  SELECT A PUZZLE\n\n" << RESET;
        for (int i = 0; i < static_cast<int>(PUZZLE_NAMES.size()); ++i) {
            const string color = i == 0 ? GREEN : (i == 1 ? YELLOW : RED);
            cout << "  " << WHITE << (i + 1) << ". " << RESET << color << PUZZLE_NAMES[i] << RESET << '\n';
        }
        cout << "  " << GRAY << "Q. Quit" << RESET << "\n\n";
        cout << WHITE << BOLD << "  Press 1, 2, 3, or Q: " << RESET << flush;
        char choice = readKey("123Q");
        if (choice == 'Q') return -1;
        return choice - '1';
    }
}
int main() {
    setupTerminal();
    const int puzzleIndex = selectPuzzle();
    if (puzzleIndex == -1) {
        cout << GREEN << "Thanks for playing MeowDoku. Goodbye! =^.^=" << RESET << '\n';
        return 0;
    }
    MeowDoku game(puzzleIndex);
    game.play();
    clearScreen();
    displayTitle();
    cout << GREEN << BOLD << "  Thanks for playing MeowDoku! Goodbye! =^.^=" << RESET << '\n';
    return 0;
}
