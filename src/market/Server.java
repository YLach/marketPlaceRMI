package market;


import bank.Account;
import bank.Bank;
import client.Trader;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

public class Server extends UnicastRemoteObject implements Market {
    private static final String USAGE = "java market.Server <REGISTRY_PORT_NUMBER>";
    private static final String BANK = "Nordea";
    private static final String DEFAULT_MARKET = "market";
    private static final int BANK_REGISTRY_PORT_NUMBER = 1099;

    private List<String> traders = new LinkedList<>();
    private Map<Item, Trader> items = new TreeMap<>(); // Store Trader (and not their name) --> callback
    private Map<Item, Trader> wishList = new TreeMap<>();
    private String bankname;
    Bank bankobj;

    public Server(String bankName, int bankPort) throws RemoteException {
        super();
        this.bankname = bankName;
        try {
            Registry bankRegistry;
            try {
                bankRegistry = LocateRegistry.getRegistry(bankPort);
                bankRegistry.list();
            } catch (RemoteException e) {
                bankRegistry = LocateRegistry.createRegistry(bankPort);
            }
            bankobj = (Bank) bankRegistry.lookup(bankname);

        } catch (Exception e) {
            System.out.println("The runtime failed: " + e.getMessage());
            System.exit(0);
        }
        System.out.println("Connected to bank: " + bankname);
    }

    @Override
    public synchronized void register(String trader) throws RemoteException, RejectedException {
        if (traders.contains(trader))
            throw new RejectedException("Trader " + trader + " already registered");
        traders.add(trader);
        System.out.println("Trader " + trader + " registered on the market.");
    }

    @Override
    public synchronized void unregister(String trader) throws RemoteException, RejectedException {
        // Remove all items belonging to that trader
        if (!trader.contains(trader))
            throw new RejectedException("Trader " + trader + " not registered");
        // TODO : Remove all objects and wishes from that trader ?
        // Remove all items belonging to this trader
        for(Map.Entry<Item, Trader> entry : items.entrySet()) {
            if (entry.getValue().equals(trader)) //TODO surcharge equal in Trader implementation
                items.remove(entry.getKey());
        }
        // Remove all wishes from this trader
        for(Map.Entry<Item, Trader> entry : wishList.entrySet()) {
            if (entry.getValue().equals(trader))
                items.remove(entry.getKey());
        }

        traders.remove(trader);
        System.out.println("Trader " + trader + " unregistered from the market.");
    }

    @Override
    public synchronized void sell(Item itemToSell, Trader trader) throws RemoteException, RejectedException {
        if (items.containsKey(itemToSell))
            throw new RejectedException("Item " + itemToSell + " already on the market.");

        // Get an account ?
        Account account = bankobj.getAccount(trader.getName());
        if (account == null)
            throw new RejectedException("You cannot sell the item " + itemToSell  +
                    " : you do not get an account at bank :  " + bankname);

        // Yes
        items.put(itemToSell, trader);
        for (Map.Entry<Item, Trader> entry : wishList.entrySet()) {
            if (entry.getKey().compareTo(itemToSell) > 0)
                break;
            //if (entry.getKey().getName().equals(itemToSell.getName()))
                    // TODO : callback on entry.getValue()
        }
    }

    @Override
    public synchronized void buy(Item itemToBuy, Trader trader) throws RemoteException, RejectedException,
            bank.RejectedException {
        if (!items.containsKey(itemToBuy))
            throw new RejectedException("Item " + itemToBuy + " no longer on the market.");

        // Get an account ?
        Account accountBuyer = bankobj.getAccount(trader.getName());
        if (accountBuyer == null)
            throw new RejectedException("You cannot buy the item " + itemToBuy  +
                    " : you do not get an account at bank :  " + bankname);

        // Enough money ?
        if (accountBuyer.getBalance() < itemToBuy.getPrice())
            throw new RejectedException("You cannot afford to buy this item : " + itemToBuy);

        // Yes
        accountBuyer.withdraw(itemToBuy.getPrice());
        Account accountSeller = bankobj.getAccount(items.get(itemToBuy).getName());
        accountSeller.deposit(itemToBuy.getPrice());
        items.remove(itemToBuy);
    }

    @Override
    public String[] getAllItems() throws RemoteException {
        return items.keySet().toArray(new String[items.size()]);
    }


    public static void main(String[] args) {
        if (args.length > 1 || (args.length > 0 && args[0].equalsIgnoreCase("-h"))) {
            System.out.println(USAGE);
            System.exit(1);
        }

        int registryPortNumber = 1099;
        try {
            if (args.length > 0) {
                registryPortNumber = Integer.parseInt(args[0]);
            }
        } catch (NumberFormatException e) {
            System.err.println("Invalid port number for the registry");
            System.exit(1);
        }

        try {
            try {
                LocateRegistry.getRegistry(registryPortNumber).list();
            } catch (RemoteException e) {
                LocateRegistry.createRegistry(registryPortNumber);
            }
            Naming.rebind("rmi://localhost:" + registryPortNumber + "/" + DEFAULT_MARKET,
                    new Server(BANK, BANK_REGISTRY_PORT_NUMBER));

        } catch (RemoteException | MalformedURLException re) {
            System.out.println(re);
            System.exit(1);
        }
    }
}
