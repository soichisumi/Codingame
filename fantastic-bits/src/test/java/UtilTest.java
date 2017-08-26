import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by s-sumi on 2017/08/17.
 */
public class UtilTest {
    @Test
    public void testDeepCloneUnchange() {
        State s = UtilForTest.generateDefaultState();
        State s2 = DeepClone.deepClone(s);
        assertEquals(s.toString(), s2.toString());
    }

    //これが通るのでdeepcloneは自作クラスには使えない
    @Test
    public void testDeepCloneChange() {
        State s = UtilForTest.generateDefaultState();
        State s2 = DeepClone.deepClone(s);
        s.wizards.get(0).x = 500;
        assertEquals(s.toString(), s2.toString());
    }

    @Test
    public void testMathRound() {
        assertEquals(1, Math.round(0.5));
        assertEquals(0, Math.round(0.49999999999999));
        assertEquals(1, Math.round(0.49999999999999999999999999));

        assertEquals(0, Math.round(-0.4));
        assertEquals(0, Math.round(-0.5));  //-0.5 + 0.5 =0 -> return 0 かな
        assertEquals(-1, Math.round(-0.52));
        assertEquals(-1, Math.round(-0.50000000000001));
    }

    @Test
    public void testThingBound() {
        Thing t = UtilForTest.generateDefaultThing();
        t.x = -100;
        t.y = 7600;
        t.bound();
        assertEquals(900, t.x, 1E-6);
        assertEquals(6600, t.y, 1E-6);
    }

    //このテストは通るので、親クラスを返すメソッドを作って、利用側でアップキャストして使っても良い
    @Test
    public void whetherDowncastCanBeRestored() {
        ClassB var = new ClassB(1, 2, 3, 4);
        ClassA tmp = (ClassA) var;
        assertEquals(1, tmp.a);
        assertEquals(2, tmp.b);
        assertEquals(3, tmp.c);

        ClassB tmp2 = (ClassB) tmp;
        assertEquals(1, tmp2.a);
        assertEquals(2, tmp2.b);
        assertEquals(3, tmp2.c);
        assertEquals(4, tmp2.d);
    }

    @Test
    public void testGetRadian90Deg() {
        Thing a = UtilForTest.generateDefaultThing();
        a.x = 0;
        a.y = 0;

        Thing b = UtilForTest.generateDefaultThing();
        b.x = 0;
        b.y = 500;

        double res = Util.getRadianAngle(a, b);
        assertEquals(Math.toRadians(90), res, 1E-6);
    }

    @Test
    public void testTreeSet_Poll() {
        TreeSet<Integer> t = new TreeSet<>();
        t.add(1);
        t.add(2);
        t.add(3);
        t.add(4);
        System.out.println("t:" + t.toString());
        assertEquals(Integer.valueOf(1), t.first());
        assertEquals(Integer.valueOf(4), t.last());

        Integer first = t.pollFirst();
        Integer last = t.pollLast();
        System.out.println("polled t:" + t.toString());
        assertEquals(Integer.valueOf(1), first);
        assertEquals(Integer.valueOf(4), last);
    }

    @Test
    public void testTreeSet_Poll2() {
        State x1 = UtilForTest.generateDefaultState();
        x1.score = 100;
        State x2 = UtilForTest.generateDefaultState();
        x2.score = 200;

        TreeSet<State> t = new TreeSet<>();
        t.add(x1);
        t.add(x2);
        System.out.println("t:" + t.toString());
        assertEquals(200, t.first().score, 1e-6);
        assertEquals(100, t.last().score, 1e-6);

        State first = t.pollFirst();
        State last = t.pollLast();
        System.out.println("polled t:" + t.toString());
        assertEquals(200, first.score, 1e-6);
        assertEquals(100, last.score, 1e-6);
    }
    @Test
    public void testTreeSet_Poll3() {
        State x1 = UtilForTest.generateDefaultState();
        x1.score = 100;

        TreeSet<State> t = new TreeSet<>();
        t.add(x1);
        System.out.println("t:" + t.toString());
        assertEquals(100, t.pollFirst().score, 1e-6);

    }

    @Test
    public void testConsumer() {
        final int[] sum = {0};
        List<Integer> list = new ArrayList<>();
        list.add(1);
        list.add(2);
        list.add(3);
        list.forEach(UtilForTest.consumeInteger(sum));
    }

    @Test
    public void testCollections_removeIf(){
        List<Integer> list=new ArrayList<>();
        list.add(1);
        list.add(2);
        list.add(3);
        list.removeIf((i)->i==3);
        assertFalse(list.contains(3));
    }

    @Test
    public void testDestGeneration(){
        for(int dir=0;dir<CONST.RADIANS.length;dir++) {
            int destX = Util.getMoveTargetDiffX(CONST.RADIANS[dir]);
            int destY = Util.getMoveTargetDiffY(CONST.RADIANS[dir]);
            System.out.println("x: "+destX);
            System.out.println("y: "+destY);
        }
    }
    @Test
    public void testGetRadian(){
        Thing t1 = UtilForTest.generateDefaultThing();
        t1.x=0;
        t1.y=0;

        Thing t2 = UtilForTest.generateDefaultThing();
        t2.x=1;
        t2.y=1;
        assertEquals(45,Math.toDegrees(Util.getRadianAngle(t1,t2)),1e-6);

        Thing t3 = UtilForTest.generateDefaultThing();
        t3.x=0;
        t3.y=1;
        assertEquals(90,Math.toDegrees(Util.getRadianAngle(t1,t3)),1e-6);

        Thing t4 = UtilForTest.generateDefaultThing();
        t4.x=-1;
        t4.y=-1;
        assertEquals(-135,Math.toDegrees(Util.getRadianAngle(t1,t4)),1e-6);
    }

    @Test
    public void testCos(){
        assertEquals(1.0,Math.cos(Math.toRadians(360)),1e-6);
    }

    /*@Test
    public void testCheckGoal(){
        State s =UtilForTest.generateDefaultState();
        s.snaffles.clear();
        Thing t = UtilForTest.generateDefaultThing();
        t.x=-50;
        t.y=50;
        s.snaffles.add(new Snaffle(t));

        t.x=50;
        s.snaffles.add(new Snaffle(t));

        t.x =CONST.FIELD_Xmax-50;
        s.snaffles.add(new Snaffle(t));

        t.x =CONST.FIELD_Xmax+50;
        s.snaffles.add(new Snaffle(t));

        assertEquals(s.snaffles.size(), 4);

        Player.removeOuterSnaffles(s);

        assertEquals(s.snaffles.size(), 2);
        assertEquals((int) s.snaffles.get(0).x, 50);
        assertEquals((int) s.snaffles.get(1).x, CONST.FIELD_Xmax-50);
    }*/

}
