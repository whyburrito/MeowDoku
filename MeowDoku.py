import os
import sys
try:
    import msvcrt
except ImportError:
    msvcrt = None
RESET = '\x1b[0m'
BOLD = '\x1b[1m'
DIM = '\x1b[2m'
WHITE = '\x1b[97m'
CYAN = '\x1b[96m'
BLUE = '\x1b[94m'
GREEN = '\x1b[92m'
YELLOW = '\x1b[93m'
RED = '\x1b[91m'
MAGENTA = '\x1b[95m'
GRAY = '\x1b[90m'
SELECTED_BG = '\x1b[48;5;57m'
BORDER_COLOR = '\x1b[38;5;39m'
REGION_COLORS = ['\x1b[38;5;45m', '\x1b[38;5;213m', '\x1b[38;5;220m', '\x1b[38;5;114m', '\x1b[38;5;141m', '\x1b[38;5;208m', '\x1b[38;5;81m']
CAT_ICON = '🐱'
EMPTY = '.'
CAT = 'C'
MARK = 'X'
REGION_LABELS = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'
GAME_KEYS = 'WASDCXEVRHQ'
PUZZLE_NAMES = ["Kitten's First Steps (5x5 Easy)", 'Cat Nap Challenge (6x6 Medium)', 'Meow Master (7x7 Hard)']
PUZZLE_REGIONS = [[[0, 0, 1, 1, 1], [0, 0, 0, 1, 1], [2, 2, 0, 3, 3], [2, 2, 4, 4, 3], [2, 4, 4, 4, 3]], [[0, 0, 1, 1, 2, 2], [0, 0, 1, 1, 2, 2], [3, 3, 3, 4, 4, 2], [3, 5, 5, 4, 4, 4], [3, 5, 5, 5, 4, 4], [3, 5, 5, 5, 4, 4]], [[0, 0, 0, 1, 1, 2, 2], [0, 3, 0, 1, 1, 2, 2], [3, 3, 3, 1, 4, 4, 2], [3, 3, 5, 5, 4, 4, 4], [6, 3, 5, 5, 5, 4, 4], [6, 6, 6, 5, 5, 5, 4], [6, 6, 6, 6, 5, 5, 5]]]

def setup_terminal():
    if os.name == 'nt':
        try:
            import ctypes
            kernel32 = ctypes.windll.kernel32
            kernel32.SetConsoleOutputCP(65001)
            handle = kernel32.GetStdHandle(-11)
            mode = ctypes.c_uint()
            if kernel32.GetConsoleMode(handle, ctypes.byref(mode)):
                ENABLE_VIRTUAL_TERMINAL_PROCESSING = 4
                kernel32.SetConsoleMode(handle, mode.value | ENABLE_VIRTUAL_TERMINAL_PROCESSING)
        except Exception:
            pass
    if hasattr(sys.stdout, 'reconfigure'):
        try:
            sys.stdout.reconfigure(encoding='utf-8', errors='replace')
        except Exception:
            pass

def clear_screen():
    print('\x1b[2J\x1b[H', end='', flush=True)

def read_key(allowed_keys):
    allowed = allowed_keys.upper()
    while True:
        if msvcrt is not None:
            key = msvcrt.getwch()
            if key in ('\x00', 'à'):
                msvcrt.getwch()
                continue
        elif sys.stdin.isatty():
            import termios
            import tty
            fd = sys.stdin.fileno()
            old_settings = termios.tcgetattr(fd)
            try:
                tty.setraw(fd)
                key = sys.stdin.read(1)
            finally:
                termios.tcsetattr(fd, termios.TCSADRAIN, old_settings)
        else:
            key = input('\nEnter a key: ')[:1]
        if key and key.upper() in allowed:
            return key.upper()

