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

public class Product
{
    
    private int _orderID, _productID;
    private String _productName;
    private double _unitPrice;

    public Product(int _orderID, int _productID, String _productName, double _unitPrice)
    {
        this._orderID = _orderID;
        this._productID = _productID;
        this._productName = _productName;
        this._unitPrice = _unitPrice;
    }

    public Product()
    {
    }

    public void setOrderID(int _orderID)
    {
        this._orderID = _orderID;
    }

    public void setProductID(int _productID)
    {
        this._productID = _productID;
    }

    public void setProductName(String _productName)
    {
        this._productName = _productName;
    }

    public void setUnitPrice(double _unitPrice)
    {
        this._unitPrice = _unitPrice;
    }

    public int getOrderID()
    {
        return _orderID;
    }

    public int getProductID()
    {
        return _productID;
    }

    public String getProductName()
    {
        return _productName;
    }

    public double getUnitPrice()
    {
        return _unitPrice;
    }
}