package client;


import bank.Account;
import bank.Bank;
import bank.RejectedException;
import market.Item;
import market.Market;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class Client extends UnicastRemoteObject implements Trader {
    private static final String USAGE = "java market.Client <clientName> <REGISTRY_PORT_NUMBER>";
    private static final String DEFAULT_BANK = "Nordea";
    private static final String DEFAULT_MARKET = "market";
    private static final int DEFAULT_REMOTE_REGISTRY_PORT_NUMBER = 1099;
    private static final int DEFAULT_LOCAL_REGISTRY_PORT_NUMBER = 2000;

    private String clientName;
    private String marketName;
    Market market;
    private String bankName;
    Bank bankobj;
    Account account;


    public Client(String clientName) throws RemoteException {
        this(clientName, DEFAULT_MARKET, DEFAULT_BANK);
    }

    public Client(String clientName, String marketName, String bankName) throws RemoteException {
        super();
        this.clientName = clientName;
        this.marketName = marketName;
        this.bankName = bankName;

        try {
            Registry remoteRegistry;
            try {
                remoteRegistry = LocateRegistry.getRegistry(DEFAULT_REMOTE_REGISTRY_PORT_NUMBER);
            } catch (RemoteException e) {
                remoteRegistry = LocateRegistry.createRegistry(DEFAULT_REMOTE_REGISTRY_PORT_NUMBER);
            }
            bankobj = (Bank) remoteRegistry.lookup(bankName);
            market = (Market) remoteRegistry.lookup(marketName);

        } catch (Exception e) {
            System.out.println("The runtime failed: " + e.getMessage());
            System.exit(0);
        }
        System.out.println("Connected to bank: " + bankName);
        System.out.println("Connected to market: " + marketName);

        // New bank account
        try {
            this.account = bankobj.newAccount(getName());
        } catch (RejectedException e) {
            System.err.println("Account creation rejected : " + e);
            System.exit(1);
        }
    }

    @Override
    public void callback(String message) throws RemoteException {
        System.out.println(message);
    }

    public String getName() {
        return clientName;
    }

    public Market getMarket() {
        return market;
    }

    public Bank getBankobj() {
        return bankobj;
    }

    public Account getAccount() {
        return account;
    }

    public static void main(String[] args) {
        if (args.length > 2 || (args.length > 0 && args[0].equalsIgnoreCase("-h"))) {
            System.out.println(USAGE);
            System.exit(1);
        }

        int localRegistryPortNumber = DEFAULT_LOCAL_REGISTRY_PORT_NUMBER;
        String clientName = "";
        try {
            if (args.length == 2) {
                clientName = args[0];
                localRegistryPortNumber = Integer.parseInt(args[1]);
            } else {
                System.out.println(USAGE);
                System.exit(1);
            }
        } catch (NumberFormatException e) {
            System.err.println("Invalid port number for the registry");
            System.exit(1);
        }

        try {
            try {
                LocateRegistry.getRegistry(localRegistryPortNumber);
            } catch (RemoteException e) {
                LocateRegistry.createRegistry(localRegistryPortNumber);
            }
            Client client = new Client(clientName);
            Naming.rebind(clientName, client);

            client.getMarket().register(client.getName());

            client.getMarket().sell(new Item("bougie", 10), client);

            client.getMarket().unregister(client.getName());

        } catch (Exception e) {
            System.err.println("The runtime failed: " + e.getMessage());
            System.exit(0);
        }
    }
}
