package aw.transporte.model;

public class Usuario {
    private String username;
    private String password;
    private String correo;

    public Usuario(String username, String password, String correo) {
        this.username = username;
        this.password = password;
        this.correo = correo;
    }

    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getCorreo() { return correo; }
}