import java.lang.reflect.Array;
import java.util.*;
import java.util.function.Consumer;

/**
 * Created by s-sumi on 2017/08/17.
 */
public class UtilForTest {
    static Thing generateDefaultThing(){
        return new Thing(1, 2, 3, 4, 5, 6, "WIZARD");
    }
    static State generateDefaultState(){
        Thing t = generateDefaultThing();
        List<Wizard> a = new ArrayList<>();
        a.add(new Wizard(t));
        List<Thing> b = new ArrayList<>();
        b.add(t);
        List<Snaffle> c = new ArrayList<>();
        c.add(new Snaffle(t));
        List<Thing> d = new ArrayList<>();
        d.add(t);
        return new State(a, b, c, d, 10, 20, 30, 40,1);
    }

    //consumer:ラムダ式を返すメソッド
    static Consumer<Integer> consumeInteger(int[] arr){
        return (num)->arr[0]+=num;
    }
}

class ClassA {
    int a;
    int b;
    int c;

    public ClassA(int a, int b, int c) {
        this.a = a;
        this.b = b;
        this.c = c;
    }
}

class ClassB extends ClassA {
    public ClassB(int a, int b, int c, int d) {
        super(a, b, c);
        this.d = d;
    }

    int d;
}

class DeepClone {

    private DeepClone(){}

    public static <X> X deepClone(final X input) {
        if (input == null) {
            return input;
        } else if (input instanceof Map<?, ?>) {
            return (X) deepCloneMap((Map<?, ?>) input);
        } else if (input instanceof Collection<?>) {
            return (X) deepCloneCollection((Collection<?>) input);
        } else if (input instanceof Object[]) {
            return (X) deepCloneObjectArray((Object[]) input);
        } else if (input.getClass().isArray()) {
            return (X) clonePrimitiveArray((Object) input);
        }

        return input;
    }

    private static Object clonePrimitiveArray(final Object input) {
        final int length = Array.getLength(input);
        final Object copy = Array.newInstance(input.getClass().getComponentType(), length);
        // deep clone not necessary, primitives are immutable
        System.arraycopy(input, 0, copy, 0, length);
        return copy;
    }

    private static <E> E[] deepCloneObjectArray(final E[] input) {
        final E[] clone = (E[]) Array.newInstance(input.getClass().getComponentType(), input.length);
        for (int i = 0; i < input.length; i++) {
            clone[i] = deepClone(input[i]);
        }

        return clone;
    }

    private static <E> Collection<E> deepCloneCollection(final Collection<E> input) {
        Collection<E> clone;
        // this is of course far from comprehensive. extend this as needed
        if (input instanceof LinkedList<?>) {
            clone = new LinkedList<E>();
        } else if (input instanceof SortedSet<?>) {
            clone = new TreeSet<E>();
        } else if (input instanceof Set) {
            clone = new HashSet<E>();
        } else {
            clone = new ArrayList<E>();
        }

        for (E item : input) {
            clone.add(deepClone(item));
        }

        return clone;
    }

    private static <K, V> Map<K, V> deepCloneMap(final Map<K, V> map) {
        Map<K, V> clone;
        // this is of course far from comprehensive. extend this as needed
        if (map instanceof LinkedHashMap<?, ?>) {
            clone = new LinkedHashMap<K, V>();
        } else if (map instanceof TreeMap<?, ?>) {
            clone = new TreeMap<K, V>();
        } else {
            clone = new HashMap<K, V>();
        }

        for (Map.Entry<K, V> entry : map.entrySet()) {
            clone.put(deepClone(entry.getKey()), deepClone(entry.getValue()));
        }

        return clone;
    }
}