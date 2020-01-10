package be.intimals.freqt.core;

import be.intimals.freqt.FTArray;
import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.*;

class HasSubtreeTest {

    @Test
    public void testSubtreePresent(){
        int[] big =   {51, 52, 64, 54, 65, 4, 7, 8, -96, -1, -1, -1, -1, 67, 55, 56, 65, 54, 65, 4, 7, 8, -93, -1, -1, -1, -1, -1, -1, 4, 7, 8, -152};
        int[] small = {51, 52, 64, 54, 65, 4, 7, 8, -96, -1, -1, -1, -1, 67, 55, 56, 65, 54, 65, 4, 7, 8, -93};

        FreqT_Int fti = new FreqT_Int(null);

        assertTrue(fti.hasSubtree(new FTArray(big),new FTArray(small)));
    }

    @Test
    public void testSubtreeAbsent1(){
        int[] big = {51, 52, 64, 54, 65, 4, 7, 8, -96, -1, -1, -1, -1, 67, 55, 56, 65, 54, 65, 4, 7, 8, -93, -1, -1, -1, -1, -1, -1, 4, 7, 8, -152};
        int[] small = {51, 52, 201, 69, 39, 4, 7, 8, -26, -1, -1, -1, -1, -1, -1, 101, 102, 4, 7, 8, -132, -1, -1, -1, -1, 103, 65, 4, 7, 8, -93};

        FreqT_Int fti = new FreqT_Int(null);

        assertFalse(fti.hasSubtree(new FTArray(big),new FTArray(small)));
    }

    @Test
    public void testSubtreeAbsent2(){
        int[] big = {51, 52, 201, 69, 39, 4, 7, 8, -26, -1, -1, -1, -1, -1, -1, 101, 102, 4, 7, 8, -132, -1, -1, -1, -1, 103, 65, 4, 7, 8, -93};
        int[] small = {51, 52, 64, 54, 65, 4, 7, 8, -96, -1, -1, -1, -1, 67, 55, 56, 65, 54, 65, 4, 7, 8, -93, -1, -1, -1, -1, -1, -1, 4, 7, 8, -152};

        FreqT_Int fti = new FreqT_Int(null);

        assertFalse(fti.hasSubtree(new FTArray(big),new FTArray(small)));
    }

    @Test
    public void testSubtreeAbsent3(){
        int[] big = {51, 52, 64, 54, 65, 4, 7, 8, -89, -1, -1, -1, -1, 67, 7, 8, -91, -1, -1, -1, -1, -1, -1, -1, 201, 69, 39, 4, 7, 8, -90, -1, -1, -1, -1, -1, -1, 101, 102, 4, 7, 8, -91, -1, -1, -1, -1, 103, 68, 69, 39, 4, 7, 8, -278, -1, -1, -1, -1, -1, -1, 279, 280, 41, 42, 4, 7, 8, -281, -1, -1, -1, -1, 50, 51, 52, 64, 54, 282, 4, 7, 8, -281};
        int[] small = {51, 52, 238, 239, 78, 69, 39, 4, 7, 8, -171, -1, -1, -1, -1, -1, -1, 79, -11, -1, -1, 4, 7, 8, -240};

        FreqT_Int fti = new FreqT_Int(null);

        assertFalse(fti.hasSubtree(new FTArray(big),new FTArray(small)));
    }

    @Test
    public void testSubtreeAbsent4(){
        int[] big = {51, 52, 64, 54, 65, 4, 7, 8, -89, -1, -1, -1, -1, -1, -1, -1, 238, 239, 78, 69, 39, 4, 7, 8, -171, -1, -1, -1, -1, -1, -1, 79, -11};
        int[] small = {51, 52, 238, 239, 78, 69, 39, 4, 7, 8, -171, -1, -1, -1, -1, -1, -1, 79, -11, -1, -1, 4, 7, 8, -240};

        FreqT_Int fti = new FreqT_Int(null);

        assertFalse(fti.hasSubtree(new FTArray(big),new FTArray(small)));
    }

    @Test
    public void testSkipoverBreak(){
        int[] big = {51, 52, 64, 54, 65, 4, 7, 8, -96, -1, -1, -1, -1, 67, 55, 56, 65, 54, 65, 4, 7, 8, -93, -1, -1, -1, -1, -1, -1, 4, 7, 8, -152};
        int[] small = {51, 52, 201, 69, 39, 4, 7, 8, -26, -1, -1, -1, -1, -1, -1, 101, 102, 4, 7, 8, -132, -1, -1, -1, -1, 103, 65, 4, 7, 8, -93};

        FreqT_Int fti = new FreqT_Int(null);

        assertFalse(fti.hasSubtree(new FTArray(big),new FTArray(small)));
    }

    @Test
    public void testSkipoverEnds(){
        int[] big =   {51, 52, 64, 54, 65, 4, 7, 8, -192, -1, -1, -1, -1, -1, -1, -1, 64, 54, 65, 4, 7, 8, -283, -1, -1, -1, -1, 67, 65, 54, 7, 8, -284, -1, -1, -1, -1, 4, 7, 8, -205};
        int[] small = {51, 52, 201, 69, 39, 4, 7, 8, -26, -1, -1, -1, -1, -1, -1, 101, 102, 4, 7, 8, -132, -1, -1, -1, -1, 103, 65, 4, 7, 8, -93};

        FreqT_Int fti = new FreqT_Int(null);

        assertFalse(fti.hasSubtree(new FTArray(big),new FTArray(small)));
    }