def display_title():
    print(CYAN + BOLD + '  +------------------------------------------------------+' + RESET)
    print(CYAN + BOLD + '  |' + RESET + '                                                      ' + CYAN + BOLD + '|' + RESET)
    print(CYAN + BOLD + '  |' + RESET + GREEN + '       /\\_/\\       M E O W D O K U                 ' + CYAN + BOLD + '|' + RESET)
    print(CYAN + BOLD + '  |' + RESET + GREEN + '      ( o.o )      ' + MAGENTA + BOLD + 'COLOR TERMINAL EDITION' + RESET + '   ' + CYAN + BOLD + '|' + RESET)
    print(CYAN + BOLD + '  |' + RESET + GREEN + '       > ^ <       ' + WHITE + 'A Cat Placement Puzzle' + RESET + '      ' + CYAN + BOLD + '|' + RESET)
    print(CYAN + BOLD + '  |' + RESET + '                                                      ' + CYAN + BOLD + '|' + RESET)
    print(CYAN + BOLD + '  +------------------------------------------------------+' + RESET)
    print()

class MeowDoku:

    def __init__(self, puzzle_index):
        self.region_map = PUZZLE_REGIONS[puzzle_index]
        self.size = len(self.region_map)
        self.board = [[EMPTY for _ in range(self.size)] for _ in range(self.size)]
        self.num_regions = max((max(row) for row in self.region_map)) + 1
        self.cursor_row = 0
        self.cursor_col = 0
        self.running = True
        self.status_message = 'Welcome! Use W/A/S/D to move, then C to place a cat.'
        self.status_color = CYAN

    def coordinate(self, row, col):
        return f"{chr(ord('A') + row)}{col + 1}"

    def cell_display(self, row, col):
        value = self.board[row][col]
        if value == CAT:
            return CAT_ICON
        if value == MARK:
            return MARK
        return REGION_LABELS[self.region_map[row][col]]

    @staticmethod
    def format_cell(symbol):
        if symbol == CAT_ICON:
            return f' {symbol} '
        return f'  {symbol} '

    def symbol_color(self, row, col):
        value = self.board[row][col]
        if value == CAT:
            return GREEN
        if value == MARK:
            return YELLOW
        return REGION_COLORS[self.region_map[row][col] % len(REGION_COLORS)]

    def print_border(self):
        print('     ' + BORDER_COLOR + '+' + RESET, end='')
        for _ in range(self.size):
            print(BORDER_COLOR + '----+' + RESET, end='')
        print()

    def display_board(self, controls=True):
        heading = 'MEOWDOKU' if controls else 'SOLVED PUZZLE'
        print(CYAN + BOLD + f'  {heading}' + RESET + WHITE + f'  |  {self.size}x{self.size} PUZZLE' + RESET)
        if controls:
            print(DIM + '  Move the purple selection with W/A/S/D, then choose an action.' + RESET)
        print()
        print('       ', end='')
        for col in range(self.size):
            print(BLUE + BOLD + f' {col + 1:^3}' + RESET, end='')
        print()
        self.print_border()
        for row in range(self.size):
            print('  ' + MAGENTA + BOLD + chr(ord('A') + row) + RESET + '  ' + BORDER_COLOR + '|' + RESET, end='')
            for col in range(self.size):
                symbol = self.cell_display(row, col)
                if row == self.cursor_row and col == self.cursor_col and controls:
                    print(SELECTED_BG + WHITE + BOLD + self.format_cell(symbol) + RESET, end='')
                else:
                    print(self.symbol_color(row, col) + BOLD + self.format_cell(symbol) + RESET, end='')
                print(BORDER_COLOR + '|' + RESET, end='')
            print()
            self.print_border()
        print('\n  ' + WHITE + BOLD + 'REGIONS  ' + RESET, end='')
        for region in range(self.num_regions):
            if region:
                print(GRAY + '  ' + RESET, end='')
            print(REGION_COLORS[region % len(REGION_COLORS)] + BOLD + REGION_LABELS[region] + RESET, end='')
        print()
        if controls:
            print('\n' + CYAN + BOLD + '  CONTROLS' + RESET)
            print('  ' + YELLOW + 'W / A / S / D' + RESET + '  Move selection (up / left / down / right)')
            print('  ' + GREEN + 'C' + RESET + '              Place or remove a ' + GREEN + CAT_ICON + RESET + ' cat')
            print('  ' + YELLOW + 'X' + RESET + '              Place or remove an elimination mark')
            print('  ' + RED + 'E' + RESET + '              Clear the selected cell')
            print('  ' + CYAN + 'V' + RESET + '              Check the puzzle')
            print('  ' + MAGENTA + 'R' + RESET + '              Reset the board')
            print('  ' + BLUE + 'H' + RESET + '              Show the rules')
            print('  ' + GRAY + 'Q' + RESET + '              Quit the game')
            print('\n  ' + WHITE + BOLD + 'STATUS  ' + RESET + self.status_color + self.status_message + RESET)
            print('\n  ' + DIM + 'Press a key: ' + RESET, end='', flush=True)

    def set_status(self, message, color):
        self.status_message = message
        self.status_color = color

    def move_cursor(self, row_change, col_change):
        old = (self.cursor_row, self.cursor_col)
        self.cursor_row = max(0, min(self.size - 1, self.cursor_row + row_change))
        self.cursor_col = max(0, min(self.size - 1, self.cursor_col + col_change))
        if old == (self.cursor_row, self.cursor_col):
            self.set_status('You are at the edge of the board.', YELLOW)
        else:
            self.set_status('Selected cell: ' + self.coordinate(self.cursor_row, self.cursor_col), CYAN)

    def place_cat(self, row, col):
        if self.board[row][col] == CAT:
            self.board[row][col] = EMPTY
            self.set_status('Cat removed from ' + self.coordinate(row, col) + '.', YELLOW)
        else:
            self.board[row][col] = CAT
            self.set_status('Cat placed at ' + self.coordinate(row, col) + '.', GREEN)

    def place_mark(self, row, col):
        if self.board[row][col] == CAT:
            self.set_status('Remove the cat before placing a mark.', RED)
        elif self.board[row][col] == MARK:
            self.board[row][col] = EMPTY
            self.set_status('Mark removed from ' + self.coordinate(row, col) + '.', YELLOW)
        else:
            self.board[row][col] = MARK
            self.set_status('Elimination mark placed at ' + self.coordinate(row, col) + '.', YELLOW)

    def clear_cell(self, row, col):
        self.board[row][col] = EMPTY
        self.set_status('Cell ' + self.coordinate(row, col) + ' cleared.', CYAN)

    def reset_board(self):
        self.board = [[EMPTY for _ in range(self.size)] for _ in range(self.size)]
        self.cursor_row = 0
        self.cursor_col = 0
        self.set_status('Board has been reset.', GREEN)

    def validate(self, verbose=False):
        valid = True
        total_cats = 0
        messages = []
        for row in range(self.size):
            count = sum((1 for value in self.board[row] if value == CAT))
            total_cats += count
            if count > 1:
                valid = False
                messages.append(f"[!] Row {chr(ord('A') + row)} has more than one cat.")
        for col in range(self.size):
            count = sum((1 for row in range(self.size) if self.board[row][col] == CAT))
            if count > 1:
                valid = False
                messages.append(f'[!] Column {col + 1} has more than one cat.')
        region_count = [0] * self.num_regions
        for row in range(self.size):
            for col in range(self.size):
                if self.board[row][col] == CAT:
                    region_count[self.region_map[row][col]] += 1
        for region, count in enumerate(region_count):
            if count > 1:
                valid = False
                messages.append(f'[!] Region {REGION_LABELS[region]} has more than one cat.')
        directions = ((0, 1), (1, -1), (1, 0), (1, 1))
        for row in range(self.size):
            for col in range(self.size):
                if self.board[row][col] != CAT:
                    continue
                for dr, dc in directions:
                    nr, nc = (row + dr, col + dc)
                    if 0 <= nr < self.size and 0 <= nc < self.size and (self.board[nr][nc] == CAT):
                        valid = False
                        messages.append(f'[!] Cats at {self.coordinate(row, col)} and {self.coordinate(nr, nc)} are adjacent.')
        solved = valid and total_cats == self.size
        if valid and (not solved):
            messages.append(f'[i] No rule violations. Place {self.size - total_cats} more cat(s) to win.')
        elif not valid:
            messages.append('[i] Fix the rule violations and check again.')
        if verbose:
            return (solved, messages)
        return (solved, messages)

    def display_rules(self):
        clear_screen()
        display_title()
        print(CYAN + BOLD + '  HOW TO PLAY' + RESET)
        print('  1. Place exactly ' + GREEN + 'ONE ' + CAT_ICON + ' cat' + RESET + ' in each row.')
        print('  2. Place exactly ' + GREEN + 'ONE ' + CAT_ICON + ' cat' + RESET + ' in each column.')
        print('  3. Place exactly ' + GREEN + 'ONE ' + CAT_ICON + ' cat' + RESET + ' in each region.')
        print('  4. No two cats may touch, including diagonally.')
        print('\n  Empty cells show their region letter.')
        print('  The selected cell has a purple background.')
        print('\n' + DIM + '  Press any game-control key to return to the puzzle.' + RESET, end='', flush=True)
        read_key(GAME_KEYS)

    def show_check_results(self):
        clear_screen()
        display_title()
        self.display_board(controls=False)
        print('\n' + CYAN + BOLD + '  CHECK RESULTS' + RESET)
        solved, messages = self.validate(verbose=True)
        for message in messages:
            color = RED if message.startswith('[!] ') else GREEN if message.startswith('[i] No') else YELLOW
            print('  ' + color + message + RESET)
        print('\n' + DIM + '  Press any game-control key to continue...' + RESET, end='', flush=True)
        read_key(GAME_KEYS)

    def handle_win(self):
        clear_screen()
        display_title()
        self.display_board(controls=False)
        print('\n' + GREEN + BOLD + '  +----------------------------------------------+')
        print('  |             CONGRATULATIONS!                 |')
        print('  |          You solved the puzzle!              |')
        print('  |           All cats are happy!                |')
        print('  +----------------------------------------------+' + RESET)
        print('\n' + WHITE + BOLD + '  Play this puzzle again? (Y/N): ' + RESET, end='', flush=True)
        answer = read_key('YN')
        if answer == 'Y':
            self.reset_board()
        else:
            self.running = False

    def play(self):
        self.running = True
        while self.running:
            clear_screen()
            display_title()
            self.display_board(controls=True)
            key = read_key(GAME_KEYS)
            if key == 'W':
                self.move_cursor(-1, 0)
            elif key == 'A':
                self.move_cursor(0, -1)
            elif key == 'S':
                self.move_cursor(1, 0)
            elif key == 'D':
                self.move_cursor(0, 1)
            elif key == 'C':
                self.place_cat(self.cursor_row, self.cursor_col)
                solved, _ = self.validate()
                if solved:
                    self.handle_win()
            elif key == 'X':
                self.place_mark(self.cursor_row, self.cursor_col)
            elif key == 'E':
                self.clear_cell(self.cursor_row, self.cursor_col)
            elif key == 'V':
                solved, _ = self.validate()
                if solved:
                    self.handle_win()
                else:
                    self.show_check_results()
            elif key == 'R':
                self.reset_board()
            elif key == 'H':
                self.display_rules()
                self.set_status('Returned to the puzzle.', CYAN)
            elif key == 'Q':
                self.running = False

