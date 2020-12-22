import com.codingame.gameengine.runner.MultiplayerGameRunner;

public class ChessMain {
    public static void main(String[] args) {
        MultiplayerGameRunner gameRunner = new MultiplayerGameRunner();

        gameRunner.addAgent(RandomAgent.class);
        gameRunner.addAgent(RandomAgent.class);
        //gameRunner.addAgent(RepetitionAgent.class);
        //gameRunner.addAgent(RepetitionAgent.class);

        gameRunner.start();
    }
}
