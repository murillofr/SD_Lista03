import java.net.*;
import java.io.*;
import java.util.*;

import java.rmi.*;
import java.rmi.registry.*;
import java.rmi.server.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Servidor extends UnicastRemoteObject implements BD, Runnable {
    
    private static Vector threads;
    private Socket conexao;
    
    // Listas para carregar os BD's nelas
    static ArrayList<String> bd01 = new ArrayList<>();
    static ArrayList<String> bd02 = new ArrayList<>();
    
    private Servidor() throws RemoteException {
        super();
    }
    
    public Servidor (Socket s) throws RemoteException {
        conexao = s;
    }

    public static void main(String[] args) throws IOException {
        
        // Implementação do RMI
        try {
            LocateRegistry.createRegistry(2335);
            Servidor f = new Servidor();
            Naming.rebind("//localhost:2335/BD", f);
            System.out.println("Servidor BD pronto.");
        } catch (RemoteException re) { 
            System.out.println("Excecao em " + re);
        } catch (Exception e) { 
            e.printStackTrace();
        }
        
        threads = new Vector();
        
        // Chama o método para carregar nas listas os conteúdos dos BD's
        carregarBD();
        
        ServerSocket s = new ServerSocket(2000);
        while (true) {
            System.out.println("\nEsperando conectar...........");
            
            Socket conexao = s.accept();
            System.out.println("\nConectou!\n" + conexao);
            
            Thread t = new Thread(new Servidor(conexao));
            t.start();
        }
    }

    @Override
    public void run() {
        try {
            BufferedReader entrada = new BufferedReader(new InputStreamReader(conexao.getInputStream()));
            PrintStream saida = new PrintStream(conexao.getOutputStream());
            
            // Criação do objeto recebendo uma instancia do serviço
            // podendo, assim, chamar seus métodos via RMI
            BD bd = null;
            try {
                bd = (BD) Naming.lookup("rmi://localhost:2335/BD");
            } catch(MalformedURLException e) {
                System.out.println("nao eh um URI RMI valida");
            } catch(RemoteException re) {
                System.err.println("Objeto Remoto tratou a execucao " + re);
            } catch(NotBoundException e) {
                System.out.println("Nao foi possivel achar o objeto remoto no servidor");
            }
            
            // Inclui a nova thread na lista de threads
            threads.add(saida);
            String linha = entrada.readLine();
            
            // Enquanto a mensagem recebida não ser vazia,
            // segue dentro do WHILE
            while ((linha != null) && (!linha.trim().equals(""))) {
                
                // Splita a mensagem recebida
                String[] msgSplit = linha.split(" ");
                
                // Se o cabeçalho for READ
                // manda uma mensagem com cabeçalho READ, concatenando:
                // Codigo da conta + ESPAÇO
                // Numero da conta, pego pelo metodo getConta, + ESPAÇO
                // Infos da conta, utilizando o método READ do objeto BD
                if (msgSplit[0].equals("READ")) {
                    sendToOne(saida, "READ " + 
                                     Integer.parseInt(msgSplit[1]) + " " + 
                                     getConta(Integer.parseInt(msgSplit[1])) + " " + 
                                     bd.read(Integer.parseInt(msgSplit[1])));
                
                // Se o cabeçalho for WRITE
                // Chama o metodo WRITE do objeto BD para efetivar o deposito
                // manda uma mensagem com cabeçalho WRITE, concatenando:
                // Codigo da conta + ESPAÇO
                // Numero da conta, pego pelo metodo getConta, + ESPAÇO
                // Infos da conta, utilizando o método READ do objeto BD
                }else if (msgSplit[0].equals("WRITE")) {
                    bd.write(Integer.parseInt(msgSplit[1]), Double.parseDouble(msgSplit[2]));
                    sendToOne(saida, "WRITE " + 
                                     Integer.parseInt(msgSplit[1]) + " " + 
                                     getConta(Integer.parseInt(msgSplit[1])) + " " + 
                                     bd.read(Integer.parseInt(msgSplit[1])));
                }
                
                linha = entrada.readLine();
            }
            sendToOne(saida, "");
            
            // Remove a thread da lista de threads e fecha sua conexao
            threads.remove(saida);
            conexao.close();
        }catch (IOException e) {
            e.getMessage();
        }
    }

    private void sendToOne(PrintStream saida, String msg) throws IOException {
        Enumeration e = threads.elements();
        
        while (e.hasMoreElements()) {
            PrintStream chat = (PrintStream) e.nextElement();
            
            if (chat == saida) {
                chat.println(msg);
            }
            if (msg.equals("")) {
                if (chat == saida) {
                    chat.println("");
                    System.err.println("\nThread " + saida + " foi encerrada!");
                }
            }
        }
    }
    
    public static void carregarBD() {
        
        // Lê cada linha do BD01.txt e a adiciona na lista "bd01"
        try {
            FileReader reader = new FileReader("src/BD01.txt");
            BufferedReader buffReader = new BufferedReader(reader);
            String linha;
            while ((linha = buffReader.readLine()) != null) {
                bd01.add(linha);
            }
            reader.close();
        } catch (IOException ex) {
            Logger.getLogger(Servidor.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        // Lê cada linha do BD02.txt e a adiciona na lista "bd02"
        try {
            FileReader reader = new FileReader("src/BD02.txt");
            BufferedReader buffReader = new BufferedReader(reader);
            String linha;
            while ((linha = buffReader.readLine()) != null) {
                bd02.add(linha);
            }
            reader.close();
        } catch (IOException ex) {
            Logger.getLogger(Servidor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public String getConta(int codigo) {
        
        // Pega a linha inteira a partir de seu codigo
        // Formato:  CODIGO  NUMERO_CONTA  SALDO
        // Exemplo:  1       13771-6       110.734706160586
        String linha;
        if (codigo < 101) {
            linha = bd01.get(codigo - 1);
        }else {
            linha = bd02.get(codigo - 101);
        }

        // Splita a linha recebida
        String[] msgSplit = linha.split(" ");        
        return msgSplit[1];
    }
    
    // Metodo responsavel por receber as infos da conta
    @Override
    public double read(int codigo) throws RemoteException {
        
        // Pega a linha inteira a partir de seu codigo
        // Formato:  CODIGO  NUMERO_CONTA  SALDO
        // Exemplo:  1       13771-6       110.734706160586
        String linha;
        if (codigo < 101) {
            linha = bd01.get(codigo - 1);
        }else {
            linha = bd02.get(codigo - 101);
        }
        
        // Splita a linha recebida
        String[] msgSplit = linha.split(" ");
        return Double.parseDouble(msgSplit[2]);
    }
    
    // Metodo responsavel por efetivar o deposito
    @Override
    public void write(int codigo, double valor) throws RemoteException {
        
        // Pega a linha inteira a partir de seu codigo
        // Formato:  CODIGO  NUMERO_CONTA  SALDO
        // Exemplo:  1       13771-6       110.734706160586
        String linha;
        if (codigo < 101) {
            linha = bd01.get(codigo - 1);
        }else {
            linha = bd02.get(codigo - 101);
        }
        
        // Splita a linha recebida
        String[] msgSplit = linha.split(" ");
        
        // Soma o saldo antigo da conta + deposito
        // Concatena a linha com o novo saldo
        msgSplit[2] = String.valueOf(Double.parseDouble(msgSplit[2]) + valor);
        linha = msgSplit[0] + " " + msgSplit[1] + " " + msgSplit[2];
        
        if (codigo < 101) {
            bd01.set(codigo - 1, linha);
        }else {
            bd02.set(codigo - 101, linha);
        }
        
        // Chama o metodo para atualizar o TXT com o novo saldo da conta
        try {
            atualizarBD(codigo);
        } catch (IOException ex) {
            Logger.getLogger(Servidor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private static void atualizarBD(int codigo) throws IOException {
        
        // Instancia um objeto "java.io.File" a ser utilizado dentro do IF/ELSE
        java.io.File arquivoOriginal;
        
        // Cria um arquivo temporario vazio
        java.io.File arquivoTemporario = new java.io.File("src/", "BDTemp.txt");
        arquivoTemporario.createNewFile();
        FileWriter fileWriter = new FileWriter(arquivoTemporario, true);
        
        // Dependendo da faixa do codigo, 
        // cria um novo objeto "java.io.File" utilizando o nome de cada BD
        // Copia cada elemento da lista "bd01" ou "bd02" e escreve no arquivo temporario
        if (codigo < 101) {
            arquivoOriginal = new java.io.File("src/", "BD01.txt");
            try (PrintWriter printWriter = new PrintWriter(fileWriter)) {
                bd01.forEach((elemento) -> {
                    printWriter.println(elemento);
                    printWriter.flush();
                });
                printWriter.close();
            }
        }else {
            arquivoOriginal = new java.io.File("src/", "BD02.txt");
            try (PrintWriter printWriter = new PrintWriter(fileWriter)) {
                bd02.forEach((elemento) -> {
                    printWriter.println(elemento);
                    printWriter.flush();
                });
                printWriter.close();
            }
        }
        
        // Deleta o arquivo original, pois está desatualizado,
        // e renomeia o arquivo temporario com o nome do arquivo original
        arquivoOriginal.delete();
        arquivoTemporario.renameTo(arquivoOriginal);
        
        System.out.println("\nARQUIVO ATUALIZADO COM SUCESSO");
    }
    
}