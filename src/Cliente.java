import java.io.*;
import java.net.*;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Cliente extends Thread {
    private static volatile boolean done = false;
    private Socket conexao;
    
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";
    
    public Cliente (Socket s) {
        conexao = s;
    }
    
    public static void main(String[] args) throws IOException {        
        BufferedReader teclado = new BufferedReader(new InputStreamReader(System.in));
        int req;
        System.out.println("Digite a quantidade de transações: ");
        req = Integer.parseInt(teclado.readLine());
        teclado.close();
        
        System.out.println("");
        
        for (int i = 0; i < req; i++) {
            Socket conexao = new Socket("localhost", 2000);
            Thread t = new Cliente(conexao);
            
            // Coloca a Thread para dormir por 2s, caso ela não seja a primeira
            if (i > 0) {
                try {
                    
                    // Coloca a thread para dormir por uma fração de segundo
                    // para que se dê tempo de printar as duas linhas a seguir
                    Thread.sleep (125);
                    System.out.println(formatarTexto(ANSI_BLUE, "\n╔────────────────────────╗"));
                    System.out.print(formatarTexto(ANSI_RED, "  Thread " + (i+1)));
                    
                    // Começa a printar os ".", indicando que a Thread está dormindo
                    for (int j = 0; j < 4; j++) {
                        Thread.sleep (500);
                        if (j < 3) {
                            System.out.print(formatarTexto(ANSI_RED, "."));
                        }
                    }
                    
                    // Volta 1 caracter para printar novamente o número da Thred sem os "."
                    // Apesar de que, por algum motivo, o \r (que é o caracter de escape não funciona)
                    System.out.print("\b");
                    System.out.print(formatarTexto(ANSI_RED, "  Thread " + (i+1)));
                    System.out.println();
                    System.out.println(formatarTexto(ANSI_BLUE, "╠────────────────────────╣"));
                    
                } catch (InterruptedException ex) {}
            }else {
                System.out.println(formatarTexto(ANSI_BLUE, "╔────────────────────────╗"));
                System.out.println(formatarTexto(ANSI_RED, "  Thread " + (i+1)));
                System.out.println(formatarTexto(ANSI_BLUE, "╠────────────────────────╣"));
            }
            
            // Inicia de fato a Thread
            t.start();
        }
        while (true){
            if (done){
                break;
            }
        }
    }
    
    @Override
    public void run(){
        BufferedReader entrada;
        try {
            entrada = new BufferedReader(new InputStreamReader(conexao.getInputStream()));
            PrintStream saida = new PrintStream(conexao.getOutputStream());
            
            // Bloco de geração das contas e depositos
            Random gerador = new Random();
            int tempConta = gerador.nextInt(200) + 1;          // Gera uma conta de 1 a 200
            double tempDeposito = gerador.nextDouble() * 1000; // Gera um deposito de 1 a 1000
            
            // Manda para o servidor uma mensagem READ
            saida.println("READ " + tempConta);
            
            String linha;
            while (true){
                linha = entrada.readLine();
                
                // Se a mensagem recebida do servidor for vazia,
                // printa o encerramento da conexão
                if (linha.trim().equals("")){
                    System.out.println(formatarTexto(ANSI_BLUE, "╠────────────────────────╣"));
                    System.out.println(formatarTexto(ANSI_PURPLE, "  Conexão da conta [" + tempConta + "] encerrada!"));
                    System.out.println(formatarTexto(ANSI_BLUE, "╚────────────────────────╝"));
                    break;
                }else {
                    
                    // Splita a mensagem recebida
                    String[] msgSplit = linha.split(" ");
                    
                    // Caso o cabeçalho seja um READ,
                    // printa as infos recebidas da conta
                    // e manda um WRITE para o servidor
                    String tempPrint = "";
                    if (msgSplit[0].equals("READ")) {
                        tempPrint = tempPrint + "  read(" + msgSplit[1] + 
                                                ")\n  conta: " + msgSplit[2] + 
                                                "\n  saldo: " + msgSplit[3];
                        saida.println("WRITE " + tempConta + " " + tempDeposito);
                    
                    // Caso o cabeçalho seja um WRITE,
                    // printa as novas infos recebidas da conta
                    // e manda uma mensagem vazia para o servidor
                    }else if (msgSplit[0].equals("WRITE")) {
                        System.out.println(formatarTexto(ANSI_BLUE, "╠────────────────────────╣"));
                        tempPrint = tempPrint + "  write(" + tempConta + ", " + tempDeposito + 
                                                ")\n  read(" + msgSplit[1] + 
                                                ")\n  conta: " + msgSplit[2] + 
                                                "\n  saldo: " + msgSplit[3];
                        saida.println("");
                    }
                    System.out.println(tempPrint);
                    
                }
            }
            System.out.println("");
            done = true;
        } catch (IOException ex) {
            Logger.getLogger(Cliente.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    // Metodo para colorir o texto a ser printado
    public static String formatarTexto (String cor, String texto) {
        return cor + texto + ANSI_RESET;
    }
    
}