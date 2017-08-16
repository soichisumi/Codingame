/**
 * Created by s-sumi on 2017/08/15.
 */

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class StateTest {
    @Test
    public void testStateCloneUnchange() {
        Thing t = new Thing(1, 2, 3, 4, 5, 6, "type");
        Map<Integer, Wizard> a = new HashMap<>();
        a.put(1, new Wizard(t));
        Map<Integer, Thing> b = new HashMap<>();
        b.put(2, t);
        Map<Integer, Snaffle> c = new HashMap<>();
        c.put(3, new Snaffle(t));
        Map<Integer, Thing> d = new HashMap<>();
        d.put(4, t);
        State state = new State(a, b, c, d, 10, 20, 30, 40);
        State state2 = state.clone();
        System.out.println("state1: " + state.toString() + "¥n");
        System.out.println("state2: " + state.toString() + "¥n");
        assertEquals(state.toString(), state2.toString());
    }
    @Test
    public void testStateCloneChange() {
        Thing t = new Thing(1, 2, 3, 4, 5, 6, "type");
        Map<Integer, Wizard> a = new HashMap<>();
        a.put(1, new Wizard(t));
        Map<Integer, Thing> b = new HashMap<>();
        b.put(2, t);
        Map<Integer, Snaffle> c = new HashMap<>();
        c.put(3, new Snaffle(t));
        Map<Integer, Thing> d = new HashMap<>();
        d.put(4, t);
        State state = new State(a, b, c, d, 10, 20, 30, 40);
        State state2 = state.clone();
        state.
        System.out.println("state1: " + state.toString() + "¥n");
        System.out.println("state2: " + state.toString() + "¥n");
        assertEquals(state.toString(), state2.toString());
    }
}
