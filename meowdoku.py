# =============================================================================
# MeowDoku Console Edition - Python Implementation
# MeowdoCrew | CS208 - Programming Languages
# =============================================================================
# A lightweight, single-player logic puzzle game played in the terminal.
# Place exactly one cat (C) per row, column, and region.
# No two cats may be adjacent (including diagonally).
# Use elimination marks (X) to help deduce the solution.
# =============================================================================

import sys

# ---------------------------------------------------------------------------
# Constants
# ---------------------------------------------------------------------------
EMPTY = '.'
CAT   = 'C'
MARK  = 'X'
REGION_LABELS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"

# ---------------------------------------------------------------------------
# Puzzle definitions
# ---------------------------------------------------------------------------
PUZZLES = [
    {
        "name": "Kitten's First Steps (5x5 Easy)",
        "regions": [
            [0, 0, 1, 1, 1],
            [0, 0, 0, 1, 1],
            [2, 2, 0, 3, 3],
            [2, 2, 4, 4, 3],
            [2, 4, 4, 4, 3],
        ]
    },
    {
        "name": "Cat Nap Challenge (6x6 Medium)",
        "regions": [
            [0, 0, 1, 1, 2, 2],
            [0, 0, 1, 1, 2, 2],
            [3, 3, 3, 4, 4, 2],
            [3, 5, 5, 4, 4, 4],
            [3, 5, 5, 5, 4, 4],
            [3, 5, 5, 5, 4, 4],
        ]
    },
    {
        "name": "Meow Master (7x7 Hard)",
        "regions": [
            [0, 0, 0, 1, 1, 2, 2],
            [0, 3, 0, 1, 1, 2, 2],
            [3, 3, 3, 1, 4, 4, 2],
            [3, 3, 5, 5, 4, 4, 4],
            [6, 3, 5, 5, 5, 4, 4],
            [6, 6, 6, 5, 5, 5, 4],
            [6, 6, 6, 6, 5, 5, 5],
        ]
    },
]


