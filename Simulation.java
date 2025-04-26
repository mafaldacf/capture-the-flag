
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;


public class Simulation {
    Grid grid;
    int blueScore;
    int redScore;
    boolean moveDetected;
    List<int[]> blueFlagPositions;
    List<int[]> redFlagPositions;
    List<Agent> blueAgents;
    List<Agent> redAgents;
    int numFlagsPerTeam;

    static final int[][] DIRECTIONS = {{-1,0},{1,0},{0,-1},{0,1}};

    public Simulation() {
        this.blueScore = 0;
        this.redScore = 0;
        this.blueFlagPositions = new ArrayList<>();
        this.redFlagPositions = new ArrayList<>();
        this.blueAgents = new ArrayList<>();
        this.redAgents = new ArrayList<>();
    }

    public void init(int gridSize, char flagPlacementType, int numBlueAgents, int numRedAgents, int numFlagsPerTeam) {
        this.grid = new Grid(gridSize);
        this.numFlagsPerTeam = numFlagsPerTeam;
        grid.init();
        grid.spawnObstacles();
        grid.spawnFlags(flagPlacementType, this.redFlagPositions, this.blueFlagPositions, numFlagsPerTeam);
        grid.spawnAgents(this.blueAgents, this.redAgents, numBlueAgents, numRedAgents);
    }

    public void run() throws InterruptedException {
        int round = 0;

        System.out.println("[INFO] round = " + round + "; score (blue, red) = (" + blueScore + ", " + redScore + ")");
        this.grid.printBoard();

        while (true) {
            round++;
            moveDetected = false;
            moveTeam(blueAgents, blueFlagPositions, blueScore);
            moveTeam(redAgents,  redFlagPositions,  redScore);
            checkDeadlock();
            //this.grid.print(round, blueScore, redScore);
            //Thread.sleep(200);
            if (checkWin()) break;
        }

        this.grid.printBoard();

        System.out.println("[INFO] final score (blue, red) = (" + blueScore + ", " + redScore + ")");
        System.out.println("[INFO] num rounds = " + round);
    }

    public boolean checkWin() {
        return blueScore == numFlagsPerTeam || redScore == numFlagsPerTeam;
    }

    // sanity check
    public void checkDeadlock() {
        if (!moveDetected) {
            boolean anyFrozen = true;
            for (Agent a : blueAgents) {
                if (a.cooldown > 0) { anyFrozen = false; break; }
            }
            if (anyFrozen) {
                for (Agent a : redAgents) {
                    if (a.cooldown > 0) { anyFrozen = false; break; }
                }
            }
            if (anyFrozen) {
                System.out.println("[WARN] no possible moves for game state:");
                for (Agent a : blueAgents) {
                    System.out.println("- blue agent @ (" + a.x + "," + a.y + ")" + ", cooldown = " + a.cooldown);
                }
                for (Agent a : redAgents) {
                    System.out.println("- red agent @ (" + a.x + "," + a.y + ")" + ", cooldown = " + a.cooldown);
                }
                throw new RuntimeException("no possible moves");
            }
        }
    }

    public boolean checkCooldown(Agent agent) {
        if (agent.cooldown > 0) {
            agent.tickCooldown();
            if (agent.cooldown == 0) {
                this.grid.setCellAgent(agent.x, agent.y, agent.isBlue());
                return true;
            }
            return false;
        }
        return true;
    }

    public void moveTeam(List<Agent> team, List<int[]> flagPositions, int score) {
        for (Agent agent : team) {
            boolean canMove = checkCooldown(agent);
            if (!canMove) continue; // skip this agent

            int[] flagPos = findNearestPosition(agent.x, agent.y, flagPositions);
            if (flagPos == null) continue;
            int tx = flagPos[0]; 
            int ty = flagPos[1];

            int[] next = bfsNextStep(agent.x, agent.y, tx, ty);
            if (next == null || this.grid.cellHasAgent(next[0], next[1])) continue;
            moveDetected = true;

            this.grid.setCellTrace(agent.x, agent.y, agent.isBlue());

            agent.x = next[0]; 
            agent.y = next[1];

            Iterator<int[]> pickIt = flagPositions.iterator();
            while (pickIt.hasNext()) {
                int[] p = pickIt.next();
                if (p[0] == agent.x && p[1] == agent.y) {
                    pickIt.remove();
                    if (agent.isBlue()) blueScore++; else redScore++;
                    agent.setCooldown();
                    break;
                }
            }
            
            if (agent.hasCooldown()) {
                this.grid.setCellAgentCaptured(agent.x, agent.y, agent.isBlue());
            } else {
                this.grid.setCellAgent(agent.x, agent.y, agent.isBlue());
            }
        }
    }

    public int[] findNearestPosition(int x, int y, List<int[]> posLst) {
        int best = 2*(this.grid.getSize()-1); // max from corners, e.g., [0, 0] to [n-1, n-1]
        int bx = -1;
        int by = -1;

        for (int[] pos : posLst) {
            // does not take any obstacles into account -- just raw distance
            int dis = Math.abs(pos[0] - x) + Math.abs(pos[1] - y); // Manhattan distance
            if (dis < best) { 
                best = dis; 
                bx = pos[0]; 
                by = pos[1]; 
            }
        }
        if (bx < 0) {
            return null;
        }
        return new int[]{bx, by};
    }

    public int[] bfsNextStep(int sx, int sy, int tx, int ty) {
        int gridSize = this.grid.getSize();
        Queue<int[]> q = new LinkedList<>();
        q.add(new int[]{sx, sy});
        boolean[][] visited = new boolean[gridSize][gridSize];
        visited[sx][sy] = true;

        // helpers for reconstructing path and return next step
        Map<Integer, Integer> parent = new HashMap<>();
        parent.put(sx * gridSize + sy, -1);

        while (!q.isEmpty()) {
            int[] c = q.poll();
            if (c[0] == tx && c[1] == ty) break;

            for (int[] dir : DIRECTIONS) {
                int nx = c[0] + dir[0];
                int ny = c[1] + dir[1];

                if (this.grid.inBounds(nx, ny) && !visited[nx][ny]) {
                    if (this.grid.cellHasFlag(nx, ny) && (nx != tx || ny != ty) 
                        || this.grid.cellHasAgent(nx, ny) 
                        || this.grid.cellHasObstacle(nx, ny)) 
                        continue;

                    q.add(new int[]{nx, ny});
                    visited[nx][ny] = true;

                    int key = nx * gridSize + ny;
                    int value = c[0] * gridSize + c[1];
                    parent.put(key, value);
                }

            }
        }
        if (!visited[tx][ty])
            return null; // no path found

        // go backwards on constructed path and extract next move
        int key = tx * gridSize + ty;
        int p = parent.get(key);
        while (parent.get(p) != -1) { 
            key = p; 
            p = parent.get(key); 
        }
        int nx = key / gridSize;
        int ny = key % gridSize;
        return new int[]{nx, ny};
    }
}
