package com.alachisoft.tayzgrid.web.mapreduce;

/**
 *
 * @author 
 */


public class TaskResultItem<Key, Value> {
    
    private Key key;
    
    private Value value;

    public Key getKey() {
        return key;
    }

    public void setKey(Key key) {
        this.key = key;
    }

    public Value getValue() {
        return value;
    }

    public void setValue(Value value) {
        this.value = value;
    }
    
}