# ---------------------------------------------------------------------------
# MeowdokuBoard class
# ---------------------------------------------------------------------------
class MeowdokuBoard:
    """Represents the game board for a MeowDoku puzzle."""

    def __init__(self, region_map):
        """Initialize the board from a region map (2D list of ints)."""
        self.size = len(region_map)
        self.region_map = region_map
        self.board = [[EMPTY] * self.size for _ in range(self.size)]
        self.num_regions = max(
            region_map[r][c]
            for r in range(self.size)
            for c in range(self.size)
        ) + 1

    def display(self):
        """Print the board to the console."""
        print()

        # Column headers
        header = "     "
        for c in range(self.size):
            header += f" {c + 1}  "
        print(header)

        # Top border
        border = "   +" + "---+" * self.size
        print(border)

        for r in range(self.size):
            row_label = chr(ord('A') + r)
            row_str = f" {row_label} |"
            for c in range(self.size):
                cell = self.board[r][c]
                if cell == CAT:
                    row_str += " C |"
                elif cell == MARK:
                    row_str += " X |"
                else:
                    region_label = REGION_LABELS[self.region_map[r][c]]
                    row_str += f" {region_label} |"
            print(row_str)
            print(border)

        # Region legend
        labels = ", ".join(REGION_LABELS[i] for i in range(self.num_regions))
        print(f"\n   Regions: {labels}")

    def place_cat(self, row, col):
        """Place or remove a cat at the given position. Returns True on success."""
        if not self._in_bounds(row, col):
            print("   [!] Position out of bounds.")
            return False
        if self.board[row][col] == CAT:
            print("   [!] A cat is already here! Removing it.")
            self.board[row][col] = EMPTY
            return True
        self.board[row][col] = CAT
        return True

    def place_mark(self, row, col):
        """Place or remove an elimination mark. Returns True on success."""
        if not self._in_bounds(row, col):
            print("   [!] Position out of bounds.")
            return False
        if self.board[row][col] == CAT:
            print("   [!] Remove the cat first before marking.")
            return False
        if self.board[row][col] == MARK:
            print("   [!] Mark removed.")
            self.board[row][col] = EMPTY
            return True
        self.board[row][col] = MARK
        return True

    def clear_cell(self, row, col):
        """Clear a cell. Returns True on success."""
        if not self._in_bounds(row, col):
            print("   [!] Position out of bounds.")
            return False
        self.board[row][col] = EMPTY
        return True

    def reset_board(self):
        """Reset the entire board to empty."""
        self.board = [[EMPTY] * self.size for _ in range(self.size)]
        print("   Board has been reset.")

    def validate(self, verbose=False):
        """
        Validate the current board state.
        Returns True if the puzzle is fully and correctly solved.
        """
        valid = True
        total_cats = 0

        # Check rows
        for r in range(self.size):
            count = sum(1 for c in range(self.size) if self.board[r][c] == CAT)
            total_cats += count
            if count > 1:
                if verbose:
                    print(f"   [!] Row {chr(ord('A') + r)} has more than one cat!")
                valid = False

        # Check columns
        for c in range(self.size):
            count = sum(1 for r in range(self.size) if self.board[r][c] == CAT)
            if count > 1:
                if verbose:
                    print(f"   [!] Column {c + 1} has more than one cat!")
                valid = False

        # Check regions
        region_count = [0] * self.num_regions
        for r in range(self.size):
            for c in range(self.size):
                if self.board[r][c] == CAT:
                    region_count[self.region_map[r][c]] += 1
        for i in range(self.num_regions):
            if region_count[i] > 1:
                if verbose:
                    print(f"   [!] Region {REGION_LABELS[i]} has more than one cat!")
                valid = False

        # Check adjacency (including diagonals)
        directions = [(-1, -1), (-1, 0), (-1, 1),
                      ( 0, -1),          ( 0, 1),
                      ( 1, -1), ( 1, 0), ( 1, 1)]
        for r in range(self.size):
            for c in range(self.size):
                if self.board[r][c] == CAT:
                    for dr, dc in directions:
                        nr, nc = r + dr, c + dc
                        if self._in_bounds(nr, nc) and self.board[nr][nc] == CAT:
                            if verbose:
                                print(f"   [!] Cats at {chr(ord('A') + r)}{c + 1}"
                                      f" and {chr(ord('A') + nr)}{nc + 1} are adjacent!")
                            valid = False

        # Win condition
        if valid and total_cats == self.size:
            return True

        if valid and total_cats < self.size and verbose:
            remaining = self.size - total_cats
            print(f"   [i] No rule violations so far. Place {remaining} more cat(s) to win!")

        return False

    def _in_bounds(self, row, col):
        """Check if a position is within the grid."""
        return 0 <= row < self.size and 0 <= col < self.size


# ---------------------------------------------------------------------------
# Utility functions
# ---------------------------------------------------------------------------
def parse_coord(text, size):
    """
    Parse a coordinate string like 'A1' into (row, col).
    Returns (row, col) tuple or None on failure.
    """
    text = text.strip().upper()
    if len(text) < 2:
        return None

    row_char = text[0]
    if not row_char.isalpha():
        return None
    row = ord(row_char) - ord('A')
    if row < 0 or row >= size:
        return None

    col_str = text[1:]
    if not col_str.isdigit():
        return None
    col = int(col_str) - 1
    if col < 0 or col >= size:
        return None

    return (row, col)


def display_title():
    """Print the game title screen."""
    print()
    print("  +====================================================+")
    print("  |                                                    |")
    print("  |      /\\_/\\    M E O W D O K U                      |")
    print("  |     ( o.o )   Console Edition                      |")
    print("  |      > ^ <                                         |")
    print("  |     /|   |\\   A Cat Placement Puzzle               |")
    print("  |                                                    |")
    print("  |   MeowdoCrew | CS208 - Programming Languages      |")
    print("  |                                                    |")
    print("  +====================================================+")
    print()


