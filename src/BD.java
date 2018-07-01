import java.rmi.*;

public interface BD extends Remote {
    public double read (int codigo) throws RemoteException;
    public void write (int codigo, double valor) throws RemoteException;
}