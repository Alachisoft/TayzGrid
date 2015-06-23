/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.alachisoft.tayzgrid.common.threading;

import java.util.Calendar;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *
 * @author 
 */
public class ThrottlingManager {
        long _limit;
        long _throtllingUnitMs = 1000;
        long _currentInternval;
        long _currentSize;
        long _currentMilliSeconds;

        java.util.Date _startTime;
        
        public ThrottlingManager(long limit)
        {
            _limit = limit;
        }

        public ThrottlingManager(long limit, long unit)
        {
            _limit = limit;
            _throtllingUnitMs = unit;
            
        }
        
        public void Start()
        {
            _startTime = new java.util.Date();
            _currentMilliSeconds = GetMilliSecondDiff();
            _currentInternval = _currentMilliSeconds / _throtllingUnitMs;
        }
        
        private long GetMilliSecondDiff()
        {          
            Date date = new Date();            
            return date.getTime() - _startTime.getTime();             
        }
        
        public void Throttle(long size)
        {
            synchronized (this)
            {
                long msNow = GetMilliSecondDiff();
                long currentInterval = msNow / _throtllingUnitMs;

                if (currentInterval == _currentInternval)
                {
                    _currentSize += size;

                    if (_currentSize >= _limit)
                    {
                        try {
                            Thread.sleep((long)(_throtllingUnitMs - (msNow - _currentMilliSeconds)));
                        } catch (Exception ex) {
                            
                        }
                    }
                }
                else
                {
                    _currentInternval = currentInterval;
                    _currentMilliSeconds = msNow;
                    _currentSize = size;
                }
            }
        }

    
}
