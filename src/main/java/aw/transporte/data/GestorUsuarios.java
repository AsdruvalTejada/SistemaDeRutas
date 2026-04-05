package aw.transporte.data;

import aw.transporte.model.Usuario;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Clase: GestorUsuarios
 * Objetivo: Manejar la persistencia de datos (guardado y carga) de los perfiles
 * administrativos del sistema utilizando un archivo JSON independiente.
 */
public class GestorUsuarios {
    private static final String RUTA_ARCHIVO = "db_usuarios.json";
    private Gson gson = new GsonBuilder().setPrettyPrinting().create();

    /**
     * Función: cargarUsuarios
     * Objetivo: Leer el archivo de base de datos de usuarios y reconstruir la lista en memoria.
     * Si el archivo no existe, crea e inyecta un superusuario "admin" por defecto para evitar
     * bloqueos de acceso en la primera ejecución.
     * @return (List<Usuario>) Lista de perfiles de administrador registrados.
     */
    public List<Usuario> cargarUsuarios() {
        try (Reader reader = new FileReader(RUTA_ARCHIVO)) {
            Type listType = new TypeToken<ArrayList<Usuario>>(){}.getType();
            List<Usuario> usuarios = gson.fromJson(reader, listType);
            return usuarios != null ? usuarios : new ArrayList<>();
        } catch (FileNotFoundException e) {
            // Si no existe, creamos un admin por defecto
            List<Usuario> porDefecto = new ArrayList<>();
            porDefecto.add(new Usuario("admin", "1234", "tu_correo@gmail.com"));
            guardarUsuarios(porDefecto);
            return porDefecto;
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Función: guardarUsuarios
     * Objetivo: Serializar la lista actual de administradores y sobreescribir el archivo JSON.
     * @param usuarios (List<Usuario>) La lista actualizada de perfiles a persistir en disco.
     */
    public void guardarUsuarios(List<Usuario> usuarios) {
        try (Writer writer = new FileWriter(RUTA_ARCHIVO)) {
            gson.toJson(usuarios, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}