package be.intimals.freqt.mdl.input;

import java.util.*;
import java.util.logging.Logger;

public class Database<T> {
    private static final Logger LOGGER = Logger.getLogger(Database.class.getName());

    private List<IDatabaseNode<T>> transactions = new ArrayList<>();

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
        IDatabaseNode<T> root = transactions.get(tid);
        return search(root, id);
    }

    private IDatabaseNode<T> search(IDatabaseNode<T> current, int searchId) {
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
