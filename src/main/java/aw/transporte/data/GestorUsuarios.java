package aw.transporte.data;

import aw.transporte.model.Usuario;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class GestorUsuarios {
    private static final String RUTA_ARCHIVO = "db_usuarios.json";
    private Gson gson = new GsonBuilder().setPrettyPrinting().create();

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

    public void guardarUsuarios(List<Usuario> usuarios) {
        try (Writer writer = new FileWriter(RUTA_ARCHIVO)) {
            gson.toJson(usuarios, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}