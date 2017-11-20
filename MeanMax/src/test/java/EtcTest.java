import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class EtcTest {

    /**
     * cloneは呼ばれたクラスのprimitiveなフィールどを全てcloneする。
     * Object型はnullになるのでoverrideして個別にコピーする必要がある
     * @throws CloneNotSupportedException
     */
    @Test
    public void testClone() throws CloneNotSupportedException{
        SubClass sc = new SubClass(5, 6);
        SubClass sc2 = (SubClass) sc.clone();

        // superのパラメータはコピーされるか
        assertEquals(5, sc2.a);
        assertEquals(2, sc2.list.size());
        assertEquals("yoyo", sc2.list.get(1));

        // subのprimitiveパラメータもコピーされるか
        assertEquals(5, sc2.x);
        assertEquals(6, sc2.y);
    }
}

class SubClass extends SuperClass{
    int x;
    int y;

    public SubClass(int x, int y) {
        this.x = x;
        this.y = y;
    }
}
class SuperClass implements Cloneable{
    int a = 5;
    List<String> list = Arrays.asList("yo", "yoyo");
    @Override
    protected SuperClass clone() throws CloneNotSupportedException {
        return (SuperClass) super.clone();
    }
}