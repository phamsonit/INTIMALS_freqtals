package be.intimals.freqt.util;

import java.util.*;

public class Util {
    /**
     * Union of two lists.
     *
     * @param list1
     * @param list2
     * @param <T>
     * @return
     */
    public static <T> List<T> union(List<T> list1, List<T> list2) {
        Set<T> set = new HashSet<>();
        set.addAll(list1);
        set.addAll(list2);
        return new ArrayList<>(set);
    }

    public static <T> Iterator<T> asSingleIterator(final T item) {
        return Collections.singleton(item).iterator();
    }

    public interface ChildNodes<T> {
        Iterator<T> get(T current);
    }

    public static <T> PeekableIterator<T> asPreOrderIterator(final Iterator<T> root, ChildNodes<T> childrenFN) {
        return new PeekableIterator<T>() {

            private T nextItem = null;
            private Deque<Iterator<T>> stack = new ArrayDeque<>();

            public boolean hasNext() {
                return !stack.isEmpty();
            }

            public T next() {
                if (stack.isEmpty()) throw new NoSuchElementException();
                Iterator<T> current = this.stack.peek();
                if (current == null) throw new IllegalArgumentException("null not allowed");

                while (!current.hasNext()) {
                    this.stack.pop();
                    if (this.stack.isEmpty()) {
                        nextItem = null;
                        return null;
                    }
                    current = this.stack.peek();
                }
                nextItem = current.next();
                this.stack.push(childrenFN.get(nextItem));
                return nextItem;
            }

            public T peek() {
                return nextItem;
            }

            private PeekableIterator<T> init(Iterator<T> first) {
                this.stack.add(first);
                return this;
            }
        }.init(root);
    }

    public static <T> PeekableIterator<T> asPreOrderIteratorWithBacktrack(final Iterator<T> root,
                                                                          ChildNodes<T> childrenFN,
                                                                          T delimiter) {
        return new PeekableIterator<T>() {

            private T nextItem = null;
            private Deque<Iterator<T>> stack = new ArrayDeque<>();

            public boolean hasNext() {
                return !stack.isEmpty();
            }

            public T next() {
                if (stack.isEmpty()) throw new NoSuchElementException();
                Iterator<T> current = this.stack.peek();

                if (current == null) {
                    throw new IllegalArgumentException("null not allowed");
                } else if (!current.hasNext()) {
                    nextItem = delimiter;
                    this.stack.pop();
                } else {
                    nextItem = current.next();
                    this.stack.push(childrenFN.get(nextItem));
                }
                return nextItem;
            }

            public T peek() {
                return nextItem;
            }

            private PeekableIterator<T> init(Iterator<T> first) {
                this.stack.add(first);
                return this;
            }
        }.init(root);
    }

    /*
    public static <T> PeekableIterator<T> asBFSIterator(final Iterator<T> root, ChildNodes<T> childrenFN) {
        return new PeekableIterator<T>() {

            private T nextItem = null;
            private Queue<Iterator<T>> stack = new ArrayDeque<>();

            public boolean hasNext() {
                return !stack.isEmpty();
            }

            public T next() {
                if (stack.isEmpty()) throw new NoSuchElementException();
                Iterator<T> current = this.stack.peek();
                if (current == null) throw new IllegalArgumentException("null not allowed");

                while (!current.hasNext()) {
                    this.stack.poll();
                    if (this.stack.isEmpty()) {
                        nextItem = null;
                        return null;
                    }
                    current = this.stack.peek();
                }
                nextItem = current.next();
                this.stack.add(childrenFN.get(nextItem));
                return nextItem;
            }

            public T peek() {
                return nextItem;
            }

            private PeekableIterator<T> init(Iterator<T> first) {
                this.stack.add(first);
                return this;
            }
        }.init(root);
    }
    */

    public static <U> List<Integer> getParentPosFromPreorder(List<U> preorder, U delimiter) {
        List<Integer> parentPos = new ArrayList<>();
        Deque<Integer> stack = new ArrayDeque<>();
        stack.push(-1);
        int countSymbols = 0;
        for (int i = 0; i < preorder.size(); i++) {
            U val = preorder.get(i);
            if (val.equals(delimiter)) {
                stack.poll();
            } else {
                parentPos.add(stack.peek());
                stack.push(countSymbols);
                countSymbols++;
            }
        }
        return parentPos;
    }

    public static boolean equalsFuzzy(final double a, final double b, final double epsilon) {
        if (a == b) return true;
        return Math.abs(a - b) < epsilon;
    }
}
