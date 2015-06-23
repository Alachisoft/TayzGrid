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

package com.alachisoft.sample.hibernate.factory;

import hibernator.BLL.Customer;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;

public class CustomerFactory
{
    Session session;
    SessionFactory factory;

    public List<Customer> GetCustomers() throws Exception
    {
        List customers = new ArrayList();
        Transaction tx = null;
        String id = "";
        try
        {
            if (!session.isConnected())
            {
                session = factory.openSession();
            }
            tx = session.beginTransaction();

            List customerEnumerator = session.createQuery("from Customer c").setCacheable(true).list(); //Retrieves the list of Customers from 2nd level cache

            for (Customer cust : (List<Customer>) customerEnumerator)
            {
                customers.add(cust);
            }

            tx.commit();
        }
        catch (Exception ex)
        {

            tx.rollback();
            session.clear();
            session.disconnect();
            throw ex;
        }
        return customers;
    }

    public Customer GetCustomer(String CustomerID) throws Exception
    {
        Customer customer = null;
        Transaction tx = null;

        try
        {
            if (!session.isConnected())
            {
                session = factory.openSession();
            }
            tx = session.beginTransaction();
            customer=(Customer)session.get( Customer.class, CustomerID);
            tx.commit();
        }
        catch (Exception ex)
        {
            tx.rollback();
            session.clear();
            session.disconnect();
            throw ex;
        }

        return customer;
    }

    public Customer GetCustomerOrders(String CustomerID) throws Exception
    {
        Customer customer = null;
        Transaction tx = null;

        try
        {
            if (!session.isConnected())
            {
                session = factory.openSession();
            }
            tx = session.beginTransaction();
            customer = (Customer) session.get(Customer.class, CustomerID);
            tx.commit();
        }
        catch (Exception ex)
        {
            tx.rollback();
            throw ex;
        }
        return customer;
    }

    public void SaveCustomer(Customer customer) throws Exception
    {
        Transaction tx = null;
        try
        {
            if (!session.isConnected())
            {
                session = factory.openSession();
            }
            tx = session.beginTransaction();
            session.save(customer);
            session.flush();
            System.out.println("\nCustomer with ID: " + customer.getCustomerID() + " succefully added into database");
            tx.commit();
        }
        catch (Exception ex)
        {
            tx.rollback();
            session.clear();
            session.disconnect();
            throw ex;
            // handle exception
        }
    }

    public void UpdateCustomer(Customer customer) throws Exception
    {
        Transaction tx = null;
        try
        {

            if (!session.isConnected())
            {
                session = factory.openSession();
            }
            tx = session.beginTransaction();
            session.merge(customer);
            session.flush();
            System.out.println("\nCustomer with ID: " + customer.getCustomerID() + " succefully updated into database");
            tx.commit();
        }
        catch (Exception ex)
        {
            tx.rollback();
            session.clear();
            session.disconnect();
            throw ex;
            // handle exception
        }
    }

    public void RemoveCustomer(String CustomerID) throws Exception
    {
        Transaction tx = null;
        Customer customer;
        try
        {

            if (!session.isConnected())
            {
                session = factory.openSession();
            }
            tx = session.beginTransaction();
            List enumerator = session.createQuery("select cust " + "from Customer cust where " + "cust.CustomerID = '" + CustomerID + "'").list();

            if (!enumerator.isEmpty())
            {
                customer = (Customer) enumerator.get(0);
                if (customer != null)
                {
                    session.delete(customer);
                    session.flush();
                }
                else
                {
                    System.out.println("No such customer exist.");
                }
            }
            else
            {
                System.out.println("No such customer exist.");
            }


            tx.commit();
        }
        catch (Exception ex)
        {
            tx.rollback();
            session.clear();
            session.disconnect();
            throw ex;
            // handle exception
        }
    }

    public void SessionDisconnect()
    {
        session.clear();
        session.disconnect();
    }

    public CustomerFactory()
    {
        Object obj = new Configuration();
        Object obj2 = ((Configuration) obj).configure();
        factory = new Configuration().configure().buildSessionFactory();
        session = factory.openSession();
    }

    public void Dispose()
    {
        session.close();
        factory.close();
    }
}
