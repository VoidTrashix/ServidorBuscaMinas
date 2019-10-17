
import java.awt.BorderLayout;
import java.awt.Font;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class Principal {

    private static List<Lobbys> lista_lobbys= new ArrayList<>();
    
    private static final int Jugadores_Maximos= 4;
    private static final String bomba= "Oculta:mina";
    private static int filas_juego= 18;
    private static int columas_juego= 18;
    private static int minas_division= 5;
    
    static JFrame frame= new JFrame("Servidor Buscaminas");
    static JTextField txtIP= new JTextField(26);
    static JTextArea messageArea= new JTextArea(26, 30);
    
    public static void main(String[] args) throws Exception {
        txtIP.setEditable(false);
        messageArea.setEditable(false);
        
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
        Font font = new Font("Arial", Font.BOLD, 20);
        messageArea.setFont(font);
        txtIP.setFont(font);
        frame.getContentPane().add(new JScrollPane(messageArea), BorderLayout.CENTER);
        frame.getContentPane().add(txtIP, BorderLayout.SOUTH);
        frame.setResizable(false);
        frame.pack();
        
        messageArea.append("El servidor se está iniciando...\n");
        ExecutorService pool= Executors.newFixedThreadPool(500);
        txtIP.setText("IP: " + obtenerMiIP());
        try(ServerSocket listener= new ServerSocket(59001)){
            messageArea.append("Servidor iniciado exitosamente...\n███████████████████████████████████\n");
            messageArea.setCaretPosition(messageArea.getText().length());
            while(true){
                pool.execute(new Handler(listener.accept()));
            }
        }catch (Exception e){}
    }
    
    private static class Handler implements Runnable{
        private String nombre;
        private Socket socket;
        private String lobby;
        private Scanner in;
        private PrintWriter out;
        
        public Handler(Socket socket){
            this.socket= socket;
        }
        
        public void run(){
            try{
                in= new Scanner(socket.getInputStream());
                out= new PrintWriter(socket.getOutputStream(), true);
                
                while (true) {                    
                    out.println("SUBMITNAME");
                    nombre= in.nextLine();
                    if(nombre==null || nombre.isEmpty() || nombre.contains("&")){
                        continue;
                    }
                    synchronized(lista_lobbys){
                        if(lista_lobbys.isEmpty()){ lista_lobbys.add(new Lobbys(new ArrayList<Usuarios>(), null, UUID.randomUUID().toString())); }
                        
                        int indice= lobbydisponible();
                        
                        if(indice==-1){
                            lista_lobbys.add(new Lobbys(new ArrayList<Usuarios>(), null, UUID.randomUUID().toString()));
                            indice= lista_lobbys.size()-1;
                        }
                        if(!yaExiste(nombre, indice)){
                                lista_lobbys.get(indice).getLista_usuarios().add(new Usuarios(nombre, out, 0, false, 0, false, -1));
                                lobby= lista_lobbys.get(indice).getID_Unico();
                                out.println("NAMEACCEPTED " + nombre);
                                if(lista_lobbys.get(indice).getLista_usuarios().size()==1){out.println("ADMIN");}
                                out.println("LIST " + nombre);
                                messageArea.append(nombre + " se ha logeado\n███████████████████████████████████\n");
                                messageArea.setCaretPosition(messageArea.getText().length());
                                enviarLista(indice);
                                if(lista_lobbys.get(indice).getLista_usuarios().size()==Jugadores_Maximos){ lista_lobbys.add(new Lobbys(new ArrayList<Usuarios>(), null, UUID.randomUUID().toString())); }
                                break;
                        }
                    }
                }
                
                while (true) {                    
                    String input= in.nextLine();
                        if(input.startsWith("/JUGAR")){
                            if(lista_lobbys.get(buscarLobby(lobby)).getLista_usuarios().size()>1){
                                limpiarJugadores(buscarLobby(lobby));
                                LlenarTablero(buscarLobby(lobby));
                            }
                    } else if(input.startsWith("/Partida")){
                        if(lista_lobbys.get(buscarLobby(lobby)).getLista_usuarios().get(buscarUsuario(nombre, buscarLobby(lobby))).getVivo()){
                        int x= Integer.parseInt(input.substring(input.indexOf("=")+1, input.indexOf(",")));
                        int y= Integer.parseInt(input.substring(input.indexOf(",")+1, input.indexOf(";")));
                        String accion= input.substring(input.indexOf(";")+1, input.indexOf("]"));
                        
                        synchronized(lista_lobbys.get(buscarLobby(lobby)).getTablero()){
                            clickTablero(x, y, buscarLobby(lobby), buscarUsuario(nombre, buscarLobby(lobby)), accion, out);
                        }
                        
                        for(Usuarios usuario : lista_lobbys.get(buscarLobby(lobby)).getLista_usuarios()){
                            usuario.getEscritor().println("ACTUALIZAR=" + x + "," + y + ";" + lista_lobbys.get(buscarLobby(lobby)).getTablero()[x][y]);
                        }
                        
                        }
                    }else if(input.startsWith("/MENSAJE")){
                            input= input.substring(8);
                        for(Usuarios usuario : lista_lobbys.get(buscarLobby(lobby)).getLista_usuarios()){
                            usuario.getEscritor().println("MESSAGE " + nombre + ": " + input);
                        }
                    } else if(input.equals("/PEDIRLISTA")){
                         actualizarInformacion(buscarLobby(lobby));
                    }
                }
                
                
            } catch (Exception e){
                //System.err.println(e);
            } finally {
                if(out!=null && nombre!=null){
                    messageArea.append(nombre + " se ha salido\n" + "███████████████████████████████████\n");
                    messageArea.setCaretPosition(messageArea.getText().length());
                    if(lista_lobbys.get(buscarLobby(lobby)).getTablero()!=null){
                        limpiarBanderas(buscarLobby(lobby), buscarUsuario(nombre, buscarLobby(lobby)));
                    }
                    lista_lobbys.get(buscarLobby(lobby)).getLista_usuarios().remove(buscarUsuario(nombre, buscarLobby(lobby)));
                    if(lista_lobbys.get(buscarLobby(lobby)).getLista_usuarios().size()==0)
                        lista_lobbys.remove(buscarLobby(lobby));
                    enviarLista(buscarLobby(lobby));
                    actualizarInformacion(buscarLobby(lobby));
                    verificarJugadores(out, buscarLobby(lobby));
                }
                try{ socket.close(); } catch (IOException e){}
            }
        }
    }
    
    public static String obtenerMiIP(){
        String ip= "";
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp())
                    continue;
                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while(addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr instanceof Inet6Address) continue;
                    ip = addr.getHostAddress();
                }
            }
        } catch (SocketException e) {throw new RuntimeException(e);}
        return ip;
    }
    
    public static String convertirarregloastring(String mat[][]) { 
        String arreglo= "";
        for (String[] row : mat){
            for (String x : row) {
                arreglo= arreglo + x + " "; 
            }
        }
        return arreglo;
    } 
    
    public static void imprimirtablero(String mat[][]) {
        messageArea.append("---------------------------------\n");
        for (String[] row : mat){
            for (String x : row) {
                messageArea.append("[" + x + "] ");
                messageArea.setCaretPosition(messageArea.getText().length());
            }
            messageArea.append("\n");
            messageArea.setCaretPosition(messageArea.getText().length());
        }
    }
    
    public static void LlenarTablero(int lobby){
        int m= filas_juego;
        int n= columas_juego;
        int minas= (int) m*n/minas_division;
        String[][] cuadricula= new String[n][n];
        for (int x = 0;x<m;x++){
            for (int y = 0;y<n;y++){
                cuadricula[x][y]= "Oculta:0";
            }
        }
        int contador=0;
        for (int x = 0;x<m;x++){
            for (int y = 0;y<n;y++){
                int ranx= (int) (Math.random()*(n));
                int rany= (int) (Math.random()*(m));
                if(!cuadricula[ranx][rany].equals(bomba) && contador<minas){
                    cuadricula[ranx][rany]= bomba + "";
                    contador++;
                }
            }
        }
        
        for (int rx = 0;rx<m;rx++){
            for (int ry = 0;ry<n;ry++){
                if(cuadricula[rx][ry]!=bomba + ""){
                    int lados=0;
                    //Primera fila
                    try{
                        if(cuadricula[rx-1][ry-1]==bomba + ""){
                            lados++;
                        }
                    }catch(Exception e){}
                        
                        try{
                            if(cuadricula[rx][ry-1]==bomba + ""){
                                lados++;
                            }
                            }catch(Exception e){}
                        
                        try{
                            if(cuadricula[rx+1][ry-1]==bomba + ""){
                                lados++;
                            }
                            }catch(Exception e){}
                        
                        //Segunda fila
                        try{
                            if(cuadricula[rx-1][ry]==bomba + ""){
                                lados++;
                            }
                            }catch(Exception e){}
                        
                        try{
                            if(cuadricula[rx+1][ry]==bomba + ""){
                                lados++;
                            }
                            }catch(Exception e){}
                        
                        //Tercera fila
                        try{
                            if(cuadricula[rx-1][ry+1]==bomba + ""){
                                lados++;
                            }
                            }catch(Exception e){}
                        try{
                            if(cuadricula[rx][ry+1]==bomba + ""){
                                lados++;
                            }
                            }catch(Exception e){}
                        try{
                            if(cuadricula[rx+1][ry+1]==bomba + ""){
                                lados++;
                            }
                            }catch(Exception e){}
                        cuadricula[rx][ry]="Oculta:" + lados;
                    }
            }
        }
                        lista_lobbys.get(lobby).setTablero(cuadricula);
                        iniciarJuego(lobby, m, n, minas);
                        for(int i=0; i<lista_lobbys.get(lobby).getLista_usuarios().size(); i++){
                            lista_lobbys.get(lobby).getLista_usuarios().get(i).setBanderas(minas);
                        }
    }
    
    public static void iniciarJuego(int lobby, int m, int n, int minas){
        for(Usuarios usuario : lista_lobbys.get(lobby).getLista_usuarios()){
            usuario.getEscritor().println("INICIAR=" + m + "," + n + ";" + minas + "]" + usuario.getColor());
        }
    }
    
    public static boolean yaExiste(String nombre, int indice){
        boolean existe= false;
        for(Usuarios usuario : lista_lobbys.get(indice).getLista_usuarios()){
            if(usuario.getNombre().equals(nombre))
                existe= true;
        }
        return existe;
    }
    
    public static int lobbydisponible(){
        int lobby=-1;
        for(int x=0; x<lista_lobbys.size(); x++){
            if(lista_lobbys.get(x).getLista_usuarios().size()<=4){
                lobby= x;
                break;
            }
        }
        return lobby;
    }
    
    public static void enviarLista(int lobby){
        int admin=0;
        for(Usuarios usuario_mgs : lista_lobbys.get(lobby).getLista_usuarios()){
            usuario_mgs.getEscritor().println("LIMPIAR_LISTA");
            if(admin==0){
                usuario_mgs.getEscritor().println("ADMIN");
                admin++;
            }else{
                usuario_mgs.getEscritor().println("NOADMIN");
            }
            for(Usuarios nombres : lista_lobbys.get(lobby).getLista_usuarios()){
                usuario_mgs.getEscritor().println("LIST " + nombres.getNombre());
            }
        }
    }
    
    public static void actualizarInformacion(int lobby){
        int admin=0;
        for(Usuarios usuario_mgs : lista_lobbys.get(lobby).getLista_usuarios()){
            usuario_mgs.getEscritor().println("LIMPIAR_TEXTOS");
            for(Usuarios nombres : lista_lobbys.get(lobby).getLista_usuarios()){
                usuario_mgs.getEscritor().println("COLOR " + nombres.getColor());
                usuario_mgs.getEscritor().println("USUARIOS " + nombres.getNombre());
                usuario_mgs.getEscritor().println("BANDERAS " + nombres.getBanderas());
                if(nombres.getVivo()){
                    usuario_mgs.getEscritor().println("ESTADO Vivo");
                }else{
                    usuario_mgs.getEscritor().println("ESTADO Muerto");
                }
            }
        }
    }
    
    public static void limpiarJugadores(int indice){
        for(int usr=0; usr<lista_lobbys.get(indice).getLista_usuarios().size(); usr++){
            lista_lobbys.get(indice).getLista_usuarios().get(usr).setPuntos(0);
            lista_lobbys.get(indice).getLista_usuarios().get(usr).setVivo(true);
            lista_lobbys.get(indice).getLista_usuarios().get(usr).setIniciado(false);
            lista_lobbys.get(indice).getLista_usuarios().get(usr).setColor(usr);
        }
    }
    
    public static void clickTablero(int x, int y, int lobby, int posicion, String accion, PrintWriter out){
        if(!lista_lobbys.get(lobby).getLista_usuarios().get(posicion).getIniciado()){
            switch (posicion){
                case 0:
                    if(y==0){
                        lista_lobbys.get(lobby).getLista_usuarios().get(posicion).setIniciado(true);
                        clickTablero(x, y, lobby, posicion, accion, out);
                    }
                    break;
                case 1:
                    if(x==0){
                        lista_lobbys.get(lobby).getLista_usuarios().get(posicion).setIniciado(true);
                        clickTablero(x, y, lobby, posicion, accion, out);
                    }
                    break;
                case 2:
                    if(y==columas_juego-1){
                        lista_lobbys.get(lobby).getLista_usuarios().get(posicion).setIniciado(true);
                        clickTablero(x, y, lobby, posicion, accion, out);
                    }
                    break;
                case 3:
                    if(x==filas_juego-1){
                        lista_lobbys.get(lobby).getLista_usuarios().get(posicion).setIniciado(true);
                        clickTablero(x, y, lobby, posicion, accion, out);
                    }
                    break;
            }
        }else{
           // messageArea.append("Color: " + lista_lobbys.get(lobby).getLista_usuarios().get(posicion).getColor() + "\n");
        if(accion.equals("IZQ")){
            if(lista_lobbys.get(lobby).getTablero()[x][y].startsWith("Oculta:")){
                if (lista_lobbys.get(lobby).getTablero()[x][y].equals(bomba)){
                lista_lobbys.get(lobby).getLista_usuarios().get(posicion).setVivo(false);
                lista_lobbys.get(lobby).getTablero()[x][y]= "Visible:mina/" + lista_lobbys.get(lobby).getLista_usuarios().get(posicion).getColor();
                for(Usuarios usuario : lista_lobbys.get(lobby).getLista_usuarios()){
                    usuario.getEscritor().println("ACTUALIZAR=" + x + "," + y + ";" + lista_lobbys.get(lobby).getTablero()[x][y]);
                }
                out.println("MUERTO");
                }else{
                    String num= (lista_lobbys.get(lobby).getTablero()[x][y].substring(7));
                    if(esNumerico(num)){
                        if(Integer.parseInt(num)==0){
                            destaparCerosDiagonal(x, y, lobby, posicion, out);
                        }else{
                            lista_lobbys.get(lobby).getTablero()[x][y]= "Visible:" + num  + "/" +  lista_lobbys.get(lobby).getLista_usuarios().get(posicion).getColor();
                        }
                    }
                }
            }
        }else{
            //Click DER
            if(lista_lobbys.get(lobby).getTablero()[x][y].startsWith("Oculta:")){
                if(lista_lobbys.get(lobby).getLista_usuarios().get(posicion).getBanderas()>0){
                //Si es Oculto
                String valor= lista_lobbys.get(lobby).getTablero()[x][y].substring(7);
                if (valor.equals("mina")){
                    lista_lobbys.get(lobby).getTablero()[x][y]= "Visible:bandera" + lista_lobbys.get(lobby).getLista_usuarios().get(posicion).getColor() + "|" + valor;
                    lista_lobbys.get(lobby).getLista_usuarios().get(posicion).setPuntos(lista_lobbys.get(lobby).getLista_usuarios().get(posicion).getPuntos() + 1);
                }else if(esNumerico(valor)){
                    lista_lobbys.get(lobby).getTablero()[x][y]= "Visible:bandera" + lista_lobbys.get(lobby).getLista_usuarios().get(posicion).getColor() + "|" + valor;
                    lista_lobbys.get(lobby).getLista_usuarios().get(posicion).setPuntos(lista_lobbys.get(lobby).getLista_usuarios().get(posicion).getPuntos() - 1);
                }
                lista_lobbys.get(lobby).getLista_usuarios().get(posicion).setBanderas(lista_lobbys.get(lobby).getLista_usuarios().get(posicion).getBanderas() - 1);
                }
            }else{
                //Si está Visible
                String bandera= lista_lobbys.get(lobby).getTablero()[x][y].substring(8);
                if(bandera.startsWith("bandera")){
                    int jugador= Integer.parseInt(bandera.substring(7, bandera.indexOf("|")));
                    String valor= bandera.substring(bandera.indexOf("|")+1);
                    if(jugador==posicion){
                        if(valor.equals("mina")){
                            lista_lobbys.get(lobby).getTablero()[x][y]= bomba;
                            lista_lobbys.get(lobby).getLista_usuarios().get(posicion).setPuntos(lista_lobbys.get(lobby).getLista_usuarios().get(posicion).getPuntos() - 1);
                        }else if(esNumerico(valor)){
                            lista_lobbys.get(lobby).getTablero()[x][y]= "Oculta:" + valor;
                            lista_lobbys.get(lobby).getLista_usuarios().get(posicion).setPuntos(lista_lobbys.get(lobby).getLista_usuarios().get(posicion).getPuntos() + 1);
                        }
                    }
                    lista_lobbys.get(lobby).getLista_usuarios().get(posicion).setBanderas(lista_lobbys.get(lobby).getLista_usuarios().get(posicion).getBanderas() + 1);
                    }
                }
            }
        }
        actualizarInformacion(lobby);
        verificarJugadores(out, lobby);
    }
    
    public static void verificarJugadores(PrintWriter out, int lobby){
        int vivos= 0;
        for(Usuarios usuario : lista_lobbys.get(lobby).getLista_usuarios()){
            if(usuario.getVivo()){
                vivos++;
            }
        }
        if(vivos==0){
            limpiarTablero(lobby, out);
        }
        int ocultas=0;
        for (String[] row : lista_lobbys.get(lobby).getTablero()){
            for (String x : row) {
                if(x.startsWith("Oculta"))
                    ocultas++;
            }
        }
        if(ocultas==0){
            limpiarTablero(lobby, out);
        }
    }
    
    public static void finalizarPartida(PrintWriter out, int lobby){
        for(Usuarios usuario_mgs : lista_lobbys.get(lobby).getLista_usuarios()){
            if(usuario_mgs.getIniciado()){
            usuario_mgs.getEscritor().println("LIMPIAR_PUNTOS");
            for(Usuarios usuario : lista_lobbys.get(lobby).getLista_usuarios()){
                usuario_mgs.getEscritor().println("PUNTOS " + usuario.getColor() + "=" + usuario.getNombre() + "&" + usuario.getPuntos() + " Puntos");
            }
            }
        }
        for(Usuarios usuario : lista_lobbys.get(lobby).getLista_usuarios()){
            if(usuario.getIniciado()){
            usuario.getEscritor().println("FIN");
            usuario.setIniciado(false);
            }
        }
    }
            
    
    public static boolean esNumerico(String mensaje){
        boolean numerico= false;
        try {
            int num= Integer.parseInt(mensaje);
            numerico= true;
        } catch (Exception e) {
        }
        return numerico;
    }
    
    public static void destaparCerosDiagonal(int filas, int columnas, int lobby, int posicion, PrintWriter out) {
        if (lista_lobbys.get(lobby).getTablero()[filas][columnas].equals("Oculta:0")) {
            
        //Primera fila
        try{
            actualizarCeldaTablero(filas, columnas, lobby, posicion, out);
            destaparCerosDiagonal(filas - 1, columnas - 1, lobby, posicion, out);
        }catch(Exception e){}
                        
        try{
            actualizarCeldaTablero(filas, columnas, lobby, posicion, out);
            destaparCerosDiagonal(filas, columnas - 1, lobby, posicion, out);
        }catch(Exception e){}
                        
        try{
            actualizarCeldaTablero(filas, columnas, lobby, posicion, out);
            destaparCerosDiagonal(filas + 1, columnas - 1, lobby, posicion, out);
        }catch(Exception e){}
                        
        //Segunda fila
        try{
            actualizarCeldaTablero(filas, columnas, lobby, posicion, out);
            destaparCerosDiagonal(filas - 1, columnas, lobby, posicion, out);
        }catch(Exception e){}
                        
        try{
            actualizarCeldaTablero(filas, columnas, lobby, posicion, out);
            destaparCerosDiagonal(filas + 1, columnas, lobby, posicion, out);
        }catch(Exception e){}
                        
        //Tercera fila
        try{
            actualizarCeldaTablero(filas, columnas, lobby, posicion, out);
            destaparCerosDiagonal(filas - 1, columnas + 1, lobby, posicion, out);
        }catch(Exception e){}
        
        try{
            actualizarCeldaTablero(filas, columnas, lobby, posicion, out);
            destaparCerosDiagonal(filas, columnas + 1, lobby, posicion, out);
        }catch(Exception e){}
        
        try{
            actualizarCeldaTablero(filas, columnas, lobby, posicion, out);
            destaparCerosDiagonal(filas + 1, columnas + 1, lobby, posicion, out);
        }catch(Exception e){}
        
        }else{
                actualizarCeldaTablero(filas, columnas, lobby, posicion, out);
            }
        }
    
    public static void actualizarCeldaTablero(int f, int c, int lobby, int posicion, PrintWriter out) {
        if(lista_lobbys.get(lobby).getTablero()[f][c].startsWith("Oculta")){
        if (esNumerico(lista_lobbys.get(lobby).getTablero()[f][c].substring(7))) {
            lista_lobbys.get(lobby).getTablero()[f][c]= "Visible:" + lista_lobbys.get(lobby).getTablero()[f][c].substring(7) + "/" +  lista_lobbys.get(lobby).getLista_usuarios().get(posicion).getColor();
            for(Usuarios usuario : lista_lobbys.get(lobby).getLista_usuarios()){
                usuario.getEscritor().println("ACTUALIZAR=" + f + "," + c + ";" + lista_lobbys.get(lobby).getTablero()[f][c]);
            }
        }
        }
    }
    
    public static void limpiarTablero(int lobby, PrintWriter out){
        for (int x=0; x < filas_juego; x++){
            for (int y=0; y < columas_juego; y++){
              if(lista_lobbys.get(lobby).getTablero()[x][y].startsWith("Oculta")){
                  String contenido= lista_lobbys.get(lobby).getTablero()[x][y].substring(7);
                  lista_lobbys.get(lobby).getTablero()[x][y]= "Visible:" + contenido;
                  for(Usuarios usuario : lista_lobbys.get(lobby).getLista_usuarios()){
                      usuario.getEscritor().println("ACTUALIZAR=" + x + "," + y + ";" + lista_lobbys.get(lobby).getTablero()[x][y]);
                  }
              }
            }
        }
        finalizarPartida(out, lobby);
    }

    /*public static void destaparCeros(int filas, int columnas, int lobby, PrintWriter out) {
        imprimirarreglo(lista_lobbys.get(lobby).getTablero());
        if (!(filas < 0) && !(columnas + 1 > columas_juego) && !(filas + 1 > filas_juego) && !(columnas < 0)) {
            if (lista_lobbys.get(lobby).getTablero()[filas][columnas].equals("Oculta:0")) {
                
                if (!(filas < 0)) {
                    actualizarCelda(filas, columnas, lobby, out);
                    destaparCeros(filas - 1, columnas, lobby, out);
                }
                if (!(columnas + 1 > columas_juego)) {
                    actualizarCelda(filas, columnas, lobby, out);
                    destaparCeros(filas, columnas + 1, lobby, out);
                }
                if (!(filas + 1 > filas_juego)) {
                    actualizarCelda(filas, columnas, lobby, out);
                    destaparCeros(filas + 1, columnas, lobby, out);
                }
                if (!(columnas < 0)) {
                    actualizarCelda(filas, columnas, lobby, out);
                    destaparCeros(filas, columnas - 1, lobby, out);
                }
            }else{
                actualizarCelda(filas, columnas, lobby, out);
            }
        }
    }*/
    
    public static int buscarUsuario(String Nombre, int lobby){
        int indice=-1;
        for(int x=0; x<lista_lobbys.get(lobby).getLista_usuarios().size(); x++){
            if(lista_lobbys.get(lobby).getLista_usuarios().get(x).getNombre().equals(Nombre)){
                indice= x;
                break;
            }
        }
        return indice;
    }
    
    public static int buscarLobby(String id){
        int indice= -1;
        for(int x=0; x<lista_lobbys.size(); x++){
            if(lista_lobbys.get(x).getID_Unico().equals(id)){
                indice= x;
                break;
            }
        }
        return indice;
    }
    
    public static void limpiarBanderas(int lobby, int usuario){
        int color= lista_lobbys.get(lobby).getLista_usuarios().get(usuario).getColor();
        for (int x=0; x < filas_juego; x++){
            for (int y=0; y < columas_juego; y++){
              if(lista_lobbys.get(lobby).getTablero()[x][y].startsWith("Visible:bandera" + color)){
                  lista_lobbys.get(lobby).getTablero()[x][y]= "Oculta:" + lista_lobbys.get(lobby).getTablero()[x][y].substring(lista_lobbys.get(lobby).getTablero()[x][y].indexOf("|"));
                  int admin=0;
                  for(Usuarios usuarios : lista_lobbys.get(lobby).getLista_usuarios()){
                      usuarios.getEscritor().println("ACTUALIZAR=" + x + "," + y + ";" + lista_lobbys.get(lobby).getTablero()[x][y]);
                      if(admin==0){
                          usuarios.getEscritor().println("ADMIN");
                          admin++;
                      }else{
                          usuarios.getEscritor().println("NOADMIN");
                      }
                  }
              }
            }
        }
    }
    
}
