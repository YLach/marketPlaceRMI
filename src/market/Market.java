package market;


import client.Trader;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Market extends Remote {

    public void register(String trader) throws RemoteException, RejectedException;

    public void unregister(String trader) throws RemoteException, RejectedException;

    public void sell(Item item, Trader trader) throws RemoteException, RejectedException;

    public void buy(Item item, Trader trader) throws RemoteException, RejectedException, bank.RejectedException;

    public String[] getAllItems() throws RemoteException;
}
