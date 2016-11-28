package market;


import client.Trader;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Market extends Remote {
    // Specifies the methods that can be remotely called on the Market Object

    void register(String trader) throws RemoteException, RejectedException;

    void unregister(String trader) throws RemoteException, RejectedException;

    void sell(Item item, Trader trader) throws RemoteException, RejectedException;

    void buy(Item item, Trader trader) throws RemoteException, RejectedException, bank.RejectedException;

    void wish(Item item, Trader trader) throws RemoteException, RejectedException, bank.RejectedException;

    String getAllItems() throws RemoteException;
}