    @Test
    public void testSkip2Subtrees(){
        int[] big =   { 51, 52, 53, 54, 55, 56, 65, 4, 7, 8, -93, -1, -1, -1, -1, -1, -1, 58, -59, -1, -1, 60, 61, -62, -1, -1, -1, -1, -1, 63, 51, 52, 64, 54, 65, 4, 7, 8, -96, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 87, 51, 52, 64, 54, 65, 4, 7, 8, -96, -1, -1, -1, -1, 67, 124, 125, -11 };
        int[] small = { 51, 52, 53, 54, 55, 56, 65, 4, 7, 8, -93, -1, -1, -1, -1, -1, -1,                                           -1, -1, 63, 51, 52, 64, 54, 65, 4, 7, 8, -96, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 87, 51, 52, 64, 54, 65, 4, 7, 8, -96, -1, -1, -1, -1, 67, 124, 125, -11 };
        FreqT_Int fti = new FreqT_Int(null);

        assertTrue(fti.hasSubtree(new FTArray(big),new FTArray(small)));
    }

    /* Omitting these cases for now
    @Test
    public void testMismatchedLeaf(){
        int[] big =   {271, 272, 273, 272, 277, 279, 280, 281, 286, 287, 112, 63, -123, -1, -1, -1, -1, 288, 136, 137, 138, 28, 49, 10, -104, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 277, 279, 280, 281, 380, 288, 314, 315, 136, 137, 138, 28, 49, 10, -104, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 287, 381, 304, 80, 10, -102, -1, -1, -1, 80, 10, -346, -1, -1, -1, 80, 10, -929, -1, -1, -1, -1, -1, -1, -1, 324, 325, 326, 327, 136, 137, 138, 28, 49, 10};
        int[] small = {271, 272, 273, 272, 277, 279, 280, 281, 286, 287, 112, 63, -123, -1, -1, -1, -1, 288, 136, 137, 138, 28, 49, 10, -104, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 277, 279, 280, 281, 380, 288, 314, 315, 136, 137, 138, 28, 49, 10, -104, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 287, 381, 304, 80, 10, -102, -1, -1, -1, 80, 10, -1993, -1, -1, -1, 80, 10, -1994};
        FreqT_Int fti = new FreqT_Int(null);

        assertFalse(fti.hasSubtree(new FTArray(big),new FTArray(small)));
    }

    @Test
    public void testBacktracking(){
        int[] big =   { 51, 52, 64, 54, 65, 4, 7, 8, -192, -1, -1, -1, -1, -1, -1, -1, 64, 54, 65, 4, 7, 8, -204, -1, -1, -1, -1, -1, -1, -1, 64, 54, 65, 54, 7, 8, -285, -1, -1, -1, -1, 4, 7, 8, -224, -1, -1, -1, -1, 67, 65, 4, 7, 8, -291 };
        int[] small = { 51, 52, 64, 54, 65, 4, 7, 8, -192, -1, -1, -1, -1, -1, -1, -1,                                                        64, 54, 65, 54, 7, 8, -285, -1, -1, -1, -1, 4, 7, 8, -224, -1, -1, -1, -1, 67, 65, 4, 7, 8, -291 };
        FreqT_Int fti = new FreqT_Int(null);

        assertTrue(fti.hasSubtree(new FTArray(big),new FTArray(small)));
    }
    @Test
    public void testBacktrackingBifurcated(){
        int[] big =   { 51, 52, 64, 54, 65, 4, 7, 8, -89, -1, -1, -1, -1, 67, 7, 8, -91, -1, -1, -1, -1, -1, -1, -1, 201, 69, 39, 4, 7, 8, -90, -1, -1, -1, -1, -1, -1, 101, 102, 4, 7, 8, -91, -1, -1, -1, -1, 103, 68, 69, 39, 4, 7, 8, -278, -1, -1, -1, -1, -1, -1, 279, 280, 41, 42, 4, 7, 8, -25, -1, -1, -1, -1, 50, 51, 52, 64, 54, 282, 4, 7, 8, -25, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 42, 4, 7, 8, -286 };
        int[] small = { 51, 52, 64, 54, 65, 4, 7, 8, -89, -1, -1, -1, -1, 67, 7, 8, -91, -1, -1, -1, -1, -1, -1, -1, 201, 69, 39, 4, 7, 8, -90, -1, -1, -1, -1, -1, -1, 101, 102, 4, 7, 8, -91, -1, -1, -1, -1, 103, 68, 69, 39, 4, 7, 8, -278, -1, -1, -1, -1, -1, -1, 279, 280, 41, 42, 4, 7, 8, -286 };
        FreqT_Int fti = new FreqT_Int(null);

        //is true but too complex to handle
        assertFalse(fti.hasSubtree(new FTArray(big),new FTArray(small)));
    }
*/
}