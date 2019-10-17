import java.io.PrintWriter;
public class Usuarios {
    private String Nombre;
    private PrintWriter Escritor;
    private int puntos;
    private boolean Vivo;
    private int Banderas;
    private boolean Iniciado;
    private int Color;

    public Usuarios(String Nombre, PrintWriter Escritor, int puntos, boolean Vivo, int Banderas, boolean Iniciado, int Color) {
        this.Nombre = Nombre;
        this.Escritor = Escritor;
        this.puntos = puntos;
        this.Vivo = Vivo;
        this.Banderas = Banderas;
        this.Iniciado = Iniciado;
        this.Color = Color;
    }

    public int getColor() {
        return Color;
    }

    public void setColor(int Color) {
        this.Color = Color;
    }


    public boolean getIniciado() {
        return Iniciado;
    }

    public void setIniciado(boolean Iniciado) {
        this.Iniciado = Iniciado;
    }

    public int getBanderas() {
        return Banderas;
    }

    public void setBanderas(int Banderas) {
        this.Banderas = Banderas;
    }

    
    public int getPuntos() {
        return puntos;
    }

    public void setPuntos(int puntos) {
        this.puntos = puntos;
    }

    public Usuarios() {
    }

    public String getNombre() {
        return Nombre;
    }

    public void setNombre(String Nombre) {
        this.Nombre = Nombre;
    }

    public PrintWriter getEscritor() {
        return Escritor;
    }

    public void setEscritor(PrintWriter Escritor) {
        this.Escritor = Escritor;
    }

    public boolean getVivo() {
        return Vivo;
    }

    public void setVivo(boolean Vivo) {
        this.Vivo = Vivo;
    }
}