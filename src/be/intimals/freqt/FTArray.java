package be.intimals.freqt;

import java.util.Arrays;

public class FTArray {

    private static int chunkSize = 512;
    protected int firstFree = 0;
    protected int[] memory = new int[chunkSize];

    public FTArray(){
    }

    public FTArray(FTArray source){
        firstFree = source.firstFree;
        memory = Arrays.copyOf(source.memory,source.memory.length);
    }

    public int get(int i){
        if ( i< 0 || i>= firstFree){
            System.out.println("Out of bounds access in FTArray.get(i). i is " + i + ", size is " + firstFree );
        }
        return memory[i];
    }

    public void set(int index, int element){
        ensureSpace(index);
        memory[index] = element;
        if(index >= firstFree)
            firstFree = index+1;
    }

    public void add(int element){
        this.set(firstFree,element);
    }

    //this can be optimized with an array copy, first ensuring length
    public void addAll(FTArray other){
        for(int i = 0; i<other.firstFree; i++){
            add(other.memory[i]);
        }
    }

    public FTArray subList(int start, int stop){
        FTArray result = new FTArray();
        result.firstFree = stop-start;
        result.ensureSpace(result.firstFree);
        System.arraycopy(memory,start,result.memory,0, result.firstFree);
        return result;
    }

    public boolean equals(Object other){
        if(other.getClass() != this.getClass()) return false;
        FTArray otherMem = (FTArray)other;
        if(firstFree != otherMem.firstFree)  return false;
        return Arrays.equals(memory,otherMem.memory);
    }

    public int hashCode(){
        return Arrays.hashCode(memory);
    }

    public int size(){
        return firstFree;
    }

    public boolean contains(int element){
        for(int el:memory ){
            if (element == el) return true;
        }
        return false;
    }

    public int indexOf(int element){
        for (int i = 0; i< firstFree; i++){
            if(element == memory[i]) return i;
        }
        return -1;
    }

    protected void ensureSpace(int index){
        if(index >= memory.length){
            int speculativeLength = memory.length + chunkSize;
            int newLength = (index >= speculativeLength) ? index + 1 :speculativeLength ;
            memory = Arrays.copyOf(memory,newLength);
        }
    }


}
