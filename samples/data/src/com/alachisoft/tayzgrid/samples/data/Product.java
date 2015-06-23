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

package com.alachisoft.tayzgrid.samples.data;

import java.io.Serializable;

public class Product implements Serializable 
{
    public int productId;
    public String name;
    public String productClass;
    public String category;

    public Product(int productId, String name, String productClass, String category) {
        
        this.productId=productId;
        this.name= name;
        this.productClass= productClass;
        this.category= category;
    }

    public Product() {
        
    }
           
    public void setId(int productId) {
        this.productId = productId;        
    }
    
    public int getId() {
        return this.productId;
    }
            
    public void setName(String name) {
        this.name = name;
    }
    
    public String getName() {
        return this.name;
    }
    
    public void setClassName (String productClass) {
        this.productClass = productClass;
    }
    
    public String getClassName () {
        return this.productClass;
    }
    
    public void setCategory(String category) {
        this.category = category;        
    }
    
    public String getCategory() {
        return this.category;
    }
            

}
