import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;


public class Grid {
    int size;
    CellType[][] cells;
    Random random; // used for spawning obstacles and flags (type C) based on a given seed (grid size)

    // helpers for printing the grid to the output
    static final String RESET      = "\u001B[0m";
    static final String BLUE_COLOR = "\u001B[94m";
    static final String RED_COLOR  = "\u001B[91m";

    static final String EMPTY               = ". ";
    static final String OBSTACLE            = "# ";

    static final String BLUE_AGENT          = BLUE_COLOR + "B " + RESET;
    static final String BLUE_AGENT_CAPTURED = BLUE_COLOR + "B*" + RESET;
    static final String BLUE_FLAG           = BLUE_COLOR + "X " + RESET;
    static final String BLUE_TRACE          = BLUE_COLOR + "• " + RESET;

    static final String RED_AGENT           = RED_COLOR + "R " + RESET;
    static final String RED_AGENT_CAPTURED  = RED_COLOR + "R*" + RESET;
    static final String RED_FLAG            = RED_COLOR + "Y " + RESET;
    static final String RED_TRACE           = RED_COLOR + "• " + RESET;

    static final int[][] DIRECTIONS = {{-1,0},{1,0},{0,-1},{0,1}};

    enum CellType {
        EMPTY,
        OBSTACLE,
        RED_FLAG,
        BLUE_FLAG,
        RED_AGENT,
        BLUE_AGENT,
        RED_AGENT_CAPTURED,
        BLUE_AGENT_CAPTURED,
        RED_TRACE,
        BLUE_TRACE
    }

    public Grid(int size) {
        this.size   = size;
        this.random = new Random(size);
        this.cells  = new CellType[size][size];
    }

    public void init() {
        for (int i = 0; i < size; i++) {
            Arrays.fill(cells[i], CellType.EMPTY);
        }
    }

    public void spawnObstacles() {
        int total = size * size;
        int count = (total / 10) & ~1; // force even
        int halfCount = count / 2;
        int placed = 0;

        while (placed < halfCount) {
            int x = random.nextInt(size);
            int y = random.nextInt(size);
            int yM = size - 1 - y;  // make spawning symmetric so that it is fair for both teams
            if (cells[x][y] == CellType.EMPTY && cells[x][yM] == CellType.EMPTY) {
                cells[x][y]  = CellType.OBSTACLE;
                cells[x][yM] = CellType.OBSTACLE;
                placed++;
            }
        }
    }

    public void spawnFlags(char placementType, List<int[]> redFlagPos, List<int[]> blueFlagPos, int numFlagsPerTeam) {
        switch (placementType) {
            case 'A': spawnFlagsTypeA(redFlagPos, blueFlagPos, numFlagsPerTeam); return;
            case 'B': spawnFlagsTypeB(redFlagPos, blueFlagPos, numFlagsPerTeam); return;
            case 'C': spawnFlagsTypeC(redFlagPos, blueFlagPos, numFlagsPerTeam); return;
            default: throw new IllegalArgumentException("Unknown flag type: " + placementType);
        }
    }

    public void spawnFlagsTypeA(List<int[]> red, List<int[]> blue, int n) {
        for (int i = 0; i < n; i++) { // top row = red
            int x = 0, y = (i * size) / n;
            placeFlag(x, y, CellType.RED_FLAG, red);
        }
        for (int i = 0; i < n; i++) { // bottom row = blue
            int x = size - 1, y = (i * size) / n;
            placeFlag(x, y, CellType.BLUE_FLAG, blue);
        }
    }

    // spread flags starting from the center of the grid
    public void spawnFlagsTypeB(List<int[]> red, List<int[]> blue, int n) {
        int center = size / 2;
        boolean[][] visited = new boolean[size][size];
        Queue<int[]> q = new LinkedList<>();
        q.add(new int[]{center, center});
        visited[center][center] = true;

        // alternate placements between blue and red flags
        int placedRed = 0, placedBlue = 0;
        boolean redNext = true;

        while (!q.isEmpty() && (placedRed < n || placedBlue < n)) {
            int[] p = q.poll();
            int x = p[0];
            int y = p[1];

            if (isValidFlagSpot(x, y)) {
                if (redNext && placedRed < n) {
                    placeFlag(x, y, CellType.RED_FLAG, red);
                    placedRed = red.size();
                    redNext = false;
                } else if (!redNext && placedBlue < n) {
                    placeFlag(x, y, CellType.BLUE_FLAG, blue);
                    placedBlue = blue.size();
                    redNext = true;
                }
            }
            for (int[] dir : DIRECTIONS) {
                int nx = x + dir[0];
                int ny = y + dir[1];
                if (inBounds(nx, ny) && !visited[nx][ny]) {
                    q.add(new int[]{nx, ny});
                    visited[nx][ny] = true;
                }
            }
        }
    }

    // spread flags across entire grid
    public void spawnFlagsTypeC(List<int[]> red, List<int[]> blue, int n) {
        for (int i = 0; i < n; i++) {
            int x, y;
            do {
                x = random.nextInt(size);
                y = random.nextInt(size);
            } while (!isValidFlagSpot(x, y));
            placeFlag(x, y, CellType.RED_FLAG, red);
        }
        for (int i = 0; i < n; i++) {
            int x, y;
            do {
                x = random.nextInt(size);
                y = random.nextInt(size);
            } while (!isValidFlagSpot(x, y));
            placeFlag(x, y, CellType.BLUE_FLAG, blue);
        }
    }

