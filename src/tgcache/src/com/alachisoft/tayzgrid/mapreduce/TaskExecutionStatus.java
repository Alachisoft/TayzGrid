 package com.alachisoft.tayzgrid.mapreduce;

/**
 *
 * @author 
 */

    public enum TaskExecutionStatus {

        Submitted(1),
        Running(2),
        Waiting(3),
        Failure(4);
        
        private int value;

        TaskExecutionStatus(int val) {
            this.value = val;
        }
        public void setValue(int value) {
            this.value = value;
        } 

    }
