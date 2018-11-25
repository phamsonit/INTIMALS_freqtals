package be.intimals.freqt.mdl.tsg;

import be.intimals.freqt.mdl.input.Database;

public interface ITSG<T> {
    void addRule(TSGRule<T> rule);

    void removeRule(TSGRule<T> rule);

    void loadDatabase(Database<T> db);
}

