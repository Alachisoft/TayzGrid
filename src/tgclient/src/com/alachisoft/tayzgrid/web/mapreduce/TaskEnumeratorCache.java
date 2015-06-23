package com.alachisoft.tayzgrid.web.mapreduce;

import com.alachisoft.tayzgrid.common.mapreduce.TaskEnumeratorResult;
import com.alachisoft.tayzgrid.common.mapreduce.TaskEnumeratorPointer;
import com.alachisoft.tayzgrid.runtime.exceptions.AggregateException;
import com.alachisoft.tayzgrid.runtime.exceptions.ConnectionException;
import com.alachisoft.tayzgrid.runtime.exceptions.GeneralFailureException;
import com.alachisoft.tayzgrid.runtime.exceptions.OperationFailedException;

public interface TaskEnumeratorCache {
    TaskEnumeratorResult getNextRecord(String serverAddress,TaskEnumeratorPointer pointer) throws OperationFailedException;
    void dispose(String serverAddress);
}
