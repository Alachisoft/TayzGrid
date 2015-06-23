package com.alachisoft.tayzgrid.web.mapreduce;

import com.alachisoft.tayzgrid.common.mapreduce.TaskEnumeratorResult;
import com.alachisoft.tayzgrid.runtime.exceptions.OperationFailedException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.NoSuchElementException;

/**
 * This is an iterator that helps iterating Map Reduce Result.
 *
 */
public class TaskEnumerator implements Enumeration {

    ArrayList<TaskPartitionedEnumerator> _partitionedEnumerators;
    TaskPartitionedEnumerator currentPartitionedEnumerator;
    int _roundRobinIndexer = 0;
    boolean _isValid;

    public TaskEnumerator(ArrayList<TaskEnumeratorResult> partitionedEnumerators, TaskEnumeratorCache cache) {
        _partitionedEnumerators = new ArrayList<TaskPartitionedEnumerator>();
        for (TaskEnumeratorResult enumeratorResultSet : partitionedEnumerators) {
            TaskPartitionedEnumerator mrResultPEnumerator = new TaskPartitionedEnumerator(cache,
                    enumeratorResultSet.getPointer(),
                    enumeratorResultSet.getRecordSet(),
                    enumeratorResultSet.getNodeAddress(),
                    enumeratorResultSet.getIsLastResult());
            _partitionedEnumerators.add(mrResultPEnumerator);
        }

        ValidatePEnumerator();
    }

    @Override
    public boolean hasMoreElements() {
        return currentPartitionedEnumerator.isValid();
    }

    @Override
    public Object nextElement() {
        try {
            if (_isValid) {
                return currentPartitionedEnumerator.nextElement();
            } else {
                return null;
            }
        } catch (NoSuchElementException ex) {
            InValidatePEnumerator();
            throw ex;
        } finally {
            ValidatePEnumerator();
        }
    }

    private void ValidatePEnumerator() {
        int validationEnumerationCount = 0;
        do {
            if (validationEnumerationCount > _partitionedEnumerators.size()) //if loop is iterated more times than _partitionedEnumerator's size, break the loop
            {
                _isValid = false;
                break;
            }
            if (_partitionedEnumerators != null || !_partitionedEnumerators.isEmpty()) {
                _roundRobinIndexer = (++_roundRobinIndexer) % _partitionedEnumerators.size();
                currentPartitionedEnumerator = _partitionedEnumerators.get(_roundRobinIndexer);
                _isValid = true;
                validationEnumerationCount++;
            }
        } while (!currentPartitionedEnumerator.isValid());
    }

    private void InValidatePEnumerator() {
        for (TaskPartitionedEnumerator pe : _partitionedEnumerators) {
            pe.setIsValid(false);
            _isValid = false;
        }
    }
}
