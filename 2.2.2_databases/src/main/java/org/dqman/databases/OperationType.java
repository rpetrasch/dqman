package org.dqman.databases;

public enum OperationType {
    INSERT(0),
    UPDATE(1),
    SELECT(2),
    DELETE(3);

    private int index;

    OperationType(int index) {
        this.index = index;
    }
    int getIndex() {
        return index;
    }

}