def display_rules():
    """Print the game rules and command reference."""
    print()
    print("  ===== HOW TO PLAY =====")
    print()
    print("  Goal: Place exactly ONE cat (C) in each row, column,")
    print("        and region of the grid.")
    print()
    print("  Rules:")
    print("    1. Each ROW must contain exactly one cat.")
    print("    2. Each COLUMN must contain exactly one cat.")
    print("    3. Each REGION (marked by letters A, B, C...) must")
    print("       contain exactly one cat.")
    print("    4. No two cats can be ADJACENT to each other")
    print("       (horizontally, vertically, or diagonally).")
    print()
    print("  Commands:")
    print("    c <pos>   - Place/remove a cat (e.g., c A1)")
    print("    x <pos>   - Place/remove an elimination mark (e.g., x B3)")
    print("    e <pos>   - Clear a cell (e.g., e C2)")
    print("    check     - Validate your current board")
    print("    reset     - Clear the entire board")
    print("    rules     - Show these rules again")
    print("    quit      - Exit the game")
    print()


def display_win():
    """Print the victory message."""
    print()
    print("  +============================================+")
    print("  |                                            |")
    print("  |    /\\_/\\   CONGRATULATIONS!                |")
    print("  |   ( ^.^ )  You solved the puzzle!         |")
    print("  |    > ^ <   All cats are happy! =^.^=      |")
    print("  |                                            |")
    print("  +============================================+")


# ---------------------------------------------------------------------------
# Main game loop
# ---------------------------------------------------------------------------
def main():
    """Entry point for MeowDoku Console Edition."""
    display_title()

    # Puzzle selection
    print("  Select a puzzle:\n")
    for i, puzzle in enumerate(PUZZLES):
        print(f"    {i + 1}. {puzzle['name']}")
    print()

    try:
        choice = int(input(f"  Enter choice (1-{len(PUZZLES)}): ").strip())
    except (ValueError, EOFError):
        choice = 0

    if choice < 1 or choice > len(PUZZLES):
        print("  Invalid choice. Defaulting to puzzle 1.")
        choice = 1

    selected = PUZZLES[choice - 1]
    board = MeowdokuBoard(selected["regions"])

    print(f"\n  Loading: {selected['name']}")
    display_rules()

    # Game loop
    running = True
    while running:
        board.display()

        try:
            line = input("\n  >> ").strip()
        except (EOFError, KeyboardInterrupt):
            print("\n\n  Thanks for playing MeowDoku! Goodbye! =^.^=\n")
            break

        if not line:
            continue

        cmd_lower = line.lower()

        # Quit
        if cmd_lower in ("quit", "exit", "q"):
            print("\n  Thanks for playing MeowDoku! Goodbye! =^.^=\n")
            running = False

        # Help / Rules
        elif cmd_lower in ("rules", "help", "h"):
            display_rules()

        # Reset
        elif cmd_lower == "reset":
            board.reset_board()

        # Validate / Check
        elif cmd_lower in ("check", "validate"):
            print()
            if board.validate(verbose=True):
                display_win()
                try:
                    answer = input("\n  Play again? (y/n): ").strip().lower()
                except (EOFError, KeyboardInterrupt):
                    answer = "n"
                if not answer or answer[0] != 'y':
                    running = False
                else:
                    board.reset_board()

        # Placement commands: c, x, e
        elif len(cmd_lower) >= 3 and cmd_lower[0] in ('c', 'x', 'e') and cmd_lower[1] == ' ':
            cmd_char = cmd_lower[0]
            coord_str = line[2:].strip()
            coord = parse_coord(coord_str, board.size)

            if coord is not None:
                row, col = coord
                success = False

                if cmd_char == 'c':
                    success = board.place_cat(row, col)
                elif cmd_char == 'x':
                    success = board.place_mark(row, col)
                elif cmd_char == 'e':
                    success = board.clear_cell(row, col)

                # Auto-check for win after placing a cat
                if success and cmd_char == 'c' and board.validate(verbose=False):
                    board.display()
                    display_win()
                    try:
                        answer = input("\n  Play again? (y/n): ").strip().lower()
                    except (EOFError, KeyboardInterrupt):
                        answer = "n"
                    if not answer or answer[0] != 'y':
                        running = False
                    else:
                        board.reset_board()
            else:
                print("   [!] Invalid position. Use format like A1, B3, etc.")

        else:
            print("   [!] Unknown command. Type 'rules' for help.")


if __name__ == "__main__":
    main()