def select_puzzle():
    while True:
        clear_screen()
        display_title()
        print(CYAN + BOLD + '  SELECT A PUZZLE\n' + RESET)
        for index, name in enumerate(PUZZLE_NAMES):
            color = GREEN if index == 0 else YELLOW if index == 1 else RED
            print(f'  {WHITE}{index + 1}.{RESET} {color}{name}{RESET}')
        print('  ' + GRAY + 'Q. Quit' + RESET)
        print('\n  ' + WHITE + BOLD + 'Press 1, 2, 3, or Q: ' + RESET, end='', flush=True)
        choice = read_key('123Q')
        if choice == 'Q':
            return -1
        return int(choice) - 1

def main():
    setup_terminal()
    puzzle_index = select_puzzle()
    if puzzle_index == -1:
        print(GREEN + 'Thanks for playing MeowDoku. Goodbye! =^.^=' + RESET)
        return
    game = MeowDoku(puzzle_index)
    game.play()
    clear_screen()
    display_title()
    print(GREEN + BOLD + '  Thanks for playing MeowDoku! Goodbye! =^.^=' + RESET)
if __name__ == '__main__':
    try:
        main()
    except KeyboardInterrupt:
        clear_screen()
        print('\n' + GREEN + 'Thanks for playing MeowDoku! Goodbye!' + RESET)
