package be.intimals.freqt.mdl.input;

import be.intimals.freqt.util.PeekableIterator;
import be.intimals.freqt.util.Util;

import java.util.*;
import java.util.logging.Logger;

public class Database<T> {
    private static final Logger LOGGER = Logger.getLogger(Database.class.getName());

    private List<IDatabaseNode<T>> transactions = new ArrayList<>();

    private Map<Integer, Map<Integer,IDatabaseNode<T>>> mapTidIdNode = null;

    private Database() {}

    private Database(List<IDatabaseNode<T>> transactions) {
        this.transactions = transactions;
    }

    public static <T> Database<T> create() {
        return new Database<>();
    }

    public static <T> Database<T> create(List<IDatabaseNode<T>> transactions) {
        return new Database<>(transactions);
    }

    public void addTransaction(IDatabaseNode<T> transaction) {
        this.transactions.add(transaction);
    }

    public List<IDatabaseNode<T>> getTransactions() {
        return transactions;
    }

    public IDatabaseNode<T> getTid(int id) {
        return transactions.get(id);
    }

    public int getSize() {
        return this.transactions.size();
    }

    public IDatabaseNode<T> findById(int tid, Integer id) {
        if (transactions.size() <= tid) throw new IllegalArgumentException("Wrong TID");
        if (mapTidIdNode == null) {
            buildIdMap();
        }
        //IDatabaseNode<T> root = transactions.get(tid);
        //return search(root, id);
        // Should exist if correctly built
        return mapTidIdNode.get(tid).get(id);
    }

    private void buildIdMap() {
        mapTidIdNode = new HashMap<>();
        for (IDatabaseNode<T> transactionRoot : transactions) {
            PeekableIterator<IDatabaseNode<T>> dfsIterator = Util.asPreOrderIterator(
                    Util.asSingleIterator(transactionRoot), (IDatabaseNode<T> node) -> node.getChildren().iterator());
            dfsIterator.next();
            while (dfsIterator.hasNext()) {
                IDatabaseNode<T> currentTreeNode = dfsIterator.peek();

                Map<Integer, IDatabaseNode<T>> mapIdNode = mapTidIdNode.getOrDefault(currentTreeNode.getTID(), new HashMap<>());
                assert (!mapIdNode.containsKey(currentTreeNode.getID())); // Should be unique
                mapIdNode.put(currentTreeNode.getID(), currentTreeNode);
                mapTidIdNode.putIfAbsent(currentTreeNode.getTID(), mapIdNode);

                dfsIterator.next();
            }
        }
    }

    private IDatabaseNode<T> search(IDatabaseNode<T> current, int searchId) {
        if (current.getID() == searchId) return current;
        int lower = current.getChildren().get(0).getID();
        int upper = current.getChildren().get(current.getChildrenCount() - 1).getID();
        if (searchId >= lower && searchId <= upper) {
            IDatabaseNode<T> res = null;
            for (IDatabaseNode<T> node: current.getChildren()) {
                if (node.getID() == searchId) {
                    res = node;
                    break;
                }
            }
            assert (res != null); // By construction
            return res;
        }

        for (IDatabaseNode<T> next : current.getChildren()) {
            if (next.isLeaf()) {
                if (next.getID() == searchId) return next;
            } else {
                int lowerBound = next.getChildren().get(0).getID();
                if (searchId >= lowerBound) {
                    IDatabaseNode<T> val = search(next, searchId);
                    if (val != null) return val;
                }
            }

        }
        return null;
    }
}
