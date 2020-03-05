package be.intimals.freqt.structure;

import be.intimals.freqt.structure.FTArray;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class FTArrayTest {


    @Test
    public void testAdd(){
        FTArray arr = new FTArray();
        arr.add(1);
        arr.add(2);
        arr.add(3);

        assertEquals(1,arr.get(0));
        assertEquals(2,arr.get(1));
        assertEquals(3,arr.get(2));
        assertEquals(3,arr.size());
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> arr.get(3));
    }

    @Test
    public void testLast() {
        FTArray arr = new FTArray();
        arr.add(1);
        arr.add(2);
        arr.add(3);

        assertEquals(3,arr.getLast());
    }

    @Test
    public void testConstructorWithArgument(){
        FTArray arr = new FTArray();
        arr.add(1);
        arr.add(2);
        arr.add(3);

        FTArray arr2 = new FTArray(arr);
        arr = null; //just to be sure the rest of the test does not mistakenly use it

        assertEquals(1,arr2.get(0));
        assertEquals(2,arr2.get(1));
        assertEquals(3,arr2.get(2));
        assertEquals(3,arr2.size());
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> arr2.get(3));
    }

    @Test
    public void testConstructorWithArgumentWithInt(){
        FTArray arr = new FTArray();
        arr.add(1);
        arr.add(Short.MAX_VALUE+1);
        arr.add(3);

        FTArray arr2 = new FTArray(arr);
        arr = null; //just to be sure the rest of the test does not mistakenly use it

        assertEquals(1,arr2.get(0));
        assertEquals(Short.MAX_VALUE+1,arr2.get(1));
        assertEquals(3,arr2.get(2));
        assertEquals(3,arr2.size());
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> arr2.get(3));
    }


    @Test
    public void testAddAll(){
        FTArray arr = new FTArray();
        arr.add(1);
        arr.add(2);
        arr.add(3);

        FTArray arr2 = new FTArray();
        arr2.add(4);
        arr2.add(5);

        arr.addAll(arr2);

        assertEquals(1,arr.get(0));
        assertEquals(2,arr.get(1));
        assertEquals(3,arr.get(2));
        assertEquals(4,arr.get(3));
        assertEquals(5,arr.get(4));
        assertEquals(5,arr.size());
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> arr.get(6));

    }

    @Test
    public void testAddAllWithIntegerInReceiver(){
        FTArray arr = new FTArray();
        arr.add(1);
        arr.add(2);
        arr.add(Short.MAX_VALUE+1);

        FTArray arr2 = new FTArray();
        arr2.add(4);
        arr2.add(5);

        arr.addAll(arr2);

        assertEquals(1,arr.get(0));
        assertEquals(2,arr.get(1));
        assertEquals(Short.MAX_VALUE+1,arr.get(2));
        assertEquals(4,arr.get(3));
        assertEquals(5,arr.get(4));
        assertEquals(5,arr.size());
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> arr.get(6));

    }

    @Test
    public void testAddAllWithIntegerInArgument(){
        FTArray arr = new FTArray();
        arr.add(1);
        arr.add(2);
        arr.add(3);

        FTArray arr2 = new FTArray();
        arr2.add(4);
        arr2.add(Short.MAX_VALUE+1);

        arr.addAll(arr2);

        assertEquals(1,arr.get(0));
        assertEquals(2,arr.get(1));
        assertEquals(3,arr.get(2));
        assertEquals(4,arr.get(3));
        assertEquals(Short.MAX_VALUE+1,arr.get(4));
        assertEquals(5,arr.size());
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> arr.get(6));

    }

    @Test
    public void testMemoryMigration(){
        FTArray arr = new FTArray();
        arr.add(1);
        arr.add(2);
        arr.add(Short.MAX_VALUE+1);

        assertEquals(1,arr.get(0));
        assertEquals(2,arr.get(1));
        assertEquals(Short.MAX_VALUE+1,arr.get(2));
        assertEquals(3,arr.size());
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> arr.get(3));
    }

    @Test
    public void testEnsureSpace(){

        FTArray arr = new FTArray(new int[511]);
        assertEquals(0,arr.get(510));
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> arr.get(511));
        arr.add(1);
        arr.add(2);
        arr.add(3);

        assertEquals(0,arr.get(510));
        assertEquals(1,arr.get(511));
        assertEquals(2,arr.get(512));
        assertEquals(3,arr.get(513));
        assertEquals(514,arr.size());
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> arr.get(514));
    }

    @Test
    void testIndexOf(){
        FTArray arr = new FTArray();
        arr.add(1);
        arr.add(2);
        arr.add(3);

        assertEquals(0,arr.indexOf(1));
        assertEquals(-1,arr.indexOf(42));
    }

    @Test
    void testContains(){
        FTArray arr = new FTArray();
        arr.add(1);
        arr.add(2);
        arr.add(3);

        assertTrue(arr.contains(1));
        assertFalse(arr.contains(42));
    }

    @Test
    void testIndexOfWithInteger(){
        FTArray arr = new FTArray();
        arr.add(1);
        arr.add(Short.MAX_VALUE+1);
        arr.add(3);

        assertEquals(0,arr.indexOf(1));
        assertEquals(-1,arr.indexOf(42));
    }

    @Test
    void testContainsWithInteger(){
        FTArray arr = new FTArray();
        arr.add(Short.MAX_VALUE+1);
        arr.add(2);
        arr.add(3);

        assertTrue(arr.contains(Short.MAX_VALUE+1));
        assertFalse(arr.contains(42));
    }

    @Test
    void testEquals(){
        FTArray arr1 = new FTArray();
        arr1.add(1);
        arr1.add(2);
        arr1.add(3);

        FTArray arr1bis = new FTArray();
        arr1bis.add(1);
        arr1bis.add(2);
        arr1bis.add(3);

        FTArray arr2 = new FTArray();
        arr2.add(2);
        arr2.add(3);

        assertEquals(arr1,arr1bis);
        assertNotEquals(arr1, arr2);
        assertNotEquals(arr1bis, arr2);
        assertNotEquals(arr1,new FTArray());
        assertNotEquals(arr1, 1234);
    }

    @Test
    void testEqualsInt(){
        FTArray arr1 = new FTArray();
        arr1.add(Short.MAX_VALUE+1);
        arr1.add(2);
        arr1.add(3);

        FTArray arr1bis = new FTArray();
        arr1bis.add(Short.MAX_VALUE+1);
        arr1bis.add(2);
        arr1bis.add(3);

        FTArray arr2 = arr1.subList(1,3);
        FTArray arr2bis = new FTArray();
        arr2bis.add(2);
        arr2bis.add(3);

        assertEquals(arr1,arr1bis);
        assertEquals(arr2,arr2bis);
        assertEquals(arr2bis,arr2);
    }

    @Test
    void testHashCode(){
        FTArray arr1 = new FTArray();
        arr1.add(1);
        arr1.add(2);
        arr1.add(3);

        FTArray arr1bis = new FTArray();
        arr1bis.add(1);
        arr1bis.add(2);
        arr1bis.add(3);

        FTArray arr2 = new FTArray();
        arr2.add(Short.MAX_VALUE+1);
        arr2.add(3);

        assertEquals(arr1.hashCode(),arr1bis.hashCode());
        assertNotEquals(arr1.hashCode(), arr2.hashCode());
        assertNotEquals(arr1bis.hashCode(), arr2.hashCode());
        assertNotEquals(arr1.hashCode(),new FTArray().hashCode());
    }

    @Test
    void testSubList(){
        FTArray arr1 = new FTArray();
        arr1.add(1);
        arr1.add(2);
        arr1.add(3);
        arr1.add(4);

        FTArray arr2 = arr1.subList(1,3);
        assertEquals(2,arr2.size());
        assertEquals(2,arr2.get(0));
        assertEquals(3,arr2.get(1));
    }
}
