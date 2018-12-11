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
}