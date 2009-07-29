
//
// NOTE: FOR APPLET COMPATIBILITY THIS CLASS SHOULD NOT INCLUCE ANY POST JDK 1.2 CONSTRUCTS
// AND SHOULD NOT BE COMPILED WITH POST JDK 1.4 COMPILERS.
//

/**
 * A class with a simple static sort method for use by applets
 * since many browsers only support JDK 1.1 which does not contain
 * a sort method in the core libraries.
 * 
 * Copyright 2005 - Superliminal Software
 * @author Don Hatch
 */
public class Sort {
        public static interface Comparable
        {
            /**
             * Implemented by the calling code such that Comparable objects
             * will declare their sort order relative to another given object.
             * @param obj is another object to compare to.
             * @return a value less than 0 if this object sorts before the given object,
             * a value greater than 0 if this object sorts before it, and 0 if they
             * sort equally.
             */
            public abstract int compareTo(Object obj);
        }

        /**
         * Sorts the given array in place.
         * @param comparables is an array of items to sort, 
         * each of which implements the Comparable interface.
         */
        public static void sort(Comparable comparables[])
        {
            sort(comparables, 0, comparables.length - 1);
        }

        private static void swap(Comparable comparables[], int i, int j)
        {
            Comparable comparable = comparables[j];
            comparables[j] = comparables[i];
            comparables[i] = comparable;
        }

        private static int partition(Comparable comparables[], int i, int j, Comparable comparable)
        {
            do
            {
                while(comparables[++i].compareTo(comparable) < 0) ;
                while(j > 0 && comparables[--j].compareTo(comparable) > 0) ;
                swap(comparables, i, j);
            } while(i < j);
            swap(comparables, i, j);
            return i;
        }

        public static void sort(Comparable comparables[], int i, int j)
        {
            if(j < i)
                return;
            int k = (i + j) / 2;
            swap(comparables, k, j);
            int l = partition(comparables, i - 1, j, comparables[j]);
            swap(comparables, l, j);
            if(l - i > 1)
                sort(comparables, i, l - 1);
            if(j - l > 1)
                sort(comparables, l + 1, j);
        }

        /**
         * To disallow instantiation.
         */
        private Sort() {}
}
