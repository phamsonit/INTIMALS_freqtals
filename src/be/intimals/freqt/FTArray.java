package be.intimals.freqt;

import java.util.Arrays;

public class FTArray {

    private static int chunkSize = 512;
    protected int firstFree = 0;

    /*
    We store data in memory, until a storage of a datum outside the range of short happens.
    Then we migrate to intMemory, setting memory to null, and keep using intMemory forever.
     */
    protected short[] memory = new short[chunkSize];
    protected int[] intMemory = null;

    private void migrateMemory() {
        intMemory = new int[memory.length];
        for(int i=0; i<firstFree; i++ ){
            intMemory[i] = memory[i];
        }
        memory = null;
    }

    public FTArray(){
    }

    /**
     * Create an FTArray from an array of integers. !! THIS IS ONLY FOR TESTS !!
     * @param testData
     */
    public FTArray(int[] testData){
        for(int datum:testData) add(datum);
    }

    public FTArray(FTArray source){
        firstFree = source.firstFree;
        if (source.memory != null){
            memory = Arrays.copyOf(source.memory,source.memory.length);
        } else {
            memory = null;
            intMemory = Arrays.copyOf(source.intMemory,source.intMemory.length);
        }
    }

    public int get(int i){
        if ( i< 0 || i>= firstFree){
            System.out.println("Out of bounds access in FTArray.get(i). i is " + i + ", size is " + firstFree );
        }
        if (memory != null) return memory[i];
        return intMemory[i];
    }

     public void set(int index, int element){
        if ((memory != null) &&
            (element > Short.MAX_VALUE || element < Short.MIN_VALUE))
            migrateMemory();

        if(memory != null){
            ensureSpaceShort(index);
            memory[index] = (short) element;
        } else {
            ensureSpaceInt(index);
            intMemory[index] = element;
        }

        if(index >= firstFree)
            firstFree = index+1;
    }

    private void setIntMemory(int index, int element){
        ensureSpaceInt(index);
        intMemory[index] = element;
        if(index >= firstFree)
            firstFree = index+1;
    }

    public void add(int element){
        this.set(firstFree,element);
    }

    //this could be optimized with an array copy ?
    public void addAll(FTArray other){
        if(this.memory != null && other.memory != null){
            for(int i = 0; i<other.firstFree; i++)
                add(other.memory[i]);
        } else {
            if (this.memory != null) migrateMemory();
            for(int i = 0; i<other.firstFree; i++)
                this.setIntMemory(firstFree,other.intMemory[i]);
        }
    }

    //this could be optimised to revert from intMemory to memory if all values fit
    //but unsure whether this is worthwhile -- next additions could need intMemory again
    public FTArray subList(int start, int stop){
        FTArray result = new FTArray();
        result.firstFree = stop-start;

        if(memory != null) {
            result.ensureSpaceShort(result.firstFree);
            System.arraycopy(memory, start, result.memory, 0, result.firstFree);
        }
        else{
            result.memory = null;
            result.ensureSpaceInt(result.firstFree);
            System.arraycopy(intMemory,start,result.intMemory,0, result.firstFree);
        }

        return result;
    }

    public boolean equals(Object other){
        if(other.getClass() != this.getClass()) return false;
        FTArray otherArray = (FTArray)other;
        if(firstFree != otherArray.firstFree)  return false;

        if(memory != null && otherArray.memory != null)
            return Arrays.equals(memory,otherArray.memory);

        if(memory == null && otherArray.memory == null)
            return Arrays.equals(intMemory,otherArray.intMemory);

        if(memory != null){
            for(int i=0; i<firstFree; i++)
                if(memory[i] != otherArray.intMemory[i]) return false;
        } else {
            for(int i=0; i<firstFree; i++)
                if(intMemory[i] != otherArray.memory[i]) return false;
        }
        return true;
    }

    public int hashCode(){
        if (memory != null) return Arrays.hashCode(memory);
        return Arrays.hashCode(intMemory);
    }

    public int size(){
        return firstFree;
    }

    public boolean contains(int element) {
        return this.indexOf(element) != -1;
    }

    public int indexOf(int element) {
        if (memory != null) {
            for (int i = 0; i < firstFree; i++) {
                if (element == memory[i]) return i;
            }
        } else {
            for (int i = 0; i < firstFree; i++) {
                if (element == intMemory[i]) return i;
            }
        }
        return -1;
    }

    private void ensureSpaceShort(int index){
        if(index >= memory.length){
            int speculativeLength = memory.length + chunkSize;
            int newLength = (index >= speculativeLength) ? index + 1 :speculativeLength ;
            memory = Arrays.copyOf(memory,newLength);
        }
    }

    private void ensureSpaceInt(int index){
        if(index >= intMemory.length){
            int speculativeLength = intMemory.length + chunkSize;
            int newLength = (index >= speculativeLength) ? index + 1 :speculativeLength ;
            intMemory = Arrays.copyOf(intMemory,newLength);
        }
    }

}
