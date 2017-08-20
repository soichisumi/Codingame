/**
 * Created by s-sumi on 2017/08/15.
 */

import org.junit.Test;

import static org.junit.Assert.*;

public class StateTest {
    @Test
    public void testStateCloneUnchange() {
        State state = UtilForTest.generateDefaultState();
        State state2 = state.clone();
        System.out.println("state1: " + state.toString() + "¥n");
        System.out.println("state2: " + state2.toString() + "¥n");
        assertEquals(state.toString(), state2.toString());
    }
    @Test
    public void testStateCloneChange() {
        State state = UtilForTest.generateDefaultState();
        State state2 = state.clone();
        state.wizards.get(0).x = 500;
        System.out.println("state1: " + state.toString() + "¥n");
        System.out.println("state2: " + state2.toString() + "¥n");
        assertNotEquals(state.toString(), state2.toString());
    }
}