    private void placeFlag(int x, int y, CellType flagType, List<int[]> outList) {
        cells[x][y] = flagType;
        outList.add(new int[]{x, y});
        clearSurroundings(x, y);
    }

    private boolean isValidFlagSpot(int x, int y) {
        if (!inBounds(x, y) || cells[x][y] != CellType.EMPTY) return false;
        // ensure there are no adjacent flags
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue;
                int nx = x + dx, ny = y + dy;
                if (inBounds(nx, ny) && (cells[nx][ny] == CellType.RED_FLAG || cells[nx][ny] == CellType.BLUE_FLAG)) {
                    return false;
                }
            }
        }
        return true;
    }

    // ensure all neighbor cells around a flag are empty so that agents can always step adjacent to flags
    public void clearSurroundings(int x, int y) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue;
                int nx = x + dx, ny = y + dy;
                if (inBounds(nx, ny) &&
                   (cells[nx][ny] != CellType.EMPTY && cells[nx][ny] != CellType.RED_FLAG && cells[nx][ny] != CellType.BLUE_FLAG)) {
                    cells[nx][ny] = CellType.EMPTY;
                }
            }
        }
    }

    private void placeAgents(List<Agent> team, CellType cellType, int startRow, int startCol, int maxAgents, boolean isBlue) {
        boolean[][] visited = new boolean[this.size][this.size];
        Queue<int[]> q = new LinkedList<>();
        q.add(new int[]{startRow, startCol});
        visited[startRow][startCol] = true;
        while (!q.isEmpty() && team.size() < maxAgents) {
            int[] p = q.poll();
            int x = p[0];
            int y = p[1];
            if (this.cells[x][y] == CellType.EMPTY) {
                team.add(new Agent(x, y, isBlue));
                this.cells[x][y] = cellType;
            }
            for (int[] dir : DIRECTIONS) {
                int nx = x + dir[0];
                int ny = y + dir[1];
                if (inBounds(nx, ny) && !visited[nx][ny]) {
                    q.add(new int[]{nx, ny});
                    visited[nx][ny] = true;
                }
            }
        }
    }

    public void spawnAgents(List<Agent> blueAgents, List<Agent> redAgents, int num_blue_agents, int num_red_agents) {
        placeAgents(blueAgents, CellType.BLUE_AGENT, this.size/2, 0, num_blue_agents, true);
        placeAgents(redAgents, CellType.RED_AGENT, this.size/2, this.size-1, num_red_agents, false);
    }

    public boolean inBounds(int x, int y) {
        return x >= 0 && x < this.size && y >= 0 && y < this.size;
    }

    public boolean cellHasAgent(int x, int y) {
        switch (this.cells[x][y]) {
            case BLUE_AGENT, BLUE_AGENT_CAPTURED, RED_AGENT, RED_AGENT_CAPTURED: return true;
            default: return false;
        }
    }

    public boolean cellHasObstacle(int x, int y) {
        return this.cells[x][y] == CellType.OBSTACLE;
    }

    public boolean cellHasFlag(int x, int y) {
        return this.cells[x][y] == CellType.BLUE_FLAG || this.cells[x][y] == CellType.RED_FLAG;
    }

    public void setCellAgent(int x, int y, boolean isBlue) {
        cells[x][y] = isBlue ? CellType.BLUE_AGENT : CellType.RED_AGENT;
    }

    public void setCellAgentCaptured(int x, int y, boolean isBlue) {
        cells[x][y] = isBlue ? CellType.BLUE_AGENT_CAPTURED : CellType.RED_AGENT_CAPTURED;
    }

    public void setCellTrace(int x, int y, boolean isBlue) {
        cells[x][y] = isBlue ? CellType.BLUE_TRACE : CellType.RED_TRACE;
    }

    public int getSize() {
        return size;
    }

    public void printBoard() {
        for (int i = 0; i < this.size; i++) {
            for (int j = 0; j < this.size; j++) {
                switch (this.cells[i][j]) {
                    case EMPTY:                 System.out.print(EMPTY); break;
                    case OBSTACLE:              System.out.print(OBSTACLE); break;
                    case BLUE_AGENT:            System.out.print(BLUE_AGENT); break;
                    case BLUE_AGENT_CAPTURED:   System.out.print(BLUE_AGENT_CAPTURED); break;
                    case BLUE_FLAG:             System.out.print(BLUE_FLAG); break;
                    case BLUE_TRACE:            System.out.print(BLUE_TRACE); break;
                    case RED_AGENT:             System.out.print(RED_AGENT); break;
                    case RED_AGENT_CAPTURED:    System.out.print(RED_AGENT_CAPTURED); break;
                    case RED_FLAG:              System.out.print(RED_FLAG); break;
                    case RED_TRACE:             System.out.print(RED_TRACE); break;
                }
            }
            System.out.println();
        }
    }
}
