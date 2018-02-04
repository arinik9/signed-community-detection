package Network.Core;

import org.junit.Test;

public class GraphTest {

    @Test
    public void testTransitionProbability() throws Exception{
        Graph graph = GraphReader.readGraph("testCases/triad.txt", true, 3);
        Graph transition = graph.getTransitionProbability();
        float[][] expectedWeights = {{0.25f, 0.75f}, {0.5f, 0.5f}, {0.75f, 0.25f}};
        MyAssert.assertArrayEquals(expectedWeights, transition.getValues());
    }
}
