import java.util.List;
public class Lobbys {
    private List<Usuarios> lista_usuarios;
    private String[][] tablero;
    private String ID_Unico;

    public Lobbys() {
    }

    public Lobbys(List<Usuarios> lista_usuarios, String[][] tablero, String uid) {
        this.lista_usuarios = lista_usuarios;
        this.tablero = tablero;
        this.ID_Unico = uid;
    }

    public List<Usuarios> getLista_usuarios() {
        return lista_usuarios;
    }

    public void setLista_usuarios(List<Usuarios> lista_usuarios) {
        this.lista_usuarios = lista_usuarios;
    }

    public String[][] getTablero() {
        return tablero;
    }

    public void setTablero(String[][] tablero) {
        this.tablero = tablero;
    }

    public String getID_Unico() {
        return ID_Unico;
    }

    public void setID_Unico(String ID_Unico) {
        this.ID_Unico = ID_Unico;
    }
}
