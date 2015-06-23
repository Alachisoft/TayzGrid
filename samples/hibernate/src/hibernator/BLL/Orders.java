/*
* Copyright (c) 2015, Alachisoft. All Rights Reserved.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package hibernator.BLL;

import java.util.Date;

public class Orders
{

    private int _orderID, _shippedName, _shipAddress, _shipCity, _shipRegion, _shipPostalCode;
    private String _customerID;
    private Date _orderDate, _shippedDate;
    
    public Orders(int _orderID, int _shippedName, int _shipAddress, int _shipCity, int _shipRegion, int _shipPostalCode, String _customerID, Date _orderDate, Date _shippedDate)
    {
        this._orderID = _orderID;
        this._shippedName = _shippedName;
        this._shipAddress = _shipAddress;
        this._shipCity = _shipCity;
        this._shipRegion = _shipRegion;
        this._shipPostalCode = _shipPostalCode;
        this._customerID = _customerID;
        this._orderDate = _orderDate;
        this._shippedDate = _shippedDate;
    }

    public Orders()
    {
    }


    public void setCustomerID(String _customerID)
    {
        this._customerID = _customerID;
    }

    public void setOrderDate(Date _orderDate)
    {
        this._orderDate = _orderDate;
    }

    public void setOrderID(int _orderID)
    {
        this._orderID = _orderID;
    }

    public void setShipAddress(int _shipAddress)
    {
        this._shipAddress = _shipAddress;
    }

    public void setShipCity(int _shipCity)
    {
        this._shipCity = _shipCity;
    }

    public void setShipPostalCode(int _shipPostalCode)
    {
        this._shipPostalCode = _shipPostalCode;
    }

    public void setShipRegion(int _shipRegion)
    {
        this._shipRegion = _shipRegion;
    }

    public void setShippedDate(Date _shippedDate)
    {
        this._shippedDate = _shippedDate;
    }

    public void setShippedName(int _shippedName)
    {
        this._shippedName = _shippedName;
    }

    public String getCustomerID()
    {
        return _customerID;
    }

    public Date getOrderDate()
    {
        return _orderDate;
    }

    public int getOrderID()
    {
        return _orderID;
    }

    public int getShipAddress()
    {
        return _shipAddress;
    }

    public int getShipCity()
    {
        return _shipCity;
    }

    public int getShipPostalCode()
    {
        return _shipPostalCode;
    }

    public int getShipRegion()
    {
        return _shipRegion;
    }

    public Date getShippedDate()
    {
        return _shippedDate;
    }

    public int getShippedName()
    {
        return _shippedName;
    }

}
