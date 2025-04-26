import java.util.List;

public class CaptureTheFlag {

    // default values
    public static char flagPlacementType = 'A';
    public static int gridSize = 40;
    public static int numBlueAgents = 5;
    public static int numRedAgents = 5;
    public static int numFlagsPerTeam;

    // usage: java CaptureTheFlag GRID_SIZE NUM_BLUE_AGENTS NUM_RED_AGENTS FLAG_SPAWN_TYPE
    public static void main(String[] args) throws InterruptedException {
        if (args.length >= 1) gridSize = Integer.parseInt(args[0]);
        if (args.length >= 2) numBlueAgents = Integer.parseInt(args[1]);
        if (args.length >= 3) numRedAgents = Integer.parseInt(args[2]);
        if (args.length >= 4 && args[3].length() > 0) flagPlacementType = args[3].toUpperCase().charAt(0);

        if (gridSize < 10) {
            throw new IllegalArgumentException("grid size must be greater or equal to 10");
        }

        if (numBlueAgents > gridSize || numRedAgents > gridSize) {
            throw new IllegalArgumentException("number of agents per team must be lower or equal to the grid size");
        }

        if (!List.of('A', 'B', 'C').contains(flagPlacementType)) {
            throw new IllegalArgumentException("flag placement type must be A, B or C");
        }

        long startTime = System.nanoTime();

        numFlagsPerTeam = gridSize/2;
        Simulation simulation = new Simulation();
        simulation.init(gridSize, flagPlacementType, numBlueAgents, numRedAgents, numFlagsPerTeam);
        simulation.run();

        long endTime = System.nanoTime();
        System.out.println("[INFO] exec time: " + ((endTime - startTime) / 1_000_000) + " ms");
    }
}
