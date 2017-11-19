import org.junit.Test;
import static org.junit.Assert.*;

public class UtilTest {

    @Test
    public void testDist(){
        Point a = new Point(-2536, 3819);
        Point b = new Point(-4496, 2233);
        double res= Util.getDistance(a,b);
        assertEquals(res, 2521.30839, 1);
    }
}
